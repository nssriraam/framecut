package com.framecut.controller;

import com.framecut.dto.AuthRequest;
import com.framecut.dto.RegisterRequest;
import com.framecut.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("authRequest", new AuthRequest());
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute AuthRequest request,
                        BindingResult result,
                        HttpServletRequest httpRequest,
                        HttpServletResponse response,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/login";
        }
        try {
            String token = authService.login(request);
            boolean isSecure = httpRequest.isSecure() || "https".equalsIgnoreCase(httpRequest.getHeader("X-Forwarded-Proto"));
            ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(86400) // 24 hours
                .sameSite("Lax")
                .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", "Invalid username or password");
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest request,
                           BindingResult result,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        try {
            authService.register(request);
            redirectAttributes.addFlashAttribute("successParam", true);
            return "redirect:/auth/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest httpRequest, HttpServletResponse response) {
        boolean isSecure = httpRequest.isSecure() || "https".equalsIgnoreCase(httpRequest.getHeader("X-Forwarded-Proto"));
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
            .httpOnly(true)
            .secure(isSecure)
            .path("/")
            .maxAge(0)
            .sameSite("Lax")
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return "redirect:/";
    }
}
