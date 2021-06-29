package com.everis.service;

import com.everis.model.Withdrawal;

import reactor.core.publisher.Mono;

public interface IWithdrawalService extends ICRUDService<Withdrawal, String>{
  
  Mono<Withdrawal> findByIdWithdrawal(String id);

}
