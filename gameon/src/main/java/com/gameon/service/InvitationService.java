package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.model.entity.GameListing;
import com.gameon.model.entity.Invitation;
import com.gameon.model.entity.User;
import com.gameon.model.enums.JoinerStatus;
import com.gameon.repository.GameJoinerRepository;
import com.gameon.repository.InvitationRepository;
import com.gameon.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class InvitationService {
    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final GameJoinerRepository gameJoinerRepository;

    public InvitationService(InvitationRepository invitationRepository,
                             UserRepository userRepository,
                             GameJoinerRepository gameJoinerRepository) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.gameJoinerRepository = gameJoinerRepository;
    }

    @Transactional
    public void createInvitations(GameListing listing, Long creatorId, List<Long> inviteeIds) {
        if (inviteeIds == null || inviteeIds.isEmpty()) return;
        LinkedHashSet<Long> distinctIds = new LinkedHashSet<>(inviteeIds);
        if (distinctIds.size() != inviteeIds.size()) {
            throw new BusinessRuleException("The same friend cannot be invited more than once.");
        }
        for (Long inviteeId : distinctIds) {
            if (creatorId.equals(inviteeId)) {
                throw new BusinessRuleException("You cannot invite yourself.");
            }
            User invitee = userRepository.findById(inviteeId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", inviteeId));
            invitationRepository.save(new Invitation(listing, invitee));
        }
    }

    @Transactional(readOnly = true)
    public boolean isInvited(Long listingId, Long userId) {
        return invitationRepository.existsByGameListingGameListingIdAndInviteeUserId(listingId, userId);
    }

    @Transactional(readOnly = true)
    public List<Invitation> getActiveInvitations(Long userId) {
        LocalDateTime browseUntil = LocalDateTime.now()
                .plusHours(GameListingService.LOCK_IN_HOURS_BEFORE_START);
        return invitationRepository.findByInviteeUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(invitation -> !invitation.getGameListing().getIsCompleted())
                .filter(invitation -> invitation.getGameListing().getScheduledDate().isAfter(browseUntil))
                .filter(invitation -> hasNoActiveRequest(invitation, userId))
                .toList();
    }

    private boolean hasNoActiveRequest(Invitation invitation, Long userId) {
        return gameJoinerRepository.findByUserAndListing(
                        userId, invitation.getGameListing().getGameListingId())
                .map(joiner -> joiner.getStatus() == JoinerStatus.REJECTED
                        || joiner.getStatus() == JoinerStatus.LEFT)
                .orElse(true);
    }
}
