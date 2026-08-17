package com.gameon.repository;

import com.gameon.model.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    boolean existsByGameListingGameListingIdAndInviteeUserId(Long listingId, Long userId);
    List<Invitation> findByInviteeUserIdOrderByCreatedAtDesc(Long userId);
}
