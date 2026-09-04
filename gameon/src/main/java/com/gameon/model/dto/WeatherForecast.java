package com.gameon.model.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A single-point weather forecast at (or nearest to) a match time.
 * Populated from the Open-Meteo forecast API.
 */
public class WeatherForecast implements Serializable {

    private LocalDateTime time;        // the forecast hour this data represents
    private Double temperatureC;       // air temperature, degrees Celsius
    private Integer humidityPercent;   // relative humidity %
    private Integer rainChancePercent; // precipitation probability %
    private Double windSpeedKmh;       // wind speed, km/h
    private Integer weatherCode;       // WMO weather interpretation code
    private String condition;          // human-readable condition, e.g. "Partly Cloudy"
    private String icon;               // Bootstrap-icons name, e.g. "bi-cloud-sun"
    private boolean available;         // false when the forecast could not be retrieved

    public WeatherForecast() {
    }

    public static WeatherForecast unavailable() {
        WeatherForecast f = new WeatherForecast();
        f.available = false;
        f.condition = "Forecast unavailable";
        f.icon = "bi-cloud-slash";
        return f;
    }

    /** Convenience: the calendar date of this forecast (used for alternative-date grouping). */
    public LocalDate getDate() {
        return time == null ? null : time.toLocalDate();
    }

    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }
    public Double getTemperatureC() { return temperatureC; }
    public void setTemperatureC(Double temperatureC) { this.temperatureC = temperatureC; }
    public Integer getHumidityPercent() { return humidityPercent; }
    public void setHumidityPercent(Integer humidityPercent) { this.humidityPercent = humidityPercent; }
    public Integer getRainChancePercent() { return rainChancePercent; }
    public void setRainChancePercent(Integer rainChancePercent) { this.rainChancePercent = rainChancePercent; }
    public Double getWindSpeedKmh() { return windSpeedKmh; }
    public void setWindSpeedKmh(Double windSpeedKmh) { this.windSpeedKmh = windSpeedKmh; }
    public Integer getWeatherCode() { return weatherCode; }
    public void setWeatherCode(Integer weatherCode) { this.weatherCode = weatherCode; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
