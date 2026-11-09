package org.rudahee.backend.security.service;

import org.rudahee.backend.security.domain.AppUser;

/**
 * Autenticacion y ciclo de vida de la cuenta.
 *
 * {@link #createUser} es publico dentro del backend pero no se expone por HTTP:
 * lo llama {@code space} al canjear una invitacion. No hay registro abierto.
 */
public interface AuthService {

    TokenService.TokenPair login(String email, String rawPassword);

    TokenService.TokenPair refresh(String rawRefreshToken);

    void logout(String rawRefreshToken);

    AppUser createUser(String email, String rawPassword, String displayName);

    AppUser requireUser(Long userId);

    void changePassword(Long userId, String currentPassword, String newPassword);
}
