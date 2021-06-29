package com.everis.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.everis.model.Deposit;
import com.everis.repository.IDepositRepository;
import com.everis.repository.IRepository;
import com.everis.service.IDepositService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class DepositService extends CRUDServiceImpl<Deposit, String> implements IDepositService {

  private final String CIRCUIT_BREAKER = "depositServiceCircuitBreaker";
  
  @Value("${msg.error.registro.notfound}")
  private String msgNotFound;
  
  @Value("${msg.error.registro.notfound.update}")
  private String msgNotFoundUpdate;
  
  @Autowired
  private IDepositRepository repository;
  
  @Override
  protected IRepository<Deposit, String> getRepository() {
  
    return repository;
  
  }

  @Override
  @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "findByIdDepositFallback")
  public Mono<Deposit> findByIdDeposit(String id) {
      
    return repository.findById(id)
               .switchIfEmpty(Mono.error(new RuntimeException(msgNotFound)));
               
  }
  
  public Mono<Deposit> findByIdDepositFallback(String id, Exception ex) {
      log.info("ups deposito con id{} no encontrado, retornando fallback",id);
      return Mono.just(Deposit.builder()
              .id(msgNotFound)
              .build());
  }

  @Override
  @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "updateDepositFallback")
  public Mono<Deposit> updateDeposit(Deposit deposit, String id) {
      Mono<Deposit> depositModification = Mono.just(deposit);
      
      Mono<Deposit> depositDatabase = repository.findById(id);
      
      return depositDatabase
              .zipWith(depositModification, (a,b) -> {
                  a.setAmount(deposit.getAmount());
                  a.setAccount(deposit.getAccount());
                  a.setDescription(deposit.getDescription());
                  return a;
              })
              .flatMap(repository::save)
              .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundUpdate)));
               
  }
  
  public Mono<Deposit> updateDepositFallback(Deposit deposit, String id, Exception ex) {
      log.info("ups deposito con id{} no encontrado para actualizar, retornando fallback",id);
      return Mono.just(Deposit.builder()
              .id(msgNotFoundUpdate)
              .build());
  }
  
}
