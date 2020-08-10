package com.example.distributeconfig.repository;

import com.example.distributeconfig.domain.GlobalConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GlobalConfigurationRepository extends JpaRepository<GlobalConfiguration, Long> {
    Optional<GlobalConfiguration> findByConfKey(String confKey);
}
