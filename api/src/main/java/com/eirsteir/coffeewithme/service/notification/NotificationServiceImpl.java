package com.eirsteir.coffeewithme.service.notification;

import com.eirsteir.coffeewithme.domain.notification.Notification;
import com.eirsteir.coffeewithme.domain.notification.NotificationType;
import com.eirsteir.coffeewithme.domain.user.User;
import com.eirsteir.coffeewithme.dto.NotificationDto;
import com.eirsteir.coffeewithme.exception.EntityType;
import com.eirsteir.coffeewithme.repository.NotificationRepository;
import com.eirsteir.coffeewithme.service.user.UserService;
import com.eirsteir.coffeewithme.util.MessageTemplateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private SimpMessagingTemplate template;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public void notify(Long toUserId, Long fromUserId, NotificationType type) {
        User toUser = userService.findUserById(toUserId);
        User fromUser = userService.findUserById(fromUserId);
        Notification notificationToSend = registerNotification(toUser, fromUser, type);

        log.debug("[x] Sending notification to user with id - {}: {}", toUserId, notificationToSend);
        sendToUser(notificationToSend);
    }

    private void sendToUser(Notification notification) {
        template.convertAndSendToUser(
                notification.getTo().getId().toString(),
                "/queue/notifications",
                notification
        );
    }

    private Notification registerNotification(User toUser, User fromUser, NotificationType type) {
        String message = getMessage(fromUser, toUser, type);

        Notification notification = Notification.builder()
                .message(message)
                .to(toUser)
                .build();

        log.debug("[x] Registering notification: {}", notification);
        return notificationRepository.save(notification);
    }

    private String getMessage(User from, User to, NotificationType type) {
        String messageTemplate = MessageTemplateUtil.getMessageTemplate(EntityType.FRIENDSHIP, type);

        if (type == NotificationType.ACCEPTED)
            return MessageTemplateUtil.format(messageTemplate, from.getName());

        return MessageTemplateUtil.format(messageTemplate, to.getName());
    }

    @Override
    public NotificationDto updateNotification(NotificationDto notificationDto) {
        return null;
    }

    @Override
    public NotificationDto findByUserId(Long userId) {
        return null;
    }

    @Override
    public NotificationDto findByUserIdAndId(Long userId, Long id) {
        return null;
    }

}