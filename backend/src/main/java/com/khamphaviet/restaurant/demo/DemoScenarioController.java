package com.khamphaviet.restaurant.demo;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/staff/demo-scenarios")
public class DemoScenarioController {
    private final DemoScenarioService service;

    public DemoScenarioController(DemoScenarioService service) { this.service=service; }

    @PostMapping
    public DemoScenarioDtos.CreateResponse create(@Valid @RequestBody DemoScenarioDtos.CreateRequest request,
                                                   Authentication authentication) {
        return service.create(request,authentication.getName());
    }
}
