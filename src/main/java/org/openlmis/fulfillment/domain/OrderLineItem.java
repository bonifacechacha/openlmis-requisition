package org.openlmis.fulfillment.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.openlmis.requisition.domain.BaseEntity;
import org.openlmis.requisition.domain.RequisitionLineItem;

import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "order_line_items")
@NoArgsConstructor
public class OrderLineItem extends BaseEntity {

  private static final String UUID = "pg-uuid";

  @ManyToOne(cascade = CascadeType.REFRESH)
  @JoinColumn(name = "orderId", nullable = false)
  @Getter
  @Setter
  private Order order;

  @Getter
  @Setter
  @Type(type = UUID)
  private UUID orderableProductId;

  @Column(nullable = false)
  @Getter
  @Setter
  private Long orderedQuantity;

  @Column(nullable = false)
  @Getter
  @Setter
  private Long filledQuantity;

  /**
   * Creates a new instance based on a given RequisitionLineItem.
   * @param requisitionLineItem RequisitionLineItem to create instance from.
   */
  public OrderLineItem(RequisitionLineItem requisitionLineItem) {
    setOrder(order);
    setOrderableProductId(requisitionLineItem.getOrderableProductId());
    setFilledQuantity(0L);
    setOrderedQuantity(requisitionLineItem.getRequestedQuantity().longValue());
  }

  /**
   * Copy values of attributes into new or updated OrderLineItem.
   *
   * @param orderLineItem OrderLineItem with new values.
   */
  public void updateFrom(OrderLineItem orderLineItem) {
    this.order = orderLineItem.getOrder();
    this.orderableProductId = orderLineItem.getOrderableProductId();
    this.orderedQuantity = orderLineItem.getOrderedQuantity();
    this.filledQuantity = orderLineItem.getFilledQuantity();
  }
}
