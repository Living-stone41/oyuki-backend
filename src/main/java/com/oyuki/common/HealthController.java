package com.oyuki.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@Controller
public class HealthController {

    /*
     * Opening the Railway backend URL redirects
     * the visitor to the Oyuki website on QServers.
     */
    @GetMapping("/")
    public RedirectView home() {

        RedirectView redirectView =
                new RedirectView();

        redirectView.setUrl(
                "https://oyukimarketplace.com"
        );

        redirectView.setExposeModelAttributes(false);

        return redirectView;
    }

    /*
     * Railway can use this endpoint to check
     * whether the backend is running.
     */
    @GetMapping("/api/health")
    @ResponseBody
    public Map<String, String> health() {
        return Map.of(
                "status", "healthy",
                "application", "Oyuki Backend",
                "website",
                "https://oyukimarketplace.com"
        );
    }
}