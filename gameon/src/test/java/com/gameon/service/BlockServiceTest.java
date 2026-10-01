package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.model.entity.User;
import com.gameon.model.enums.UserRole;
import com.gameon.repository.BlockRepository;
import com.gameon.repository.FollowRepository;
import com.gameon.repository.InvitationRepository;
import com.gameon.repository.JoinRequestRepository;
import com.gameon.repository.NotificationRepository;
import com.gameon.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    @Mock BlockRepository blockRepository;
    @Mock UserRepository userRepository;
    @Mock FollowRepository followRepository;
    @Mock InvitationRepository invitationRepository;
    @Mock JoinRequestRepository joinRequestRepository;
    @Mock NotificationRepository notificationRepository;
    @InjectMocks BlockService service;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = new User("Alice", "password", UserRole.USER);
        alice.setUserId(1L);
        bob = new User("Bob", "password", UserRole.USER);
        bob.setUserId(2L);
    }

    // ===== isBlockedEitherWay: the central mutual check =====

    @Test
    void isBlockedEitherWayIsTrueWhenBlockExistsInEitherDirection() {
        when(blockRepository.existsBlockEitherWay(1L, 2L)).thenReturn(true);
        assertThat(service.isBlockedEitherWay(1L, 2L)).isTrue();
    }

    @Test
    void isBlockedEitherWayIsFalseWhenNoBlockExists() {
        when(blockRepository.existsBlockEitherWay(1L, 2L)).thenReturn(false);
        assertThat(service.isBlockedEitherWay(1L, 2L)).isFalse();
    }

    @Test
    void isBlockedEitherWayIsFalseForSameUser() {
        assertThat(service.isBlockedEitherWay(1L, 1L)).isFalse();
        verify(blockRepository, never()).existsBlockEitherWay(any(), any());
    }

    @Test
    void isBlockedEitherWayIsFalseWhenAnyIdIsNull() {
        assertThat(service.isBlockedEitherWay(null, 2L)).isFalse();
        assertThat(service.isBlockedEitherWay(1L, null)).isFalse();
        verify(blockRepository, never()).existsBlockEitherWay(any(), any());
    }

    // ===== getBlockedUserIds: the block wall (both directions merged) =====

    @Test
    void getBlockedUserIdsMergesOutgoingAndIncomingWithoutDuplicates() {
        when(blockRepository.findBlockedUserIds(1L)).thenReturn(List.of(2L, 3L));
        when(blockRepository.findBlockerUserIds(1L)).thenReturn(List.of(3L, 4L));

        assertThat(service.getBlockedUserIds(1L)).containsExactlyInAnyOrder(2L, 3L, 4L);
    }

    @Test
    void getBlockedUserIdsIsEmptyForNullUser() {
        assertThat(service.getBlockedUserIds(null)).isEmpty();
    }

    // ===== block: creates the row and applies mutual side effects =====

    @Test
    void blockSavesRelationshipAndAppliesMutualSideEffects() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(blockRepository.existsByIdBlockerUserIdAndIdBlockedUserId(1L, 2L)).thenReturn(false);

        service.block(1L, 2L);

        verify(blockRepository).save(any());
        // 5. follow removed both ways
        verify(followRepository).deleteByFollowerAndFollowed(1L, 2L);
        verify(followRepository).deleteByFollowerAndFollowed(2L, 1L);
        // 6. invitations expired both ways
        verify(invitationRepository).expirePendingBetweenUsers(1L, 2L);
        verify(invitationRepository).expirePendingBetweenUsers(2L, 1L);
        // 7. join requests expired both ways
        verify(joinRequestRepository).expirePendingBetweenUsers(1L, 2L);
        verify(joinRequestRepository).expirePendingBetweenUsers(2L, 1L);
        // 8. pending notifications between the two removed
        verify(notificationRepository).deleteBetweenUsers(1L, 2L);
    }

    @Test
    void blockDoesNotDuplicateRowWhenAlreadyBlockedButStillCleansUp() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(blockRepository.existsByIdBlockerUserIdAndIdBlockedUserId(1L, 2L)).thenReturn(true);

        service.block(1L, 2L);

        verify(blockRepository, never()).save(any());
        verify(followRepository).deleteByFollowerAndFollowed(1L, 2L);
        verify(notificationRepository).deleteBetweenUsers(1L, 2L);
    }

    @Test
    void blockRejectsSelfBlock() {
        assertThatThrownBy(() -> service.block(1L, 1L))
                .isInstanceOf(BusinessRuleException.class);
        verify(blockRepository, never()).save(any());
    }

    // ===== unblock: removes the directional block only when present =====

    @Test
    void unblockDeletesWhenBlockExists() {
        when(blockRepository.existsByIdBlockerUserIdAndIdBlockedUserId(1L, 2L)).thenReturn(true);
        service.unblock(1L, 2L);
        verify(blockRepository).deleteByBlockerAndBlocked(1L, 2L);
    }

    @Test
    void unblockIsNoOpWhenNoBlockExists() {
        when(blockRepository.existsByIdBlockerUserIdAndIdBlockedUserId(1L, 2L)).thenReturn(false);
        service.unblock(1L, 2L);
        verify(blockRepository, never()).deleteByBlockerAndBlocked(any(), any());
    }

    // ===== toggleBlock =====

    @Test
    void toggleBlockBlocksWhenNotAlreadyBlocked() {
        when(blockRepository.existsByIdBlockerUserIdAndIdBlockedUserId(1L, 2L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));

        assertThat(service.toggleBlock(1L, 2L)).isTrue();
        verify(blockRepository).save(any());
    }

    @Test
    void toggleBlockUnblocksWhenAlreadyBlocked() {
        when(blockRepository.existsByIdBlockerUserIdAndIdBlockedUserId(1L, 2L)).thenReturn(true);

        assertThat(service.toggleBlock(1L, 2L)).isFalse();
        verify(blockRepository, times(1)).deleteByBlockerAndBlocked(1L, 2L);
    }
}
