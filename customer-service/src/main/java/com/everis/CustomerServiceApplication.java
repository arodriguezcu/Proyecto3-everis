package com.everis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * Configuracion Automatica.
 */
@SpringBootApplication
@EnableEurekaClient
public class CustomerServiceApplication {

  /** Principal. */
  public static void main(String[] args) {
  
    SpringApplication.run(CustomerServiceApplication.class, args);
  
  }

}
