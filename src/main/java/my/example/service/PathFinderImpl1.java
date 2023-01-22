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
@Component("VER_1")
public class PathFinderImpl1 implements PathFinder {
    protected final static Log log = LogFactory.getLog(PathFinderImpl1.class);


    @Override
    public CountriesGraph parseData(List<CountryJSON> countries) {
        final Map<Short, Map<Integer, Set<Short>>> found = new HashMap<>();
        final CountriesGraph graph = new CountriesGraph();
        final AtomicInteger counter = new AtomicInteger();

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
                        Map<Integer, Set<Short>> map = new HashMap<>();
                        map.put(0, new HashSet<>());
                        return map;
                    }).get(0).add(index2);
                    found.computeIfAbsent(index2, k -> {
                        Map<Integer, Set<Short>> map = new HashMap<>();
                        map.put(0, new HashSet<>());
                        return map;
                    }).get(0).add(index);
                }

            }
        }

        calculateRouts(graph.getRouting(), found, 0);


        return graph;
    }


    private void calculateRouts(final Map<Integer, short[]> routing, final Map<Short, Map<Integer, Set<Short>>> found, final int level) {

        int foundNewRouts = 0;
        for (final Map.Entry<Short, Map<Integer, Set<Short>>> entry : found.entrySet()) {
            final short index1 = entry.getKey();


            final Set<Short> connections = getAllConnections(entry.getValue(), Collections.emptySet(), (short) -1);

            for (final short index2 : connections) {
                final Set<Short> connections2 = getAllConnections(found.get(index2), connections, index1);

                for (final short index3 : connections2) {

                    final int hash3 = HashUtil.genHash(index1, index3);
                    final short[] existingRoute = routing.get(hash3);

                    final short[] routeA = getDirectionalRoute(routing, index1, index2);
                    final short[] routeB = getDirectionalRoute(routing, index2, index3);
                    int routeLength = routeA.length + routeB.length + 1;
                    if (existingRoute != null && routeLength >= existingRoute.length) {
                        continue;
                    }
                    final short[] routeC = new short[routeLength];
                    routeC[routeA.length] = index2;
                    if (routeA.length > 0) {
                        System.arraycopy(routeA, 0, routeC, 0, routeA.length);
                    }
                    if (routeB.length > 0) {
                        System.arraycopy(routeB, 0, routeC, routeA.length + 1, routeB.length);
                    }

                    foundNewRouts++;

                    if (index1 > index3) {
                        routing.put(hash3, reverse(routeC));
                    } else {
                        routing.put(hash3, routeC);
                    }
                    entry.getValue().compute(routeLength, (k, v) -> {
                        if (v == null) v = new HashSet<>();
                        v.add(index3);
                        return v;
                    });
                    found.get(index3).compute(routeLength, (k, v) -> {
                        if (v == null) v = new HashSet<>();
                        v.add(index1);
                        return v;
                    });


                }
            }


        }
        if (foundNewRouts == 0) {
            log.info("Finish routing search on level: " + level);
            return;
        }

        calculateRouts(routing, found, level + 1);


    }

    private Set<Short> getAllConnections(Map<Integer, Set<Short>> connections, Set<Short> excludeConnections, short excludeIndex) {
        final Set<Short> result = new HashSet<>();
        connections.values().forEach(result::addAll);
        result.removeAll(excludeConnections);
        result.remove(excludeIndex);
        return result;
    }


}
