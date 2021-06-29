package com.everis.service.impl;

import com.everis.dto.Response;
import com.everis.exception.EntityNotFoundException;
import com.everis.model.Customer;
import com.everis.repository.InterfaceCustomerRepository;
import com.everis.repository.InterfaceRepository;
import com.everis.service.InterfaceCustomerService;
import com.everis.topic.producer.CustomerProducer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Slf4j : Logback.
 */
@Slf4j
@Service
public class CustomerService extends CrudServiceImpl<Customer, String> 
    implements InterfaceCustomerService {

  private final String circuitBreaker = "customerServiceCircuitBreaker";
  
  @Value("${msg.error.registro.notfound}")
  private String msgNotFound;
  
  @Value("${msg.error.registro.notfound.update}")
  private String msgNotFoundUpdate;
  
  @Value("${msg.error.registro.notfound.delete}")
  private String msgNotFoundDelete;
  
  @Autowired
  private InterfaceCustomerRepository repository;
  
  @Autowired
  private InterfaceCustomerService service;
  
  @Autowired
  private CustomerProducer producer;

  @Override
  protected InterfaceRepository<Customer, String> getRepository() {
    
    return repository;
    
  }
  
  @Override
  @CircuitBreaker(name = circuitBreaker, fallbackMethod = "customerFallback")
  public Mono<Customer> findByIdentityNumber(String identityNumber) {
    
    return repository.findByIdentityNumber(identityNumber)
        .switchIfEmpty(Mono.error(new EntityNotFoundException(msgNotFound)));
  
  }
  
  @Override
  @CircuitBreaker(name = circuitBreaker, fallbackMethod = "updateCustomerFallback")
  public Mono<Customer> updateCustomer(Customer customer, String indentityNumber) {
  
    Mono<Customer> customerModification = Mono.just(customer);
  
    Mono<Customer> customerDatabase = repository.findByIdentityNumber(indentityNumber);
    
    return customerDatabase
        .zipWith(customerModification, (a, b) -> {
          
          a.setName(b.getName());
          a.setAddress(b.getAddress());
          a.setPhoneNumber(b.getPhoneNumber());
          
          return a;
          
        })
        .flatMap(service::update)
        .map(objectUpdated -> {
          
          producer.sendSavedCustomerTopic(objectUpdated);
          return objectUpdated;
          
        })
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundUpdate)));
    
  }
  
  @Override
  @CircuitBreaker(name = circuitBreaker, fallbackMethod = "deleteFallback")
  public Mono<Response> deleteCustomer(String indentityNumber) {
    
    Mono<Customer> customerDatabase = repository.findByIdentityNumber(indentityNumber);
    
    return customerDatabase
        .flatMap(objectDelete -> {
          
          service.delete(objectDelete.getId());
          
          return Mono.just(Response.builder().data("Cliente Eliminado").build());
          
        })
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundDelete)));
    
  }
  
  /** Mensaje si no hay customer. */
  public Mono<Customer> customerFallback(String identityNumber, Exception ex) {
    
    log.info("Cliente con numero de identidad {} no encontrado, "
        + "retornando fallback", identityNumber);
  
    return Mono.just(Customer
        .builder()
        .identityNumber(identityNumber)
        .name(msgNotFound)
        .build());
    
  }
  
  /** Mensaje si falla el update. */
  public Mono<Customer> updateCustomerFallback(Customer customer, 
      String identityNumber, Exception ex) {
  
    log.info("Cliente con numero de identidad {} no encontrado para actualizar, "
        + "retornando fallback", identityNumber);
  
    return Mono.just(Customer
        .builder()
        .identityNumber(identityNumber)
        .name(msgNotFoundUpdate)
        .build());
    
  }
  
  /** Mensaje si falla el delete. */
  public Mono<Customer> deleteFallback(String identityNumber, Exception ex) {
  
    log.info("Cliente con numero de identidad {} no encontrado para eliminar, "
        + "retornando fallback", identityNumber);
  
    return Mono.just(Customer
        .builder()
        .identityNumber(identityNumber)
        .name(msgNotFoundDelete)
        .build());
    
  }
  
}
