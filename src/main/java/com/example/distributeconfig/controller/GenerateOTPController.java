package com.example.distributeconfig.controller;

import com.example.distributeconfig.domain.OTP;
import com.example.distributeconfig.service.OTPService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GenerateOTPController {
    private final OTPService otpService;

    @PostMapping("/otp")
    public OTP handle() {
        return otpService.create();
    }
}
