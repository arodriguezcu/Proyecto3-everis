package com.everis.controller;

import com.everis.dto.Response;
import com.everis.model.Customer;
import com.everis.service.InterfaceCustomerService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controlador para manejar crud del cliente.
 */
@RestController
@RequestMapping("/customer")
public class CustomerController {
  
  @Autowired
  private InterfaceCustomerService service;
    
  /** Listado de clientes. */
  @GetMapping
  public Mono<ResponseEntity<List<Customer>>> findAll() { 
  
    return service.findAll()
        .collectList()
        .flatMap(list -> {
          
          return list.size() > 0 
              ?
                  Mono.just(ResponseEntity
                      .ok()
                      .contentType(MediaType.APPLICATION_JSON)
                      .body(list))
              :
                  Mono.just(ResponseEntity
                      .noContent()
                      .build());
          
        });
  
  }
  
  /** Buscar cliente por numero de identidad. */
  @GetMapping("/{indentityNumber}")
  public Mono<ResponseEntity<Customer>> findByIdentityNumber(@PathVariable("indentityNumber") 
      String indentityNumber) {
    
    return service.findByIdentityNumber(indentityNumber)
        .map(objectFound -> ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectFound));
    
  }
  
  /** Crear cliente. */
  @PostMapping
  public Mono<ResponseEntity<Customer>> create(@RequestBody 
      Customer customer, final ServerHttpRequest request) {
    
    return service.createCustomer(customer)
        .map(objectFound -> ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectFound));
    
  }
  
  /** Actualizar cliente por numero de identidad. */
  @PutMapping("/{indentityNumber}")
  public Mono<ResponseEntity<Customer>> update(@RequestBody 
      Customer customer, @PathVariable("indentityNumber") String indentityNumber) {
  
    return service.updateCustomer(customer, indentityNumber)
        .map(objectFound -> ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectFound));
  
  }
  
  /** Eliminar cliente por numero de identidad. */
  @DeleteMapping("/{indentityNumber}")
  public Mono<ResponseEntity<Response>> delete(@PathVariable("indentityNumber") 
      String indentityNumber) {
    
    return service.deleteCustomer(indentityNumber)
        .map(objectFound -> ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectFound));
    
  }
  
}