package com.kleadingsolutions.expenseshare.security;

import com.kleadingsolutions.expenseshare.model.User;
import com.kleadingsolutions.expenseshare.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads user info from Google, upserts domain User and returns a DefaultOAuth2User
 * whose attributes include "uid" (domain user id).
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oauth2User = super.loadUser(userRequest);

        // Google attributes: "email", "name", "sub"
        String email = (String) oauth2User.getAttributes().get("email");
        String name = (String) oauth2User.getAttributes().get("name");
        String oauthId = Optional.ofNullable(oauth2User.getAttributes().get("sub")).map(Object::toString).orElse(null);

        if (email == null) {
            throw new IllegalStateException("OAuth2 provider did not provide email");
        }

        // Upsert domain user
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = User.builder()
                    .email(email)
                    .name(name)
                    .oauthId(oauthId)
                    .roles(Set.of("USER"))
                    .build();
            return userRepository.save(u);
        });

        // Ensure oauthId saved
        if (user.getOauthId() == null && oauthId != null) {
            user.setOauthId(oauthId);
            userRepository.save(user);
        }

        // Map roles to GrantedAuthorities
        Set<GrantedAuthority> authorities = (user.getRoles() == null ? Set.of() :
                user.getRoles().stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toSet()));

        // Build attributes and include uid for easy domain mapping
        Map<String, Object> attrs = new HashMap<>(oauth2User.getAttributes());
        attrs.put("uid", user.getId().toString());
        attrs.put("roles", user.getRoles());

        // The "nameAttributeKey" is typically "email" or "sub"; use "email" so DefaultOAuth2User.getName() returns email
        return new DefaultOAuth2User(authorities, attrs, "email");
    }
}