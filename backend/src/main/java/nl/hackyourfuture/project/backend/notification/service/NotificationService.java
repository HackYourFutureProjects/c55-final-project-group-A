package nl.hackyourfuture.project.backend.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.hackyourfuture.project.backend.notification.dto.response.NotificationPageResponse;
import nl.hackyourfuture.project.backend.notification.dto.response.NotificationResponse;
import nl.hackyourfuture.project.backend.notification.repository.NotificationRepository;
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

    @Transactional(readOnly = true)
    public NotificationPageResponse getNotificationPage(
            UUID userId,
            int page,
            int size,
            boolean unreadOnly
    ) {
        userService.getUserOrThrow(userId);

        log.debug(
                "Fetching notification page for user {}, page={}, size={}, unreadOnly={}",
                userId,
                page,
                size,
                unreadOnly
        );

        int offset = page * size;

        List<NotificationResponse> notifications =
                notificationRepository.findNotificationsByUserId(
                                userId,
                                size,
                                offset,
                                unreadOnly
                        )
                        .stream()
                        .map(NotificationResponse::from)
                        .toList();

        long totalElements = notificationRepository.countNotificationsByUserId(
                userId,
                unreadOnly
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
}
