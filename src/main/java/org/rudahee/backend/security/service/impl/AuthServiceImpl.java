package org.rudahee.backend.security.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rudahee.backend.common.exception.BusinessRuleException;
import org.rudahee.backend.common.exception.NotFoundException;
import org.rudahee.backend.security.domain.AppUser;
import org.rudahee.backend.security.repository.AppUserRepository;
import org.rudahee.backend.security.service.AuthService;
import org.rudahee.backend.security.service.TokenService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    @Transactional
    public TokenService.TokenPair login(String email, String rawPassword) {
        AppUser user = userRepository.findByEmail(AppUser.normalizeEmail(email))
                .orElse(null);

        // Se compara la contrasenya incluso si el usuario no existe, para que
        // el tiempo de respuesta no revele que correos estan registrados.
        boolean valid = user != null
                && passwordEncoder.matches(rawPassword, user.getPasswordHash());
        if (user == null) {
            passwordEncoder.encode(rawPassword);
        }

        if (!valid || !user.isEnabled()) {
            // Mensaje unico: nunca distinguir "no existe" de "contrasenya mal".
            throw new BadCredentialsException("Credenciales no validas");
        }

        user.setLastLoginAt(Instant.now());
        return tokenService.issue(user);
    }

    @Override
    @Transactional
    public TokenService.TokenPair refresh(String rawRefreshToken) {
        return tokenService.rotate(rawRefreshToken);
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        tokenService.revoke(rawRefreshToken);
    }

    @Override
    @Transactional
    public AppUser createUser(String email, String rawPassword, String displayName) {
        String normalized = AppUser.normalizeEmail(email);
        if (userRepository.existsByEmail(normalized)) {
            throw new BusinessRuleException("EMAIL_TAKEN", "Ese correo ya tiene cuenta");
        }
        AppUser user = new AppUser(normalized, passwordEncoder.encode(rawPassword), displayName);
        AppUser saved = userRepository.save(user);
        log.info("Cuenta creada para el usuario {}", saved.getId());
        return saved;
    }

    @Override
    public AppUser requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> NotFoundException.of("Usuario", userId));
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        AppUser user = requireUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("La contrasenya actual no es correcta");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // Cambiar la contrasenya tumba el resto de sesiones: es lo que se
        // espera cuando alguien la cambia porque cree que se la han robado.
        tokenService.revokeAllSessions(userId);
    }
}
