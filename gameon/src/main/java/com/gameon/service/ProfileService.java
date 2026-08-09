package com.gameon.service;

import com.gameon.exception.BusinessRuleException;
import com.gameon.exception.DuplicateResourceException;
import com.gameon.exception.ResourceNotFoundException;
import com.gameon.model.entity.Sport;
import com.gameon.model.entity.User;
import com.gameon.model.entity.UserSportProfile;
import com.gameon.model.entity.UserSportProfileId;
import com.gameon.model.enums.SkillLevel;
import com.gameon.repository.FollowRepository;
import com.gameon.repository.SportRepository;
import com.gameon.repository.UserRepository;
import com.gameon.repository.UserSportProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service handling user profile management.
 * Covers D200 (Manage Profile), D300 (Add Sport), D400 (View Profile).
 * Enforces BR7: A user can play multiple sports.
 */
@Service
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    private final UserRepository userRepository;
    private final UserSportProfileRepository userSportProfileRepository;
    private final SportRepository sportRepository;
    private final FollowRepository followRepository;

    public ProfileService(UserRepository userRepository,
                          UserSportProfileRepository userSportProfileRepository,
                          SportRepository sportRepository,
                          FollowRepository followRepository) {
        this.userRepository = userRepository;
        this.userSportProfileRepository = userSportProfileRepository;
        this.sportRepository = sportRepository;
        this.followRepository = followRepository;
    }

    @Transactional(readOnly = true)
    public User getProfile(Long userId) {
        return userRepository.findByIdWithSportProfiles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    @Transactional(readOnly = true)
    public List<UserSportProfile> getUserSports(Long userId) {
        return userSportProfileRepository.findByIdUserId(userId);
    }

    /**
     * Adds a sport to the user's profile.
     * BR7: A user can play multiple sports (no limit).
     */
    @Transactional
    public UserSportProfile addSportToProfile(Long userId, Long sportId, SkillLevel skillLevel) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Sport sport = sportRepository.findById(sportId)
                .orElseThrow(() -> new ResourceNotFoundException("Sport", sportId));

        if (userSportProfileRepository.existsByIdUserIdAndIdSportId(userId, sportId)) {
            throw new DuplicateResourceException("Sport already on your profile: " + sport.getSportName());
        }

        UserSportProfile profile = new UserSportProfile();
        profile.setId(new UserSportProfileId(userId, sportId));
        profile.setUser(user);
        profile.setSport(sport);
        profile.setSkillLevel(skillLevel);
        profile.setWins(0);
        profile.setLosses(0);
        profile.setWinPercentage(0.0);

        UserSportProfile saved = userSportProfileRepository.save(profile);
        logger.info("User {} added sport {} (skill: {})", user.getUsername(), sport.getSportName(), skillLevel);
        return saved;
    }

    /**
     * Removes a sport from the user's profile.
     * Cannot remove if it's the last sport (minimum 1 required).
     */
    @Transactional
    public void removeSportFromProfile(Long userId, Long sportId) {
        if (!userSportProfileRepository.existsByIdUserIdAndIdSportId(userId, sportId)) {
            throw new ResourceNotFoundException("Sport not found on your profile");
        }

        long sportCount = userSportProfileRepository.countByIdUserId(userId);
        if (sportCount <= 1) {
            throw new BusinessRuleException("Cannot remove your last sport. You must have at least one sport on your profile.");
        }

        userSportProfileRepository.deleteByIdUserIdAndIdSportId(userId, sportId);
        logger.info("User {} removed sport ID {} from profile", userId, sportId);
    }

    /**
     * Updates skill level for an existing sport on profile.
     */
    @Transactional
    public UserSportProfile updateSkillLevel(Long userId, Long sportId, SkillLevel newLevel) {
        UserSportProfile profile = userSportProfileRepository.findByIdUserIdAndIdSportId(userId, sportId)
                .orElseThrow(() -> new ResourceNotFoundException("Sport not found on your profile"));

        profile.setSkillLevel(newLevel);
        logger.info("User {} updated skill level for sport {} to {}", userId, sportId, newLevel);
        return userSportProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public long getFollowerCount(Long userId) {
        return followRepository.countByIdFollowedUserId(userId);
    }

    @Transactional(readOnly = true)
    public long getFollowingCount(Long userId) {
        return followRepository.countByIdFollowerUserId(userId);
    }

    /**
     * Checks if a user has a specific sport on their profile.
     * Used by GameListingService (BR8) and GameJoinerService (BR9).
     */
    @Transactional(readOnly = true)
    public boolean hasSportOnProfile(Long userId, Long sportId) {
        return userSportProfileRepository.existsByIdUserIdAndIdSportId(userId, sportId);
    }

    /**
     * Gets the list of sport IDs on a user's profile.
     */
    @Transactional(readOnly = true)
    public List<Long> getUserSportIds(Long userId) {
        return userSportProfileRepository.findDistinctSportIdsByUserId(userId);
    }
}
