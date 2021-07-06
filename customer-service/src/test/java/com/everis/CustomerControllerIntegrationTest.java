package com.everis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.everis.controller.CustomerController;
import com.everis.model.Customer;
import com.everis.repository.InterfaceCustomerRepository;

@SpringBootTest
@RunWith(SpringRunner.class)
class CustomerControllerIntegrationTest {
   
  @Autowired
  InterfaceCustomerRepository repository;
  
  @Autowired
  CustomerController controller;
  
  Customer customer = new Customer();
  
  @BeforeEach
  void setUp() {
    
    customer.setId("1");
    customer.setName("MIGUEL");
    customer.setIdentityType("DNI");
    customer.setIdentityNumber("74185263");
    customer.setCustomerType("PERSONAL");
    customer.setAddress("PERU");
    customer.setPhoneNumber("963852741");
    
  }  
  
  @Test
  void testCreateCustomer() throws Exception {
    
    controller.create(customer);
    
    assertThat(customer.getId()).isNotNull();
      
  }
  
  @Test
  void testDeleteCustomer() {
    
    repository.deleteAll();
    
    assertThat(repository.findAll()).toString().isEmpty();
      
  }
  
}