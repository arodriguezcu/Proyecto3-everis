package com.everis.controller;

import java.time.LocalDateTime;
import java.util.List;
import javax.validation.Valid;
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
import com.everis.dto.Response;
import com.everis.model.Customer;
import com.everis.model.Product;
import com.everis.model.Purchase;
import com.everis.service.ICustomerService;
import com.everis.service.IProductService;
import com.everis.service.IPurchaseService;
import com.everis.topic.producer.PurchaseProducer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/purchase")
public class PurchaseController {

  @Autowired
  private IPurchaseService service;
  
  @Autowired
  private ICustomerService customerService;
  
  @Autowired
  private IProductService productService;
  
  @Autowired
  private PurchaseProducer producer;
  
  @GetMapping
  public Mono<ResponseEntity<List<Purchase>>> findAll(){ 
  
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
  
  @GetMapping("/{identityNumber}")
  public Mono<ResponseEntity<List<Purchase>>> findByIndentityNumber(@PathVariable("identityNumber") String identityNumber) {
  
    Flux<Purchase> purchaseDatabase = service.findAll()
        .filter(p -> p.getCustomerOwner().get(0).getIdentityNumber().equals(identityNumber));
  
    return purchaseDatabase
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
  
  @GetMapping("/available/{identityNumber}")
  public Mono<ResponseEntity<List<Product>>> findByAvailableProduct(@PathVariable("identityNumber") String identityNumber) {
  
    Flux<Purchase> purchaseDatabase = service.findAll()
        .filter(p -> p.getCustomerOwner().get(0).getIdentityNumber().equals(identityNumber));
        
    Mono<Customer> customerDatabase = customerService.findByIdentityNumber(identityNumber);
        
    return purchaseDatabase
        .collectList()
        .flatMap(list -> {
          
          return customerDatabase
              .flatMap(c -> {
                
                if (c.getId() == null) {
                  
                  return Mono.just(ResponseEntity
                      .noContent()
                      .build());
                              
                }
                
                Flux<Product> productDatabase = productService.findAll();                
                
                if (c.getCustomerType().equalsIgnoreCase("PERSONAL")) {
                  
                  productDatabase = productDatabase.filter(p -> !p.getCondition().getProductPerPersonLimit().equals(0));
                  
                  for (Purchase purchase : list) {
                    
                    if (purchase.getProduct().getCondition().getProductPerPersonLimit().equals(1)) {
                      
                      productDatabase = productDatabase.filter(p -> !p.getProductName().equals(purchase.getProduct().getProductName()));                    
                      
                    }
                    
                  }
                  
                } else if (c.getCustomerType().equalsIgnoreCase("EMPRESARIAL")) {
                  
                  productDatabase = productDatabase.filter(p -> !p.getCondition().getProductPerBusinessLimit().equals(0));
                  
                  for (Purchase purchase : list) {
                    
                    if (purchase.getProduct().getCondition().getProductPerBusinessLimit().equals(1)) {
                      
                      productDatabase = productDatabase.filter(p -> !p.getProductName().equals(purchase.getProduct().getProductName()));
                      
                    }
                    
                  }
                  
                }
                
                return productDatabase
                    .collectList()
                    .flatMap(products -> {
                
                      return products.size() > 0
                          ?
                              Mono.just(ResponseEntity
                                  .ok()
                                  .contentType(MediaType.APPLICATION_JSON)
                                  .body(products))
                          :
                              Mono.just(ResponseEntity
                                  .noContent()
                                  .build());
                      
                    });
                
              });
          
        });
  
  }
  
  @PostMapping
  public Mono<ResponseEntity<Response>> create(@Valid @RequestBody Purchase purchase, final ServerHttpRequest request){
  
    Mono<Purchase> monoPurchase = Mono.just(purchase.toBuilder().build());
    
    Mono<Product> productDatabase = productService.findByProductName(purchase.getProduct().getProductName());
    
    Flux<Purchase> purchaseDatabase = service.findAll()
        .filter(p -> p.getCardNumber().equals(purchase.getCardNumber()));
      
    Mono<List<Customer>> monoListCust = Flux.fromIterable(purchase.getCustomerOwner())
        .flatMap(p1 -> customerService.findByIdentityNumber(p1.getIdentityNumber()))
        .collectList();
    
    Mono<List<Purchase>> monoListPur = service.findAll()
        .filter(p -> p.getCustomerOwner().get(0).getIdentityNumber().equals(purchase.getCustomerOwner().get(0).getIdentityNumber()))
        .filter(p -> p.getProduct().getProductName().equals("TARJETA DE CREDITO")).collectList();
    
    return productDatabase
        .flatMap(pro -> {
        
          return purchaseDatabase
              .collectList()
              .flatMap(listPurchase -> {
                
                if (listPurchase.size() > 0) {
              
                  return Mono.just(ResponseEntity
                      .badRequest()
                      .body(Response
                          .builder()
                          .data("El numero de tarjeta " + purchase.getCardNumber() + " ya existe")
                          .build()));      
                }
                
                return monoPurchase
                    .zipWith(productDatabase, (p,b) -> {

                      p.setProduct(b);
                                                        
                      if (p.getProduct().getProductName().equals("AHORRO")) {
                        
                        p.getProduct().getCondition().setMonthlyTransactionLimit(purchase.getProduct().getCondition().getMonthlyTransactionLimit());
                        
                      } else if (p.getProduct().getProductName().equals("CUENTA CORRIENTE")) {
                        
                        p.getProduct().getCondition().setMaintenanceFee(purchase.getProduct().getCondition().getMaintenanceFee());
                        
                      } else if (p.getProduct().getProductName().equals("PLAZO FIJO")) {
                        
                        p.getProduct().getCondition().setDailyMonthlyTransactionLimit(purchase.getProduct().getCondition().getDailyMonthlyTransactionLimit());
                        
                      }
                      
                      return p;
                      
                    })
                .zipWith(monoListCust, (p,list) -> {
                  
                  p.setCustomerOwner(list);
                  return p;
                  
                })
                .flatMap(purchasebd -> {
                  
                  purchasebd.setAmountFin(purchase.getAmountIni());
                  purchasebd.setPurchaseDate(LocalDateTime.now());
                  purchase.getProduct().setProductName(purchasebd.getProduct().getProductName());
                  purchase.getProduct().setProductType(purchasebd.getProduct().getProductType());
                  purchase.setAmountFin(purchase.getAmountIni());
                            
                  if (purchasebd.getAmountIni() < 0) {
                    
                    return Mono.just(ResponseEntity
                        .badRequest()
                        .body(Response
                            .builder()
                            .error("El monto inicial no puede ser negativo.")
                            .build()));
                    
                  }
        
                  if(purchasebd.getProduct().getId() == null) {
                    
                    return Mono.just(ResponseEntity
                        .badRequest()
                        .body(Response
                            .builder()
                            .error("El producto ingresado no existe.")
                            .build()));
                    
                  }
                
                if(purchasebd.getCustomerOwner().size() != purchase.getCustomerOwner().size()) {
                  
                  return Mono.just(ResponseEntity
                      .badRequest()
                      .body(Response
                          .builder()
                          .error("El(los) cliente(s) ingresado(s) no existe.")
                          .build()));
                  
                }
                
                long quantityOwners = purchasebd.getCustomerOwner().size();
                long quantityBusinessOwners = purchasebd.getCustomerOwner().stream().filter(c -> c.getCustomerType().equals("EMPRESARIAL")).count();
                long quantityPersonalOwners = purchasebd.getCustomerOwner().stream().filter(c -> c.getCustomerType().equals("PERSONAL")).count();
                boolean isEmpresarial = false;
                boolean isPersonal = false;
                
                if(quantityOwners > 1) {
                  
                  isEmpresarial = quantityBusinessOwners == quantityOwners;
                  isPersonal = quantityPersonalOwners == quantityOwners;
                  
                  if(quantityBusinessOwners >= 1 && quantityPersonalOwners >= 1) {
                  
                    return Mono.just(ResponseEntity
                        .badRequest()
                        .body(Response
                            .builder()
                            .error("Los titulares deben pertenecer al mismo tipo de cliente. Empresarial o Personal")
                            .build()));
                  
                  }
                  
                  if(isEmpresarial) {              
                  
                    return Mono.just(ResponseEntity
                        .badRequest()
                        .body(Response
                            .builder()
                            .error("Para cliente empresarial, sólo debe haber como máximo 1 titular.")
                            .build()));
                  
                  }
                  
                } else if(quantityOwners == 0) {
                  
                  return Mono.just(ResponseEntity
                      .badRequest()
                      .body(Response
                          .builder()
                          .error("Debe existir por lo menos 1 titular.")
                          .build()));
                  
                } else if(quantityOwners == 1) {
                  
                  isEmpresarial = quantityBusinessOwners == quantityOwners && quantityPersonalOwners == 0;
                  isPersonal = quantityPersonalOwners == quantityOwners && quantityBusinessOwners == 0;
                  
                }
                
                if (purchasebd.getProduct().getProductName().equals("CREDITO PERSONAL") || 
                    purchasebd.getProduct().getProductName().equals("CREDITO EMPRESARIAL")) {
                  
                  purchasebd.setAmountFin(0);
                  purchase.setAmountFin(0);
                  
                }
                
                if(isPersonal) {      
                  
                  return service.findAll()
                      .collectList()
                      .flatMap(p -> {
                        
                        int i = 0;
                        for (Purchase purchase2 : p) {
                          for (Customer customer : purchase2.getCustomerOwner()) {
                            for (Customer customer2 : purchasebd.getCustomerOwner()) {
                              if(customer.getIdentityNumber().equals(customer2.getIdentityNumber())
                                  && purchase2.getProduct().getId().equals(purchasebd.getProduct().getId())) {
                                i++;
                              }
                            }
                          }
                        }
                        if(i > 0) {
                          
                          return Mono.just(ResponseEntity
                              .badRequest()
                              .body(Response
                                  .builder()
                                  .error("El cliente ya cuenta con el producto " + purchasebd.getProduct().getProductType()
                                      .concat("-")
                                      .concat(purchasebd.getProduct().getProductName()))
                                  .build()));
                          
                    }
                    
                    return monoListPur
                        .flatMap(list -> {
                      
                          if (list.size() == 0 && purchase.getProduct().getProductName().equals("AHORRO VIP")) {
                            
                            return Mono.just(ResponseEntity
                                .badRequest()
                                .body(Response
                                    .builder()
                                    .error("Necesita tener una tarjeta de credito.")
                                    .build()));
                            
                          }
                      
                          return service.create(purchasebd)
                              .flatMap(createdObject -> {
                                
                                purchase.setId(createdObject.getId());            
                                producer.sendCreatePurchase(purchase);            
                                
                                return Mono.just(ResponseEntity
                                    .ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(Response
                                        .builder()
                                        .data(createdObject)
                                        .build()));
        
                              });
                          
                          });           
                    
                    });
                  
                } else if(isEmpresarial) {
                  
                  if(!purchasebd.toBuilder().build().getProduct().getCondition().getCustomerTypeTarget().stream()
                      .filter(o -> o.equals("EMPRESARIAL")).findFirst().isPresent()) {
                  
                    return Mono.just(ResponseEntity
                        .badRequest()
                        .body(Response
                            .builder()
                            .error("No se puede asignar el producto " + purchasebd.getProduct().getProductType()
                                .concat("-")
                                .concat(purchasebd.getProduct().getProductName().concat(" a un cliente EMPRESARIAL")))
                            .build()));
                    
                  }
                  
                }
                
                return monoListPur
                    .flatMap(list -> {
                  
                      if (list.size() == 0 && purchase.getProduct().getProductName().equals("CUENTA CORRIENTE PYME")) {
                        
                        return Mono.just(ResponseEntity
                            .badRequest()
                            .body(Response
                                .builder()
                                .error("Necesita tener una tarjeta de credito.")
                                .build()));
                        
                      }
                  
                      return service.create(purchasebd)
                          .flatMap(created -> {
                            
                            purchase.setId(created.getId());  
                            producer.sendCreatePurchase(purchase);
                            
                            return Mono.just(ResponseEntity
                                .ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(Response
                                    .builder()
                                    .data(created)
                                    .build()));
                            
                          });
        
                    });
                        
                });
              
            });
          
        })
        .defaultIfEmpty(ResponseEntity
            .badRequest()
            .body(Response
                .builder()
                .error("El producto " + purchase.getProduct().getProductName() + " no existe")
                .build()));
        
  }
  
  @PutMapping("/{id}")
  public Mono<ResponseEntity<Purchase>> update(@RequestBody Purchase purchase, @PathVariable("id") String id){
  
    return service.updatePurchase(purchase, id)
        .map(objectUpdated -> ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectUpdated)
            );
  
  }
  
  @DeleteMapping("/{cardName}")
  public Mono<ResponseEntity<Response>> delete(@PathVariable("cardName") String cardName){
  
    return service.findByCardNumber(cardName)
        .flatMap(objectDeleted -> {
          
          return service.delete(objectDeleted.getId())
              .then(Mono.just(ResponseEntity
                  .ok()
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(Response
                      .builder()
                      .data("La compra con numero de tarjeta " + cardName + " ha sido eliminado")
                      .build()))); 
          
      })
      .defaultIfEmpty(ResponseEntity
          .badRequest()
          .body(Response
              .builder()
              .data("La compra no existe")
              .build()));
      
  }   
   
}
