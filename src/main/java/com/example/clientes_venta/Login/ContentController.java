package com.example.clientes_venta.Login;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ContentController {
    
    @GetMapping("/login")
    public String login(){
        return "login";
    }

    @GetMapping("/req/signup")
    public String signup(){
        return "registro";
    }

    /*@GetMapping("/home")
    public String landingPage(){
        return "landing";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }
    /*
    @GetMapping("/config")
    public String config(){
        return "config";
    }*/

}