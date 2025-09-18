package org.example.bookingtower.web.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        // Check if this error is related to logout process
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/logout")) {
            // If error occurred during logout, redirect to login page
            return "redirect:/login?logout=true";
        }

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());

            if (statusCode == 404) {
                // Для 404 ошибок перенаправляем на dashboard
                return "redirect:/dashboard";
            } else if (statusCode == 403) {
                // Для 403 ошибок перенаправляем на страницу доступа запрещен
                return "redirect:/access-denied";
            }
        }

        // Для всех остальных ошибок перенаправляем на dashboard
        return "redirect:/dashboard";
    }
}
