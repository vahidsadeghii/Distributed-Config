package com.example.distributeconfig.service.implement;

import com.example.distributeconfig.config.cache.DistributedConfigManager;
import com.example.distributeconfig.domain.GlobalConfiguration;
import com.example.distributeconfig.domain.OTP;
import com.example.distributeconfig.service.OTPService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OTPServiceImpl implements OTPService {
    private final DistributedConfigManager distributedConfigManager;

    @Override
    public OTP create() {
        Integer otpLen = distributedConfigManager.getConfigValue(GlobalConfiguration.Keys.OTP_LEN.name())
                .map(configValue -> Integer.parseInt(configValue.getValue())).orElse(6);

        Integer otpExpInSeconds = distributedConfigManager.getConfigValue(GlobalConfiguration.Keys.OTP_EXP_IN_SECONDS.name())
                .map(configValue -> Integer.parseInt(configValue.getValue())).orElse(120);

        return OTP.builder()
                .value(RandomStringUtils.randomNumeric(otpLen))
                .createDate(LocalDateTime.now())
                .expireDate(LocalDateTime.now().plusSeconds(otpExpInSeconds))
                .build();
    }
}
