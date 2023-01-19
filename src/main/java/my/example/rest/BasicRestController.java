package my.example.rest;

import my.example.service.CountryGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class BasicRestController {


    @Autowired
    private CountryGraphService graphService;


    @RequestMapping(path = "/routing/{origin}/{destination}")
    public RoutingResponse getRouting(@PathVariable String origin, @PathVariable String destination) {
        List<String> cCodes = graphService.findRoute(origin, destination);
        if (cCodes == null || cCodes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is no land crossing!");
        }
        return new RoutingResponse(cCodes);
    }
}
