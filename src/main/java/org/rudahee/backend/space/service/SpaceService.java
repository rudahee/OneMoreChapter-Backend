package org.rudahee.backend.space.service;

import org.rudahee.backend.space.domain.Space;
import org.rudahee.backend.space.domain.SpaceMember;

import java.util.List;

public interface SpaceService {

    /** Crea el espacio y deja a quien lo crea como OWNER. */
    Space create(String name, Integer maxMembers, Long ownerId);

    List<Space> findMine(Long userId);

    Space requireById(Long spaceId);

    Space rename(Long spaceId, String newName);

    List<SpaceMember> members(Long spaceId);

    /** Expulsa a un miembro. No se permite dejar el espacio sin propietario. */
    void removeMember(Long spaceId, Long userId);
}
