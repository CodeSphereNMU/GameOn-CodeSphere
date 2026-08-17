package com.gameon.security;

import com.gameon.model.entity.User;
import com.gameon.model.enums.AccountStatus;
import com.gameon.model.enums.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {

    @Test
    void onlyActiveAccountsAreEnabled() {
        User user = new User("testuser", "password123", UserRole.USER);
        CustomUserDetails details = new CustomUserDetails(user);

        user.setAccountStatus(AccountStatus.ACTIVE);
        assertThat(details.isEnabled()).isTrue();

        user.setAccountStatus(AccountStatus.SUSPENDED);
        assertThat(details.isEnabled()).isFalse();

        user.setAccountStatus(AccountStatus.BANNED);
        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void exposesOnlyTheApprovedUserTypes() {
        User user = new User("admin", "Admin123", UserRole.ADMIN);
        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }
}
