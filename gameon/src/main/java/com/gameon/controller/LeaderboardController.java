package com.gameon.controller;

import com.gameon.model.entity.Sport;
import com.gameon.model.entity.UserSportProfile;
import com.gameon.service.LeaderboardService;
import com.gameon.service.SportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Controller for B500 (View Leaderboards).
 * Shows rankings per sport based on win percentage.
 */
@Controller
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final SportService sportService;

    public LeaderboardController(LeaderboardService leaderboardService, SportService sportService) {
        this.leaderboardService = leaderboardService;
        this.sportService = sportService;
    }

    @GetMapping("/leaderboard")
    public String leaderboard(@RequestParam(required = false) Long sportId,
                              @RequestParam(defaultValue = "0") int page,
                              Model model) {
        List<Sport> sports = sportService.getAllSports();
        model.addAttribute("sports", sports);

        if (sportId == null && !sports.isEmpty()) {
            sportId = sports.get(0).getSportId();
        }

        if (sportId != null) {
            Page<UserSportProfile> rankings = leaderboardService.getLeaderboard(sportId, PageRequest.of(page, 50));
            model.addAttribute("rankings", rankings);
            model.addAttribute("selectedSportId", sportId);
        }

        return "social/leaderboard";
    }
}
