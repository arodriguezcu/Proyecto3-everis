package com.everis.service.impl;

import com.everis.dto.Response;
import com.everis.model.Account;
import com.everis.model.Purchase;
import com.everis.repository.InterfaceAccountRepository;
import com.everis.repository.InterfaceRepository;
import com.everis.service.InterfaceAccountService;
import com.everis.service.InterfacePurchaseService;
import com.everis.topic.producer.AccountProducer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementacion de Metodos del Service Account.
 */
@Slf4j
@Service
public class AccountServiceImpl extends CrudServiceImpl<Account, String> 
    implements InterfaceAccountService {

  private final String circuitBreaker = "accountServiceCircuitBreaker";
  
  @Value("${msg.error.registro.notfound.all}")
  public String msgNotFoundAll;
  
  @Value("${msg.error.registro.notfound}")
  private String msgNotFound;
  
  @Value("${msg.error.registro.if.exists}")
  public String msgIfExists;
  
  @Value("${msg.error.registro.card.notexists}")
  public String msgCardNotExists;
  
  @Value("${msg.error.registro.notfound.create}")
  public String msgNotFoundCreate;  
  
  @Value("${msg.error.registro.notfound.update}")
  private String msgNotFoundUpdate;
  
  @Value("${msg.error.registro.notfound.delete}")
  public String msgNotFoundDelete;

  @Value("${msg.error.registro.account.delete}")
  public String msgAccountDelete;
  
  @Autowired
  private InterfaceAccountRepository repository;

  @Autowired
  private InterfaceAccountService service;

  @Autowired
  private InterfacePurchaseService purchaseService;
  
  @Autowired
  private AccountProducer producer;
  
  @Override
  protected InterfaceRepository<Account, String> getRepository() {
  
    return repository;
  
  }
  
  @Override
  @CircuitBreaker(name = circuitBreaker, fallbackMethod = "findAllFallback")
  public Mono<List<Account>> findAllAccount() {
    
    Flux<Account> accountDatabase = service.findAll()
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundAll)));
    
    return accountDatabase.collectList().flatMap(Mono::just);
  
  }

  @Override
  @CircuitBreaker(name = circuitBreaker, fallbackMethod = "accountFallback")
  public Mono<Account> findByAccountNumber(String accountNumber) {

    return repository.findByAccountNumber(accountNumber)
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFound)));
  
  }

  @Override
  @CircuitBreaker(name = circuitBreaker, fallbackMethod = "createFallback")
  public Mono<Account> createAccount(Account account) {
    
    Mono<Purchase> purchaseDatabase = purchaseService
        .findByCardNumber(account.getPurchase().getCardNumber())
        .switchIfEmpty(Mono.error(new RuntimeException(msgCardNotExists)));
    
    Flux<Account> accountDatabase = service.findAll()
        .filter(list -> list.getAccountNumber().equals(account.getAccountNumber()))
        .mergeWith(service.findAll().filter(list -> list.getPurchase().getCardNumber()
            .equals(account.getPurchase().getCardNumber())));
    
    return purchaseDatabase
        .flatMap(purchase -> {
          
          account.setPurchase(purchase);
          
          return accountDatabase
              .collectList()
              .flatMap(list -> {
                
                account.setCurrentBalance(purchase.getAmountIni());
                account.setDateOpened(LocalDateTime.now());
                
                return list.size() > 0
                    ?
                        Mono.error(new RuntimeException(msgIfExists))
                    :                        
                        service.create(account)
                        .map(createdObject -> {
                          
                          producer.sendCreatedAccount(createdObject);                          
                          return createdObject;
                          
                        })
                        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundCreate)));
              
              });
    
        });
    
  }

  @Override
  @CircuitBreaker(name = circuitBreaker, fallbackMethod = "updateFallback")  
  public Mono<Account> updateAccount(Account account, String accountNumber) {
    
    Mono<Account> accountModification = Mono.just(account);
    
    Mono<Account> accountDatabase = findByAccountNumber(accountNumber);
    
    return accountDatabase
        .zipWith(accountModification, (a, b) -> {
          
          a.setAccountNumber(b.getAccountNumber());                
          return a;
            
        })
        .flatMap(service::update)
        .map(objectUpdated -> {
          
          producer.sendCreatedAccount(objectUpdated);
          return objectUpdated;
          
        })
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundUpdate)));
    
  }
  
  @Override
  @CircuitBreaker(name = circuitBreaker, fallbackMethod = "deleteFallback")
  public Mono<Response> deleteAccount(String accountNumber) {
    
    Mono<Account> accountDatabase = findByAccountNumber(accountNumber);
    
    return accountDatabase
        .flatMap(objectDelete -> {
          
          return service.delete(objectDelete.getId())
              .then(Mono.just(Response.builder().data(msgAccountDelete).build()));
          
        })
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundDelete)));
    
  }
  
  /** Mensaje si no existen account. */
  public Mono<List<Account>> findAllFallback(Exception ex) {
    
    log.info("Cuentas no encontradas, retornando fallback");
  
    List<Account> list = new ArrayList<>();
    
    list.add(Account
        .builder()
        .id(ex.getMessage())
        .build());
    
    return Mono.just(list);
    
  }
  
  /** Mensaje si no encuentra el account. */
  public Mono<Account> accountFallback(String accountNumber, Exception ex) {
      
    log.info("Account {} no encontrado , retornando fallback", accountNumber);
      
    return Mono.just(Account
        .builder()
        .accountNumber(accountNumber)
        .id(ex.getMessage())
        .build());
    
  }
  
  /** Mensaje si falla el create. */
  public Mono<Account> createFallback(Account account, Exception ex) {
  
    log.info("Cuenta {} no se pudo crear, "
        + "retornando fallback", account.getAccountNumber());
  
    return Mono.just(Account
        .builder()
        .accountNumber(account.getAccountNumber())
        .currentBalance(Double.parseDouble(account.getPurchase().getCardNumber()))
        .id(ex.getMessage())
        .build());
    
  }
  
  /** Mensaje si falla el update. */  
  public Mono<Account> updateFallback(Account account, String accountNumber, Exception ex) { 
      
    log.info("Cuenta {} no encontrada para actualizar, retornando fallback", accountNumber);
      
    return Mono.just(Account
        .builder()
        .accountNumber(accountNumber)
        .id(ex.getMessage())
        .build());
    
  }
  
  /** Mensaje si falla el delete. */
  public Mono<Response> deleteFallback(String accountNumber, Exception ex) {
  
    log.info("Cuenta {} no encontrado para eliminar, "
        + "retornando fallback", accountNumber);
  
    return Mono.just(Response
        .builder()
        .data(accountNumber)
        .error(ex.getMessage())
        .build());
    
  }

}
