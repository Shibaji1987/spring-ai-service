package com.agentic.ai.spring_ai_service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTests {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig();
        ReflectionTestUtils.setField(securityConfig, "username", "analyst");
        ReflectionTestUtils.setField(securityConfig, "password", "Analyst@12345");
        ReflectionTestUtils.setField(securityConfig, "adminUsername", "admin");
        ReflectionTestUtils.setField(securityConfig, "adminPassword", "Admin@12345");
        ReflectionTestUtils.setField(securityConfig, "policyManagerUsername", "policy-manager");
        ReflectionTestUtils.setField(securityConfig, "policyManagerPassword", "Policy@12345");
        ReflectionTestUtils.setField(securityConfig, "viewerUsername", "viewer");
        ReflectionTestUtils.setField(securityConfig, "viewerPassword", "Viewer@12345");
    }

    @Test
    void configuresEachApplicationRole() {
        UserDetailsService users = securityConfig.userDetailsService(securityConfig.passwordEncoder());

        assertThat(users.loadUserByUsername("admin").getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        assertThat(users.loadUserByUsername("policy-manager").getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_POLICY_MANAGER");
        assertThat(users.loadUserByUsername("analyst").getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ANALYST");
        assertThat(users.loadUserByUsername("viewer").getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_VIEWER");
    }

    @Test
    void mapsJwtRolesClaimToSpringAuthoritiesWithoutChangingRoleNames() {
        Instant now = Instant.now();
        Jwt jwt = new Jwt(
                "token",
                now,
                now.plusSeconds(300),
                java.util.Map.of("alg", "HS256"),
                java.util.Map.of("sub", "analyst", "roles", List.of("ROLE_ANALYST"))
        );

        var authentication = securityConfig.jwtAuthenticationConverter().convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ANALYST");
    }
}
