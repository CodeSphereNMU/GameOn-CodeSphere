package com.gameon.config;

import com.gameon.model.entity.*;
import com.gameon.model.enums.SkillLevel;
import com.gameon.model.enums.UserRole;
import com.gameon.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds test data when running with 'local' profile (H2 database).
 * Replicates the Flyway V2/V3 seed data programmatically since Flyway is disabled on H2.
 */
@Configuration
@Profile("local")
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initData(SportRepository sportRepository,
                                      PositionRepository positionRepository,
                                      SportFormatRepository sportFormatRepository,
                                      FormatPositionRepository formatPositionRepository,
                                      UserRepository userRepository,
                                      UserSportProfileRepository userSportProfileRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            logger.info("Initializing seed data for local profile...");

            // === SPORTS ===
            Sport padel = sportRepository.save(new Sport("Padel", 4));
            Sport tennis = sportRepository.save(new Sport("Tennis", 4));
            Sport basketball = sportRepository.save(new Sport("Basketball", 10));
            Sport football = sportRepository.save(new Sport("Football", 22));
            Sport rugby = sportRepository.save(new Sport("Rugby", 30));

            // === POSITIONS ===
            Position anyPos = positionRepository.save(new Position("Any Position"));
            Position goalkeeper = positionRepository.save(new Position("Goalkeeper"));
            Position defense = positionRepository.save(new Position("Defense"));
            Position midfield = positionRepository.save(new Position("Midfield"));
            Position attack = positionRepository.save(new Position("Attack"));
            Position guard = positionRepository.save(new Position("Guard"));
            Position forward = positionRepository.save(new Position("Forward"));
            Position center = positionRepository.save(new Position("Center"));
            Position scrumhalf = positionRepository.save(new Position("Scrumhalf"));
            Position flyhalf = positionRepository.save(new Position("Flyhalf"));
            Position wing = positionRepository.save(new Position("Wing"));
            Position fullback = positionRepository.save(new Position("Fullback"));

            // === SPORT FORMATS ===
            SportFormat padelDoubles = sportFormatRepository.save(new SportFormat("Doubles", 4, false, padel));
            SportFormat tennisSingles = sportFormatRepository.save(new SportFormat("Singles", 2, false, tennis));
            SportFormat tennisDoubles = sportFormatRepository.save(new SportFormat("Doubles", 4, false, tennis));
            SportFormat basketball3v3 = sportFormatRepository.save(new SportFormat("3v3", 6, true, basketball));
            SportFormat basketball5v5 = sportFormatRepository.save(new SportFormat("5v5", 10, true, basketball));
            SportFormat football5v5 = sportFormatRepository.save(new SportFormat("5v5", 10, true, football));
            SportFormat football7v7 = sportFormatRepository.save(new SportFormat("7v7", 14, true, football));
            SportFormat football11v11 = sportFormatRepository.save(new SportFormat("11v11", 22, true, football));
            SportFormat rugby7s = sportFormatRepository.save(new SportFormat("7s", 14, true, rugby));
            SportFormat rugby15s = sportFormatRepository.save(new SportFormat("15s", 30, true, rugby));

            // === FORMAT POSITIONS ===
            formatPositionRepository.save(new FormatPosition(basketball3v3, anyPos));
            formatPositionRepository.save(new FormatPosition(basketball3v3, guard));
            formatPositionRepository.save(new FormatPosition(basketball3v3, forward));
            formatPositionRepository.save(new FormatPosition(basketball3v3, center));

            formatPositionRepository.save(new FormatPosition(basketball5v5, anyPos));
            formatPositionRepository.save(new FormatPosition(basketball5v5, guard));
            formatPositionRepository.save(new FormatPosition(basketball5v5, forward));
            formatPositionRepository.save(new FormatPosition(basketball5v5, center));

            formatPositionRepository.save(new FormatPosition(football5v5, anyPos));
            formatPositionRepository.save(new FormatPosition(football5v5, goalkeeper));
            formatPositionRepository.save(new FormatPosition(football5v5, defense));
            formatPositionRepository.save(new FormatPosition(football5v5, midfield));
            formatPositionRepository.save(new FormatPosition(football5v5, attack));

            formatPositionRepository.save(new FormatPosition(football7v7, anyPos));
            formatPositionRepository.save(new FormatPosition(football7v7, goalkeeper));
            formatPositionRepository.save(new FormatPosition(football7v7, defense));
            formatPositionRepository.save(new FormatPosition(football7v7, midfield));
            formatPositionRepository.save(new FormatPosition(football7v7, attack));

            formatPositionRepository.save(new FormatPosition(football11v11, anyPos));
            formatPositionRepository.save(new FormatPosition(football11v11, goalkeeper));
            formatPositionRepository.save(new FormatPosition(football11v11, defense));
            formatPositionRepository.save(new FormatPosition(football11v11, midfield));
            formatPositionRepository.save(new FormatPosition(football11v11, attack));

            formatPositionRepository.save(new FormatPosition(rugby7s, anyPos));
            formatPositionRepository.save(new FormatPosition(rugby7s, scrumhalf));
            formatPositionRepository.save(new FormatPosition(rugby7s, flyhalf));
            formatPositionRepository.save(new FormatPosition(rugby7s, wing));
            formatPositionRepository.save(new FormatPosition(rugby7s, fullback));

            formatPositionRepository.save(new FormatPosition(rugby15s, anyPos));
            formatPositionRepository.save(new FormatPosition(rugby15s, defense));
            formatPositionRepository.save(new FormatPosition(rugby15s, scrumhalf));
            formatPositionRepository.save(new FormatPosition(rugby15s, flyhalf));
            formatPositionRepository.save(new FormatPosition(rugby15s, wing));
            formatPositionRepository.save(new FormatPosition(rugby15s, fullback));

            // === TEST USERS ===
            String encodedPassword = passwordEncoder.encode("Test123");
            String adminPassword = passwordEncoder.encode("Admin123");

            User zane = userRepository.save(new User("Zane", encodedPassword, UserRole.USER));
            User lihlumelo = userRepository.save(new User("Lihlumelo", encodedPassword, UserRole.USER));
            User gerard = userRepository.save(new User("Gerard", encodedPassword, UserRole.USER));
            User robert = userRepository.save(new User("Robert", encodedPassword, UserRole.USER));
            User moderator = userRepository.save(new User("Moderator", adminPassword, UserRole.MODERATOR));
            User admin = userRepository.save(new User("Admin", adminPassword, UserRole.ADMIN));

            // === USER SPORT PROFILES ===
            userSportProfileRepository.save(createProfile(zane, tennis, SkillLevel.ADVANCED, 15, 5));
            userSportProfileRepository.save(createProfile(zane, football, SkillLevel.INTERMEDIATE, 8, 7));
            userSportProfileRepository.save(createProfile(lihlumelo, football, SkillLevel.ADVANCED, 20, 3));
            userSportProfileRepository.save(createProfile(lihlumelo, basketball, SkillLevel.BEGINNER, 2, 5));
            userSportProfileRepository.save(createProfile(gerard, basketball, SkillLevel.INTERMEDIATE, 10, 8));
            userSportProfileRepository.save(createProfile(gerard, padel, SkillLevel.ADVANCED, 12, 4));
            userSportProfileRepository.save(createProfile(robert, tennis, SkillLevel.BEGINNER, 3, 10));
            userSportProfileRepository.save(createProfile(robert, padel, SkillLevel.INTERMEDIATE, 6, 6));

            logger.info("=== SEED DATA COMPLETE ===");
            logger.info("  Users: Zane / Lihlumelo / Gerard / Robert  (password: Test123)");
            logger.info("  Moderator: Moderator  (password: Admin123)");
            logger.info("  Admin: Admin  (password: Admin123)");
            logger.info("==========================");
        };
    }

    private UserSportProfile createProfile(User user, Sport sport, SkillLevel level, int wins, int losses) {
        UserSportProfile profile = new UserSportProfile();
        profile.setId(new UserSportProfileId(user.getUserId(), sport.getSportId()));
        profile.setUser(user);
        profile.setSport(sport);
        profile.setSkillLevel(level);
        profile.setWins(wins);
        profile.setLosses(losses);
        double total = wins + losses;
        profile.setWinPercentage(total > 0 ? (wins / total) * 100.0 : 0.0);
        return profile;
    }
}
