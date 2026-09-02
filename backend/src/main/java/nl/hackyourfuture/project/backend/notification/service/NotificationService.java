package nl.hackyourfuture.project.backend.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.hackyourfuture.project.backend.notification.NotificationNotFoundException;
import nl.hackyourfuture.project.backend.notification.dto.response.NotificationPageResponse;
import nl.hackyourfuture.project.backend.notification.dto.response.NotificationResponse;
import nl.hackyourfuture.project.backend.notification.dto.response.NotificationUnreadCountResponse;
import nl.hackyourfuture.project.backend.notification.model.Notification;
import nl.hackyourfuture.project.backend.notification.repository.NotificationRepository;
import nl.hackyourfuture.project.backend.user.Role;
import nl.hackyourfuture.project.backend.user.User;
import nl.hackyourfuture.project.backend.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    private static boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse getNotificationPage(
            UUID userId,
            int page,
            int size,
            boolean unreadOnly
    ) {
        User user = userService.getUserOrThrow(userId);
        boolean includeNewFeedback = isAdmin(user);

        log.debug(
                "Fetching notification page for user {}, page={}, size={}, unreadOnly={}",
                userId,
                page,
                size,
                unreadOnly
        );

        long offset = (long) page * size;

        List<NotificationResponse> notifications =
                notificationRepository.findNotificationsByUserId(
                                userId,
                                size,
                                offset,
                                unreadOnly,
                                includeNewFeedback
                        )
                        .stream()
                        .map(NotificationResponse::from)
                        .toList();

        long totalElements = notificationRepository.countNotificationsByUserId(
                userId,
                unreadOnly,
                includeNewFeedback
        );
        int totalPages = (int) Math.ceil(
                (double) totalElements / size
        );
        boolean hasNext = page + 1 < totalPages;

        log.debug(
                "Returning {} notifications for user {} (totalElements={})",
                notifications.size(),
                userId,
                totalElements
        );

        return new NotificationPageResponse(
                notifications,
                page,
                size,
                totalElements,
                totalPages,
                hasNext
        );
    }

    @Transactional(readOnly = true)
    public NotificationUnreadCountResponse getUnreadCount(UUID userId) {
        User user = userService.getUserOrThrow(userId);

        long count = notificationRepository.countNotificationsByUserId(
                userId,
                true,
                isAdmin(user)
        );

        return new NotificationUnreadCountResponse(count);
    }

    @Transactional
    public NotificationResponse openNotification(UUID userId, UUID notificationId) {
        User user = userService.getUserOrThrow(userId);
        boolean includeNewFeedback = isAdmin(user);

        log.debug(
                "Opening notification {} for user {}",
                notificationId,
                userId
        );

        Notification notification = notificationRepository
                .markAsRead(notificationId, userId, includeNewFeedback)
                .or(() -> notificationRepository.findNotificationByIdAndUserId(
                        notificationId,
                        userId,
                        includeNewFeedback
                ))
                .orElseThrow(() -> new NotificationNotFoundException(
                        "Notification not found: " + notificationId
                ));

        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        User user = userService.getUserOrThrow(userId);

        int markedCount = notificationRepository.markAllAsRead(
                userId,
                isAdmin(user)
        );

        log.debug(
                "Marked {} notifications as read for user {}",
                markedCount,
                userId
        );
    }
}
