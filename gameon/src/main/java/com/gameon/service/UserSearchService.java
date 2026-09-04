package com.gameon.service;

import com.gameon.dto.UserSearchDto;
import com.gameon.model.entity.User;
import com.gameon.model.enums.AccountStatus;
import com.gameon.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for user search functionality.
 * Searches active users by username and returns DTO results.
 */
@Service
public class UserSearchService {

    private static final Logger logger = LoggerFactory.getLogger(UserSearchService.class);
    private static final int MAX_RESULTS = 10;

    private final UserRepository userRepository;

    public UserSearchService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Searches active users by username (case-insensitive).
     * Returns a maximum of 10 results mapped to UserSearchDto.
     *
     * @param query the search term (minimum 2 characters)
     * @return list of matching user DTOs
     */
    @Transactional(readOnly = true)
    public List<UserSearchDto> searchUsers(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        String trimmedQuery = query.trim();
        logger.debug("Searching users with query: '{}'", trimmedQuery);

        List<User> users = userRepository.searchActiveUsersByUsername(
                trimmedQuery, AccountStatus.ACTIVE, PageRequest.of(0, MAX_RESULTS));

        return users.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private UserSearchDto mapToDto(User user) {
        return new UserSearchDto(
                user.getUserId(),
                user.getUsername(),
                null,  // displayName - not yet available on User entity
                user.getProfilePictureUrl()  // null when no picture uploaded -> UI shows default avatar
        );
    }
}
