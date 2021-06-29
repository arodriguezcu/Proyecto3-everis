package com.everis.controller;

import com.everis.dto.Response;
import com.everis.model.Customer;
import com.everis.service.InterfaceCustomerService;
import com.everis.topic.producer.CustomerProducer;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Controlador para manejar crud del cliente.
 */
@RestController
@RequestMapping("/customer")
public class CustomerController {
  
  @Autowired
  private InterfaceCustomerService service;
  
  @Autowired
  private CustomerProducer producer;
  
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
  public Mono<ResponseEntity<Response>> create(@RequestBody 
      Customer customer, final ServerHttpRequest request) {
    
    Flux<Customer> customerDatabase = service.findAll()
        .filter(list -> list.getIdentityNumber().equals(customer.getIdentityNumber()));
  
    return customerDatabase
        .collectList()
        .flatMap(list -> {
          
          if (list.size() > 0) {
            return Mono.just(ResponseEntity
                .badRequest()
                .body(Response
                    .builder()
                    .data("El cliente con numero de identificacion " 
                        + customer.getIdentityNumber() + " ya existe")
                    .build()));      
          }
          
          return service.create(customer)
              .map(createdObject -> {
                
                producer.sendSavedCustomerTopic(createdObject);
                
                return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Response
                        .builder()
                        .data(createdObject)
                        .build());
        
              });
    
        });
    
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
  
//    return service.findByIdentityNumber(indentityNumber)
//      .flatMap(objectDelete -> {
//        
//        return service.delete(objectDelete.getId())
//            .then(Mono.just(ResponseEntity
//                .ok()
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(Response
//                    .builder()
//                    .data("El cliente con numero de identificacion " 
//                        + indentityNumber + " ha sido eliminado")
//                    .build())));
//        
//      })
//      .defaultIfEmpty(ResponseEntity
//          .badRequest()
//          .body(Response
//              .builder()
//              .data("El cliente no existe")
//              .build()));
    
    return service.deleteCustomer(indentityNumber)
        .map(objectFound -> ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectFound));
    
    }
  
}