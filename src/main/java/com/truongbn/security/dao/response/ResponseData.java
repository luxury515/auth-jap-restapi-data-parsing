package com.truongbn.security.dao.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseData {

    private String status;
    private DataDto data;
    private Errors errors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataDto {
        @JsonProperty("종합소득금액")
        private int totalIncome;

        @JsonProperty("이름")
        private String name;

        @JsonProperty("소득공제")
        private Deduction deduction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Deduction {
        @JsonProperty("국민연금")
        private List<PensionDeduction> pensionDeductions;

        @JsonProperty("신용카드소득공제")
        private CreditCardDeduction creditCardDeduction;

        @JsonProperty("세액공제")
        private int taxDeduction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PensionDeduction {
        @JsonProperty("월")
        private String month;

        @JsonProperty("공제액")
        private String deductionAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditCardDeduction {
        @JsonProperty("month")
        private List<Map<String, String>> monthlyDeductions;

        @JsonProperty("year")
        private int year;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Errors {
        private String code;
        private String message;
        private String validations;
    }
}
