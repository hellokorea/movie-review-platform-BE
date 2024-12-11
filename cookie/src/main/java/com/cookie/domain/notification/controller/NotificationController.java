package com.cookie.domain.notification.controller;

import com.cookie.domain.notification.dto.request.FcmTokenRequest;
import com.cookie.domain.notification.service.FcmTokenService;
import com.cookie.domain.user.dto.response.auth.CustomOAuth2User;
import com.cookie.domain.user.service.UserService;
import com.cookie.global.util.ApiUtil;
import com.cookie.global.util.ApiUtil.ApiSuccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "푸쉬 알림", description = "푸쉬 알림 설정 및 FCM 토큰 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification/")
public class NotificationController {

    private final FcmTokenService fcmTokenService;
    private final UserService userService;

    @Operation(summary = "FCM 토큰 저장", description = "사용자의 FCM 토큰을 저장합니다. 푸쉬 알림이 활성화된 사용자만 저장 가능합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "FCM 토큰 저장 성공", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content(mediaType = "application/json"))
            }
    )
    @PostMapping("/fcm-token")
    public ApiSuccess<?> saveFcmToken(@AuthenticationPrincipal CustomOAuth2User customOAuth2User, @RequestBody FcmTokenRequest fcmTokenRequest) {
        Long userId = customOAuth2User.getId();
        fcmTokenService.saveFcmToken(userId, fcmTokenRequest.getToken()); // 토큰 저장
        return ApiUtil.success("SUCCESS");
    }

    @Operation(summary = "FCM 토큰 삭제", description = "사용자의 FCM 토큰을 삭제합니다. 로그아웃 및 푸쉬 알림을 비활성화할 때 호출됩니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "FCM 토큰 삭제 성공", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content(mediaType = "application/json"))
            }
    )
    @DeleteMapping("/fcm-token")
    public ApiSuccess<?> deleteFcmToken(@AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        Long userId = customOAuth2User.getId();
        fcmTokenService.deleteFcmToken(userId); // 토큰 삭제
        return ApiUtil.success("SUCCESS");
    }

    @Operation(summary = "푸쉬 알림 설정 변경", description = "사용자의 푸쉬 알림을 설정합니다. 알림이 활성화되면 FCM 토큰이 저장되고, 비활성화되면 FCM 토큰이 삭제됩니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "푸쉬 알림 설정 변경 성공", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content(mediaType = "application/json"))
            }
    )
    @PostMapping("/settings")
    public ApiSuccess<?> toggleNotification(@AuthenticationPrincipal CustomOAuth2User customOAuth2User, @RequestBody FcmTokenRequest fcmTokenRequest) {
        Long userId = customOAuth2User.getId();
        userService.togglePushNotification(userId, fcmTokenRequest);
        return ApiUtil.success("SUCCESS");
    }

}
