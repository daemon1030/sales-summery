package com.example.sales_summery.global.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendRouteController {

    @GetMapping({"/service", "/service/"})
    public String service() {
        return "forward:/service/index.html";
    }
}
