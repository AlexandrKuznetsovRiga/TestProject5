package my.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import my.example.entity.CountryJSON;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CountryGraphService {
    private final static Log log = LogFactory.getLog(CountryGraphService.class);
    private final static short[] DIRECT_CONNECTION = {};

    @Value("${my.sourceUrl}")
    private String sourceUrl;
    private final Map<String, Short> ccIndexMap = new HashMap<>();
    private final Map<Short, String> revCcIndex = new HashMap<>();
    private final AtomicInteger counter = new AtomicInteger(0);
    private final Map<Integer, short[]> routings = new HashMap<>();


    @PostConstruct
    public void _init() throws JsonProcessingException {
        log.info("Source: " + sourceUrl);

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ResponseEntity<String> response = restTemplate.getForEntity(sourceUrl, String.class);

        List<CountryJSON> countries = objectMapper.readValue(response.getBody(), new TypeReference<List<CountryJSON>>() {
        });
        for (CountryJSON ccj : countries) {
            //log.info("Country: " + ccj.countryCode);

            final short index = toIndex(ccj.countryCode);
            if (ccj.borders != null && ccj.borders.length > 0) {
                for (String borderCountry : ccj.borders) {
                    final short index2 = toIndex(borderCountry);
                    final int hash = HashUtil.genHash(index, index2);
                    routings.compute(hash, (k, v) -> {
                        if (v == null || v.length > 0) v = DIRECT_CONNECTION;

                        return v;
                    });
                }

            }
        }

        final List<Short> allCountryCodes = new ArrayList<>();
        allCountryCodes.addAll(revCcIndex.keySet());
        allCountryCodes.sort(Short::compareTo);

        final List<Integer> allPairsBackLog = new ArrayList<>();
        for (final short cCode : allCountryCodes) {
            for (int i = 0; i < allCountryCodes.size(); i++) {
                final short cCode2 = allCountryCodes.get(i);
                //fast way to exclude duplicates
                if (cCode2 <= cCode) {
                    continue;
                }
                final int hash = HashUtil.genHash(cCode, cCode2);
                if (!routings.containsKey(hash)) {
                    allPairsBackLog.add(hash);
                }
            }
        }
        Set<Integer> testSet = new HashSet<>(allPairsBackLog);
        log.info("Found " + allCountryCodes.size() + " countries, " + routings.size() + " direct connections, " + allPairsBackLog.size() + "(" + testSet.size() + ") possible pairs ");

        calculateRouts(allPairsBackLog, allCountryCodes.size());

    }

    private void calculateRouts(List<Integer> allPairsBackLog, int routePointsLeft) {
        if (allPairsBackLog.isEmpty()) return;

        for (int i = allPairsBackLog.size() - 1; i > -1; i--) {

        }
    }

    private short toIndex(final String countryCode) {
        return ccIndexMap.computeIfAbsent(countryCode, k -> {
            short index = (short) counter.incrementAndGet();
            revCcIndex.put(index, countryCode);
            return index;

        });
    }

    public List<String> findRoute(String ccOrigin, String ccDestination) {
        return List.of(ccOrigin, "UNKNONW", "SOMEWHERE", ccDestination);
    }
}
