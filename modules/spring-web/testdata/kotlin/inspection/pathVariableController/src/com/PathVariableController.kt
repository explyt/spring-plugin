package com.example.springcoredemo.web

import org.springframework.web.bind.annotation.*

@RestController
class PathVariableController {
    // Simple mapping
    @RequestMapping(path = "/api/employees/{id}/{omittedInUrl}", method = RequestMethod.GET)
    @ResponseBody
    fun getEmployeesById(@PathVariable id: String, @PathVariable omittedInSignature: String?): String {
        return "ID: $id"
    }

    // Specifying name
    @GetMapping("/api/employeeswithvariable/{id}")
    @ResponseBody
    fun getEmployeesByIdWithVariableName(
        @PathVariable(ID_NAMED) employeeId: String
    ): String {
        return "ID: $employeeId"
    }

    // Multiple variables in a map
    @GetMapping("/api/employeeswithmapvariable/{id}/{name}")
    @ResponseBody
    fun getEmployeesByIdAndNameWithMapVariable(
        @PathVariable pathVarsMap: Map<String?, String?>
    ): String {
        val id = pathVarsMap["id"]
        val name = pathVarsMap["name"]
        return if (id != null && name != null) {
            "ID: $id, name: $name"
        } else {
            "Missing Parameters"
        }
    }

    // Not required
    @GetMapping(
        value = ["/api/employeeswithrequiredfalse", "/api/employeeswithrequiredfalse/{id}"
        ]
    )
    @ResponseBody
    fun getEmployeesByIdWithRequiredFalse(
        @PathVariable(required = false) id: String?
    ): String {
        return if (id != null) {
            "ID: $id"
        } else {
            "ID missing"
        }
    }

    // Optional
    @GetMapping(
        value = ["/api/employeeswithoptional", "/api/employeeswithoptional/{id}"
        ]
    )
    @ResponseBody
    fun getEmployeesByIdWithOptional(
        @PathVariable id: java.util.Optional<String?>
    ): String {
        return id.map<String>(java.util.function.Function<String, String> { s: String -> "ID: $s" })
            .orElse("ID missing")
    }

    companion object {
        const val ID_NAMED: String = "id"
    }
}