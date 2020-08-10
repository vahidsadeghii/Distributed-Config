package com.example.distributeconfig.config.cache;

import com.example.distributeconfig.exception.UpdateEnterpriseCacheException;
import com.example.distributeconfig.domain.GlobalConfiguration;
import com.example.distributeconfig.repository.GlobalConfigurationRepository;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedConfigManager {
    @Value("${etcd.endpoints}")
    private String etcdEndPoints;

    @Value("${etcd.key-prefix}")
    private String enterpriseConfigKeyPrefix;

    @Data
    @Builder
    public static class ConfigValue {
        private String value;
        private LocalDateTime updateDate;
    }

    private Map<String, ConfigValue> localCache = new HashMap<>();

    private final GlobalConfigurationRepository globalConfigurationRepository;

    @Bean
    public KV kv() {
        return client().getKVClient();
    }

    @Bean
    public Client client() {
        return Client.builder().endpoints(etcdEndPoints).build();
    }


    @PostConstruct
    public void initLocalCache() {
        globalConfigurationRepository.findAll().forEach(
                globalConfiguration ->
                        localCache.put(globalConfiguration.getConfKey(),
                                ConfigValue.builder()
                                        .value(globalConfiguration.getConfValue())
                                        .updateDate(LocalDateTime.now())
                                        .build()
                        )
        );

        refreshEnterpriseCache();
        startWatchingEnterpriseCache();
    }

    private String toEnterpriseKey(String key) {
        return (enterpriseConfigKeyPrefix + key);
    }

    private String fromEnterpriseKey(String key) {
        return key.substring(key.indexOf(enterpriseConfigKeyPrefix) + enterpriseConfigKeyPrefix.length());
    }

    public Optional<ConfigValue> getConfigValue(String key) {
        return Optional.ofNullable(localCache.get(key));
    }

    public void updateConfigValue(String key, String value) {
        updateConfigValue(key, value, true);
    }

    private void updateConfigValue(String key, String value, Boolean updateEnterpriseCache) {
        localCache.put(key,
                ConfigValue.builder()
                        .value(value)
                        .updateDate(LocalDateTime.now())
                        .build());

        if (updateEnterpriseCache) {
            try {
                kv().put(ByteSequence.from(toEnterpriseKey(key).getBytes()), ByteSequence.from(value.getBytes())).get();

            } catch (Exception e) {
                log.error("Error in update config", e);

                throw new UpdateEnterpriseCacheException();
            }
        }

        updateStorage(key, value);
    }

    private void updateStorage(String key, String value) {
        globalConfigurationRepository.findByConfKey(key).ifPresentOrElse(
                globalConfiguration -> {
                    globalConfiguration.setConfValue(value);
                    globalConfigurationRepository.save(globalConfiguration);
                }, () -> globalConfigurationRepository.save(
                        GlobalConfiguration.builder()
                                .confKey(key)
                                .confValue(value)
                                .build()
                )
        );
    }

    private void refreshEnterpriseCache() {
        localCache.forEach(
                (key, value) -> {
                    try {
                        kv().put(ByteSequence.from(toEnterpriseKey(key).getBytes()),
                                ByteSequence.from(value.value.getBytes())).get();
                    } catch (Exception e) {
                        log.error("Error in refresh cache ", e);
                        throw new UpdateEnterpriseCacheException();
                    }
                }
        );
    }

    private void startWatchingEnterpriseCache() {
        Watch.Listener listener = Watch.listener(watchResponse -> watchResponse.getEvents().forEach(
                watchEvent -> {
                    if (watchEvent.getEventType() == WatchEvent.EventType.PUT) {
                        String key = fromEnterpriseKey(new String(watchEvent.getKeyValue().getKey().getBytes()));
                        String value = new String(watchEvent.getKeyValue().getValue().getBytes());

                        ConfigValue configValue = localCache.get(key);

                        if (configValue == null || !value.equals(configValue.value)) {
                            updateConfigValue(key, value, false);
                        }
                    }
                }));

        client().getWatchClient().watch(
                ByteSequence.from("".getBytes()),
                WatchOption.newBuilder().withPrefix(
                        ByteSequence.from(enterpriseConfigKeyPrefix.getBytes())
                ).build(), listener
        );
    }
}
