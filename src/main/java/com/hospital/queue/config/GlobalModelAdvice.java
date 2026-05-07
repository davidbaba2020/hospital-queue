package com.hospital.queue.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute
    public void addRequestUri(HttpServletRequest request, Model model) {
        model.addAttribute("requestUri", request.getRequestURI());
    }
}
