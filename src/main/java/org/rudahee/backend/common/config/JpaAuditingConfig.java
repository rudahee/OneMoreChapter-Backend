package org.rudahee.backend.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Activa el relleno automatico de {@code createdAt} y {@code updatedAt}.
 *
 * Va en una clase propia y no sobre la clase de aplicacion a proposito: asi los
 * tests de porcion ({@code @WebMvcTest}, {@code @DataJpaTest}) deciden si la
 * importan en lugar de arrastrarla siempre.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing
public class JpaAuditingConfig {
}
