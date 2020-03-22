package com.james.Coronavirustracker.controller;

import com.james.Coronavirustracker.models.DataSource;
import com.james.Coronavirustracker.models.LocationStats;
import com.james.Coronavirustracker.services.CoronaVirusDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private CoronaVirusDataService coronaVirusDataService;

    @GetMapping("/{date}/{location}")
    public String home(@PathVariable String date, @PathVariable String location, Model model) {
        DataSource dataSource = coronaVirusDataService.getDataSource();
        List<LocationStats> displayedStat = dataSource.getLocationStats().get(coronaVirusDataService.convertPathParameterToDateFormat(date));
        List<LocationStats> filterDisplayStats = (location.equals("all")) ? displayedStat : coronaVirusDataService.filterByLocation(location, displayedStat);
        model.addAttribute("liveData", filterDisplayStats);
        model.addAttribute("monthListMap", dataSource.getMonthListMap());
        model.addAttribute("currentTotalReportedCases", dataSource.getTotalFromCurrentDay());
        model.addAttribute("previousTotalReportedCases", dataSource.getTotalFromPreviousDay());
        return "home";
    }
}
