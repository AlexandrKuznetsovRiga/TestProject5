package my.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import my.example.entity.CountryJSON;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class CountryGraphService {
    private final static Log log = LogFactory.getLog(CountryGraphService.class);

    @Value("${my.sourceUrl}")
    private String sourceUrl;

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
            log.info("Country: " + ccj.countryCode);
        }
    }

    public List<String> findRoute(String ccOrigin, String ccDestination) {
        return List.of(ccOrigin, "UNKNONW", "SOMEWHERE", ccDestination);
    }
}
