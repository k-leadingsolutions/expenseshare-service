package com.kleadingsolutions.expenseshare.service;

import com.kleadingsolutions.expenseshare.model.User;
import com.kleadingsolutions.expenseshare.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public User findOrCreate(String provider, String providerId, String email, String name) {
        String oauthId = provider + ":" + providerId; // e.g. "google:1164442009..."
        return repo.findByOauthId(oauthId).map(u -> {
            boolean changed = false;
            if (email != null && !email.equals(u.getEmail())) { u.setEmail(email); changed = true; }
            if (name != null && !name.equals(u.getName())) { u.setName(name); changed = true; }
            if (changed) repo.save(u);
            return u;
        }).orElseGet(() -> {
            User user = User.builder()
                    .email(email)
                    .name(name)
                    .oauthId(oauthId)
                    .roles(Set.of("USER")) // default roles
                    .build();
            return repo.save(user);
        });
    }

    public Optional<User> findById(UUID id) {
        return repo.findById(id);
    }
}