package com.anasdidi.uam.config;

import com.anasdidi.common.CommonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UamConfig {

  @Bean
  public ObjectMapper objectMapper() {
    return CommonUtils.prepareObjectMapper();
  }
}
