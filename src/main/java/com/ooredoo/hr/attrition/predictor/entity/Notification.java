package com.ooredoo.hr.attrition.predictor.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;        // recalcul | critique | rapport | planifie

    @Column(nullable = false)
    private String message;

    @Column
    private String valeur;      // nombre ou date en String

    @Column(nullable = false)
    private boolean lue = false;

    @Column(nullable = false)
    private String moisRecalcul; // ex: "2026-05"

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
    }
}