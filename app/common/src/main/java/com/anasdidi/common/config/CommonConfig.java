package com.anasdidi.common.config;

import com.anasdidi.common.CommonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class CommonConfig {

  @Bean
  ObjectMapper objectMapper() {
    return CommonUtils.prepareObjectMapper();
  }

  @Bean
  AuditorAware<String> auditorAware() {
    return () -> Optional.ofNullable("SYSTEM");
  }
}
