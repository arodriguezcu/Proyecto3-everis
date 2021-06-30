package com.everis.controller;

import com.everis.dto.Response;
import com.everis.model.Product;
import com.everis.service.InterfaceProductService;
import com.everis.topic.producer.ProductProducer;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 
 */
@RestController
@RequestMapping("/product")
public class ProductController {
  
  @Autowired
  private ProductProducer producer;
  
  @Autowired
  private InterfaceProductService service;
  
  /** */
  @GetMapping  
  public Mono<ResponseEntity<List<Product>>> findAll() { 
    
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
  
  /** */
  @GetMapping("/{id}")
  public Mono<ResponseEntity<Product>> findById(@PathVariable("id") String id) {
    
    return service.findById(id)
        .map(objectFound -> ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(objectFound));
  
  }
  
  /** */
  @PostMapping
  public Mono<ResponseEntity<Response>> create(@RequestBody Product product) {
    
    Flux<Product> productDatabase = service.findAll()
        .filter(list -> list.getProductName().equals(product.getProductName()));
        
    return productDatabase
        .collectList()
        .flatMap(list -> {
          
          if (list.size() > 0) {
            
            return Mono.just(ResponseEntity
                .badRequest()
                .body(Response
                    .builder()
                    .data("El producto " + product.getProductName() + " ya existe")
                    .build()));      
          }
      
          return service.create(product)
              .map(createdObject -> {
                
                producer.sendSavedProductTopic(createdObject);
        
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
  
  /** */
  @PutMapping("/{productName}")
  public Mono<ResponseEntity<Product>> update(@RequestBody 
      Product product, @PathVariable("productName") String productName) {
    
    return service.updateProduct(product, productName)
        .map(objectFound -> ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectFound)
        );
  
  }
  
  /** */
  @DeleteMapping("/{productName}")
  public Mono<ResponseEntity<Response>> delete(@PathVariable("productName") String productName) {
    
    return service.findByProductName(productName)
        .flatMap(objectDelete -> {
          
          return service.delete(objectDelete.getId())
              .then(Mono.just(ResponseEntity
                  .ok()
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(Response
                      .builder()
                      .data("El producto " + productName + " ha sido eliminado")
                      .build())));
          
        })
        .defaultIfEmpty(ResponseEntity
            .badRequest()
            .body(Response
                .builder()
                .data("El producto no existe")
                .build()));
  
  }
  
}
