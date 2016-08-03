package org.openlmis.referencedata.domain;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openlmis.product.domain.Product;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "stocks", schema = "referencedata")
@NoArgsConstructor
public class Stock extends BaseEntity {

  @ManyToOne
  @JoinColumn(name = "stockInventoryId", nullable = false)
  @Getter
  @Setter
  @JsonIdentityInfo(
          generator = ObjectIdGenerators.IntSequenceGenerator.class,
          property = "@stockInventoryId")
  private StockInventory stockInventory;

  @ManyToOne
  @JoinColumn(name = "productId", nullable = false)
  @Getter
  @Setter
  private Product product;

  @Getter
  @Setter
  private Long storedQuantity;
}
