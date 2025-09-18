package org.example.bookingtower.application.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class DaDataService {

    private static final Logger logger = LoggerFactory.getLogger(DaDataService.class);
    private static final String DADATA_API_URL = "https://suggestions.dadata.ru/suggestions/api/4_1/rs/findById/party";
    
    private final WebClient webClient;
    private final String apiKey;
    private final String secretKey;

    public DaDataService(@Value("${app.dadata.api-key:}") String apiKey,
                        @Value("${app.dadata.secret-key:}") String secretKey) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.webClient = WebClient.builder()
                .baseUrl(DADATA_API_URL)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    public Mono<CompanyInfo> findCompanyByInn(String inn) {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.warn("DaData API key is not configured");
            return Mono.empty();
        }

        logger.info("Looking up company by INN: {}", inn);

        Map<String, String> requestBody = Map.of("query", inn);

        return webClient.post()
                .header("Authorization", "Token " + apiKey)
                .header("X-Secret", secretKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(DaDataResponse.class)
                .timeout(Duration.ofSeconds(5))
                .map(this::mapToCompanyInfo)
                .doOnSuccess(companyInfo -> {
                    if (companyInfo != null) {
                        logger.info("Found company: {}", companyInfo.getName());
                    } else {
                        logger.info("No company found for INN: {}", inn);
                    }
                })
                .doOnError(error -> logger.error("Error looking up company by INN {}: {}", inn, error.getMessage()))
                .onErrorReturn(null);
    }

    private CompanyInfo mapToCompanyInfo(DaDataResponse response) {
        if (response == null || response.getSuggestions() == null || response.getSuggestions().isEmpty()) {
            return null;
        }

        DaDataSuggestion suggestion = response.getSuggestions().get(0);
        DaDataCompany data = suggestion.getData();

        if (data == null) {
            return null;
        }

        CompanyInfo companyInfo = new CompanyInfo();
        companyInfo.setName(data.getName() != null ? data.getName().getFullWithOpf() : null);
        companyInfo.setShortName(data.getName() != null ? data.getName().getShort() : null);
        companyInfo.setInn(data.getInn());
        companyInfo.setOgrn(data.getOgrn());
        companyInfo.setKpp(data.getKpp());
        
        if (data.getAddress() != null) {
            companyInfo.setLegalAddress(data.getAddress().getValue());
        }
        
        if (data.getManagement() != null && !data.getManagement().isEmpty()) {
            DaDataManagement management = data.getManagement().get(0);
            companyInfo.setDirectorName(management.getName());
            companyInfo.setDirectorPost(management.getPost());
        }

        return companyInfo;
    }

    // DTO classes for DaData API response
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DaDataResponse {
        private List<DaDataSuggestion> suggestions;

        public List<DaDataSuggestion> getSuggestions() {
            return suggestions;
        }

        public void setSuggestions(List<DaDataSuggestion> suggestions) {
            this.suggestions = suggestions;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DaDataSuggestion {
        private String value;
        private DaDataCompany data;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public DaDataCompany getData() {
            return data;
        }

        public void setData(DaDataCompany data) {
            this.data = data;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DaDataCompany {
        private String inn;
        private String ogrn;
        private String kpp;
        private DaDataName name;
        private DaDataAddress address;
        private List<DaDataManagement> management;

        public String getInn() {
            return inn;
        }

        public void setInn(String inn) {
            this.inn = inn;
        }

        public String getOgrn() {
            return ogrn;
        }

        public void setOgrn(String ogrn) {
            this.ogrn = ogrn;
        }

        public String getKpp() {
            return kpp;
        }

        public void setKpp(String kpp) {
            this.kpp = kpp;
        }

        public DaDataName getName() {
            return name;
        }

        public void setName(DaDataName name) {
            this.name = name;
        }

        public DaDataAddress getAddress() {
            return address;
        }

        public void setAddress(DaDataAddress address) {
            this.address = address;
        }

        public List<DaDataManagement> getManagement() {
            return management;
        }

        public void setManagement(List<DaDataManagement> management) {
            this.management = management;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DaDataName {
        @JsonProperty("full_with_opf")
        private String fullWithOpf;
        @JsonProperty("short_with_opf")
        private String shortWithOpf;
        @JsonProperty("short")
        private String shortName;

        public String getFullWithOpf() {
            return fullWithOpf;
        }

        public void setFullWithOpf(String fullWithOpf) {
            this.fullWithOpf = fullWithOpf;
        }

        public String getShortWithOpf() {
            return shortWithOpf;
        }

        public void setShortWithOpf(String shortWithOpf) {
            this.shortWithOpf = shortWithOpf;
        }

        public String getShort() {
            return shortName;
        }

        public void setShort(String shortName) {
            this.shortName = shortName;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DaDataAddress {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DaDataManagement {
        private String name;
        private String post;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPost() {
            return post;
        }

        public void setPost(String post) {
            this.post = post;
        }
    }

    // Response DTO for frontend
    public static class CompanyInfo {
        private String name;
        private String shortName;
        private String inn;
        private String ogrn;
        private String kpp;
        private String legalAddress;
        private String directorName;
        private String directorPost;

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getShortName() {
            return shortName;
        }

        public void setShortName(String shortName) {
            this.shortName = shortName;
        }

        public String getInn() {
            return inn;
        }

        public void setInn(String inn) {
            this.inn = inn;
        }

        public String getOgrn() {
            return ogrn;
        }

        public void setOgrn(String ogrn) {
            this.ogrn = ogrn;
        }

        public String getKpp() {
            return kpp;
        }

        public void setKpp(String kpp) {
            this.kpp = kpp;
        }

        public String getLegalAddress() {
            return legalAddress;
        }

        public void setLegalAddress(String legalAddress) {
            this.legalAddress = legalAddress;
        }

        public String getDirectorName() {
            return directorName;
        }

        public void setDirectorName(String directorName) {
            this.directorName = directorName;
        }

        public String getDirectorPost() {
            return directorPost;
        }

        public void setDirectorPost(String directorPost) {
            this.directorPost = directorPost;
        }
    }
}