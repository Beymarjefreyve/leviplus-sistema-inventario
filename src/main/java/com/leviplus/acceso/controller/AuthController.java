package com.leviplus.acceso.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** RF-01. Pantalla de acceso; la autenticacion la procesa Spring Security en POST /login. */
@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "acceso/login";
    }
}
