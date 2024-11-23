package com.cookie.domain.director.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Director {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long tmdbCasterId;
    private String name;
    private String profileImage;

    @Builder
    public Director(Long id, Long tmdbCasterId, String name, String profileImage) {
        this.id = id;
        this.tmdbCasterId = tmdbCasterId;
        this.name = name;
        this.profileImage = profileImage;
    }
}
