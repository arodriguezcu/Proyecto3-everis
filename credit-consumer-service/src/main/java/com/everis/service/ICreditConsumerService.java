package com.everis.service;

import com.everis.model.CreditConsumer;

import reactor.core.publisher.Mono;

public interface ICreditConsumerService extends ICRUDService<CreditConsumer, String> {

  Mono<CreditConsumer> findByIdCreditConsumer(String id);
  
}
