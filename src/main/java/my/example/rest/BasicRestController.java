package my.example.rest;

import my.example.service.CountryGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 * @author Alexandr Kuznetsov (alexandr@power.lv)
 */
@RestController
public class BasicRestController {


    @Autowired
    private CountryGraphService graphService;


    @RequestMapping(path = "/routing/{origin}/{destination}")
    public RoutingResponse getRouting(@PathVariable String origin, @PathVariable String destination, Model model) {
        List<String> cCodes = null;
        try {
            cCodes = graphService.findRoute(origin, destination);
        } catch (IllegalArgumentException e) {

            model.addAttribute("ERROR_MESSAGE", "Invalid input!");
            model.addAttribute("ERROR_MESSAGE_en_US", "Invalid input!");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        if (cCodes == null || cCodes.isEmpty()) {
            model.addAttribute("ERROR_MESSAGE", "There is no land crossing!");
            model.addAttribute("ERROR_MESSAGE_en_US", "There is no land crossing!");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is no land crossing!");

        }

        return new RoutingResponse(cCodes);
    }
}
