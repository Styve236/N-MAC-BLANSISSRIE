package com.pressing.pressing.api.photo;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLConnection;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    @GetMapping("/lignes/{idligne}/photos")
    public List<PhotoDTO> lister(@PathVariable Long idligne) {
        return photoService.listerParLigne(idligne);
    }

    @PostMapping("/lignes/{idligne}/photos")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','AGENT_PRODUCTION')")
    public PhotoDTO ajouter(@PathVariable Long idligne,
                            @RequestParam("file") MultipartFile fichier,
                            @RequestParam(required = false) String type) {
        return photoService.ajouter(idligne, fichier, type);
    }

    @DeleteMapping("/photos/{idphoto}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONNISTE','AGENT_PRODUCTION')")
    public void supprimer(@PathVariable Long idphoto) {
        photoService.supprimer(idphoto);
    }

    @GetMapping("/photos/{idphoto}/fichier")
    public ResponseEntity<Resource> fichier(@PathVariable Long idphoto) {
        Resource resource = photoService.getFichier(idphoto);
        String type = URLConnection.guessContentTypeFromName(resource.getFilename());
        MediaType mediaType = type != null ? MediaType.parseMediaType(type) : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header("Cache-Control", "public, max-age=3600")
                .body(resource);
    }
}