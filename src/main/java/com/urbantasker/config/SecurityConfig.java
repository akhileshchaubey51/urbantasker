package com.urbantasker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // Allow all static assets, templates and public endpoints
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/login.html",
                    "/register.html",
                    "/booking.html",
                    "/dashboard.html",
                    "/providers.html",
                    "/provider-info.html",
                    "/provider-requests.html",
                    "/admin.html",
                    "/provider-detail.html",
                    "/provider.html",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/uploads/**",
                    "/favicon.ico"
                ).permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/services/**").permitAll()
                .requestMatchers("/api/providers/public/**").permitAll()
                .requestMatchers("/api/customer/**").permitAll() // session validated in controllers
                .requestMatchers("/api/bookings/**").permitAll() // session validated in controllers
                .requestMatchers("/api/provider/**").permitAll() // session validated in controllers
                .requestMatchers("/api/admin/**").permitAll()    // session validated in controllers
                .anyRequest().permitAll()
            )
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}
