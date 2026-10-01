package com.gameon.service;

import com.gameon.model.entity.Report;
import com.gameon.model.entity.User;
import com.gameon.model.entity.UserWarning;
import com.gameon.model.enums.AccountStatus;
import com.gameon.model.enums.ContentType;
import com.gameon.model.enums.NotificationType;
import com.gameon.model.enums.UserRole;
import com.gameon.repository.ReportRepository;
import com.gameon.repository.UserRepository;
import com.gameon.repository.UserWarningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WarningService} - the permanent strike audit system.
 * Verifies a strike creates a persistent record, increments the warning count, sends a
 * notification, and that the third strike bans the account with full attribution.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WarningServiceTest {

    @Mock UserWarningRepository userWarningRepository;
    @Mock UserRepository userRepository;
    @Mock ReportRepository reportRepository;
    @Mock NotificationService notificationService;
    @InjectMocks WarningService service;

    private User offender;
    private User moderator;

    @BeforeEach
    void setUp() {
        offender = new User("Robert", "password", UserRole.USER);
        offender.setUserId(8L);
        moderator = new User("Admin", "password", UserRole.ADMIN);
        moderator.setUserId(1L);
        when(userRepository.findById(8L)).thenReturn(Optional.of(offender));
        when(userRepository.findById(1L)).thenReturn(Optional.of(moderator));
        when(userWarningRepository.save(any(UserWarning.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void firstStrikeCreatesPermanentRecordWithAuditFields() {
        UserWarning result = service.issueStrike(8L, 1L,
                "Offensive language towards another player", ContentType.POST, 105L, 22L);

        ArgumentCaptor<UserWarning> captor = ArgumentCaptor.forClass(UserWarning.class);
        verify(userWarningRepository).save(captor.capture());
        UserWarning saved = captor.getValue();

        assertThat(saved.getUser()).isSameAs(offender);
        assertThat(saved.getModerator()).isSameAs(moderator);
        assertThat(saved.getWarningReason()).isEqualTo("Offensive language towards another player");
        assertThat(saved.getContentType()).isEqualTo(ContentType.POST);
        assertThat(saved.getContentId()).isEqualTo(105L);
        assertThat(saved.getStrikeNumber()).isEqualTo(1);
        assertThat(saved.getWarningDate()).isNotNull();
        assertThat(result).isSameAs(saved);
    }

    @Test
    void firstStrikeIncrementsWarningCountButKeepsActive() {
        service.issueStrike(8L, 1L, "spam", ContentType.POST, 105L, 22L);

        assertThat(offender.getWarningCount()).isEqualTo(1);
        assertThat(offender.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(offender.getBannedAt()).isNull();
    }

    @Test
    void strikeSendsNotificationToOffenderWithModeratorAsActor() {
        service.issueStrike(8L, 1L, "spam", ContentType.POST, 105L, 22L);

        verify(notificationService).createNotification(
                eq(8L), any(String.class), eq(NotificationType.STRIKE_ISSUED),
                eq(moderator), isNull(), isNull(), isNull());
    }

    @Test
    void reportIsLinkedWhenProvided() {
        Report report = new Report();
        report.setReportId(22L);
        when(reportRepository.findById(22L)).thenReturn(Optional.of(report));

        service.issueStrike(8L, 1L, "spam", ContentType.POST, 105L, 22L);

        ArgumentCaptor<UserWarning> captor = ArgumentCaptor.forClass(UserWarning.class);
        verify(userWarningRepository).save(captor.capture());
        assertThat(captor.getValue().getReport()).isSameAs(report);
    }

    @Test
    void thirdStrikeBansAccountWithAttribution() {
        offender.setWarningCount(2); // already has two strikes

        service.issueStrike(8L, 1L, "Repeated abuse", ContentType.USER, 8L, 30L);

        assertThat(offender.getWarningCount()).isEqualTo(3);
        assertThat(offender.getAccountStatus()).isEqualTo(AccountStatus.BANNED);
        assertThat(offender.getBannedAt()).isNotNull();
        assertThat(offender.getBannedByUserId()).isEqualTo(1L);
        assertThat(offender.getBanReason()).contains("Repeated abuse");
        assertThat(offender.getLastModerationAction()).contains("Banned after 3 strikes");
    }

    @Test
    void strikeWithBlankReasonUsesDefaultReason() {
        UserWarning result = service.issueStrike(8L, 1L, "  ", ContentType.OTHER, null, null);
        assertThat(result.getWarningReason()).isEqualTo("Community guideline violation");
    }

    @Test
    void reverseWarningDecrementsActiveCountButKeepsAuditRecord() {
        offender.setWarningCount(2);
        UserWarning warning = new UserWarning(offender, moderator, "spam", ContentType.POST, 105L, 2, null);
        warning.setWarningId(50L);
        when(userWarningRepository.findById(50L)).thenReturn(Optional.of(warning));

        User result = service.reverseWarning(50L, 1L);

        assertThat(result.getWarningCount()).isEqualTo(1);
        // The audit record is never deleted.
        verify(userWarningRepository, org.mockito.Mockito.never()).delete(any(UserWarning.class));
        verify(userWarningRepository, org.mockito.Mockito.never()).deleteById(any());
    }

    @Test
    void reverseWarningLiftsBanWhenBelowThreshold() {
        offender.setWarningCount(3);
        offender.setAccountStatus(AccountStatus.BANNED);
        offender.setBannedByUserId(1L);
        offender.setBanReason("Reached 3 strikes");
        UserWarning warning = new UserWarning(offender, moderator, "spam", ContentType.POST, 105L, 3, null);
        warning.setWarningId(51L);
        when(userWarningRepository.findById(51L)).thenReturn(Optional.of(warning));

        User result = service.reverseWarning(51L, 1L);

        assertThat(result.getWarningCount()).isEqualTo(2);
        assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(result.getBannedAt()).isNull();
        assertThat(result.getBannedByUserId()).isNull();
        assertThat(result.getBanReason()).isNull();
    }

    @Test
    void reverseWarningNeverGoesBelowZero() {
        offender.setWarningCount(0);
        UserWarning warning = new UserWarning(offender, moderator, "spam", ContentType.POST, 105L, 1, null);
        warning.setWarningId(52L);
        when(userWarningRepository.findById(52L)).thenReturn(Optional.of(warning));

        User result = service.reverseWarning(52L, 1L);

        assertThat(result.getWarningCount()).isZero();
    }
}
