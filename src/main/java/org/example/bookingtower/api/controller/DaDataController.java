package org.example.bookingtower.api.controller;

import org.example.bookingtower.application.service.DaDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/dadata")
public class DaDataController {

    private static final Logger logger = LoggerFactory.getLogger(DaDataController.class);

    private final DaDataService daDataService;

    @Autowired
    public DaDataController(DaDataService daDataService) {
        this.daDataService = daDataService;
    }

    @GetMapping("/company/{inn}")
    public Mono<ResponseEntity<DaDataService.CompanyInfo>> getCompanyByInn(@PathVariable String inn) {
        logger.info("Received request to lookup company by INN: {}", inn);

        // Validate INN format
        if (inn == null || inn.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        String cleanInn = inn.replaceAll("\\D", "");
        if (cleanInn.length() != 10 && cleanInn.length() != 12) {
            logger.warn("Invalid INN format: {}", inn);
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return daDataService.findCompanyByInn(cleanInn)
                .map(companyInfo -> {
                    if (companyInfo != null) {
                        logger.info("Successfully found company for INN: {}", cleanInn);
                        return ResponseEntity.ok(companyInfo);
                    } else {
                        logger.info("No company found for INN: {}", cleanInn);
                        return ResponseEntity.notFound().<DaDataService.CompanyInfo>build();
                    }
                })
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(error -> logger.error("Error processing company lookup for INN {}: {}", cleanInn, error.getMessage()));
    }

    @PostMapping("/company/lookup")
    public Mono<ResponseEntity<DaDataService.CompanyInfo>> lookupCompany(@RequestBody CompanyLookupRequest request) {
        logger.info("Received company lookup request for INN: {}", request.getInn());

        if (request.getInn() == null || request.getInn().trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        String cleanInn = request.getInn().replaceAll("\\D", "");
        if (cleanInn.length() != 10 && cleanInn.length() != 12) {
            logger.warn("Invalid INN format in request: {}", request.getInn());
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return daDataService.findCompanyByInn(cleanInn)
                .map(companyInfo -> {
                    if (companyInfo != null) {
                        return ResponseEntity.ok(companyInfo);
                    } else {
                        return ResponseEntity.notFound().<DaDataService.CompanyInfo>build();
                    }
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Request DTO
    public static class CompanyLookupRequest {
        private String inn;

        public CompanyLookupRequest() {}

        public CompanyLookupRequest(String inn) {
            this.inn = inn;
        }

        public String getInn() {
            return inn;
        }

        public void setInn(String inn) {
            this.inn = inn;
        }
    }
}