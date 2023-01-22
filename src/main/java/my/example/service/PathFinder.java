package my.example.service;

import my.example.entity.CountriesGraph;
import my.example.entity.CountryJSON;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public interface PathFinder {
    short[] DIRECT_CONNECTION = {};

    CountriesGraph parseData(List<CountryJSON> countries);

    default short[] getDirectionalRoute(final Map<Integer, short[]> routing, short fromIndex, short toIndex) {
        final int hash = HashUtil.genHash(fromIndex, toIndex);
        final short[] route = routing.get(hash);
        if (null == route) return null;
        return fromIndex < toIndex ? route : reverse(route);
    }

    default short[] reverse(short[] arr) {
        short[] arrRev = new short[arr.length];
        for (int i = 0; i < arr.length; i++) {
            arrRev[arrRev.length - (i + 1)] = arr[i];
        }
        return arrRev;
    }

    default short toIndex(final String countryCode, final Map<String, Short> ccIndexMap, final Map<Short, String> revCcIndex, final AtomicInteger counter) {
        return ccIndexMap.computeIfAbsent(countryCode, k -> {
            short index = (short) counter.incrementAndGet();
            revCcIndex.put(index, countryCode);
            return index;

        });
    }
}
