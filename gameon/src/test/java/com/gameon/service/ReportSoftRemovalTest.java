package com.gameon.service;

import com.gameon.model.entity.Post;
import com.gameon.model.entity.Report;
import com.gameon.model.entity.User;
import com.gameon.model.enums.PrivacySetting;
import com.gameon.model.enums.ReportStatus;
import com.gameon.model.enums.UserRole;
import com.gameon.repository.PostRepository;
import com.gameon.repository.ReportRepository;
import com.gameon.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportSoftRemovalTest {

    @Mock ReportRepository reportRepository;
    @Mock UserRepository userRepository;
    @Mock PostRepository postRepository;
    @Mock PostService postService;
    @InjectMocks ReportService service;

    @Test
    void postRemovalAndReportResolutionUseSameServiceTransactionFlow() {
        User reporter = user(1L, "Reporter");
        User reviewer = user(9L, "Moderator");
        Post post = new Post(user(2L, "Author"), "Original", PrivacySetting.PUBLIC);
        post.setPostId(20L);
        Report report = new Report(reporter, "Spam");
        report.setReportId(30L);
        report.setReportedPost(post);
        when(reportRepository.findById(30L)).thenReturn(Optional.of(report));
        when(userRepository.findById(9L)).thenReturn(Optional.of(reviewer));
        when(reportRepository.save(report)).thenReturn(report);

        Report resolved = service.resolvePostReport(30L, 9L);

        verify(postService).deletePostAsModerator(20L, 9L);
        assertThat(resolved.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(resolved.getReviewedBy()).isSameAs(reviewer);
        assertThat(resolved.getReviewedAt()).isNotNull();
    }

    private User user(Long id, String name) {
        User user = new User(name, "password", UserRole.USER);
        user.setUserId(id);
        return user;
    }
}
