package com.gameon.controller;

import com.gameon.dto.UserSearchDto;
import com.gameon.service.UserSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for AJAX user search.
 * Returns JSON results for the navbar search dropdown.
 */
@RestController
public class UserSearchController {

    private final UserSearchService userSearchService;

    public UserSearchController(UserSearchService userSearchService) {
        this.userSearchService = userSearchService;
    }

    /**
     * Searches active users by username.
     * Requires authentication (enforced by SecurityConfig).
     *
     * @param q search query (minimum 2 characters)
     * @return list of matching users as JSON
     */
    @GetMapping("/users/search")
    public ResponseEntity<List<UserSearchDto>> searchUsers(@RequestParam String q) {
        List<UserSearchDto> results = userSearchService.searchUsers(q);
        return ResponseEntity.ok(results);
    }
}
