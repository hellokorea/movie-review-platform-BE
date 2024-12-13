package com.cookie.global.service;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.cookie.domain.actor.repository.ActorRepository;
import com.cookie.domain.director.repository.DirectorRepository;
import com.cookie.domain.movie.repository.MovieImageRepository;
import com.cookie.domain.movie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class AWSS3CDNService {

    private final AmazonS3 amazonS3;

    private final MovieRepository movieRepository;
    private final ActorRepository actorRepository;
    private final DirectorRepository directorRepository;
    private final MovieImageRepository movieImageRepository;

    @Value("${cloud.front.baseUrl}")
    private String cloudFrontBaseUrl;

    @Transactional
    public void updateImagesByOnce() {
        processAndUploadImages(movieRepository.findAllTMDBImages(), movieRepository::updateImageByFileName);
        processAndUploadImages(actorRepository.findAllTMDBImages(), actorRepository::updateImageByFileName);
        processAndUploadImages(directorRepository.findAllTMDBImages(), directorRepository::updateImageByFileName);
        processAndUploadImages(movieImageRepository.findAllTMDBImages(), movieImageRepository::updateImageByFileName);
    }

    private void processAndUploadImages(List<String> tmdbImageUrls, BiConsumer<String, String> updateRepository) {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<?>> futures = new ArrayList<>();

        for (String url : tmdbImageUrls) {
            if (url == null || url.isEmpty() || url.endsWith("/null") || url.startsWith("https://d320gmmmso0682")) {
                continue;
            }

            futures.add(executor.submit(() -> {
                try {
                    byte[] imageBytes = downloadImageFromTMDB(url);

                    String fileName = extractFileNameFromUrl(url);

                    uploadToS3(fileName, imageBytes);

                    String cloudFrontUrl = cloudFrontBaseUrl + fileName;

                    updateRepository.accept(url, cloudFrontUrl);

                    Thread.sleep(200);

                } catch (Exception e) {
                    log.error("Failed to process URL: {}", url, e);
                }
            }));
        }

        // 모든 작업이 끝날 때까지 기다림
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.error("Error while processing tasks", e);
            }
        }

        executor.shutdown();
    }

    private byte[] downloadImageFromTMDB(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        try (InputStream inputStream = url.openStream()) {
            return inputStream.readAllBytes();
        }
    }

    private String extractFileNameFromUrl(String imageUrl) throws MalformedURLException {
        return Paths.get(new URL(imageUrl).getPath()).getFileName().toString();
    }

    private void uploadToS3(String s3Key, byte[] imageBytes) {
        String bucketName = "movie-images-bucket";
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(imageBytes.length);
        amazonS3.putObject(bucketName, s3Key, new ByteArrayInputStream(imageBytes), metadata);
    }
}
