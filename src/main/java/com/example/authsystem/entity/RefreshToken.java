package com.example.authsystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens",indexes = {
        @Index(name = "refresh_token_idx",columnList = "Jti",unique = true),
        @Index(name = "refresh_token_user_id",columnList = "user_id")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    //Jwt token id
    @Column(name = "jti",unique = true,nullable = false,updatable = false)
    private String jti;

    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false,updatable = false)
    private User user;

    @Column(updatable = false,nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant expireAt;

    @Column(nullable = false)
    private boolean revoked;
    private String replaceByToken;
}
