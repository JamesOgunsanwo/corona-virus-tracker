package com.james.Coronavirustracker.models;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;

public class DataSource {

    private Map<String, List<LocationStats>> locationStats;

    private int totalFromCurrentDay;

    private int totalFromPreviousDay;

    private Map<Month, List<LocalDate>> monthListMap;

    public  Map<String, List<LocationStats>> getLocationStats() {
        return locationStats;
    }

    public int getTotalFromCurrentDay() {
        return totalFromCurrentDay;
    }

    public DataSource(Map<String, List<LocationStats>> locationStats, int totalFromCurrentDay, int totalFromPreviousDay, Map<Month, List<LocalDate>> monthListMap) {
        this.locationStats = locationStats;
        this.totalFromCurrentDay = totalFromCurrentDay;
        this.totalFromPreviousDay = totalFromPreviousDay;
        this.monthListMap = monthListMap;
    }

    public int getTotalFromPreviousDay() {
        return totalFromPreviousDay;
    }

    public Map<Month, List<LocalDate>> getMonthListMap() {
        return monthListMap;
    }
}
