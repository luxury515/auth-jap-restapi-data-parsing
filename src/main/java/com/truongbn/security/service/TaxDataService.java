package com.truongbn.security.service;

import static com.truongbn.security.entities.DataEntity.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truongbn.security.dao.response.ResponseData;
import com.truongbn.security.dao.response.ResponseData.DataDto;
import com.truongbn.security.entities.DataEntity;
import com.truongbn.security.entities.User;
import com.truongbn.security.repository.DataRepository;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class TaxDataService {

    final private DataRepository dataRepository;

    public TaxDataService(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Transactional
    public void processTaxData(String jsonString, User user) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        ResponseData response = mapper.readValue(jsonString, ResponseData.class);

        if ("success".equals(response.getStatus())) {
            DataEntity dataEntity = convertToEntity(user,response.getStatus(),response.getData(), mapper);
            dataRepository.save(dataEntity);
            log.info("data 저장 완료!{}", response);
        }

        log.info("responseData{}", response);
    }

    private DataEntity convertToEntity(User user,String status,DataDto dataDto, ObjectMapper mapper) throws JsonProcessingException {
        String deductionJson = mapper.writeValueAsString(dataDto.getDeduction());
        return builder()
                .user(user)
                .status(status)
                .totalIncome(dataDto.getTotalIncome())
                .name(dataDto.getName())
                .deductionJson(deductionJson)
                .build();
    }
}

