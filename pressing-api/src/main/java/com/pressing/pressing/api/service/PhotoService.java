package com.pressing.pressing.api.service;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.pressing.pressing.api.common.exception.RessourceNotFoundException;
import com.pressing.pressing.api.dto.response.PhotoDTO;
import com.pressing.pressing.api.entite.LigneCommande;
import com.pressing.pressing.api.entite.Photo;
import com.pressing.pressing.api.repository.LigneCommandeRepository;
import com.pressing.pressing.api.repository.PhotoRepository;
import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;




@Service
@RequiredArgsConstructor
public class PhotoService {

    private static final int TAILLE_MAX = 1600;
    private static final float QUALITE_JPG = 0.75f;
    // Marque les photos stockees sur Cloudinary : url = "cloudinary:<public_id>".
    private static final String PREFIXE_CLOUDINARY = "cloudinary:";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PhotoService.class);

    @Value("${photo.upload.dir}")
    private String uploadDir;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String cloudApiKey;

    @Value("${cloudinary.api-secret:}")
    private String cloudApiSecret;

    // Non nul uniquement si les identifiants Cloudinary sont fournis.
    private Cloudinary cloudinary;

    private final PhotoRepository photoRepository;
    private final LigneCommandeRepository ligneCommandeRepository;

    @PostConstruct
    void initStockage() {
        if (estRenseigne(cloudName) && estRenseigne(cloudApiKey) && estRenseigne(cloudApiSecret)) {
            // trim() : un espace colle dans la variable d'env casserait l'URL Cloudinary
            // ("Illegal character in path").
            cloudName = cloudName.trim();
            cloudApiKey = cloudApiKey.trim();
            cloudApiSecret = cloudApiSecret.trim();
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", cloudApiKey,
                    "api_secret", cloudApiSecret,
                    "secure", true));
            log.info("Stockage des photos : Cloudinary (cloud_name={})", cloudName);
        } else {
            log.info("Stockage des photos : disque local ({}) — non persistant. "
                    + "Renseignez CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET pour la persistance.", uploadDir);
        }
    }

    private static boolean estRenseigne(String valeur) {
        return valeur != null && !valeur.isBlank();
    }

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
        String stockage = enregistrer(jpeg, dossier);

        Photo photo = Photo.builder()
                .url(stockage)
                .typephoto(typePhoto)
                .dateTime(LocalDateTime.now())
                .build();
        ligne.addPhoto(photo);
        photoRepository.save(photo);
        return toDTO(photo);
    }

    // Envoie l'image sur Cloudinary si configuré, sinon l'écrit sur le disque local.
    // Renvoie la référence stockée dans Photo.url.
    private String enregistrer(byte[] jpeg, String dossier) {
        if (cloudinary != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultat = cloudinary.uploader().upload(jpeg, ObjectUtils.asMap(
                        "folder", dossier,
                        "resource_type", "image",
                        "overwrite", false));
                String publicId = (String) resultat.get("public_id");
                if (!estRenseigne(publicId)) {
                    throw new IllegalStateException("Réponse Cloudinary inattendue (public_id absent)");
                }
                return PREFIXE_CLOUDINARY + publicId;
            } catch (IOException e) {
                throw new IllegalStateException("Échec de l'envoi de la photo vers Cloudinary : " + e.getMessage(), e);
            }
        }

        String nomFichier = UUID.randomUUID().toString().replace("-", "") + ".jpg";
        String cheminRelatif = dossier + "/" + nomFichier;
        try {
            Files.createDirectories(Path.of(uploadDir, dossier));
            Files.write(Path.of(uploadDir, cheminRelatif), jpeg);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'enregistrer la photo : " + e.getMessage(), e);
        }
        return cheminRelatif;
    }

    @Transactional
    public void supprimer(Long idphoto) {
        Photo photo = photoRepository.findById(idphoto)
                .orElseThrow(() -> new RessourceNotFoundException("Photo introuvable avec l'id : " + idphoto));
        String url = photo.getUrl();
        if (estReferenceCloudinary(url)) {
            if (cloudinary != null) {
                try {
                    cloudinary.uploader().destroy(url.substring(PREFIXE_CLOUDINARY.length()), ObjectUtils.emptyMap());
                } catch (IOException e) {
                    log.warn("Photo Cloudinary non supprimée : {}", url, e);
                }
            }
        } else {
            try {
                Files.deleteIfExists(Path.of(uploadDir, url));
            } catch (IOException e) {
                log.warn("Fichier photo non supprimé sur le disque : {}", url, e);
            }
        }
        photoRepository.delete(photo);
    }

    public Resource getFichier(Long idphoto) {
        Photo photo = photoRepository.findById(idphoto)
                .orElseThrow(() -> new RessourceNotFoundException("Photo introuvable avec l'id : " + idphoto));
        String url = photo.getUrl();

        if (estReferenceCloudinary(url)) {
            String publicId = url.substring(PREFIXE_CLOUDINARY.length());
            String urlLivraison = cloudinary.url().secure(true).generate(publicId);
            try {
                byte[] octets = telecharger(urlLivraison);
                return new PhotoResource(octets, "photo.jpg");
            } catch (IOException e) {
                throw new IllegalStateException("Impossible de récupérer la photo depuis Cloudinary : " + e.getMessage(), e);
            }
        }

        Path chemin = Path.of(uploadDir, url);
        if (!Files.exists(chemin)) {
            throw new RessourceNotFoundException("Fichier photo introuvable sur le disque");
        }
        return new FileSystemResource(chemin);
    }

    private static boolean estReferenceCloudinary(String url) {
        return url != null && url.startsWith(PREFIXE_CLOUDINARY);
    }

    private static byte[] telecharger(String url) throws IOException {
        HttpRequest requete = HttpRequest.newBuilder(URI.create(url)).GET().build();
        try {
            HttpResponse<byte[]> reponse = HTTP.send(requete, HttpResponse.BodyHandlers.ofByteArray());
            if (reponse.statusCode() >= 400) {
                throw new IOException("Cloudinary a répondu HTTP " + reponse.statusCode());
            }
            return reponse.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Téléchargement de la photo interrompu", e);
        }
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

    // Resource en mémoire avec un nom de fichier (pour déduire le Content-Type côté contrôleur).
    private static final class PhotoResource extends ByteArrayResource {
        private final String nomFichier;

        PhotoResource(byte[] contenu, String nomFichier) {
            super(contenu);
            this.nomFichier = nomFichier;
        }

        @Override
        public String getFilename() {
            return nomFichier;
        }
    }
}
