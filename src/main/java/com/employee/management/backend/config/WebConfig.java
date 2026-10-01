package com.employee.management.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// CORS is configured centrally in security.SecurityConfig, which is CORS-origin-configurable
// via app.cors.allowed-origin; keep this class only if MVC-level customization is needed later.
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
