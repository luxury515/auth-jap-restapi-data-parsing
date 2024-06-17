package com.truongbn.security.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.truongbn.security.service.ExternalAPIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/szs")
@Slf4j
public class PersonalController {

    private final ExternalAPIService externalAPIService;

    @Autowired
    public PersonalController(ExternalAPIService externalAPIService) {
        this.externalAPIService = externalAPIService;
    }


    @PostMapping("/scrap")
    public ResponseEntity<String> getData() {
        externalAPIService.callExternalAPI();
        log.info("getData------>수행");
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/refund")
    public ResponseEntity<?> refund() throws JsonProcessingException {
        try {
            log.info("refund ------> 수행");
            int totalResult = externalAPIService.calculateTax();
            String formattedTotalResult = String.format("%,d", totalResult);
            // JSON 형식으로 응답
            String resultJson = "{\"결정세액\": \"" + formattedTotalResult + "\"}";
            return ResponseEntity.ok().body(resultJson);
        } catch (JsonProcessingException e) {
            log.error("Error during tax calculation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error calculating tax");
        }
    }
}
