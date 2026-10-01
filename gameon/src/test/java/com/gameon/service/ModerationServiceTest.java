package com.gameon.service;

import com.gameon.model.entity.ModerationAction;
import com.gameon.model.entity.Report;
import com.gameon.model.entity.User;
import com.gameon.model.enums.ContentType;
import com.gameon.model.enums.ModerationActionType;
import com.gameon.model.enums.UserRole;
import com.gameon.repository.ModerationActionRepository;
import com.gameon.repository.ReportRepository;
import com.gameon.repository.UserRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ModerationService} - the permanent moderation action audit trail.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModerationServiceTest {

    @Mock ModerationActionRepository moderationActionRepository;
    @Mock UserRepository userRepository;
    @Mock ReportRepository reportRepository;
    @InjectMocks ModerationService service;

    private User moderator;
    private User target;
    private Report report;

    @BeforeEach
    void setUp() {
        moderator = new User("Admin", "password", UserRole.ADMIN);
        moderator.setUserId(1L);
        target = new User("Robert", "password", UserRole.USER);
        target.setUserId(8L);
        report = new Report();
        report.setReportId(22L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(moderator));
        when(userRepository.findById(8L)).thenReturn(Optional.of(target));
        when(reportRepository.findById(22L)).thenReturn(Optional.of(report));
        when(moderationActionRepository.save(any(ModerationAction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void logActionPersistsAllFields() {
        service.logAction(1L, 8L, 22L, ModerationActionType.BAN, "Repeated abuse",
                ContentType.USER, 8L);

        ArgumentCaptor<ModerationAction> captor = ArgumentCaptor.forClass(ModerationAction.class);
        verify(moderationActionRepository).save(captor.capture());
        ModerationAction saved = captor.getValue();

        assertThat(saved.getModerator()).isSameAs(moderator);
        assertThat(saved.getTargetUser()).isSameAs(target);
        assertThat(saved.getReport()).isSameAs(report);
        assertThat(saved.getActionType()).isEqualTo(ModerationActionType.BAN);
        assertThat(saved.getReason()).isEqualTo("Repeated abuse");
        assertThat(saved.getContentType()).isEqualTo(ContentType.USER);
        assertThat(saved.getContentId()).isEqualTo(8L);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void logActionAllowsNullTargetAndReport() {
        service.logAction(1L, null, null, ModerationActionType.DISMISS_REPORT, "no action", null, null);

        ArgumentCaptor<ModerationAction> captor = ArgumentCaptor.forClass(ModerationAction.class);
        verify(moderationActionRepository).save(captor.capture());
        ModerationAction saved = captor.getValue();

        assertThat(saved.getTargetUser()).isNull();
        assertThat(saved.getReport()).isNull();
        assertThat(saved.getActionType()).isEqualTo(ModerationActionType.DISMISS_REPORT);
    }
}
