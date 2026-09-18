package com.pressing.pressing.api.service;
import com.pressing.pressing.api.dto.response.NotificationDTO;
import com.pressing.pressing.api.entite.Commande;
import com.pressing.pressing.api.entite.Notification;
import com.pressing.pressing.api.entite.NotificationLecture;
import com.pressing.pressing.api.entite.Role;
import com.pressing.pressing.api.entite.TypeNotificationInterne;
import com.pressing.pressing.api.entite.Users;
import com.pressing.pressing.api.repository.NotificationLectureRepository;
import com.pressing.pressing.api.repository.NotificationRepository;
import com.pressing.pressing.api.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationLectureRepository lectureRepository;
    private final UserRepository userRepository;

    // L'agent de production reçoit une notification interne (message personnalisable).
    @Transactional
    public void notifierNouvelleCommande(Commande commande, String messagePersonnalise) {
        List<Users> agents = userRepository.findByRoleAndActifTrue(Role.AGENT_PRODUCTION);
        String message = (messagePersonnalise == null || messagePersonnalise.isBlank())
                ? "Nouvelle commande " + commande.getNumeroTicket()
                        + " enregistrée. Veuillez venir la chercher pour prendre en charge le traitement."
                : messagePersonnalise.trim();

        Notification notification = Notification.builder()
                .message(message)
                .commandeId(commande.getIdcommande())
                .numeroTicket(commande.getNumeroTicket())
                .type(TypeNotificationInterne.NOUVELLE_COMMANDE)
                .roleCible(Role.AGENT_PRODUCTION)
                .build();

        for (Users agent : agents) {
            NotificationLecture lecture = NotificationLecture.builder()
                    .notification(notification)
                    .utilisateurId(agent.getIdusers())
                    .build();
            notification.getLectures().add(lecture);
        }

        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> mesNotifications(Long utilisateurId, Role role) {
        List<Notification> notifs = notificationRepository.findByRoleCibleOrderByDateCreationDesc(role);
        List<NotificationDTO> dtos = new ArrayList<>();
        for (Notification n : notifs) {
            NotificationLecture lecture = lectureRepository
                    .findByNotificationIdAndUtilisateurId(n.getId(), utilisateurId)
                    .orElseGet(() -> lectureRepository.save(NotificationLecture.builder()
                            .notification(n)
                            .utilisateurId(utilisateurId)
                            .lue(false)
                            .build()));
            dtos.add(toDTO(n, lecture.isLue()));
        }
        return dtos;
    }

    @Transactional
    public int compterNonLues(Long utilisateurId, Role role) {
        List<Notification> notifs = notificationRepository.findByRoleCibleOrderByDateCreationDesc(role);
        int nonLues = 0;
        for (Notification n : notifs) {
            NotificationLecture lecture = lectureRepository
                    .findByNotificationIdAndUtilisateurId(n.getId(), utilisateurId)
                    .orElseGet(() -> lectureRepository.save(NotificationLecture.builder()
                            .notification(n)
                            .utilisateurId(utilisateurId)
                            .lue(false)
                            .build()));
            if (!lecture.isLue()) {
                nonLues++;
            }
        }
        return nonLues;
    }

    @Transactional
    public void marquerLue(Long notificationId, Long utilisateurId) {
        lectureRepository.findByNotificationIdAndUtilisateurId(notificationId, utilisateurId)
                .filter(l -> !l.isLue())
                .ifPresent(l -> l.setLue(true));
    }

    @Transactional
    public void toutLue(Long utilisateurId) {
        lectureRepository.marquerToutesLues(utilisateurId);
    }

    private NotificationDTO toDTO(Notification n, boolean lue) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(n.getId());
        dto.setMessage(n.getMessage());
        dto.setCommandeId(n.getCommandeId());
        dto.setNumeroTicket(n.getNumeroTicket());
        dto.setType(n.getType());
        dto.setDateCreation(n.getDateCreation());
        dto.setLue(lue);
        return dto;
    }
}