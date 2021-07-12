package com.everis.service;

import com.everis.dto.Response;
import com.everis.model.Customer;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Interface de Metodos del Service Customer.
 */
public interface InterfaceCustomerService extends InterfaceCrudService<Customer, String> {

  Mono<List<Customer>> findAllCustomer();

  Mono<Customer> findByIdentityNumber(String identityNumber);

  Mono<Customer> createCustomer(Customer customer);

  Mono<Customer> updateCustomer(Customer customer, String identityNumber);

  Mono<Response> deleteCustomer(String identityNumber);

}
