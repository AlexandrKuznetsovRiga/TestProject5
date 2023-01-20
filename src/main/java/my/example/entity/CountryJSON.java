package my.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Alexandr Kuznetsov (alexandr@power.lv)
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CountryJSON {

    @JsonProperty("cca3")
    public String countryCode;

    @JsonProperty("borders")
    public String[] borders;

}
