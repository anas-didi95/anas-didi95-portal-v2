package com.anasdidi.uam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.anasdidi")
public class UamApplication {

  public static void main(String[] args) {
    SpringApplication.run(UamApplication.class, args);
  }
}
