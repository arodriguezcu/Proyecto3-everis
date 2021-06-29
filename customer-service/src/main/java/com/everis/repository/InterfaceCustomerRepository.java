package com.everis.repository;

import com.everis.model.Customer;
import reactor.core.publisher.Mono;

/**
 * Interface del Repositorio con metodos externos al crud.
 */
public interface InterfaceCustomerRepository extends InterfaceRepository<Customer, String> {
  
  Mono<Customer> findByIdentityNumber(String identityNumber);

}