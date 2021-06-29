package com.everis.service;

import com.everis.model.CreditPayment;

import reactor.core.publisher.Mono;

public interface ICreditPaymentService extends ICRUDService<CreditPayment, String> {

  Mono<CreditPayment> findByIdCreditPayment(String id);

}
