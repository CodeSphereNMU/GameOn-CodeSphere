package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.model.entity.Report;
import com.gameon.model.entity.User;
import com.gameon.model.enums.ReportType;
import com.gameon.model.enums.UserRole;
import com.gameon.repository.PostRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Report &amp; Block workflow in {@link ReportService#reportAndBlockUser}.
 * Verifies a report is created AND the reported user is blocked, both in one transaction,
 * and that failures propagate (so the transaction rolls back) rather than silently leaving
 * one half done.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportAndBlockServiceTest {

    @Mock ReportRepository reportRepository;
    @Mock UserRepository userRepository;
    @Mock PostRepository postRepository;
    @Mock PostService postService;
    @Mock BlockService blockService;
    @InjectMocks ReportService service;

    private User reporter;   // User A
    private User reported;   // User B

    @BeforeEach
    void setUp() {
        reporter = user(1L, "Alice");
        reported = user(2L, "Bob");
        when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reported));
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void reportAndBlockCreatesReportAndBlocksUser() {
        Report result = service.reportAndBlockUser(1L, 2L, "Harassment or bullying", "was abusive");

        // Report created with the correct reporter, target, reason and content.
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository, atLeastOnce()).save(captor.capture());
        Report saved = captor.getValue();
        assertThat(saved.getReporter()).isSameAs(reporter);
        assertThat(saved.getReportedUser()).isSameAs(reported);
        assertThat(saved.getReportReason()).isEqualTo("Harassment or bullying");
        assertThat(saved.getContent()).isEqualTo("was abusive");
        assertThat(saved.getReportType()).isEqualTo(ReportType.USER);

        // Block performed in the same flow, in the correct direction (reporter blocks reported).
        verify(blockService).block(1L, 2L);
        assertThat(result).isNotNull();
    }

    @Test
    void reportAndBlockRejectsSelfReport() {
        // createReport guards self-report; no block should be attempted.
        assertThatThrownBy(() -> service.reportAndBlockUser(1L, 1L, "Other", null))
                .isInstanceOf(BusinessRuleException.class);
        verify(blockService, never()).block(anyLong(), anyLong());
    }

    @Test
    void blockFailurePropagatesSoTransactionRollsBack() {
        // If the block fails, the exception must propagate out of the combined method so the
        // surrounding @Transactional rolls back the report too (no report without a block).
        doThrow(new BusinessRuleException("You cannot block yourself."))
                .when(blockService).block(1L, 2L);

        assertThatThrownBy(() -> service.reportAndBlockUser(1L, 2L, "Spam or advertising", null))
                .isInstanceOf(BusinessRuleException.class);

        verify(blockService).block(1L, 2L);
    }

    private User user(Long id, String name) {
        User u = new User(name, "password", UserRole.USER);
        u.setUserId(id);
        return u;
    }
}
