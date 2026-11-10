package ru.konyaev.priem.entity;

import jakarta.persistence.*;

/**
 * Таблица администраторов приёмной комиссии (таблица admin).
 */
@Entity
@Table(name = "admin")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 30)
    private Role role = Role.ROLE_ADMIN;

    public User() {
    }

    @PrePersist
    public void prePersist() {
        if (this.role == null) {
            this.role = Role.ROLE_ADMIN;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
