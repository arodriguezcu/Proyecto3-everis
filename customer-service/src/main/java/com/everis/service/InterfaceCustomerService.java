package com.everis.service;

import com.everis.dto.Response;
import com.everis.model.Customer;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Interface del Service con metodos externos al crud.
 */
public interface InterfaceCustomerService extends InterfaceCrudService<Customer, String> {

  Mono<List<Customer>> findAllCustomer();
  
  Mono<Customer> findByIdentityNumber(String identityNumber);
  
  Mono<Customer> createCustomer(Customer customer);
  
  Mono<Customer> updateCustomer(Customer customer, String indentityNumber);
  
  Mono<Response> deleteCustomer(String indentityNumber);
  
}
