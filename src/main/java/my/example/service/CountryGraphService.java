package my.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import my.example.entity.CountriesGraph;
import my.example.entity.CountryJSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Alexandr Kuznetsov (alexandr@power.lv)
 */
@Component
public class CountryGraphService {
    private final static Log log = LogFactory.getLog(CountryGraphService.class);

    @Autowired
    private PathFinderImpl1 pathFinderImpl1;

    @Autowired
    private PathFinderImpl2 pathFinderImpl2;

    @Autowired
    private PathFinderImpl3 pathFinderImpl3;

    @Value("${my.sourceUrl}")
    private String sourceUrl;

    private CountriesGraph graph;


    @PostConstruct
    public void _init() throws InterruptedException {
        while (true) {
            try {
                List<CountryJSON> data = downloadRemoteData();
                graph = calculate(data);
                break;
            } catch (Throwable e) {
                log.warn("Can not initialize Service, sleep for 5 seconds", e);
                TimeUnit.SECONDS.sleep(5);
            }
        }
    }

    private CountriesGraph calculate(List<CountryJSON> inputData) {

        long start = System.nanoTime();

        CountriesGraph graph1 = pathFinderImpl1.parseData(inputData);
        log.info("Pathfinder1: " + TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start));
        log.info("Pathfinder1 found " + graph1.getRouting().size() + " connections");

        start = System.nanoTime();

        CountriesGraph graph3 = pathFinderImpl3.parseData(inputData);
        log.info("Pathfinder3: " + TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start));
        log.info("Pathfinder3 found " + graph3.getRouting().size() + " connections");

        start = System.nanoTime();

        CountriesGraph graph2 = pathFinderImpl2.parseData(inputData);
        log.info("Pathfinder2: " + TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start));
        log.info("Pathfinder2 found " + graph2.getRouting().size() + " connections");




        int nonEqualRouting = 0;
        int notFound = 0;
        for (Map.Entry<Integer, short[]> entry : graph3.getRouting().entrySet()) {

            final short[] indexes2 = HashUtil.extractCodes(entry.getKey());

            final String cc2A = graph3.getRevCcIndex().get(indexes2[0]);
            final String cc2B = graph3.getRevCcIndex().get(indexes2[1]);

            final short index1A = graph1.getCcIndexMap().get(cc2A);
            final short index1B = graph1.getCcIndexMap().get(cc2B);


            final short[] routing2 = pathFinderImpl3.getDirectionalRoute(graph3.getRouting(), indexes2[0], indexes2[1]);
            final short[] routing1 = pathFinderImpl1.getDirectionalRoute(graph1.getRouting(), index1A, index1B);

            if (routing1 == null) {
                log.info("Not found: [" + cc2A + "]-[" + cc2B + "]: ");
                List<String> r2 = toRoute(routing2, graph3);
                log.info(StringUtils.join(r2, ","));
                notFound++;
                if (notFound > 100) {
                    break;
                }

            } else {
                List<String> r2 = toRoute(routing2, graph3);
                List<String> r1 = toRoute(routing1, graph1);
                if (!r2.equals(r1) && r1.size() == r2.size()) {
                    log.info("Non equal: [" + cc2A + "]-[" + cc2B + "]: ");
                    log.info(StringUtils.join(r2, ","));
                    log.info(StringUtils.join(r1, ","));


                    nonEqualRouting++;
                    if (nonEqualRouting > 100)
                        break;
                }

            }

        }
        log.info("Non equal routs: " + nonEqualRouting);

        return graph2;
    }


    private List<CountryJSON> downloadRemoteData() throws JsonProcessingException {
        log.info("Source: " + sourceUrl);

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ResponseEntity<String> response = restTemplate.getForEntity(sourceUrl, String.class);

        List<CountryJSON> countries = objectMapper.readValue(response.getBody(), new TypeReference<List<CountryJSON>>() {
        });

        return countries;


    }


    public List<String> findRoute(String ccOrigin, String ccDestination) {
        if (!graph.getCcIndexMap().containsKey(ccOrigin)) {
            throw new IllegalArgumentException("Invalid country code: " + ccOrigin);
        }
        if (!graph.getCcIndexMap().containsKey(ccDestination)) {
            throw new IllegalArgumentException("Invalid country code: " + ccDestination);
        }
        short index1 = graph.getCcIndexMap().get(ccOrigin);
        short index2 = graph.getCcIndexMap().get(ccDestination);


        final short[] route = pathFinderImpl2.getDirectionalRoute(graph.getRouting(), index1, index2);
        if (null == route) return null;
        final List<String> routeCodes = new ArrayList<>();
        routeCodes.add(ccOrigin);
        for (short index : route) {
            String code = graph.getRevCcIndex().get(index);
            routeCodes.add(code);
        }
        routeCodes.add(ccDestination);


        return routeCodes;
    }

    public List<String> toRoute(short[] route, CountriesGraph graph) {
        final List<String> routeCodes = new ArrayList<>();

        for (short index : route) {
            String code = graph.getRevCcIndex().get(index);
            routeCodes.add(code);
        }

        return routeCodes;
    }
}
