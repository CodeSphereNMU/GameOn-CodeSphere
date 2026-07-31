package com.gameon.dto.auth;

import com.gameon.model.enums.SkillLevel;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for registration Step 2: sport selection with skill levels.
 */
public class RegisterStep2Dto {

    @NotEmpty(message = "Please select at least one sport")
    private List<SportSelection> sportSelections = new ArrayList<>();

    public RegisterStep2Dto() {
    }

    public List<SportSelection> getSportSelections() {
        return sportSelections;
    }

    public void setSportSelections(List<SportSelection> sportSelections) {
        this.sportSelections = sportSelections;
    }

    /**
     * Inner class representing a single sport + skill level selection.
     */
    public static class SportSelection {

        @NotNull(message = "Sport ID is required")
        private Long sportId;

        @NotNull(message = "Skill level is required")
        private SkillLevel skillLevel;

        public SportSelection() {
        }

        public SportSelection(Long sportId, SkillLevel skillLevel) {
            this.sportId = sportId;
            this.skillLevel = skillLevel;
        }

        public Long getSportId() {
            return sportId;
        }

        public void setSportId(Long sportId) {
            this.sportId = sportId;
        }

        public SkillLevel getSkillLevel() {
            return skillLevel;
        }

        public void setSkillLevel(SkillLevel skillLevel) {
            this.skillLevel = skillLevel;
        }
    }
}
