package com.everis.service.impl;

import com.everis.dto.Response;
import com.everis.model.Customer;
import com.everis.repository.InterfaceCustomerRepository;
import com.everis.repository.InterfaceRepository;
import com.everis.service.InterfaceCustomerService;
import com.everis.topic.producer.CustomerProducer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementacion de Metodos del Service Customer.
 */
@Slf4j
@Service
public class CustomerServiceImpl extends CrudServiceImpl<Customer, String>
    implements InterfaceCustomerService {

  static final String CIRCUIT = "customerServiceCircuitBreaker";

  @Value("${msg.error.registro.notfound.all}")
  private String msgNotFoundAll;

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
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "findAllFallback")
  public Mono<List<Customer>> findAllCustomer() {

    Flux<Customer> customerDatabase = service.findAll()
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundAll)));

    return customerDatabase.collectList().flatMap(Mono::just);

  }

  @Override
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "customerFallback")
  public Mono<Customer> findByIdentityNumber(String identityNumber) {

    return repository.findByIdentityNumber(identityNumber)
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFound)));

  }

  @Override
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "createFallback")
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

                producer.sendSavedCustomerTopic(customer);
                return createdObject;

              })
              .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundCreate)));

        });

  }

  @Override
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "updateFallback")
  public Mono<Customer> updateCustomer(Customer customer, String identityNumber) {

    Mono<Customer> customerModification = Mono.just(customer);

    Mono<Customer> customerDatabase = repository.findByIdentityNumber(identityNumber);

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
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "deleteFallback")
  public Mono<Response> deleteCustomer(String identityNumber) {

    Mono<Customer> customerDatabase = repository.findByIdentityNumber(identityNumber);

    return customerDatabase
        .flatMap(objectDelete -> service.delete(objectDelete.getId())
            .then(Mono.just(Response.builder().data(msgCustomerDelete).build())))
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundDelete)));

  }

  /** Mensaje si no existen clientes. */
  public Mono<List<Customer>> findAllFallback(Exception ex) {

    log.info("Clientes no encontrados, retornando fallback");

    List<Customer> list = new ArrayList<>();

    list.add(Customer
        .builder()
        .name(ex.getMessage())
        .build());

    return Mono.just(list);

  }

  /** Mensaje si no encuentra el cliente. */
  public Mono<Customer> customerFallback(String identityNumber, Exception ex) {

    log.info("Cliente con numero de identidad {} no encontrado.", identityNumber);

    return Mono.just(Customer
        .builder()
        .identityNumber(identityNumber)
        .name(ex.getMessage())
        .build());

  }

  /** Mensaje si falla el create. */
  public Mono<Customer> createFallback(Customer customer, Exception ex) {

    log.info("Cliente con numero de identidad {} no se pudo crear.", customer.getIdentityNumber());

    return Mono.just(Customer
        .builder()
        .identityNumber(customer.getIdentityNumber())
        .name(ex.getMessage())
        .build());

  }

  /** Mensaje si falla el update. */
  public Mono<Customer> updateFallback(Customer customer,
      String identityNumber, Exception ex) {

    log.info("Cliente con numero de identidad {} no encontrado para actualizar.",
        customer.getIdentityNumber());

    return Mono.just(Customer
        .builder()
        .identityNumber(identityNumber)
        .name(ex.getMessage())
        .build());

  }

  /** Mensaje si falla el delete. */
  public Mono<Response> deleteFallback(String identityNumber, Exception ex) {

    log.info("Cliente con numero de identidad {} no encontrado para eliminar.", identityNumber);

    return Mono.just(Response
        .builder()
        .data(identityNumber)
        .error(ex.getMessage())
        .build());

  }

}
