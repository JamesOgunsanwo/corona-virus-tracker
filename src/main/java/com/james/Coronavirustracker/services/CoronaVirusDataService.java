package com.james.Coronavirustracker.services;

import com.james.Coronavirustracker.models.DataSource;
import com.james.Coronavirustracker.models.LocationStats;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

@Service
public class CoronaVirusDataService {

    private static final String START_DATE = "1/22/20";
    private static final String VIRUS_DATA_URL = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_time_series/time_series_19-covid-Confirmed.csv";

    private DataSource dataSource;

    public DataSource getDataSource() {
        return dataSource;
    }

    @PostConstruct
    @Scheduled(cron = "* 1 * * * *")
    public void fetchVirusData() throws IOException {
        String dataFromRequest = fetchData();
        Map<String, List<LocationStats>> coronaVirusDataByDate = convertDataIntoList(dataFromRequest);
        int totalFromCurrentDay = coronaVirusDataByDate.get(returnFormattedDate(1)).stream().mapToInt(LocationStats::getLatestTotalCases).sum();
        int totalFromPreviousDay = coronaVirusDataByDate.get(returnFormattedDate(2)).stream().mapToInt(LocationStats::getDiffFromPreviousDay).sum();
        Map<Month, List<LocalDate>> monthListMap = mapDateHeadersToMonths(getDataHeaders(dataFromRequest));
        this.dataSource = new DataSource(coronaVirusDataByDate, totalFromCurrentDay, totalFromPreviousDay, monthListMap);
    }

    public String convertPathParameterToDateFormat(String pathVariable) {
        String[] split = pathVariable.split("-");
        return split[0] + "/" + split[1] + "/" + split[2];
    }

    public List<LocationStats> filterByLocation(String pathVariable, List<LocationStats> locationStats) {
        return locationStats.stream()
                .filter(value -> value.getCountry().toLowerCase().contains(pathVariable.toLowerCase()))
                .collect(Collectors.toList());
    }

    private static String createUrl(String date) {
        String[] split = date.split("/");
        return "http://localhost:8080/" + split[0] + "-" + split[1] + "-" + split[2];
    }

    private String returnFormattedDate(int decrement) {
        LocalDate localDate = LocalDate.now().minusDays(decrement);
        return localDate.getMonthValue() + "/" + localDate.getDayOfMonth() + "/" + String.valueOf(localDate.getYear()).substring(2);
    }

    private String fetchData() throws IOException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(VIRUS_DATA_URL))
                .build();

        HttpResponse<String> httpResponse;
        try {
            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new InterruptedIOException();
        }
        return httpResponse.body();
    }

    private List<String> getDataHeaders(String dataFromRequest) throws IOException {
        return CSVFormat.DEFAULT.withHeader()
                .withSkipHeaderRecord(false)
                .parse(new StringReader(dataFromRequest))
                .getHeaderNames().stream()
                .filter(value -> value.contains("/20")).collect(Collectors.toList());
    }

    // For dropdown
    private Map<Month, List<LocalDate>> mapDateHeadersToMonths(List<String> data) {
        List<LocalDate> headersToDateList =
                data.stream()
                        .map(this::stringToLocalDate)
                        .collect(Collectors.toList());
        return headersToDateList.stream().collect(Collectors.toMap(LocalDate::getMonth, value -> new ArrayList<>(Collections.singleton(value)), merge));
    }

    private BinaryOperator<List<LocalDate>> merge = (old, latest) -> {
        old.addAll(latest);
        return old;
    };

    private LocalDate stringToLocalDate(String header) {
        String[] splitDate = header.split("/");
        return LocalDate.of(Integer.parseInt(splitDate[2]), Integer.parseInt(splitDate[0]), Integer.parseInt(splitDate[1]));
    }

    private Map<String, List<LocationStats>> convertDataIntoList(String dataFromRequest) throws IOException {
        List<String> dataHeaders = getDataHeaders(dataFromRequest);
        Map<String, List<LocationStats>> resultsList = new HashMap<>();

        for (String date : dataHeaders) {
            List<LocationStats> locationStatsList = new ArrayList<>();
            for (CSVRecord record : CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new StringReader(dataFromRequest))) {
                LocationStats locationStats = new LocationStats();
                locationStats.setState(record.get("Province/State"));
                locationStats.setCountry(record.get("Country/Region"));

                int latestTotalCases = Integer.parseInt(record.get(date));
                String previousDate = localDateToString(stringToLocalDate(date), 1);
                int prevDayCases = date.equals(START_DATE) ? 0 : Integer.parseInt(record.get(previousDate));
                locationStats.setDiffFromPreviousDay(date.equals(START_DATE) ? 0 : (latestTotalCases - prevDayCases));
                locationStats.setLatestTotalCases(Integer.parseInt(record.get(date)));
                locationStatsList.add(locationStats);
            }
            resultsList.put(date, locationStatsList);
        }
        return resultsList;
    }

    private String localDateToString(LocalDate localDate, int decrement) {
        localDate = localDate.minusDays(decrement);
        return localDate.getMonthValue() + "/" + localDate.getDayOfMonth() + "/" + localDate.getYear();
    }
}
