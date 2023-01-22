package my.example.service;

import my.example.entity.CountriesGraph;
import my.example.entity.CountryJSON;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Alexandr Kuznetsov (alexandr@power.lv)
 */
@Component
public class PathFinderImpl2 implements PathFinder {
    protected final static Log log = LogFactory.getLog(PathFinderImpl2.class);


    private List<Integer> getAllCountryPairs(Map<Integer, short[]> routing, List<Short> allCountryCodes) {
        final List<Integer> allPairsBackLog = new ArrayList<>();
        for (final short cCode : allCountryCodes) {
            for (int i = 0; i < allCountryCodes.size(); i++) {
                final short cCode2 = allCountryCodes.get(i);
                //exclude duplicates
                if (cCode2 <= cCode) {
                    continue;
                }
                final int hash = HashUtil.genHash(cCode, cCode2);
                if (!routing.containsKey(hash)) {
                    allPairsBackLog.add(hash);
                }
            }
        }
        return allPairsBackLog;
    }


    public CountriesGraph parseData(List<CountryJSON> countries) {
        final CountriesGraph graph = new CountriesGraph();
        final AtomicInteger counter = new AtomicInteger();
        final Map<Short, Set<Short>> found = new HashMap<>();

        for (CountryJSON ccj : countries) {

            final short index = toIndex(ccj.countryCode, graph.getCcIndexMap(), graph.getRevCcIndex(), counter);
            if (ccj.borders != null && ccj.borders.length > 0) {
                for (String borderCountry : ccj.borders) {
                    final short index2 = toIndex(borderCountry, graph.getCcIndexMap(), graph.getRevCcIndex(), counter);
                    final int hash = HashUtil.genHash(index, index2);
                    graph.getRouting().compute(hash, (k, v) -> {
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
        final List<Short> allCountryCodes = new ArrayList<>(graph.getRevCcIndex().keySet());
        allCountryCodes.sort(Short::compareTo);

        final List<Integer> allPairsBackLog = getAllCountryPairs(graph.getRouting(), allCountryCodes);


        calculateRouts(graph.getRouting(), found, allPairsBackLog);
        return graph;

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

    private void calculateRouts(Map<Integer, short[]> routing, Map<Short, Set<Short>> found, List<Integer> allPairsBackLog) {

        int foundNewRouts = 0;
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

                final short[] routeA = getDirectionalRoute(routing, indexes[0], index3);
                final short[] routeB = getDirectionalRoute(routing, index3, indexes[1]);
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
                foundNewRouts++;
            }


        }
        if (allPairsBackLog.isEmpty() || foundNewRouts == 0) return;

        calculateRouts(routing, found, allPairsBackLog);


    }


}
