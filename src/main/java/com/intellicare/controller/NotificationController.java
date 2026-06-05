package com.intellicare.controller;

import com.intellicare.dto.request.NotificationRequest;
import com.intellicare.dto.response.ApiResponse;
import com.intellicare.dto.response.NotificationResponse;
import com.intellicare.entity.User;
import com.intellicare.service.NotificationService;
import com.intellicare.util.RequestUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications", description = "Send and manage notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final RequestUtil requestUtil;

    @GetMapping
    @Operation(summary = "Get my notifications")
    public ResponseEntity<ApiResponse<ApiResponse.PagedData<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<NotificationResponse> page = notificationService.getNotifications(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(ApiResponse.PagedData.of(page)));
    }

    @GetMapping("/unread")
    @Operation(summary = "Get my unread notifications")
    public ResponseEntity<ApiResponse<ApiResponse.PagedData<NotificationResponse>>> getUnread(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<NotificationResponse> page = notificationService.getUnread(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(ApiResponse.PagedData.of(page)));
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Count unread notifications")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countUnread(@AuthenticationPrincipal User user) {
        long count = notificationService.countUnread(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("unreadCount", count)));
    }

    @GetMapping("/filter")
    @Operation(summary = "Filter notifications")
    public ResponseEntity<ApiResponse<ApiResponse.PagedData<NotificationResponse>>> filter(
            @AuthenticationPrincipal User user,
            NotificationRequest.Filter filter,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<NotificationResponse> page = notificationService.filter(user.getId(), filter, pageable);
        return ResponseEntity.ok(ApiResponse.ok(ApiResponse.PagedData.of(page)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(ApiResponse.ok(notificationService.markAsRead(id, user.getId())));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(@AuthenticationPrincipal User user) {
        int updated = notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("updated", updated)));
    }

    // ===== Admin / System =====

    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Send a notification to a user")
    public ResponseEntity<ApiResponse<NotificationResponse>> send(
            @Valid @RequestBody NotificationRequest.Send request,
            HttpServletRequest httpRequest) {

        NotificationResponse response = notificationService.send(request, requestUtil.extractClientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Notification queued", response));
    }

    @PostMapping("/send-bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Send notifications to multiple users")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> sendBulk(
            @Valid @RequestBody NotificationRequest.SendBulk request,
            HttpServletRequest httpRequest) {

        List<NotificationResponse> responses = notificationService.sendBulk(
                request, requestUtil.extractClientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Bulk notifications queued", responses));
    }
}
