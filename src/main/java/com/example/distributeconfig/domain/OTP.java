package com.example.distributeconfig.domain;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OTP {
    private String value;
    private LocalDateTime createDate;
    private LocalDateTime expireDate;
}
