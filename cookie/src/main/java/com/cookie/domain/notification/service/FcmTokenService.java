package com.cookie.domain.notification.service;

import com.cookie.domain.notification.entity.FcmToken;
import com.cookie.domain.notification.repository.FcmTokenRepository;
import com.cookie.domain.user.entity.User;
import com.cookie.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTokenService {
    private final UserRepository userRepository;
    private final FcmTokenRepository fcmTokenRepository;

    @Transactional
    public void saveFcmToken(Long userId, String token) {
        log.info("Start saveToken userId: {} and token: {}", userId, token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("not found userId: " + userId));

        // 사용자가 알림을 활성화한 경우에만 토큰 저장
        if (user.isPushEnabled()) {
            // 해당 유저에 토큰이 이미 존재하면 업데이트
            Optional<FcmToken> existingFcmToken = fcmTokenRepository.findByUser(user);
            if (existingFcmToken.isPresent()) {
                FcmToken fcmToken = existingFcmToken.get();
                fcmToken.update(token);
                fcmTokenRepository.save(fcmToken);
                log.info("FCM 토큰 업데이트 완료: userId = {}, newToken = {}", userId, token);
            } else {
                // 존재하지 않으면 새로운 토큰 저장
                FcmToken fcmToken = FcmToken.builder()
                        .token(token)
                        .user(user)
                        .build();
                fcmTokenRepository.save(fcmToken);
                log.info("새로운 FCM 토큰 저장: userId = {}, token = {}", userId, token);
            }
        } else {
            log.info("Push notifications are disabled for userId: {}", userId);
        }
    }

    @Transactional
    public void deleteFcmToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("not found userId: " + userId));

        Optional<FcmToken> optionalFcmToken = fcmTokenRepository.findByUserId(userId);

        if (optionalFcmToken.isPresent()) {
            FcmToken fcmToken = optionalFcmToken.get();
            fcmTokenRepository.delete(fcmToken);
            log.info("Deleted fcm token for userId: {}", userId);

        } else {
            log.info("userId:{} 에 삭제 할 FCM 토큰이 존재하지 않습니다.", userId);
        }
    }
}
