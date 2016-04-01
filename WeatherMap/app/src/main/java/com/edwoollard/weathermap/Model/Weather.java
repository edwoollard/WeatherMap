package com.edwoollard.weathermap.Model;

import android.graphics.Bitmap;

import java.util.Date;

public class Weather {

    private String currentCondition;
    private double temperature;
    private double windSpeed;
    private String windDirection;
    private Bitmap currentConditionImage;
    private String lastUpdatedDate;
    private String city;

    public Weather(String currentCondition, double temperature, double windSpeed, String windDirection, Bitmap currentConditionImage, String lastUpdatedDate, String city) {
        this.currentCondition = currentCondition;
        this.temperature = temperature;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.currentConditionImage = currentConditionImage;
        this.lastUpdatedDate = lastUpdatedDate;
        this.city = city;
    }

    public String getCurrentCondition() {
        return currentCondition;
    }

    public void setCurrentCondition(String currentCondition) {
        this.currentCondition = currentCondition;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public String getWindDirection() {
        return windDirection;
    }

    public void setWindDirection(String windDirection) {
        this.windDirection = windDirection;
    }

    public Bitmap getCurrentConditionImage() {
        return currentConditionImage;
    }

    public void setCurrentConditionImage(Bitmap currentConditionImage) {
        this.currentConditionImage = currentConditionImage;
    }

    public String getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(String lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}