package com.gameon.controller;

import com.gameon.model.dto.WeatherDTO;
import com.gameon.service.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * REST controller for weather forecast data.
 * Provides an AJAX endpoint used by the listing creation form to fetch
 * live weather previews when date/time/venue changes.
 */
@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private static final Logger logger = LoggerFactory.getLogger(WeatherController.class);

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * Fetches a weather forecast for the given coordinates and scheduled date/time.
     * Called via AJAX from the listing creation page when the user selects or changes
     * the date, time, or venue.
     *
     * @param lat  venue latitude
     * @param lng  venue longitude
     * @param date scheduled date/time in ISO format (yyyy-MM-ddTHH:mm)
     * @return WeatherDTO as JSON
     */
    @GetMapping("/forecast")
    public ResponseEntity<WeatherDTO> getForecast(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam String date) {

        // Validate coordinates
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            logger.warn("Invalid coordinates: lat={}, lng={}", lat, lng);
            return ResponseEntity.badRequest().body(WeatherDTO.unavailable());
        }

        // Parse the date
        LocalDateTime scheduledDate;
        try {
            scheduledDate = LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            logger.warn("Invalid date format: {}", date);
            return ResponseEntity.badRequest().body(WeatherDTO.unavailable());
        }

        // Date must be in the future
        if (scheduledDate.isBefore(LocalDateTime.now())) {
            return ResponseEntity.ok(WeatherDTO.unavailable());
        }

        WeatherDTO forecast = weatherService.getForecast(lat, lng, scheduledDate);
        return ResponseEntity.ok(forecast);
    }
}
