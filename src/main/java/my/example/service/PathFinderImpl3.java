package my.example.service;

import my.example.entity.CountriesGraph;
import my.example.entity.CountryJSON;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Alexandr Kuznetsov (alexandr@power.lv)
 */
@Component
public class PathFinderImpl3 implements PathFinder {
    protected final static Log log = LogFactory.getLog(PathFinderImpl3.class);


    @Override
    public CountriesGraph parseData(List<CountryJSON> countries) {
        final Map<Short, MutableObject<short[]>> found = new HashMap<>();
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
                    found.compute(index, (k, v) -> {
                        if (v == null) {
                            v = new MutableObject<>(new short[]{index2});
                            return v;
                        }
                        short[] values = new short[v.getValue().length + 1];
                        System.arraycopy(v.getValue(), 0, values, 0, v.getValue().length);
                        values[values.length - 1] = index2;
                        Arrays.sort(values);
                        v.setValue(values);

                        return v;
                    });
                    found.compute(index2, (k, v) -> {
                        if (v == null) {
                            v = new MutableObject<>(new short[]{index});
                            return v;
                        }
                        short[] values = new short[v.getValue().length + 1];
                        System.arraycopy(v.getValue(), 0, values, 0, v.getValue().length);
                        values[values.length - 1] = index;
                        Arrays.sort(values);
                        v.setValue(values);

                        return v;
                    });
                }

            }
        }

        calculateRouts(graph.getRouting(), found, 0);


        return graph;
    }


    private void calculateRouts(final Map<Integer, short[]> routing, final Map<Short, MutableObject<short[]>> found, final int level) {

        int foundNewRouts = 0;
        for (final Map.Entry<Short, MutableObject<short[]>> entry : found.entrySet()) {
            final short index1 = entry.getKey();


            final short[] connections = entry.getValue().getValue();

            for (final short index2 : connections) {

                final short[] connections2 = found.get(index2).getValue();

                for (final short index3 : connections2) {


                    if (index3 == index1 || Arrays.binarySearch(entry.getValue().getValue(), index3) >= 0) {
                        continue;
                    }

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
                    final short[] conn1 = entry.getValue().getValue();
                    if (Arrays.binarySearch(conn1, index3) < 0) {
                        short[] newEntry = new short[conn1.length + 1];
                        System.arraycopy(conn1, 0, newEntry, 0, conn1.length);
                        newEntry[newEntry.length - 1] = index3;
                        Arrays.sort(newEntry);
                        entry.getValue().setValue(newEntry);
                    }
                    final short[] conn3 = found.get(index3).getValue();
                    if (Arrays.binarySearch(conn3, index1) < 0) {
                        short[] newEntry = new short[conn3.length + 1];
                        System.arraycopy(conn3, 0, newEntry, 0, conn3.length);
                        newEntry[newEntry.length - 1] = index1;
                        Arrays.sort(newEntry);
                        found.get(index3).setValue(newEntry);
                    }


                }
            }


        }
        if (foundNewRouts == 0) {
            log.info("Finish routing search on level: " + level);
            return;
        }

        calculateRouts(routing, found, level + 1);


    }


}
