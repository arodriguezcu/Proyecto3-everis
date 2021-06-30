package com.everis.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

/**
 * Slf4j : Logback.
 */
@Slf4j
@RestControllerAdvice
public class CustomerControllerAdvice {

  /** Manejo de excepciones. */
  @ExceptionHandler(RuntimeException.class)
  public Mono<ResponseEntity<String>> exceptionHandler(RuntimeException ex) {
  
    log.info("Error : ", ex.getMessage());
    
    return Mono.defer(() -> Mono
        .just(ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ex.getMessage())));
  
  }
  
}
