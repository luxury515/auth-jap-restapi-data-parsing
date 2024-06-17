package com.truongbn.security.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.truongbn.security.entities.DataEntity;
import com.truongbn.security.entities.User;
import com.truongbn.security.repository.DataRepository;
import com.truongbn.security.repository.UserRepository;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class ExternalAPIService {

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final TaxDataService taxDataService;
    private final DataRepository dataRepository;

    public ExternalAPIService(RestTemplateBuilder restTemplateBuilder,
            UserRepository userRepository,
            TaxDataService taxDataService, DataRepository dataRepository) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMinutes(1))
                .setReadTimeout(Duration.ofMinutes(1))
                .build();
        this.userRepository = userRepository;
        this.taxDataService = taxDataService;
        this.dataRepository = dataRepository;
    }
    public int calculateTax() throws JsonProcessingException {
        String userId = getLoggedInUserName();
        Optional<DataEntity> optional = dataRepository.findByUserId(userId);
        if (optional.isPresent()) {
            DataEntity dataEntity = optional.get();
            String deductionJson = dataEntity.getDeductionJson();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(deductionJson);

            // 종합소득금액
            int userIncomeTotal = dataEntity.getTotalIncome();

            // 소득공제 합산 (국민연금 + 신용카드소득공제)
            double totalDeduction = calculateTotalDeduction(rootNode);

            // 과세표준 = 종합소득금액 - 소득공제
            int taxableIncome = (int) (userIncomeTotal - totalDeduction);

            // 산출세액 계산
            int taxAmount = calculateTaxAmount(taxableIncome);

            //세액공제
            JsonNode 세액공제 = rootNode.get("세액공제");
            // 결과 출력
            log.info("종합소득금액: {}", userIncomeTotal);
            log.info("총 소득공제(국민연금 + 신용카드소득공제): {}", totalDeduction);
            log.info("세액 공제",세액공제);
            log.info("과세표준: {}", taxableIncome);
            log.info("산출세액: {}", taxAmount);

            //결정세액 = 산출세액 - 세액공제
            int totalResult =  taxAmount-Integer.parseInt(String.valueOf(세액공제));
            log.info("총 결정세액: {}",totalResult);

            //{
            //" ": "150,000"
            //}
            return totalResult;
        }
        return 0;
    }

    private double calculateTotalDeduction(JsonNode rootNode) {
        double totalDeduction = 0.0;

        // 국민연금 공제액 합산
        JsonNode 국민연금Array = rootNode.get("국민연금");
        if (국민연금Array != null && 국민연금Array.isArray()) {
            for (JsonNode node : 국민연금Array) {
                JsonNode 공제액Node = node.get("공제액");
                if (공제액Node != null) {
                    totalDeduction += Double.parseDouble(공제액Node.asText().replace(",", ""));
                }
            }
        }

        // 신용카드소득공제 공제액 합산
        JsonNode 신용카드소득공제Object = rootNode.get("신용카드소득공제");
        JsonNode 신용카드소득공제Array = 신용카드소득공제Object.get("month");
        if (신용카드소득공제Array != null && 신용카드소득공제Array.isArray()) {
            for (JsonNode node : 신용카드소득공제Array) {
                JsonNode 공제액Node = node.elements().next();
                if (공제액Node != null) {
                    totalDeduction += Double.parseDouble(공제액Node.asText().replace(",", ""));
                }
            }
        }

        return totalDeduction;
    }

    private int calculateTaxAmount(int taxableIncome) {
        int taxAmount;

        if (taxableIncome <= 14000000) {
            // 1,400만원 이하
            taxAmount = (int) (taxableIncome * 0.06);
        } else if (taxableIncome <= 50000000) {
            // 1,400만원 초과 ~ 5,000만원 이하
            taxAmount = (int) (840000 + (taxableIncome - 14000000) * 0.15);
        } else if (taxableIncome <= 88000000) {
            // 5,000만원 초과 ~ 8,800만원 이하
            taxAmount = (int) (6240000 + (taxableIncome - 50000000) * 0.24);
        } else if (taxableIncome <= 150000000) {
            // 8,800만원 초과 ~ 1억5천만원 이하
            taxAmount = (int) (15360000 + (taxableIncome - 88000000) * 0.35);
        } else if (taxableIncome <= 300000000) {
            // 1억5천만원 초과 ~ 3억 이하
            taxAmount = (int) (37060000 + (taxableIncome - 150000000) * 0.38);
        } else if (taxableIncome <= 500000000) {
            // 3억 원 초과 ~ 5억 이하
            taxAmount = (int) (94060000 + (taxableIncome - 300000000) * 0.4);
        } else if (taxableIncome <= 1000000000) {
            // 5억 원 초과 ~ 10억 원 이하
            taxAmount = (int) (174060000 + (taxableIncome - 500000000) * 0.42);
        } else {
            // 10억 원 초과
            taxAmount = (int) (384060000 + (taxableIncome - 1000000000) * 0.45);
        }

        return taxAmount;
    }
//    public void getRefund() throws JsonProcessingException {
//        String userId = getLoggedInUserName();
//        Optional<DataEntity> optional = dataRepository.findByUserId(userId);
//        if (optional.isPresent()) {
//            DataEntity dataEntity = optional.get();
//            String data = optional.get().getDeductionJson();
//            String deductionJson = dataEntity.getDeductionJson();
//
//            ObjectMapper mapper = new ObjectMapper();
//            JsonNode rootNode = mapper.readTree(deductionJson);
//            //종합소득금액
//            int userIncomeTotal = dataEntity.getTotalIncome();
//            //소득공제
//            //"국민연금","신용카드소득공제","세액공제";
//            JsonNode 국민연금Node = rootNode.get("국민연금");
//            JsonNode 신용카드소득공제Node = rootNode.get("신용카드소득공제");
//            JsonNode 세액공제Node = rootNode.get("세액공제");
//
//            // 국민연금 공제액 합산
//            JsonNode 국민연금Array = rootNode.get("국민연금");
//            double 총국민연금 = 0.0;
//            if (국민연금Array != null && 국민연금Array.isArray()) {
//                for (JsonNode node : 국민연금Array) {
//                    JsonNode 공제액Node = node.get("공제액");
//                    if (공제액Node != null) {
//                        총국민연금 += Double.parseDouble(공제액Node.asText().replace(",", ""));
//                    }
//                }
//            }
//            log.info("총 국민연금 공제액: {}" + 총국민연금);
//
//            // 신용카드소득공제 공제액 합산
//            JsonNode 신용카드소득공제Object = rootNode.get("신용카드소득공제");
//            JsonNode 신용카드소득공제Array = 신용카드소득공제Object.get("month");
//            double 총신용카드소득공제 = 0.0;
//            if (신용카드소득공제Array != null && 신용카드소득공제Array.isArray()) {
//                for (JsonNode node : 신용카드소득공제Array) {
//                    JsonNode 공제액Node = node.elements().next();
//                    if (공제액Node != null) {
//                        총신용카드소득공제 += Double.parseDouble(공제액Node.asText().replace(",", ""));
//                    }
//                }
//            }
//            log.info("총 신용카드소득공제 공제액: {}" + 총신용카드소득공제);
//
//
//            //소득공제 (api 받은 데이터 ----->  총국민연금 + 총신용카드소득공제)
//            log.info("총 국민연금 및 신용카드소득공제 합계: {}" + (총국민연금 + 총신용카드소득공제));
//
//            //과세표준 = 종합소득 - 소득공제
//            int 과세표준 = (int) (userIncomeTotal - (총국민연금 + 총신용카드소득공제));
//            log.info("과세표준-------> {}",과세표준);
//
//
//
//        }
//    }

    public void callExternalAPI() {
        HttpHeaders headers = new HttpHeaders();
        String apiKey = "aXC8zK6puHIf9l53L8TiQg==";
        headers.set("x-api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Building request body
        String userId = getLoggedInUserName();
        Optional<User> optional = userRepository.findByUserId(userId);
        JsonObject jsonObject = new JsonObject();

        optional.ifPresent(user -> {
            if (user.getRegNo() != null) {
                jsonObject.addProperty("name", user.getName());
                jsonObject.addProperty("regNo", user.getRegNo());
            }
        });

        String requestBody = jsonObject.toString();

        // Creating HttpEntity with headers and body
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // Making the API call
        try {
            String externalUrl = "https://codetest-v4.3o3.co.kr/scrap";
            ResponseEntity<String> response = restTemplate.exchange(
                    externalUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("External API Response: {}", response.getBody());
            String jsonString = response.getBody();
            taxDataService.processTaxData(jsonString, optional.get());


        } catch (HttpServerErrorException ex) {
            log.info("External API Error: {} - {}", ex.getStatusCode(), ex.getStatusText());
            log.info("Response body: {}", ex.getResponseBodyAsString());
            // 예외 처리 로직 추가
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getLoggedInUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            } else {
                return principal.toString();
            }
        }
        return null;
    }
}
