package com.everis;

import com.everis.model.Customer;
import com.everis.repository.InterfaceCustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.RequestBodySpec;

/**
 * Test del Controlador.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class CustomerControllerIntegrationTest {
    
  @MockBean
  InterfaceCustomerRepository repository;
        
  @Test
  public void testCreateCustomer() throws Exception {
      
    Customer customer = new Customer();
    
    customer.setId("1");
    customer.setName("MIGUEL");
    customer.setIdentityType("DNI");
    customer.setIdentityNumber("741852963");
    customer.setCustomerType("PERSONAL");
    customer.setAddress("PERU");
    customer.setPhoneNumber("963852741");
    
    ((RequestBodySpec) WebTestClient.bindToServer()
    .baseUrl("http://localhost:8090")
    .build()
    .post()
    .uri("/customer")
    .bodyValue(customer))
    .contentType(MediaType.APPLICATION_JSON)
    .accept(MediaType.APPLICATION_JSON)
    .exchange()
    .expectStatus().isOk();
    
  }
  
}