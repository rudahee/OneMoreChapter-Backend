package org.rudahee.backend.space.domain;

/**
 * Rol dentro de un espacio.
 *
 * Solo dos niveles a proposito: un espacio es de dos personas, y una jerarquia
 * de permisos mas fina seria burocracia sin funcion. La diferencia se limita a
 * quien puede invitar, renombrar y expulsar.
 */
public enum SpaceRole {

    OWNER,
    MEMBER;

    public boolean canManage() {
        return this == OWNER;
    }
}
