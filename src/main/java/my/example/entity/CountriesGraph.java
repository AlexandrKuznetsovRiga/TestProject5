package my.example.entity;

import java.util.HashMap;
import java.util.Map;

public class CountriesGraph {
    private final Map<String, Short> ccIndexMap = new HashMap<>();
    private final Map<Short, String> revCcIndex = new HashMap<>();
    private final Map<Integer, short[]> routing = new HashMap<>();

    public Map<String, Short> getCcIndexMap() {
        return ccIndexMap;
    }

    public Map<Short, String> getRevCcIndex() {
        return revCcIndex;
    }

    public Map<Integer, short[]> getRouting() {
        return routing;
    }
}
