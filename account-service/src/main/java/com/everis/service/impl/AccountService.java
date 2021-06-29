package com.everis.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.everis.model.Account;
import com.everis.repository.IAccountRepository;
import com.everis.repository.IRepository;
import com.everis.service.IAccountService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class AccountService extends CRUDServiceImpl<Account, String> implements IAccountService {

  private final String CIRCUIT_BREAKER = "accountServiceCircuitBreaker";
  
  @Value("${msg.error.registro.notfound}")
  private String msgNotFound;
  
  @Value("${msg.error.registro.notfound.update}")
  private String msgNotFoundUpdate;
  
  @Autowired
  private IAccountRepository repository;
  
  @Override
  protected IRepository<Account, String> getRepository() {
  
    return repository;
  
  }

  @Override
  public Mono<Account> findByAccountNumber(String accuntNumber) {

    return repository.findByAccountNumber(accuntNumber);
  
  }

  @Override
  @CircuitBreaker(name = CIRCUIT_BREAKER, fallbackMethod = "updateAccountFallback")  
  public Mono<Account> updateAccount(Account account, String id) {
    
    Mono<Account> customerModification = Mono.just(account);
    
    Mono<Account> customerDatabase = repository.findById(id);
    
    return customerDatabase
        .zipWith(customerModification, (a,b) -> {
          
          a.setMaintenance_charge(account.getMaintenance_charge());
          a.setLimitMovementsMonth(account.getLimitMovementsMonth());
          a.setDateMovement(account.getDateMovement());
                
          return a;
            
        })
        .flatMap(repository::save)
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundUpdate)));
    
  }
  
  public Mono<Account> updateAccountFallback(Account account, String id, Exception ex){ 
      
    log.info("ups cuenta con id{} no encontrada para actualizar, retornando fallback", id);
      
    return Mono.just(Account.builder()
        .accountNumber(msgNotFoundUpdate).build());
    
  }

}
