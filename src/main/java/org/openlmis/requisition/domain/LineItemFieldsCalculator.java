package org.openlmis.requisition.domain;

import org.openlmis.requisition.dto.StockAdjustmentReasonDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Optional;

import static org.apache.commons.lang.BooleanUtils.isTrue;

public final class LineItemFieldsCalculator {

  private LineItemFieldsCalculator() {}

  /**
   * Calculates beginningBalance (A) value of current requsition line item based on previous one
   * and returns it.
   * The formula is A = E + K
   * E = Stock on Hand
   * K = Approved Quantity
   *
   * @param previous line item from previous requsition
   */
  public static int calculateBeginningBalance(RequisitionLineItem previous) {

    if (null != previous) {
      return zeroIfNull(previous.getStockOnHand())
          + zeroIfNull(previous.getApprovedQuantity());
    }
    return 0;
  }

  /**
   * Calculates TotalConsumedQuantity (C) value and returns it.
   * The formula is C = A + B (+/-) D - E
   * A = Beginning Balance
   * B = Total Received Quantity
   * C = Total Consumed Quantity
   * D = Total Losses/Adjustments
   * E = Stock on Hand
   */
  public static int calculateTotalConsumedQuantity(RequisitionLineItem lineItem) {
    return zeroIfNull(lineItem.getBeginningBalance())
        + zeroIfNull(lineItem.getTotalReceivedQuantity())
        + zeroIfNull(lineItem.getTotalLossesAndAdjustments())
        - zeroIfNull(lineItem.getStockOnHand());
  }

  /**
   * Calculates Total (Y) value and returns it.
   * The formula is Y = A + B
   * A = Beginning Balance
   * B = Total Received Quantity
   */
  public static int calculateTotal(RequisitionLineItem lineItem) {
    return zeroIfNull(lineItem.getBeginningBalance())
        + zeroIfNull(lineItem.getTotalReceivedQuantity());
  }

  /**
   * Calculates StockOnHand (E) value and returns it.
   * The formula is E = A + B (+/-) D - C
   * A = Beginning Balance
   * B = Total Received Quantity
   * C = Total Consumed Quantity
   * D = Total Losses/Adjustments
   * E = Stock on Hand
   */
  public static int calculateStockOnHand(RequisitionLineItem lineItem) {
    return zeroIfNull(lineItem.getBeginningBalance())
        + zeroIfNull(lineItem.getTotalReceivedQuantity())
        + zeroIfNull(lineItem.getTotalLossesAndAdjustments())
        - zeroIfNull(lineItem.getTotalConsumedQuantity());
  }

  /**
   * Calculates TotalLossesAndAdjustments (D) value and returns it.
   * The property is calculated by taking all item's StockAdjustments and adding their quantities.
   * Values, whose StockAdjustmentReasons are additive, count as positive, and negative otherwise.
   */
  public static int calculateTotalLossesAndAdjustments(RequisitionLineItem lineItem,
                                                       Collection<StockAdjustmentReasonDto>
                                                           reasons) {
    int totalLossesAndAdjustments = 0;
    if (null != lineItem.getStockAdjustments()) {
      for (StockAdjustment adjustment : lineItem.getStockAdjustments()) {
        Optional<StockAdjustmentReasonDto> reason = reasons
            .stream()
            .filter(r -> r.getId().equals(adjustment.getReasonId()))
            .findFirst();

        if (reason.isPresent()) {
          int sign = isTrue(reason.get().getAdditive()) ? 1 : -1;

          totalLossesAndAdjustments += adjustment.getQuantity() * sign;
        }
      }
    }
    return totalLossesAndAdjustments;
  }

  /**
   * Calculates the total cost of the requisition line item, by multiplying price per pack
   * and packs to ship. If either one is null, zero will be returned.
   *
   * @param lineItem the line item to calculate the value for
   * @return a {@link Money} object representing the total cost for this line
   */
  public static Money calculateTotalCost(RequisitionLineItem lineItem) {
    Money pricePerPack = lineItem.getPricePerPack();
    if (pricePerPack == null) {
      pricePerPack = new Money(RequisitionLineItem.PRICE_PER_PACK_IF_NULL);
    }

    long packsToShip = zeroIfNull(lineItem.getPacksToShip());

    return pricePerPack.mul(packsToShip);
  }

  /**
   * Calculates Adjusted Consumption (N) value and returns it.
   * The formula is N = C * RoundUp((M * 30) / ((M * 30) - X))
   * C = Total Consumed Quantity
   * M = Months in the previous period
   * N = Adjusted Consumption
   * X = Total Stockout Days
   * If non-stockout days is zero the formula is N = C
   */
  public static int calculateAdjustedConsumption(RequisitionLineItem lineItem,
                                                 int monthsInThePeriod) {
    int consumedQuantity = zeroIfNull(lineItem.getTotalConsumedQuantity());

    if (consumedQuantity == 0) {
      return 0;
    }

    int totalDays = 30 * monthsInThePeriod;
    int stockoutDays = zeroIfNull(lineItem.getTotalStockoutDays());
    int nonStockoutDays = totalDays - stockoutDays;

    if (nonStockoutDays == 0) {
      return consumedQuantity;
    }

    BigDecimal adjustedConsumption = new BigDecimal(consumedQuantity).multiply(
        divideAndRoundUp(totalDays, new BigDecimal(nonStockoutDays)));

    return adjustedConsumption.intValue();
  }

  private static BigDecimal divideAndRoundUp(int totalDays, BigDecimal nonStockoutDays) {
    return new BigDecimal(totalDays).divide(nonStockoutDays, 0, RoundingMode.CEILING);
  }

  private static int zeroIfNull(Integer value) {
    return null == value ? 0 : value;
  }

  private static long zeroIfNull(Long value) {
    return null == value ? 0 : value;
  }
}
