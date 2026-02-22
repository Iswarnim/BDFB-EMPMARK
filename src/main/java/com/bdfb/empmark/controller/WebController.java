package com.bdfb.empmark;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
    @GetMapping("/")
    public String index() {
        // This tells Spring to look for index.html in the static folder
        return "forward:/index.html";
    }
}
