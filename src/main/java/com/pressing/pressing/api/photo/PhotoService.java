package com.pressing.pressing.api.photo;

import com.pressing.pressing.api.commande.LigneCommande;
import com.pressing.pressing.api.commande.LigneCommandeRepository;
import com.pressing.pressing.api.common.exception.RessourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoService {

    private static final int TAILLE_MAX = 1600;
    private static final float QUALITE_JPG = 0.75f;

    @Value("${photo.upload.dir}")
    private String uploadDir;

    private final PhotoRepository photoRepository;
    private final LigneCommandeRepository ligneCommandeRepository;

    @Transactional(readOnly = true)
    public List<PhotoDTO> listerParLigne(Long idligne) {
        LigneCommande ligne = ligneCommandeRepository.findById(idligne)
                .orElseThrow(() -> new RessourceNotFoundException("Ligne de commande introuvable avec l'id : " + idligne));
        return ligne.getPhotos().stream().map(this::toDTO).toList();
    }

    @Transactional
    public PhotoDTO ajouter(Long idligne, MultipartFile fichier, String typePhoto) {
        LigneCommande ligne = ligneCommandeRepository.findById(idligne)
                .orElseThrow(() -> new RessourceNotFoundException("Ligne de commande introuvable avec l'id : " + idligne));

        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalArgumentException("Le fichier photo est obligatoire");
        }
        if (fichier.getContentType() == null || !fichier.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Le fichier doit être une image (jpg, png, ...)");
        }

        // Compression : redimensionnement + encodage JPEG pour limiter la place en mémoire / sur disque
        BufferedImage image;
        try (InputStream in = fichier.getInputStream()) {
            image = ImageIO.read(in);
        } catch (IOException e) {
            throw new IllegalArgumentException("Impossible de lire l'image : " + e.getMessage(), e);
        }
        if (image == null) {
            throw new IllegalArgumentException("Image illisible ou format non supporté");
        }
        image = redimensionnerSiNecessaire(image);
        byte[] jpeg = encodageJpeg(image);
        if (jpeg == null) {
            throw new IllegalStateException("Compression de l'image impossible");
        }

        String dossier = "commande_" + ligne.getCommande().getIdcommande() + "/ligne_" + ligne.getIdligne();
        String nomFichier = UUID.randomUUID().toString().replace("-", "") + ".jpg";
        String cheminRelatif = dossier + "/" + nomFichier;

        try {
            Files.createDirectories(Path.of(uploadDir, dossier));
            Files.write(Path.of(uploadDir, cheminRelatif), jpeg);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'enregistrer la photo : " + e.getMessage(), e);
        }

        Photo photo = Photo.builder()
                .url(cheminRelatif)
                .typephoto(typePhoto)
                .dateTime(LocalDateTime.now())
                .build();
        ligne.addPhoto(photo);
        photoRepository.save(photo);
        return toDTO(photo);
    }

    @Transactional
    public void supprimer(Long idphoto) {
        Photo photo = photoRepository.findById(idphoto)
                .orElseThrow(() -> new RessourceNotFoundException("Photo introuvable avec l'id : " + idphoto));
        try {
            Files.deleteIfExists(Path.of(uploadDir, photo.getUrl()));
        } catch (IOException ignore) {
        }
        photoRepository.delete(photo);
    }

    public Resource getFichier(Long idphoto) {
        Photo photo = photoRepository.findById(idphoto)
                .orElseThrow(() -> new RessourceNotFoundException("Photo introuvable avec l'id : " + idphoto));
        Path chemin = Path.of(uploadDir, photo.getUrl());
        if (!Files.exists(chemin)) {
            throw new RessourceNotFoundException("Fichier photo introuvable sur le disque");
        }
        return new FileSystemResource(chemin);
    }

    private BufferedImage redimensionnerSiNecessaire(BufferedImage image) {
        int largeur = image.getWidth();
        int hauteur = image.getHeight();
        int plusGrandCote = Math.max(largeur, hauteur);
        if (plusGrandCote <= TAILLE_MAX) {
            return image;
        }
        double ratio = (double) TAILLE_MAX / plusGrandCote;
        int nouvelleLargeur = Math.max(1, (int) Math.round(largeur * ratio));
        int nouvelleHauteur = Math.max(1, (int) Math.round(hauteur * ratio));
        BufferedImage reduite = new BufferedImage(nouvelleLargeur, nouvelleHauteur, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = reduite.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, nouvelleLargeur, nouvelleHauteur, null);
        g.dispose();
        return reduite;
    }

    private byte[] encodageJpeg(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BufferedImage rgb = image;
            if (image.getColorModel().hasAlpha()) {
                rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgb.createGraphics();
                g.setBackground(Color.WHITE);
                g.clearRect(0, 0, rgb.getWidth(), rgb.getHeight());
                g.drawImage(image, 0, 0, null);
                g.dispose();
            }
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                ImageWriteParam params = writer.getDefaultWriteParam();
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(QUALITE_JPG);
                writer.write(null, new IIOImage(rgb, null, null), params);
            } finally {
                writer.dispose();
            }
            return out.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private PhotoDTO toDTO(Photo photo) {
        PhotoDTO dto = new PhotoDTO();
        dto.setIdphoto(photo.getIdphoto());
        dto.setLigneId(photo.getLigneCommande() != null ? photo.getLigneCommande().getIdligne() : null);
        dto.setUrl("/api/photos/" + photo.getIdphoto() + "/fichier");
        dto.setTypephoto(photo.getTypephoto());
        dto.setDateTime(photo.getDateTime());
        return dto;
    }
}