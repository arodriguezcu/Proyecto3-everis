package com.everis.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Data;

/**
 * 
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Condition {
  
  private List<String> customerTypeTarget;
  
  private boolean hasMaintenanceFee;
  private Double maintenanceFee;
  
  private boolean hasMonthlyTransactionLimit;
  private Integer monthlyTransactionLimit;
  
  private boolean hasDailyMonthlyTransactionLimit;
  private Integer dailyMonthlyTransactionLimit;
  
  //  private boolean hasDailyWithdrawalTransactionLimit;
  //  private Integer dailyWithdrawalTransactionLimit;
  //  
  //  private boolean hasDailyDepositTransactionLimit;
  //  private Integer dailyDepositTransactionLimit;
  //  
  //  private Integer creditPerPersonLimit;
  //  private Integer creditPerBusinessLimit;
  
  private Integer productPerPersonLimit;
  private Integer productPerBusinessLimit; 
  
}
