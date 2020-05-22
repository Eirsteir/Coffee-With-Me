package com.eirsteir.coffeewithme.web.request;

import com.eirsteir.coffeewithme.notificationapi.domain.NotificationType;
import lombok.Getter;

@Getter
public class NotificationRequest {

    private Long subjectId;
    private NotificationType type;
    private Long userId;
    private String name;
    private String username;

}
