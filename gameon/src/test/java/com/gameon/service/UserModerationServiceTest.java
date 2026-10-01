package com.gameon.service;

import com.gameon.model.entity.User;
import com.gameon.model.enums.AccountStatus;
import com.gameon.model.enums.UserRole;
import com.gameon.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the account-status / warning-strike moderation logic in {@link UserService}.
 * Verifies the 3-strike auto-ban, reinstatement reset, suspension, and that moderation
 * metadata (ban date, last action) is recorded. No user is ever deleted.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserModerationServiceTest {

    @Mock UserRepository userRepository;
    @Mock BlockService blockService;
    @InjectMocks UserService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Robert", "password", UserRole.USER);
        user.setUserId(15L);
        when(userRepository.findById(15L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void newUserStartsActiveWithZeroWarnings() {
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(user.getWarningCount()).isZero();
        assertThat(user.getBannedAt()).isNull();
    }

    @Test
    void firstStrikeIncrementsWarningButKeepsActive() {
        User result = service.issueStrike(15L, "spam");

        assertThat(result.getWarningCount()).isEqualTo(1);
        assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(result.getBannedAt()).isNull();
        assertThat(result.getLastModerationAction()).contains("Strike issued");
        assertThat(result.getLastModerationAt()).isNotNull();
    }

    @Test
    void thirdStrikeAutomaticallyBansWithBanDate() {
        service.issueStrike(15L, "one");
        service.issueStrike(15L, "two");
        User result = service.issueStrike(15L, "three");

        assertThat(result.getWarningCount()).isEqualTo(3);
        assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.BANNED);
        assertThat(result.getBannedAt()).isNotNull();
        assertThat(result.isBanned()).isTrue();
        assertThat(result.getLastModerationAction()).contains("Banned after 3 strikes");
    }

    @Test
    void reinstateResetsStatusWarningsAndBanDate() {
        service.issueStrike(15L, "one");
        service.issueStrike(15L, "two");
        service.issueStrike(15L, "three");
        assertThat(user.isBanned()).isTrue();

        User result = service.reinstateUser(15L);

        assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(result.getWarningCount()).isZero();
        assertThat(result.getBannedAt()).isNull();
        assertThat(result.getLastModerationAction()).contains("Reinstated");
    }

    @Test
    void suspendChangesStatusWithoutChangingWarnings() {
        service.issueStrike(15L, "one");
        User result = service.suspendUser(15L, "under review");

        assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(result.getWarningCount()).isEqualTo(1);
        assertThat(result.getLastModerationAction()).contains("Suspended");
    }

    @Test
    void directBanSetsStatusAndBanDateAndPreservesUser() {
        service.deactivateUser(15L);

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.BANNED);
        assertThat(user.getBannedAt()).isNotNull();
        assertThat(user.getLastModerationAction()).contains("Banned by administrator");
    }

    @Test
    void banUserStoresReasonAndModeratorAttribution() {
        User result = service.banUser(15L, 1L, "Repeated harassment");

        assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.BANNED);
        assertThat(result.getBannedAt()).isNotNull();
        assertThat(result.getBannedByUserId()).isEqualTo(1L);
        assertThat(result.getBanReason()).isEqualTo("Repeated harassment");
    }

    @Test
    void suspendStoresReasonForLoginMessage() {
        User result = service.suspendUser(15L, "Pending review");

        assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(result.getBanReason()).isEqualTo("Pending review");
    }

    @Test
    void resetWarningCountZeroesCountKeepingStatus() {
        user.setWarningCount(2);
        user.setAccountStatus(AccountStatus.SUSPENDED);

        User result = service.resetWarningCount(15L);

        assertThat(result.getWarningCount()).isZero();
        assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(result.getLastModerationAction()).contains("reset");
    }

    @Test
    void activateUserClearsBanAttribution() {
        service.banUser(15L, 1L, "abuse");
        service.activateUser(15L);

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(user.getBannedAt()).isNull();
        assertThat(user.getBannedByUserId()).isNull();
        assertThat(user.getBanReason()).isNull();
    }
}
