package com.leviplus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers("/login", "/css/**", "/js/**", "/img/**", "/error").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(salida -> salida
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?salio")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .sessionManagement(sesion -> sesion
                        .sessionFixation(fijacion -> fijacion.migrateSession()));
        return http.build();
    }

    @Bean
    PasswordEncoder codificadorDeContrasenas() {
        return new BCryptPasswordEncoder();
    }
}
