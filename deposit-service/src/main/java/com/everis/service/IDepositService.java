package com.everis.service;

import com.everis.model.Deposit;

import reactor.core.publisher.Mono;

public interface IDepositService extends ICRUDService<Deposit, String> {
  
  public Mono<Deposit> findByIdDeposit(String id);
  
  public Mono<Deposit> updateDeposit(Deposit deposit,String id);
  
}
