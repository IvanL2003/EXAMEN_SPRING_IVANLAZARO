package com.salesianos.consultame.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GraficosController {

    @GetMapping("/graficos")
    public String graficos() {
        return "graficos/dashboard";
    }
}
