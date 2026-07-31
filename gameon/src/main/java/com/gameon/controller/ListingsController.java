package com.gameon.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for the Listings tab - the main landing page after login.
 * Displays available game listings for browsing and joining.
 */
@Controller
public class ListingsController {

    @GetMapping("/listings")
    public String index(Model model) {
        return "listings/index";
    }
}
