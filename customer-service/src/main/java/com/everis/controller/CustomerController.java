package com.everis.controller;

import com.everis.dto.Response;
import com.everis.model.Customer;
import com.everis.service.InterfaceCustomerService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
 * Controlador del Customer.
 */
@RestController
@RequestMapping("/customer")
public class CustomerController {

  @Autowired
  private InterfaceCustomerService service;

  /** Metodo para listar todos los clientes. */
  @GetMapping
  public Mono<ResponseEntity<List<Customer>>> findAll() {

    return service.findAllCustomer()
        .map(objectFound -> ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectFound));

  }

  /** Metodo para buscar cliente por numero de identidad. */
  @GetMapping("/{identityNumber}")
  public Mono<ResponseEntity<Customer>> findByIdentityNumber(@PathVariable("identityNumber")
      String identityNumber) {

    return service.findByIdentityNumber(identityNumber)
        .map(objectFound -> ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectFound));

  }

  /** Metodo para crear cliente. */
  @PostMapping
  public Mono<ResponseEntity<Customer>> create(@RequestBody Customer customer) {

    return service.createCustomer(customer)
        .map(objectFound -> ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectFound));

  }

  /** Metodo para actualizar cliente por numero de identidad. */
  @PutMapping("/{identityNumber}")
  public Mono<ResponseEntity<Customer>> update(@RequestBody
      Customer customer, @PathVariable("identityNumber") String identityNumber) {

    return service.updateCustomer(customer, identityNumber)
        .map(objectFound -> ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectFound));

  }

  /** Metodo para eliminar cliente por numero de identidad. */
  @DeleteMapping("/{identityNumber}")
  public Mono<ResponseEntity<Response>> delete(@PathVariable("identityNumber")
      String identityNumber) {

    return service.deleteCustomer(identityNumber)
        .map(objectFound -> ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectFound));

  }

}
