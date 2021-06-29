package com.everis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class CreditConsumerServiceApplication {

  public static void main(String[] args) {
  SpringApplication.run(CreditConsumerServiceApplication.class, args);
  }

}
