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
    private final Map<Integer, short[]> routing = new HashMap<>();
    private final Map<Short, Set<Short>> found = new HashMap<>();


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
                    routing.compute(hash, (k, v) -> {
                        if (v == null || v.length > 0) v = DIRECT_CONNECTION;

                        return v;
                    });
                    found.computeIfAbsent(index, k -> {
                        return new HashSet<>();
                    }).add(index2);
                    found.computeIfAbsent(index2, k -> {
                        return new HashSet<>();
                    }).add(index);
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
                if (!routing.containsKey(hash)) {
                    allPairsBackLog.add(hash);
                }
            }
        }
        Set<Integer> testSet = new HashSet<>(allPairsBackLog);
        log.info("Found " + allCountryCodes.size() + " countries, " + routing.size() + " direct connections, " + allPairsBackLog.size() + "(" + testSet.size() + ") possible pairs ");

        calculateRouts(allPairsBackLog, allCountryCodes.size());
        int maxLength = 0;
        int maxLengthHash = 0;
        for (Map.Entry<Integer, short[]> entry : routing.entrySet()) {
            if (entry.getValue().length > maxLength) {
                maxLength = entry.getValue().length;
                maxLengthHash = entry.getKey();
            }
        }

        short[] indexes = HashUtil.extractCodes(maxLengthHash);
        String cc1 = revCcIndex.get(indexes[0]);
        String cc2 = revCcIndex.get(indexes[1]);

        log.info("Calculated " + routing.size() + " connections, longest route: " + cc1 + " - " + cc2 + ", " + maxLength + " points");


    }

    private <T> List<T> findIntersections(Collection<T> values1, Collection<T> values2) {
        if (null == values1 || values1.isEmpty() || values2 == null || values2.isEmpty())
            return Collections.emptyList();
        List<T> found = null;
        for (T value : values1) {
            if (values2.contains(value)) {
                if (found == null) found = new ArrayList<>();
                found.add(value);
            }
        }

        return found != null ? found : Collections.emptyList();
    }

    private void calculateRouts(List<Integer> allPairsBackLog, int routePointsLeft) {
        if (allPairsBackLog.isEmpty() || routePointsLeft <= 0) return;

        for (int i = allPairsBackLog.size() - 1; i > -1; i--) {
            final int hash = allPairsBackLog.get(i);
            if (routing.containsKey(hash)) {
                allPairsBackLog.remove(i);
                continue;
            }
            final short[] indexes = HashUtil.extractCodes(hash);
            final List<Short> intersections = findIntersections(found.get(indexes[0]), found.get(indexes[1]));
            final List<short[]> foundedRoutes = new ArrayList<>();
            for (short index3 : intersections) {

                final short[] routeA = getDirectionalRoute(indexes[0], index3);
                final short[] routeB = getDirectionalRoute(index3, indexes[1]);
                final short[] routeC = new short[routeA.length + routeB.length + 1];
                routeC[routeA.length] = index3;
                if (routeA.length > 0) {
                    System.arraycopy(routeA, 0, routeC, 0, routeA.length);
                }
                if (routeB.length > 0) {
                    System.arraycopy(routeB, 0, routeC, routeA.length + 1, routeB.length);
                }
                foundedRoutes.add(routeC);
            }
            if (!foundedRoutes.isEmpty()) {
                foundedRoutes.sort(Comparator.comparingInt(arr -> arr.length));
                routing.put(hash, foundedRoutes.get(0));
                found.get(indexes[0]).add(indexes[1]);
                found.get(indexes[1]).add(indexes[0]);
                allPairsBackLog.remove(i);
            }


        }
        calculateRouts(allPairsBackLog, routePointsLeft - 1);
    }

    private short[] getDirectionalRoute(short fromIndex, short toIndex) {
        final int hash = HashUtil.genHash(fromIndex, toIndex);
        final short[] route = routing.get(hash);
        if (null == route) return null;
        return fromIndex < toIndex ? route : reverse(route);
    }

    private short[] reverse(short[] arr) {
        short[] arrRev = new short[arr.length];
        for (int i = 0; i < arr.length; i++) {
            arrRev[arrRev.length - (i + 1)] = arr[i];
        }
        return arrRev;
    }

    private short toIndex(final String countryCode) {
        return ccIndexMap.computeIfAbsent(countryCode, k -> {
            short index = (short) counter.incrementAndGet();
            revCcIndex.put(index, countryCode);
            return index;

        });
    }

    public List<String> findRoute(String ccOrigin, String ccDestination) {
        if (!ccIndexMap.containsKey(ccOrigin)) {
            throw new IllegalArgumentException("Invalid country code: " + ccOrigin);
        }
        if (!ccIndexMap.containsKey(ccDestination)) {
            throw new IllegalArgumentException("Invalid country code: " + ccDestination);
        }
        short index1 = ccIndexMap.get(ccOrigin);
        short index2 = ccIndexMap.get(ccDestination);


        short[] route = getDirectionalRoute(index1, index2);
        if (null == route) return null;
        final List<String> routeCodes = new ArrayList<>();
        routeCodes.add(ccOrigin);
        for (short index : route) {
            String code = revCcIndex.get(index);
            routeCodes.add(code);
        }
        routeCodes.add(ccDestination);


        return routeCodes;
    }
}
