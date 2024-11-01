package com.example.springcoredemo.web;

import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
public class PathVariableController {
    final static String ID_NAMED = "id";
    
    // Simple mapping
    @RequestMapping(path = "/api/employees/{id}/{omittedInUrl}", method = RequestMethod.GET)
    @ResponseBody
    public String getEmployeesById(@PathVariable String id, @PathVariable String omittedInSignature) {
        return "ID: " + id;
    }
    
    // Specifying name
    @GetMapping("/api/employeeswithvariable/{id}")
    @ResponseBody
    public String getEmployeesByIdWithVariableName(
            @PathVariable(ID_NAMED) String employeeId
    ) {
        return "ID: " + employeeId;
    }
    
    // Multiple variables in a map
    @GetMapping("/api/employeeswithmapvariable/{id}/{name}")
    @ResponseBody
    public String getEmployeesByIdAndNameWithMapVariable(
            @PathVariable Map<String, String> pathVarsMap
    ) {
        String id = pathVarsMap.get("id");
        String name = pathVarsMap.get("name");
        if (id != null && name != null) {
            return "ID: " + id + ", name: " + name;
        } else {
            return "Missing Parameters";
        }
    }
    
    // Not required
    @GetMapping(value = {
            "/api/employeeswithrequiredfalse",
            "/api/employeeswithrequiredfalse/{id}"
    })
    @ResponseBody
    public String getEmployeesByIdWithRequiredFalse(
            @PathVariable(required = false) String id
    ) {
        if (id != null) {
            return "ID: " + id;
        } else {
            return "ID missing";
        }
    }
    
    // Optional
    @GetMapping(value = {
            "/api/employeeswithoptional",
            "/api/employeeswithoptional/{id}"
    })
    @ResponseBody
    public String getEmployeesByIdWithOptional(
            @PathVariable Optional<String> id
    ) {
        return id.map(s -> "ID: " + s)
                .orElse("ID missing");
    }
    
}
