package com.everis.controller;

import com.everis.model.Customer;
import com.everis.service.InterfaceCustomerService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class CustomerControllerTest {

  @MockBean
  InterfaceCustomerService service;

  @Autowired
  private WebTestClient client;

  @Test
  void testFindAll() {

    Customer customer = new Customer();

    customer.setId("1");
    customer.setName("ALEJANDRO");
    customer.setIdentityType("DNI");
    customer.setIdentityNumber("87654321");
    customer.setCustomerType("PERSONAL");
    customer.setAddress("PERU");
    customer.setPhoneNumber("963852741");

    Mockito.when(service.findAllCustomer())
        .thenReturn(Flux.just(customer).collectList());

    client.get()
    .uri("/customer")
    .accept(MediaType.APPLICATION_NDJSON)
    .exchange()
    .expectStatus().isOk();

  }

  @Test
  void testFindByIdentityNumber() {

    Customer customer = new Customer();

    customer.setId("2");
    customer.setName("MANUEL");
    customer.setIdentityType("DNI");
    customer.setIdentityNumber("99663388");
    customer.setCustomerType("PERSONAL");
    customer.setAddress("PERU");
    customer.setPhoneNumber("963852741");

    Mockito.when(service.findByIdentityNumber(customer.getIdentityNumber()))
        .thenReturn(Mono.just(customer));

    client.get()
    .uri("/customer/{identityNumber}", customer.getIdentityNumber())
    .accept(MediaType.APPLICATION_NDJSON)
    .exchange()
    .expectStatus().isOk();


  }

}