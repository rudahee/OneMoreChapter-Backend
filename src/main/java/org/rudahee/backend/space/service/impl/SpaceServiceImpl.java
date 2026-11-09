package org.rudahee.backend.space.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rudahee.backend.common.exception.BusinessRuleException;
import org.rudahee.backend.common.exception.NotFoundException;
import org.rudahee.backend.security.domain.AppUser;
import org.rudahee.backend.security.service.AuthService;
import org.rudahee.backend.space.domain.Space;
import org.rudahee.backend.space.domain.SpaceMember;
import org.rudahee.backend.space.domain.SpaceRole;
import org.rudahee.backend.space.repository.SpaceMemberRepository;
import org.rudahee.backend.space.repository.SpaceRepository;
import org.rudahee.backend.space.service.SpaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceServiceImpl implements SpaceService {

    private final SpaceRepository spaceRepository;
    private final SpaceMemberRepository memberRepository;
    private final AuthService authService;

    @Override
    @Transactional
    public Space create(String name, Integer maxMembers, Long ownerId) {
        AppUser owner = authService.requireUser(ownerId);

        Space space = spaceRepository.save(new Space(name, maxMembers));
        memberRepository.save(new SpaceMember(space, owner, SpaceRole.OWNER));

        log.info("Espacio {} creado por el usuario {}", space.getId(), ownerId);
        return space;
    }

    @Override
    public List<Space> findMine(Long userId) {
        return spaceRepository.findAllByMember(userId);
    }

    @Override
    public Space requireById(Long spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(() -> NotFoundException.of("Espacio", spaceId));
    }

    @Override
    @Transactional
    public Space rename(Long spaceId, String newName) {
        Space space = requireById(spaceId);
        space.setName(newName);
        return space;
    }

    @Override
    public List<SpaceMember> members(Long spaceId) {
        return memberRepository.findAllBySpaceWithUser(spaceId);
    }

    @Override
    @Transactional
    public void removeMember(Long spaceId, Long userId) {
        SpaceMember member = memberRepository.findBySpaceIdAndUserId(spaceId, userId)
                .orElseThrow(() -> NotFoundException.of("Miembro del espacio", userId));

        // Un espacio sin propietario no se puede gestionar ni borrar: quedaria
        // inaccesible con todos sus datos dentro.
        if (member.getRole().canManage()
                && memberRepository.countBySpaceIdAndRole(spaceId, SpaceRole.OWNER) <= 1) {
            throw new BusinessRuleException("LAST_OWNER",
                    "No puedes quitar al unico propietario del espacio");
        }

        memberRepository.delete(member);
        log.info("Usuario {} eliminado del espacio {}", userId, spaceId);
    }
}
