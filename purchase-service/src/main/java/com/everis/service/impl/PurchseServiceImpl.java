package com.everis.service.impl;

import com.everis.dto.Response;
import com.everis.model.Customer;
import com.everis.model.Product;
import com.everis.model.Purchase;
import com.everis.repository.InterfacePurchaseRepository;
import com.everis.repository.InterfaceRepository;
import com.everis.service.InterfaceCustomerService;
import com.everis.service.InterfaceProductService;
import com.everis.service.InterfacePurchaseService;
import com.everis.topic.producer.PurchaseProducer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementacion de Metodos del Service Purchase.
 */
@Slf4j
@Service
public class PurchseServiceImpl extends CrudServiceImpl<Purchase, String> 
    implements InterfacePurchaseService {

  static final String CIRCUIT = "purchaseServiceCircuitBreaker";

  @Value("${msg.error.registro.notfound.all}")
  private String msgNotFoundAll;

  @Value("${msg.error.registro.product.all}")
  private String msgProductAll;

  @Value("${msg.error.registro.customer.all}")
  private String msgCustomerAll;

  @Value("${msg.error.registro.notfound}")
  private String msgNotFound;

  @Value("${msg.error.registro.product.exists}")
  private String msgProductNotExists;

  @Value("${msg.error.registro.card.exists}")
  private String msgCardExists;

  @Value("${msg.error.registro.positive}")
  private String msgPositive;

  @Value("${msg.error.registro.customer.exists}")
  private String msgCustomerNotExists;

  @Value("${msg.error.registro.owners}")
  private String msgOwners;

  @Value("${msg.error.registro.business.owner}")
  private String msgBusinessOwner;

  @Value("${msg.error.registro.minimum.owner}")
  private String msgMinimumOwner;

  @Value("${msg.error.registro.product.available}")
  private String msgProductNotAvailable;

  @Value("${msg.error.registro.credit.card}")
  private String msgCreditCard;

  @Value("${msg.error.registro.product.business}")
  private String msgProductNotBusiness;

  @Value("${msg.error.registro.notfound.create}")
  private String msgNotFoundCreate;  

  @Value("${msg.error.registro.notfound.update}")
  private String msgNotFoundUpdate;

  @Value("${msg.error.registro.notfound.delete}")
  private String msgNotFoundDelete;

  @Value("${msg.error.registro.purchase.delete}")
  private String msgPurchaseDelete;

  @Autowired
  private InterfacePurchaseRepository repository;

  @Autowired
  private InterfacePurchaseService service;

  @Autowired
  private InterfaceCustomerService customerService;

  @Autowired
  private InterfaceProductService productService;

  @Autowired
  private PurchaseProducer producer;

  static final String EMPRESARIAL = "EMPRESARIAL";

  static final String PERSONAL = "PERSONAL";

  @Override
  protected InterfaceRepository<Purchase, String> getRepository() {

    return repository;

  }

  @Override
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "findAllFallback")
  public Mono<List<Purchase>> findAllPurchase() {

    Flux<Purchase> purchaseDatabase = service.findAll()
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundAll)));

    return purchaseDatabase.collectList().flatMap(Mono::just);

  }

  @Override
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "findAllFallback")
  public Mono<List<Purchase>> findByIndentityNumber(String identityNumber) {

    Flux<Purchase> purchaseDatabase = service.findAll()
        .filter(p -> p.getCustomerOwner().get(0).getIdentityNumber().equals(identityNumber))
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundAll)));

    return purchaseDatabase.collectList().flatMap(Mono::just);

  }

  @Override
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "findAllProductFallback")
  public Mono<List<Product>> findByAvailableProduct(String identityNumber) {

    Flux<Purchase> purchaseDatabase = service.findAll()
        .filter(p -> p.getCustomerOwner().get(0).getIdentityNumber().equals(identityNumber));

    Mono<Customer> customerDatabase = customerService.findByIdentityNumber(identityNumber)
        .switchIfEmpty(Mono.error(new RuntimeException(msgCustomerAll)));

    return purchaseDatabase
        .collectList()
        .flatMap(list -> customerDatabase
            .flatMap(c -> {

              Flux<Product> productDatabase = productService.findAll()
                  .switchIfEmpty(Mono.error(new RuntimeException(msgProductAll)));

              if (c.getCustomerType().equalsIgnoreCase(PERSONAL)) {

                productDatabase = productDatabase
                    .filter(p -> !p.getCondition().getProductPerPersonLimit().equals(0));

              } else if (c.getCustomerType().equalsIgnoreCase(EMPRESARIAL)) {

                productDatabase = productDatabase
                    .filter(p -> !p.getCondition().getProductPerBusinessLimit().equals(0));

              }

              for (Purchase purchase : list) {

                if ((c.getCustomerType().equalsIgnoreCase(PERSONAL)
                    && purchase.getProduct().getCondition().getProductPerPersonLimit().equals(1))
                    ||
                    (c.getCustomerType().equalsIgnoreCase(EMPRESARIAL)
                    && purchase.getProduct().getCondition().getProductPerBusinessLimit().equals(1))) {

                  productDatabase = productDatabase.filter(p -> !p.getProductName()
                      .equals(purchase.getProduct().getProductName()));

                }

              }

              return productDatabase.collectList().flatMap(Mono::just);

            }));

  }

  @Override
  public Mono<Purchase> findByCardNumber(String cardNumber) {

    return repository.findByCardNumber(cardNumber)
        .switchIfEmpty(Mono.error(new RuntimeException(msgCardExists)));

  }

  @Override
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "createFallback")
  public Mono<Purchase> createPurchase(Purchase purchase) {

    Mono<Purchase> monoPurchase = Mono.just(purchase.toBuilder().build());

    Mono<Product> productDatabase = productService
        .findByProductName(purchase.getProduct().getProductName())
        .switchIfEmpty(Mono.error(new RuntimeException(msgProductNotExists)));

    Flux<Purchase> purchaseDatabase = service.findAll()
        .filter(p -> p.getCardNumber().equals(purchase.getCardNumber()));

    Mono<List<Customer>> monoListCust = Flux.fromIterable(purchase.getCustomerOwner())
        .flatMap(p1 -> customerService.findByIdentityNumber(p1.getIdentityNumber()))
        .collectList();

    Mono<List<Purchase>> monoListPur = service.findAll()
        .filter(p -> p.getCustomerOwner().get(0).getIdentityNumber()
            .equals(purchase.getCustomerOwner().get(0).getIdentityNumber()))
        .filter(p -> p.getProduct().getProductName().equals("TARJETA DE CREDITO")).collectList();

    return purchaseDatabase
        .collectList()
        .flatMap(listPurchase -> {

          if (listPurchase.size() > 0) {

            return Mono.error(new RuntimeException(msgCardExists));

          }

          return monoPurchase
              .zipWith(productDatabase, (p, b) -> {

                p.setProduct(b);

                if (p.getProduct().getProductName().equals("AHORRO")) {

                  p.getProduct().getCondition().setMonthlyTransactionLimit(purchase
                      .getProduct().getCondition().getMonthlyTransactionLimit());

                } else if (p.getProduct().getProductName().equals("CUENTA CORRIENTE")) {

                  p.getProduct().getCondition().setMaintenanceFee(purchase
                      .getProduct().getCondition().getMaintenanceFee());

                } else if (p.getProduct().getProductName().equals("PLAZO FIJO")) {

                  p.getProduct().getCondition().setDailyMonthlyTransactionLimit(purchase
                      .getProduct().getCondition().getDailyMonthlyTransactionLimit());

                }

                return p;

              })
              .zipWith(monoListCust, (p, list) -> {

                p.setCustomerOwner(list);
                return p;

              })
              .flatMap(purchasebd -> {

                purchasebd.setAmountFin(purchase.getAmountIni());
                purchasebd.setPurchaseDate(LocalDateTime.now());
                purchase.getProduct().setProductName(purchasebd
                    .getProduct().getProductName());
                purchase.getProduct().setProductType(purchasebd
                    .getProduct().getProductType());
                purchase.setAmountFin(purchase.getAmountIni());

                if (purchasebd.getAmountIni() < 0) {

                  return Mono.error(new RuntimeException(msgPositive));

                }

                if (purchasebd.getCustomerOwner().size()
                    != purchase.getCustomerOwner().size()) {

                  return Mono.error(new RuntimeException(msgCustomerNotExists));

                }

                long quantityOwners = purchasebd.getCustomerOwner().size();
                long quantityBusinessOwners = purchasebd.getCustomerOwner().stream()
                    .filter(c -> c.getCustomerType().equals(EMPRESARIAL)).count();
                long quantityPersonalOwners = purchasebd.getCustomerOwner().stream()
                    .filter(c -> c.getCustomerType().equals(PERSONAL)).count();
                boolean isEmpresarial = false;
                boolean isPersonal = false;

                if (quantityOwners > 1) {

                  isEmpresarial = quantityBusinessOwners == quantityOwners;
                  isPersonal = quantityPersonalOwners == quantityOwners;

                  if (quantityBusinessOwners >= 1 && quantityPersonalOwners >= 1) {

                    return Mono.error(new RuntimeException(msgOwners));

                  }

                  if (isEmpresarial) {

                    return Mono.error(new RuntimeException(msgBusinessOwner));

                  }

                } else if (quantityOwners == 0) {

                  return Mono.error(new RuntimeException(msgMinimumOwner));

                } else if (quantityOwners == 1) {

                  isEmpresarial = quantityBusinessOwners == quantityOwners
                      && quantityPersonalOwners == 0;
                  isPersonal = quantityPersonalOwners == quantityOwners
                      && quantityBusinessOwners == 0;

                }

                if (purchasebd.getProduct().getProductName().equals("CREDITO PERSONAL")
                    || purchasebd.getProduct().getProductName().equals("CREDITO EMPRESARIAL")) {

                  purchasebd.setAmountFin(0);
                  purchase.setAmountFin(0);

                }

                if (isPersonal) {

                  return service.findAll()
                      .collectList()
                      .flatMap(p -> {

                        int i = 0;

                        for (Purchase purchase2 : p) {
                          for (Customer customer : purchase2.getCustomerOwner()) {
                            for (Customer customer2 : purchasebd.getCustomerOwner()) {
                              if (customer.getIdentityNumber()
                                  .equals(customer2.getIdentityNumber())
                                  && purchase2.getProduct().getId()
                                  .equals(purchasebd.getProduct().getId())) {
                                i++;
                              }
                            }
                          }
                        }

                        if (i > 0) {

                          return Mono.error(new RuntimeException(msgProductNotAvailable
                              + " " + purchasebd.getProduct().getProductType()
                              + "-" + purchasebd.getProduct().getProductName()));

                        }

                        return monoListPur
                            .flatMap(list -> {

                              if (list.size() == 0 && purchase.getProduct().getProductName()
                                  .equals("AHORRO VIP")) {

                                return Mono.error(new RuntimeException(msgCreditCard));

                              }

                              return service.create(purchasebd)
                                  .map(createdObject -> {

                                    purchase.setId(createdObject.getId());
                                    producer.sendCreatePurchase(purchase);

                                    return createdObject;

                                  })
                                  .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundCreate)));

                            });

                      });

                } else if (isEmpresarial
                    && (!purchasebd.toBuilder().build().getProduct()
                      .getCondition().getCustomerTypeTarget().stream()
                      .filter(o -> o.equals(EMPRESARIAL)).findFirst().isPresent())) {

                  return Mono.error(new RuntimeException(msgProductNotBusiness
                      + " " + purchasebd.getProduct().getProductType()
                      + "-" + purchasebd.getProduct().getProductName()));

                }


                return monoListPur
                    .flatMap(list -> {

                      if (list.size() == 0 && purchase.getProduct().getProductName()
                          .equals("CUENTA CORRIENTE PYME")) {

                        return Mono.error(new RuntimeException(msgCreditCard));

                      }

                      return service.create(purchasebd)
                          .map(createdObject -> {

                            purchase.setId(createdObject.getId());
                            producer.sendCreatePurchase(purchase);

                            return createdObject;

                          })
                          .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundCreate)));

                    });

              });

        });

  }

  @Override
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "updateFallback")
  public Mono<Purchase> updatePurchase(Purchase purchase, String id) {

    Mono<Purchase> customerModification = Mono.just(purchase);

    Mono<Purchase> customerDatabase = repository.findById(id);

    return customerDatabase
        .zipWith(customerModification, (a, b) -> {

          a.setProduct(purchase.getProduct());
          a.setCustomerOwner(purchase.getCustomerOwner());
          a.setAuthorizedSigner(purchase.getAuthorizedSigner());

          return a;

        })
        .flatMap(service::update)
        .map(objectUpdated -> {

          producer.sendCreatePurchase(objectUpdated);
          return objectUpdated;

        })
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundUpdate)));

  }

  @Override
  @CircuitBreaker(name = CIRCUIT, fallbackMethod = "deleteFallback")
  public Mono<Response> deletePurchase(String cardNumber) {

    Mono<Purchase> purchaseDatabase = findByCardNumber(cardNumber);

    return purchaseDatabase
        .flatMap(objectDelete -> service.delete(objectDelete.getId())
            .then(Mono.just(Response.builder().data(msgPurchaseDelete).build())))
        .switchIfEmpty(Mono.error(new RuntimeException(msgNotFoundDelete)));

  }

  /** Mensaje si no existen purchase. */
  public Mono<List<Purchase>> findAllFallback(Exception ex) {

    log.info("Compras no encontrados.");

    List<Purchase> list = new ArrayList<>();

    list.add(Purchase
        .builder()
        .cardNumber(ex.getMessage())
        .build());

    return Mono.just(list);

  }

  /** Mensaje si no existen productos. */
  public Mono<List<Product>> findAllProductFallback(Exception ex) {

    log.info("Productos no encontrados.");

    List<Product> list = new ArrayList<>();

    list.add(Product
        .builder()
        .productName(ex.getMessage())
        .build());

    return Mono.just(list);

  }

  /** Mensaje si falla el create. */
  public Mono<Purchase> createFallback(Purchase purchase, Exception ex) {

    log.info("Compra con numero de tarjeta {} no se pudo crear.", purchase.getCardNumber());

    return Mono.just(Purchase
        .builder()
        .id(ex.getMessage())
        .cardNumber(purchase.getCardNumber())
        .build());

  }

  /** Mensaje si falla el update. */
  public Mono<Purchase> updateFallback(Purchase purchase, String id,
      Exception ex) {

    log.info("Compra con id {} no encontrado para actualizar.", purchase.getId());

    return Mono.just(Purchase
        .builder()
        .id(ex.getMessage())
        .cardNumber(id)
        .build());

  }

  /** Mensaje si falla el delete. */
  public Mono<Response> deleteFallback(String cardNumber, Exception ex) {

    log.info("Compra con numero de tarjeta {} no encontrado para eliminar.", cardNumber);

    return Mono.just(Response
        .builder()
        .data(cardNumber)
        .error(ex.getMessage())
        .build());

  }

}
