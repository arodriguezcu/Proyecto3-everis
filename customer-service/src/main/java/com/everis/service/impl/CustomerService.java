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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Clase Service que implementa el crud y otros metodos adicionales.
 */
@Slf4j
@Service
public class CustomerService extends CrudServiceImpl<Customer, String> 
    implements InterfaceCustomerService {

  private final String circuitBreaker = "customerServiceCircuitBreaker";
  
  @Value("${msg.error.registro.notfound}")
  private String msgNotFound;
  
  @Value("${msg.error.registro.if.exists}")
  private String msgIfExists;
  
  @Value("${msg.error.registro.notfound.create}")
  private String msgNotFoundCreate;  
  
  @Value("${msg.error.registro.notfound.update}")
  private String msgNotFoundUpdate;
  
  @Value("${msg.error.registro.notfound.delete}")
  private String msgNotFoundDelete;

  @Value("${msg.error.registro.customer.delete}")
  private String msgCustomerDelete;
  
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
  @CircuitBreaker(name = circuitBreaker, fallbackMethod = "createFallback")
  public Mono<Customer> createCustomer(Customer customer) {
    
    Flux<Customer> customerDatabase = service.findAll()
        .filter(list -> list.getIdentityNumber().equals(customer.getIdentityNumber()));
  
    return customerDatabase
        .collectList()
        .flatMap(list -> {
          
          if (list.size() > 0) {
            
            return Mono.error(new RuntimeException(msgIfExists));
            
          }
          
          return service.create(customer)
              .map(createdObject -> {
                
                producer.sendSavedCustomerTopic(createdObject);                
                return createdObject;
                
              })
              .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundCreate)));
    
        });
    
  }
  
  @Override
  @CircuitBreaker(name = circuitBreaker, fallbackMethod = "updateFallback")
  public Mono<Customer> updateCustomer(Customer customer, String indentityNumber) {
  
    Mono<Customer> customerModification = Mono.just(customer);
  
    Mono<Customer> customerDatabase = repository.findByIdentityNumber(indentityNumber);
    
    return customerDatabase
        .zipWith(customerModification, (a, b) -> {
                    
          if (b.getName() != null) a.setName(b.getName());
          if (b.getAddress() != null) a.setAddress(b.getAddress());
          if (b.getPhoneNumber() != null) a.setPhoneNumber(b.getPhoneNumber());
          
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
          
          return service.delete(objectDelete.getId())
              .then(Mono.just(Response.builder().data(msgCustomerDelete).build()));
          
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
        .name(ex.getMessage())
        .build());
    
  }
  
  /** Mensaje si falla el create. */
  public Mono<Customer> createFallback(Customer customer, Exception ex) {
  
    log.info("Cliente con numero de identidad {} no se pudo crear, "
        + "retornando fallback", customer.getIdentityNumber());
  
    return Mono.just(Customer
        .builder()
        .identityNumber(customer.getIdentityNumber())
        .name(ex.getMessage())
        .build());
    
  }
  
  /** Mensaje si falla el update. */
  public Mono<Customer> updateFallback(Customer customer, 
      String identityNumber, Exception ex) {
  
    log.info("Cliente con numero de identidad {} no encontrado para actualizar, "
        + "retornando fallback", identityNumber);
  
    return Mono.just(Customer
        .builder()
        .identityNumber(identityNumber)
        .name(ex.getMessage())
        .build());
    
  }
  
  /** Mensaje si falla el delete. */
  public Mono<Response> deleteFallback(String identityNumber, Exception ex) {
  
    log.info("Cliente con numero de identidad {} no encontrado para eliminar, "
        + "retornando fallback", identityNumber);
  
    return Mono.just(Response
        .builder()
        .data(identityNumber)
        .error(ex.getMessage())
        .build());
    
  }
  
}
