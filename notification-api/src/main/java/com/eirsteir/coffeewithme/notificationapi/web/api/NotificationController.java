package com.eirsteir.coffeewithme.notificationapi.web.api;


import com.eirsteir.coffeewithme.commons.security.UserDetailsImpl;
import com.eirsteir.coffeewithme.notificationapi.domain.Notification;
import com.eirsteir.coffeewithme.notificationapi.dto.NotificationDto;
import com.eirsteir.coffeewithme.notificationapi.service.NotificationService;
import com.eirsteir.coffeewithme.notificationapi.web.request.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void notify(@RequestBody NotificationRequest notificationRequest,
                @AuthenticationPrincipal UserDetailsImpl principal) {
        log.debug("[x] Received request from user: {}\n{}", notificationRequest, principal);

        Notification.UserDetails userDetails = Notification.UserDetails.builder()
                .id(principal.getId())
                .name(notificationRequest.getName())
                .username(principal.getUsername())
                .build();

        service.notify(notificationRequest.getSubjectId(), userDetails, notificationRequest.getType());
    }

    @GetMapping("/users/{id}")
    List<NotificationDto> getNotifications(@PathVariable Long id, Pageable pageable,
                                           @AuthenticationPrincipal UserDetailsImpl principal) {
        log.debug("[x] Received request with principal: {}", principal);

        List<NotificationDto> notifications = service.findAllByUserId(id, pageable);

        if (notifications.isEmpty())
            throw new ResponseStatusException(
                    HttpStatus.NO_CONTENT, "User with id " + id + " has no notifications");

        return notifications;
    }

    @PutMapping("/users/{id}")
    NotificationDto updateNotificationToRead(@PathVariable Long id,
                                             @RequestBody NotificationDto notificationDto,
                                             @AuthenticationPrincipal UserDetailsImpl principal) {
        log.debug("[x] Received request with principal: {}", principal);

        validateNotificationUpdate(notificationDto, principal.getId());

        return service.updateNotificationToRead(notificationDto);
    }

    private void validateNotificationUpdate(NotificationDto notificationDto, Long userId) {
        if (notificationDto.getUser().getId().equals(userId))
            return;

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Notification does not belong to current user");
    }
}