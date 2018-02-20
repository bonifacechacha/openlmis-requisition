/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.requisition.domain.requisition;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.openlmis.requisition.dto.OrderableDto.COMMODITY_TYPE_IDENTIFIER;
import static org.openlmis.requisition.dto.ProofOfDeliveryStatus.CONFIRMED;
import static org.openlmis.requisition.dto.ProofOfDeliveryStatus.INITIATED;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openlmis.requisition.CurrencyConfig;
import org.openlmis.requisition.domain.RequisitionTemplate;
import org.openlmis.requisition.dto.ApprovedProductDto;
import org.openlmis.requisition.dto.ObjectReferenceDto;
import org.openlmis.requisition.dto.OrderableDto;
import org.openlmis.requisition.dto.ProofOfDeliveryDto;
import org.openlmis.requisition.dto.ProofOfDeliveryLineItemDto;
import org.openlmis.requisition.dto.SupervisoryNodeDto;
import org.openlmis.requisition.dto.SupplyLineDto;
import org.openlmis.requisition.exception.ValidationMessageException;
import org.openlmis.requisition.testutils.ApprovedProductDtoDataBuilder;
import org.openlmis.requisition.testutils.DtoGenerator;
import org.openlmis.requisition.testutils.OrderableDtoDataBuilder;
import org.openlmis.requisition.testutils.SupplyLineDtoDataBuilder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@PrepareForTest({LineItemFieldsCalculator.class})
@RunWith(PowerMockRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class RequisitionTest {
  private static final UUID ORDERABLE_ID = UUID.randomUUID();

  private static final CurrencyUnit CURRENCY_UNIT = CurrencyUnit.USD;
  private static final Money PRICE_PER_PACK = Money.of(CURRENCY_UNIT, 9);
  private static final int ADJUSTED_CONSUMPTION = 1;
  private static final int AVERAGE_CONSUMPTION = 1;
  private static final Money TOTAL_COST = Money.of(CURRENCY_UNIT, 5);
  private static final int MONTHS_IN_PERIOD = 1;
  private static final int CALCULATED_ORDER_QUANTITY = 5;
  private static final int REQUESTED_QUANTITY = 5;
  private static final int STOCK_ON_HAND = 10;
  private static final int STOCK_ON_HAND_2 = 11;

  private Requisition requisition;
  private RequisitionLineItem requisitionLineItem;

  private UUID productId = UUID.randomUUID();
  private UUID programId = UUID.randomUUID();

  @Mock
  private RequisitionTemplate template;

  @Before
  public void setUp() {
    requisition = new Requisition();
    requisitionLineItem = new RequisitionLineItem();

    requisitionLineItem.setId(UUID.randomUUID());
    requisitionLineItem.setRequestedQuantity(REQUESTED_QUANTITY);
    requisitionLineItem.setStockOnHand(20);
    requisitionLineItem.setPricePerPack(PRICE_PER_PACK);
    requisitionLineItem.setRequisition(requisition);
    requisitionLineItem.setOrderableId(productId);
    requisitionLineItem.setCalculatedOrderQuantity(CALCULATED_ORDER_QUANTITY);

    requisition.setStatus(RequisitionStatus.INITIATED);
    requisition.setRequisitionLineItems(Lists.newArrayList(requisitionLineItem));
    requisition.setNumberOfMonthsInPeriod(MONTHS_IN_PERIOD);
    requisition.setStockAdjustmentReasons(Collections.emptyList());
    requisition.setEmergency(false);
  }

  @Test
  public void shouldChangeStatusToRejectedAfterReject() {
    // given
    requisition.setTemplate(template);
    requisition.setStatus(RequisitionStatus.AUTHORIZED);

    // when
    requisition.reject(Collections.emptyList(), UUID.randomUUID());

    // then
    assertEquals(requisition.getStatus(), RequisitionStatus.REJECTED);
  }

  @Test
  public void shouldSubmitRequisitionIfItsStatusIsInitiated() {
    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.setStatus(RequisitionStatus.INITIATED);
    requisition.submit(Collections.emptyList(), UUID.randomUUID(), false);

    assertEquals(requisition.getStatus(), RequisitionStatus.SUBMITTED);
  }

  @Test
  public void shouldSubmitRequisitionIfItsStatusIsRejected() {
    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.setStatus(RequisitionStatus.REJECTED);
    requisition.submit(Collections.emptyList(), UUID.randomUUID(), false);

    assertEquals(requisition.getStatus(), RequisitionStatus.SUBMITTED);
  }

  @Test
  public void shouldSubmitRequisitionAndMarkAsAuthorizedWhenAuthorizeIsSkipped() {
    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.setStatus(RequisitionStatus.INITIATED);
    requisition.submit(Collections.emptyList(), UUID.randomUUID(), true);

    assertEquals(requisition.getStatus(), RequisitionStatus.AUTHORIZED);
  }

  @Test
  public void shouldAuthorizeRequisitionIfItsStatusIsSubmitted() {
    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.setStatus(RequisitionStatus.SUBMITTED);
    requisition.authorize(Collections.emptyList(), UUID.randomUUID());

    assertEquals(requisition.getStatus(), RequisitionStatus.AUTHORIZED);
  }

  @Test
  public void shouldInApprovalRequisitionIfItsStatusIsAuthorizedAndParentNodeExists() {
    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.setStatus(RequisitionStatus.AUTHORIZED);
    SupervisoryNodeDto parentNode = mockSupervisoryParentNode(UUID.randomUUID());

    requisition.approve(parentNode.getId(), Collections.emptyList(), Collections.emptyList(),
        UUID.randomUUID());

    assertEquals(requisition.getStatus(), RequisitionStatus.IN_APPROVAL);
  }

  @Test
  public void shouldInApprovalRequisitionIfItsStatusIsInApprovalAndParentNodeExists() {
    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.setStatus(RequisitionStatus.IN_APPROVAL);
    SupervisoryNodeDto parentNode = mockSupervisoryParentNode(UUID.randomUUID());

    requisition.approve(parentNode.getId(), Collections.emptyList(), Collections.emptyList(),
        UUID.randomUUID());

    assertEquals(requisition.getStatus(), RequisitionStatus.IN_APPROVAL);
  }

  @Test
  public void shouldApproveRequisitionIfItsStatusIsAuthorizedAndParentNodeDoesNotExist() {
    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.setStatus(RequisitionStatus.AUTHORIZED);
    SupervisoryNodeDto parentNode = mockSupervisoryParentNode(null);
    SupplyLineDto supplyLine = new SupplyLineDtoDataBuilder().build();

    requisition.approve(parentNode.getId(), Collections.emptyList(),
        Collections.singletonList(supplyLine), UUID.randomUUID());

    assertEquals(requisition.getStatus(), RequisitionStatus.APPROVED);
  }

  @Test
  public void shouldApproveRequisitionIfItsStatusIsInApprovalAndParentNodeNotExists() {
    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.setStatus(RequisitionStatus.IN_APPROVAL);
    SupervisoryNodeDto parentNode = mockSupervisoryParentNode(null);
    SupplyLineDto supplyLine = new SupplyLineDtoDataBuilder().build();

    requisition.approve(parentNode.getId(), Collections.emptyList(),
        Collections.singletonList(supplyLine), UUID.randomUUID());

    assertEquals(requisition.getStatus(), RequisitionStatus.APPROVED);
  }

  @Test(expected = ValidationMessageException.class)
  public void shouldThrowExceptionWhenAuthorizingRequisitionWithNotSubmittedStatus() {
    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.authorize(Collections.emptyList(), UUID.randomUUID());
  }

  @Test
  public void shouldCalculateStockOnHandForRequisitionLineItemsWhenAuthorizing() {
    RequisitionTemplate requisitionTemplate = mock(RequisitionTemplate.class);
    PowerMockito.spy(LineItemFieldsCalculator.class);
    RequisitionLineItem requisitionLineItem = mock(RequisitionLineItem.class);

    requisition.setRequisitionLineItems(new ArrayList<>(
        Collections.singletonList(requisitionLineItem)));

    when(requisitionTemplate.isColumnDisplayed("stockOnHand")).thenReturn(true);
    when(requisitionTemplate.isColumnCalculated("stockOnHand")).thenReturn(true);
    when(LineItemFieldsCalculator.calculateTotal(requisitionLineItem))
        .thenReturn(1);

    requisition.setTemplate(requisitionTemplate);
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    requisition.authorize(Collections.emptyList(), UUID.randomUUID());
    requisition.updateFrom(new Requisition(), Collections.emptyList(), true);

    assertEquals(requisition.getStatus(), RequisitionStatus.AUTHORIZED);
    verifyStatic(times(1));
  }

  @Test
  public void shouldDefaultApprovedQuantityOnAuthorize() {
    requisition.setTemplate(template);
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    when(template.isColumnDisplayed(RequisitionLineItem.CALCULATED_ORDER_QUANTITY))
        .thenReturn(true);

    requisitionLineItem.setRequestedQuantity(null);

    requisition.authorize(Collections.emptyList(), null);

    assertEquals(Integer.valueOf(CALCULATED_ORDER_QUANTITY),
        requisitionLineItem.getApprovedQuantity());
  }

  @Test
  public void shouldDefaultApprovedQuantityOnSubmitWhenAuthorizationStepSkipped() {
    requisition.setTemplate(template);
    requisition.setStatus(RequisitionStatus.INITIATED);

    when(template.isColumnDisplayed(RequisitionLineItem.CALCULATED_ORDER_QUANTITY))
        .thenReturn(true);

    requisitionLineItem.setRequestedQuantity(null);

    requisition.submit(Collections.emptyList(), null, true);

    assertEquals(Integer.valueOf(CALCULATED_ORDER_QUANTITY),
        requisitionLineItem.getApprovedQuantity());
  }

  @Test
  public void shouldPopulateWithReqQuantityWhenIsNotNullAndCalcOrderQuantityColumnDisplayed() {
    requisition.setTemplate(template);
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    when(template.isColumnDisplayed(RequisitionLineItem.CALCULATED_ORDER_QUANTITY))
        .thenReturn(true);

    requisition.authorize(Collections.emptyList(), null);

    assertEquals(Integer.valueOf(REQUESTED_QUANTITY), requisitionLineItem.getApprovedQuantity());
  }

  @Test
  public void shouldPopulateWithRequestedQuantityWhenCalculatedOrderQuantityIsNotDisplayed() {
    requisition.setTemplate(template);
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    when(template.isColumnDisplayed(RequisitionLineItem.CALCULATED_ORDER_QUANTITY))
        .thenReturn(false);

    requisitionLineItem.setRequestedQuantity(REQUESTED_QUANTITY);

    requisition.authorize(Collections.emptyList(), null);

    assertEquals(Integer.valueOf(REQUESTED_QUANTITY), requisitionLineItem.getApprovedQuantity());
  }

  @Test
  public void shouldPopulateNonFullSupplyLineItems() {
    requisition.setTemplate(template);
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    when(template.isColumnDisplayed(RequisitionLineItem.CALCULATED_ORDER_QUANTITY))
        .thenReturn(false);

    requisitionLineItem.setRequestedQuantity(REQUESTED_QUANTITY);
    requisitionLineItem.setNonFullSupply(true);

    requisition.authorize(Collections.emptyList(), null);

    assertEquals(Integer.valueOf(REQUESTED_QUANTITY), requisitionLineItem.getApprovedQuantity());
  }

  @Test
  public void shouldPopulateWithNullRequestedQuantityWhenCalculatedOrderQuantityIsNotDisplayed() {
    requisition.setTemplate(template);
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    when(template.isColumnDisplayed(RequisitionLineItem.CALCULATED_ORDER_QUANTITY))
        .thenReturn(false);

    requisitionLineItem.setRequestedQuantity(null);

    requisition.authorize(Collections.emptyList(), null);

    assertNull(requisitionLineItem.getApprovedQuantity());
  }

  @Test
  public void shouldCalculateTotalValueWhenUpdatingRequisition() {
    RequisitionTemplate requisitionTemplate = mock(RequisitionTemplate.class);
    PowerMockito.spy(LineItemFieldsCalculator.class);
    RequisitionLineItem requisitionLineItem = mock(RequisitionLineItem.class);

    when(requisitionTemplate.isColumnDisplayed("total")).thenReturn(true);
    when(LineItemFieldsCalculator.calculateStockOnHand(requisitionLineItem))
        .thenReturn(1);

    requisition.setRequisitionLineItems(new ArrayList<>(
        Collections.singletonList(requisitionLineItem)));

    requisition.setTemplate(requisitionTemplate);
    requisition.updateFrom(new Requisition(), Collections.emptyList(), true);
    verifyStatic(times(1));
  }

  @Test
  public void shouldAddOnlyNonFullSupplyLinesForRegularRequisition() throws Exception {
    requisition.getRequisitionLineItems().clear();
    requisition.setEmergency(false);

    RequisitionLineItem fullSupply = new RequisitionLineItem();
    RequisitionLineItem nonFullSupply = new RequisitionLineItem();
    nonFullSupply.setNonFullSupply(true);

    Requisition newRequisition = new Requisition();
    newRequisition.setRequisitionLineItems(Lists.newArrayList(fullSupply, nonFullSupply));

    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.updateFrom(newRequisition, Collections.emptyList(), true);

    assertThat(requisition.getRequisitionLineItems(), hasSize(1));

    RequisitionLineItem item = requisition.getRequisitionLineItems().get(0);
    assertThat(item.isNonFullSupply(), is(true));
  }

  @Test
  public void shouldNotRemoveFullSupplyLinesForRegularRequisition() throws Exception {
    requisition.setEmergency(false);

    RequisitionLineItem nonFullSupply = new RequisitionLineItem();
    nonFullSupply.setNonFullSupply(true);

    Requisition newRequisition = new Requisition();
    newRequisition.setRequisitionLineItems(Lists.newArrayList(nonFullSupply));

    int count = requisition.getRequisitionLineItems().size();
    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.updateFrom(newRequisition, Collections.emptyList(), true);

    assertThat(requisition.getRequisitionLineItems(), hasSize(count + 1));

    assertThat(
        requisition.getRequisitionLineItems().stream()
            .filter(RequisitionLineItem::isNonFullSupply).count(),
        is(1L)
    );

    assertThat(
        requisition.getRequisitionLineItems().stream()
            .filter(line -> !line.isNonFullSupply()).count(),
        is((long) count)
    );
  }

  @Test
  public void shouldAddFullSupplyLinesForEmergencyRequisition() throws Exception {
    requisition.setRequisitionLineItems(Lists.newArrayList());
    requisition.setEmergency(true);

    RequisitionLineItem fullSupply = new RequisitionLineItem();
    RequisitionLineItem nonFullSupply = new RequisitionLineItem();
    nonFullSupply.setNonFullSupply(true);

    Requisition newRequisition = new Requisition();
    newRequisition.setRequisitionLineItems(Lists.newArrayList(fullSupply, nonFullSupply));

    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.updateFrom(newRequisition, Collections.emptyList(), true);

    assertThat(requisition.getRequisitionLineItems(), hasSize(2));
    assertThat(requisition.getRequisitionLineItems().get(0).isNonFullSupply(), is(false));
    assertThat(requisition.getRequisitionLineItems().get(1).isNonFullSupply(), is(true));
  }

  @Test
  public void shouldRemoveFullSupplyLinesForEmergencyRequisition() throws Exception {
    RequisitionLineItem fullSupply = new RequisitionLineItem();
    fullSupply.setId(UUID.randomUUID());

    requisition.setRequisitionLineItems(Lists.newArrayList(fullSupply));
    requisition.setEmergency(true);

    RequisitionLineItem nonFullSupply = new RequisitionLineItem();
    nonFullSupply.setNonFullSupply(true);

    Requisition newRequisition = new Requisition();
    newRequisition.setRequisitionLineItems(Lists.newArrayList(nonFullSupply));

    requisition.setTemplate(mock(RequisitionTemplate.class));
    requisition.updateFrom(newRequisition, Collections.emptyList(), true);

    assertThat(requisition.getRequisitionLineItems(), hasSize(1));
    assertThat(requisition.getRequisitionLineItems().get(0).isNonFullSupply(), is(true));
  }

  @Test
  public void shouldFindRequisitionLineItemByProductId() {
    RequisitionLineItem found = requisition.findLineByProductId(productId);

    assertNotNull(found);
    assertEquals(requisitionLineItem, found);
  }

  @Test
  public void shouldSetRequisitionFieldForLineItemsAfterUpdate() throws Exception {
    // given
    Requisition newRequisition = new Requisition();

    // existing line
    RequisitionLineItem firstRequisitionLineItem = new RequisitionLineItem();
    firstRequisitionLineItem.setId(requisitionLineItem.getId());
    firstRequisitionLineItem.setRequestedQuantity(10);
    firstRequisitionLineItem.setStockOnHand(20);
    firstRequisitionLineItem.setRequisition(newRequisition);
    firstRequisitionLineItem.setOrderableId(productId);

    // new line
    RequisitionLineItem secondRequisitionLineItem = new RequisitionLineItem();
    secondRequisitionLineItem.setRequestedQuantity(10);
    secondRequisitionLineItem.setStockOnHand(20);
    secondRequisitionLineItem.setRequisition(newRequisition);
    secondRequisitionLineItem.setOrderableId(productId);

    newRequisition.setId(UUID.randomUUID());
    newRequisition.setStatus(RequisitionStatus.INITIATED);
    newRequisition.setRequisitionLineItems(
        Lists.newArrayList(firstRequisitionLineItem, secondRequisitionLineItem)
    );

    // when
    requisition.setTemplate(template);
    requisition.setId(UUID.randomUUID());
    requisition.updateFrom(newRequisition, Collections.emptyList(), true);

    // then
    requisition
        .getRequisitionLineItems()
        .forEach(line -> assertThat(line.getRequisition().getId(), is(requisition.getId())));
  }

  @Test
  public void shouldSetNullForCalculatedValuesIfColumnIsHidden() throws Exception {
    requisitionLineItem.setStockOnHand(10);
    requisitionLineItem.setTotalConsumedQuantity(15);

    RequisitionTemplate requisitionTemplate = mock(RequisitionTemplate.class);

    when(requisitionTemplate.isColumnDisplayed("stockOnHand")).thenReturn(false);
    when(requisitionTemplate.isColumnDisplayed("totalConsumedQuantity")).thenReturn(false);

    requisition.setTemplate(requisitionTemplate);
    requisition.updateFrom(new Requisition(), Collections.emptyList(), true);

    assertThat(requisitionLineItem.getStockOnHand(), is(nullValue()));
    assertThat(requisitionLineItem.getTotalConsumedQuantity(), is(nullValue()));

  }

  @Test
  public void shouldSetDatePhysicalStockCountCompletedWhenUpdateRequisition() {
    Requisition requisition = updateWithDatePhysicalCountCompleted(true);
    assertEquals(
        requisition.getDatePhysicalStockCountCompleted(),
        this.requisition.getDatePhysicalStockCountCompleted());
  }

  @Test
  public void shouldNotSetDatePhysicalStockCountCompletedWhenItIsDisabled() {
    updateWithDatePhysicalCountCompleted(false);
    assertNull(this.requisition.getDatePhysicalStockCountCompleted());
  }

  @Test
  public void shouldReturnZeroIfNoTotalCosts() {
    // given;
    requisition.setRequisitionLineItems(Collections.emptyList());

    // when
    Money result = requisition.getTotalCost();
    BigDecimal amount = result.getAmount();

    // then
    assertEquals(0, 0, amount.doubleValue());
  }

  @Test
  public void shouldGetTotalCost() {
    // given;
    setUpGetTotalCost(BigDecimal.ONE, BigDecimal.ONE);

    // when
    Money result = requisition.getTotalCost();
    BigDecimal amount = result.getAmount();

    // then
    assertEquals(2, 0, amount.doubleValue());
  }

  @Test
  public void shouldGetFullSupplyTotalCost() {
    // given;
    setUpGetTotalCost(BigDecimal.ONE, BigDecimal.ZERO);

    // when
    Money result = requisition.getFullSupplyTotalCost();
    BigDecimal amount = result.getAmount();

    // then
    assertEquals(1, 0, amount.doubleValue());
  }

  @Test
  public void shouldGetNonFullSupplyTotalCost() {
    // given;
    setUpGetTotalCost(BigDecimal.ZERO, BigDecimal.ONE);

    // when
    Money result = requisition.getNonFullSupplyTotalCost();
    BigDecimal amount = result.getAmount();

    // then
    assertEquals(1, 0, amount.doubleValue());
  }

  @Test
  public void shouldInitiateRequisitionLineItemFieldsIfPreviousRequisitionProvided() {
    // given
    final UUID productId1 = UUID.randomUUID();
    final UUID productId2 = UUID.randomUUID();

    Requisition previousRequisition = mock(Requisition.class);
    List<RequisitionLineItem> items = new ArrayList<>();
    items.add(mockReqLine(previousRequisition, productId1, STOCK_ON_HAND));
    items.add(mockReqLine(previousRequisition, productId2, STOCK_ON_HAND_2));

    when(previousRequisition.getRequisitionLineItems()).thenReturn(items);

    when(template.isColumnDisplayed(RequisitionLineItem.BEGINNING_BALANCE)).thenReturn(true);

    ApprovedProductDto product1 = mockApprovedProduct(productId1);
    ApprovedProductDto product2 = mockApprovedProduct(productId2);

    // when
    Requisition req = new Requisition();
    req.setProgramId(programId);
    req.initiate(template, asList(product1, product2),
        Collections.singletonList(previousRequisition), 0, null, emptyMap(), UUID.randomUUID(),
        emptyMap());

    // then
    List<RequisitionLineItem> lineItems = req.getRequisitionLineItems();

    assertEquals(2, lineItems.size());
    assertThat(req.findLineByProductId(productId1).getBeginningBalance(), is(STOCK_ON_HAND));
    assertThat(req.findLineByProductId(productId2).getBeginningBalance(), is(STOCK_ON_HAND_2));
    assertThat(req.findLineByProductId(productId1).getTotalReceivedQuantity(), is(nullValue()));
    assertThat(req.findLineByProductId(productId2).getTotalReceivedQuantity(), is(nullValue()));
  }

  @Test
  public void shouldInitiateRequisitionLineItemFieldWithOrderableId() {
    // given
    final UUID productId1 = UUID.randomUUID();

    ApprovedProductDto product1 = mockApprovedProduct(productId1);

    // when
    Requisition req = new Requisition();
    req.setProgramId(programId);
    req.initiate(template, Collections.singleton(product1),
        Collections.emptyList(), 0, null, emptyMap(), UUID.randomUUID(), emptyMap());

    // then
    List<RequisitionLineItem> lineItems = req.getRequisitionLineItems();

    assertThat(lineItems.get(0).getOrderableId(), is(productId1));
  }

  @Test
  public void shouldInsertValueFromProofOfDeliveryToTotalReceivedQuantity() throws Exception {
    // given
    final UUID productId1 = UUID.randomUUID();
    final UUID productId2 = UUID.randomUUID();

    Requisition previousRequisition = mock(Requisition.class);
    List<RequisitionLineItem> items = new ArrayList<>();
    items.add(mockReqLine(previousRequisition, productId1, STOCK_ON_HAND));
    items.add(mockReqLine(previousRequisition, productId2, STOCK_ON_HAND_2));

    when(previousRequisition.getRequisitionLineItems()).thenReturn(items);

    when(template.isColumnDisplayed(RequisitionLineItem.BEGINNING_BALANCE)).thenReturn(true);

    ProofOfDeliveryDto pod = DtoGenerator.of(ProofOfDeliveryDto.class);
    pod.setStatus(CONFIRMED);
    pod.setLineItems(DtoGenerator.of(ProofOfDeliveryLineItemDto.class, 2));
    pod.getLineItems().get(0).setOrderable(new ObjectReferenceDto(productId1));
    pod.getLineItems().get(1).setOrderable(new ObjectReferenceDto(productId2));

    ApprovedProductDto product1 = mockApprovedProduct(productId1);
    ApprovedProductDto product2 = mockApprovedProduct(productId2);

    // when
    Requisition req = new Requisition();
    req.setProgramId(programId);
    req.initiate(template, asList(product1, product2),
        Collections.singletonList(previousRequisition), 0, pod, emptyMap(), UUID.randomUUID(),
        emptyMap());

    // then
    List<RequisitionLineItem> lineItems = req.getRequisitionLineItems();

    assertEquals(2, lineItems.size());
    assertThat(req.findLineByProductId(productId1).getBeginningBalance(), is(STOCK_ON_HAND));
    assertThat(req.findLineByProductId(productId2).getBeginningBalance(), is(STOCK_ON_HAND_2));
    assertThat(
        req.findLineByProductId(productId1).getTotalReceivedQuantity(),
        is(pod.getLineItems().get(0).getQuantityAccepted()));
    assertThat(
        req.findLineByProductId(productId2).getTotalReceivedQuantity(),
        is(pod.getLineItems().get(1).getQuantityAccepted()));
  }

  @Test
  public void shouldNotInsertValueFromProofOfDeliveryIfNotSubmitted() throws Exception {
    // given
    final UUID productId1 = UUID.randomUUID();
    final UUID productId2 = UUID.randomUUID();

    Requisition previousRequisition = mock(Requisition.class);
    List<RequisitionLineItem> items = new ArrayList<>();
    items.add(mockReqLine(previousRequisition, productId1, STOCK_ON_HAND));
    items.add(mockReqLine(previousRequisition, productId2, STOCK_ON_HAND_2));

    when(previousRequisition.getRequisitionLineItems()).thenReturn(items);

    when(template.isColumnDisplayed(RequisitionLineItem.BEGINNING_BALANCE)).thenReturn(true);

    ProofOfDeliveryDto pod = DtoGenerator.of(ProofOfDeliveryDto.class);
    pod.setStatus(INITIATED);

    ApprovedProductDto product1 = mockApprovedProduct(productId1);
    ApprovedProductDto product2 = mockApprovedProduct(productId2);

    // when
    Requisition req = new Requisition();
    req.setProgramId(programId);
    req.initiate(template, asList(product1, product2),
        Collections.singletonList(previousRequisition), 0, pod, emptyMap(), UUID.randomUUID(),
        emptyMap());

    // then
    List<RequisitionLineItem> lineItems = req.getRequisitionLineItems();

    assertEquals(2, lineItems.size());
    assertThat(req.findLineByProductId(productId1).getBeginningBalance(), is(STOCK_ON_HAND));
    assertThat(req.findLineByProductId(productId2).getBeginningBalance(), is(STOCK_ON_HAND_2));
    assertThat(req.findLineByProductId(productId1).getTotalReceivedQuantity(), is(nullValue()));
    assertThat(req.findLineByProductId(productId2).getTotalReceivedQuantity(), is(nullValue()));
  }

  @Test
  public void shouldReturnNonSkippedRequisitionLineItems() {
    RequisitionLineItem notSkipped = getRequisitionLineItem(false);
    RequisitionLineItem skipped = getRequisitionLineItem(true);

    Requisition requisition = getRequisition(notSkipped, skipped);
    List<RequisitionLineItem> nonSkippedRequisitionLineItems =
        requisition.getNonSkippedRequisitionLineItems();
    RequisitionLineItem requisitionLineItem =
        nonSkippedRequisitionLineItems.get(0);

    assertEquals(1, nonSkippedRequisitionLineItems.size());
    assertEquals(notSkipped.getId(), requisitionLineItem.getId());
  }

  @Test
  public void shouldReturnNonSkippedFullSupplyRequisitionLineItems() {
    RequisitionLineItem notSkipped = getRequisitionLineItem(false);
    RequisitionLineItem skipped = getRequisitionLineItem(true);
    RequisitionLineItem nullSkippedValueLine = getRequisitionLineItem(null);

    Requisition requisition = getRequisition(notSkipped, skipped);
    requisition.getRequisitionLineItems().add(nullSkippedValueLine);
    List<RequisitionLineItem> nonSkippedRequisitionLineItems =
        requisition.getNonSkippedFullSupplyRequisitionLineItems();
    RequisitionLineItem requisitionLineItem =
        nonSkippedRequisitionLineItems.get(0);

    assertEquals(2, nonSkippedRequisitionLineItems.size());
    assertEquals(notSkipped.getId(), requisitionLineItem.getId());
  }

  @Test
  public void shouldGetNonSkippedNonFullSupplyRequisitionLineItems() {
    // given
    RequisitionLineItem notSkipped = getRequisitionLineItem(false, false);
    RequisitionLineItem skipped = getRequisitionLineItem(true, false);
    Requisition requisition = getRequisition(notSkipped, skipped);

    // when
    List<RequisitionLineItem> result = requisition.getNonSkippedNonFullSupplyRequisitionLineItems();

    // then
    assertEquals(1, result.size());
    assertEquals(notSkipped.getId(), result.get(0).getId());
  }

  @Test
  public void shouldReturnSkippedRequisitionLineItems() {
    RequisitionLineItem notSkipped = getRequisitionLineItem(false);
    RequisitionLineItem skipped = getRequisitionLineItem(true);

    Requisition requisition = getRequisition(notSkipped, skipped);
    List<RequisitionLineItem> skippedRequisitionLineItems =
        requisition.getSkippedRequisitionLineItems();
    RequisitionLineItem requisitionLineItem =
        skippedRequisitionLineItems.get(0);

    assertEquals(1, skippedRequisitionLineItems.size());
    assertEquals(skipped.getId(), requisitionLineItem.getId());
  }

  @Test
  public void shouldUpdatePacksToShipOnSubmit() {
    // given
    long packsToShip = 5L;
    OrderableDto product = new OrderableDtoDataBuilder().withNetContent(1).build();

    setUpTestUpdatePacksToShip(product, packsToShip);

    // when
    requisition.submit(Collections.singletonList(product), UUID.randomUUID(), false);

    // then
    assertEquals(packsToShip, requisitionLineItem.getPacksToShip().longValue());
  }

  @Test
  public void shouldUpdatePacksToShipOnAuthorize() {
    // given
    long packsToShip = 5L;
    OrderableDto product = new OrderableDtoDataBuilder().withNetContent(1).build();

    setUpTestUpdatePacksToShip(product, packsToShip);
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    // when
    requisition.authorize(Collections.singletonList(product), UUID.randomUUID());

    // then
    assertEquals(packsToShip, requisitionLineItem.getPacksToShip().longValue());
  }

  @Test
  public void shouldUpdatePacksToShipOnApprove() {
    // given
    long packsToShip = 5L;
    OrderableDto product = new OrderableDtoDataBuilder().withNetContent(1).build();

    setUpTestUpdatePacksToShip(product, packsToShip);
    requisition.setStatus(RequisitionStatus.AUTHORIZED);

    // when
    requisition.approve(null, Collections.singletonList(product), Collections.emptyList(),
        UUID.randomUUID());

    // then
    assertEquals(packsToShip, requisitionLineItem.getPacksToShip().longValue());
  }

  @Test
  public void shouldCalculateAdjustedConsumptionTotalCostAndAverageConsumptionWhenSubmit() {
    // given
    prepareForTestAdjustedConcumptionTotalCostAndAverageConsumption();

    //when
    requisition.submit(Collections.emptyList(), UUID.randomUUID(), false);

    //then
    assertEquals(ADJUSTED_CONSUMPTION, requisitionLineItem.getAdjustedConsumption().longValue());
    assertEquals(AVERAGE_CONSUMPTION, requisitionLineItem.getAverageConsumption().longValue());
    assertEquals(TOTAL_COST, requisitionLineItem.getTotalCost());
  }

  @Test
  public void shouldCalculateAdjustedConsumptionTotalCostAndAverageConsumptionWhenReject() {
    // given
    prepareForTestAdjustedConcumptionTotalCostAndAverageConsumption();
    requisition.setStatus(RequisitionStatus.AUTHORIZED);

    //when
    requisition.reject(Collections.emptyList(), UUID.randomUUID());

    //then
    assertEquals(ADJUSTED_CONSUMPTION, requisitionLineItem.getAdjustedConsumption().longValue());
    assertEquals(AVERAGE_CONSUMPTION, requisitionLineItem.getAverageConsumption().longValue());
    assertEquals(TOTAL_COST, requisitionLineItem.getTotalCost());
  }

  @Test
  public void shouldCalculateAverageConsumptionWhenSubmitWithOnePreviousRequisition() {
    // given

    List<Integer> adjustedConsumptions = new ArrayList<>();
    adjustedConsumptions.add(5);
    prepareForTestAverageConsumption(adjustedConsumptions);
    when(LineItemFieldsCalculator
        .calculateAverageConsumption(Arrays.asList(5, ADJUSTED_CONSUMPTION)))
        .thenReturn(AVERAGE_CONSUMPTION);

    //when
    requisition.submit(Collections.emptyList(), UUID.randomUUID(), false);

    //then
    assertEquals(ADJUSTED_CONSUMPTION, requisitionLineItem.getAdjustedConsumption().longValue());
    assertEquals(AVERAGE_CONSUMPTION, requisitionLineItem.getAverageConsumption().longValue());
  }

  @Test
  public void shouldCalculateAverageConsumptionWhenSubmitWithManyPreviousRequisitions() {
    // given
    List<Integer> adjustedConsumptions = new ArrayList<>();
    adjustedConsumptions.add(5);
    adjustedConsumptions.add(5);
    adjustedConsumptions.add(5);
    prepareForTestAverageConsumption(adjustedConsumptions);
    when(LineItemFieldsCalculator
        .calculateAverageConsumption(Arrays.asList(5, 5, 5, ADJUSTED_CONSUMPTION)))
        .thenReturn(AVERAGE_CONSUMPTION);

    //when
    requisition.submit(Collections.emptyList(), UUID.randomUUID(), false);

    //then
    assertEquals(ADJUSTED_CONSUMPTION, requisitionLineItem.getAdjustedConsumption().longValue());
    assertEquals(AVERAGE_CONSUMPTION, requisitionLineItem.getAverageConsumption().longValue());
  }

  @Test
  public void shouldCalculateAdjustedConsumptionTotalCostAndAverageConsumptionWhenAuthorize() {
    // given
    prepareForTestAdjustedConcumptionTotalCostAndAverageConsumption();
    requisition.setStatus(RequisitionStatus.SUBMITTED);

    //when
    requisition.authorize(Collections.emptyList(), UUID.randomUUID());

    //then
    assertEquals(ADJUSTED_CONSUMPTION, requisitionLineItem.getAdjustedConsumption().longValue());
    assertEquals(AVERAGE_CONSUMPTION, requisitionLineItem.getAverageConsumption().longValue());
    assertEquals(TOTAL_COST, requisitionLineItem.getTotalCost());
  }

  @Test
  public void shouldCalculateAdjustedConsumptionTotalCostAndAverageConsumptionWhenApprove() {
    // given
    prepareForTestAdjustedConcumptionTotalCostAndAverageConsumption();
    requisition.setStatus(RequisitionStatus.APPROVED);

    //when
    requisition.approve(null, Collections.emptyList(), Collections.emptyList(),
        UUID.randomUUID());

    //then
    assertEquals(ADJUSTED_CONSUMPTION, requisitionLineItem.getAdjustedConsumption().longValue());
    assertEquals(AVERAGE_CONSUMPTION, requisitionLineItem.getAverageConsumption().longValue());
    assertEquals(TOTAL_COST, requisitionLineItem.getTotalCost());
  }

  @Test
  public void shouldSetPreviousAdjustedConsumptionsWhenOnePreviousRequisition() {
    RequisitionLineItem requisitionLineItem = new RequisitionLineItem();
    requisitionLineItem.setOrderableId(ORDERABLE_ID);

    RequisitionLineItem previousRequisitionLineItem = new RequisitionLineItem();
    previousRequisitionLineItem.setAdjustedConsumption(5);
    previousRequisitionLineItem.setOrderableId(ORDERABLE_ID);
    Requisition previousRequisition = new Requisition();
    previousRequisition.setRequisitionLineItems(
        Collections.singletonList(previousRequisitionLineItem));

    Requisition requisition = new Requisition();
    requisition.setRequisitionLineItems(Lists.newArrayList(requisitionLineItem));

    requisition.setPreviousRequisitions(Collections.singletonList(previousRequisition));
    requisition.setPreviousAdjustedConsumptions(1);

    assertEquals(Collections.singletonList(5),
        requisitionLineItem.getPreviousAdjustedConsumptions());
  }

  @Test
  public void shouldSetPreviousAdjustedConsumptionsFromManyPreviousRequisitions() {
    RequisitionLineItem requisitionLineItem = new RequisitionLineItem();
    requisitionLineItem.setOrderableId(ORDERABLE_ID);

    RequisitionLineItem previousRequisitionLineItem = new RequisitionLineItem();
    previousRequisitionLineItem.setAdjustedConsumption(5);
    previousRequisitionLineItem.setOrderableId(ORDERABLE_ID);
    Requisition previousRequisition = new Requisition();
    previousRequisition
        .setRequisitionLineItems(Arrays.asList(previousRequisitionLineItem,
            previousRequisitionLineItem, previousRequisitionLineItem));

    Requisition requisition = new Requisition();
    requisition.setRequisitionLineItems(Lists.newArrayList(requisitionLineItem));

    requisition.setPreviousRequisitions(Collections.singletonList(previousRequisition));
    requisition.setPreviousAdjustedConsumptions(1);

    assertEquals(Arrays.asList(5, 5, 5), requisitionLineItem.getPreviousAdjustedConsumptions());
  }

  @Test
  public void shouldNotAddPreviousAdjustedConsumptionIfLineSkipped() {
    RequisitionLineItem requisitionLineItem = new RequisitionLineItem();
    requisitionLineItem.setOrderableId(ORDERABLE_ID);

    RequisitionLineItem previousRequisitionLineItem = new RequisitionLineItem();
    previousRequisitionLineItem.setAdjustedConsumption(5);
    previousRequisitionLineItem.setSkipped(true);
    previousRequisitionLineItem.setOrderableId(ORDERABLE_ID);
    Requisition previousRequisition = new Requisition();
    previousRequisition.setRequisitionLineItems(
        Collections.singletonList(previousRequisitionLineItem));

    Requisition requisition = new Requisition();
    requisition.setRequisitionLineItems(Lists.newArrayList(requisitionLineItem));

    requisition.setPreviousRequisitions(Collections.singletonList(previousRequisition));
    requisition.setPreviousAdjustedConsumptions(1);

    assertEquals(Collections.emptyList(),
        requisitionLineItem.getPreviousAdjustedConsumptions());
  }

  @Test
  public void shouldNotAddPreviousAdjustedConsumptionIfNull() {
    RequisitionLineItem requisitionLineItem = new RequisitionLineItem();
    requisitionLineItem.setOrderableId(ORDERABLE_ID);

    RequisitionLineItem previousRequisitionLineItem = new RequisitionLineItem();
    previousRequisitionLineItem.setAdjustedConsumption(null);
    previousRequisitionLineItem.setOrderableId(ORDERABLE_ID);
    Requisition previousRequisition = new Requisition();
    previousRequisition.setRequisitionLineItems(
        Collections.singletonList(previousRequisitionLineItem));

    Requisition requisition = new Requisition();
    requisition.setRequisitionLineItems(Lists.newArrayList(requisitionLineItem));

    requisition.setPreviousRequisitions(Collections.singletonList(previousRequisition));
    requisition.setPreviousAdjustedConsumptions(1);

    assertEquals(Collections.emptyList(),
        requisitionLineItem.getPreviousAdjustedConsumptions());
  }

  @Test
  public void shouldNotUpdatePreviousAdjustedConsumptions() {
    requisitionLineItem.setPreviousAdjustedConsumptions(Lists.newArrayList(1));

    RequisitionLineItem newLineItem = new RequisitionLineItem();
    newLineItem.setPreviousAdjustedConsumptions(Collections.singletonList(2));
    newLineItem.setId(requisitionLineItem.getId());

    Requisition newRequisition = new Requisition();
    newRequisition.setRequisitionLineItems(Lists.newArrayList(newLineItem));

    RequisitionTemplate requisitionTemplate = mock(RequisitionTemplate.class);
    requisition.setTemplate(requisitionTemplate);
    requisition.updateFrom(newRequisition, Collections.emptyList(), true);

    assertEquals(Integer.valueOf(1), requisitionLineItem.getPreviousAdjustedConsumptions().get(0));
  }

  @Test
  public void shouldRecordStatusChangeOnInitiate() {
    UUID initiatorId = UUID.randomUUID();
    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);

    requisition.initiate(template, Collections.emptyList(), Collections.emptyList(), 0, null,
        emptyMap(), initiatorId, emptyMap());

    assertStatusChangeExistsAndAuthorIdMatches(requisition, RequisitionStatus.INITIATED,
        initiatorId);
  }

  @Test
  public void shouldRecordStatusChangeOnSubmit() {
    UUID submitterId = UUID.randomUUID();
    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    requisition.setTemplate(template);

    requisition.submit(Collections.emptyList(), submitterId, false);

    assertStatusChangeExistsAndAuthorIdMatches(requisition, RequisitionStatus.SUBMITTED,
        submitterId);
  }

  @Test
  public void shouldRecordStatusChangeOnAuthorize() {
    UUID authorizerId = UUID.randomUUID();
    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.SUBMITTED);
    requisition.setTemplate(template);
    requisition.setRequisitionLineItems(Collections.emptyList());

    requisition.authorize(Collections.emptyList(), authorizerId);

    assertStatusChangeExistsAndAuthorIdMatches(requisition, RequisitionStatus.AUTHORIZED,
        authorizerId);
  }

  @Test
  public void shouldRecordStatusChangeOnApprove() {
    UUID approverId = UUID.randomUUID();
    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.AUTHORIZED);
    requisition.setTemplate(template);
    requisition.setRequisitionLineItems(Collections.emptyList());

    requisition.approve(null, Collections.emptyList(), Collections.emptyList(), approverId);

    assertStatusChangeExistsAndAuthorIdMatches(requisition, RequisitionStatus.APPROVED,
        approverId);
  }

  @Test
  public void shouldRecordStatusChangeOnReject() {
    UUID rejectorId = UUID.randomUUID();
    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.AUTHORIZED);
    requisition.setTemplate(template);

    requisition.reject(Collections.emptyList(), rejectorId);

    assertStatusChangeExistsAndAuthorIdMatches(requisition, RequisitionStatus.REJECTED,
        rejectorId);
  }

  @Test
  public void shouldRecordStatusChangeOnRelease() {
    UUID releaserId = UUID.randomUUID();
    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.APPROVED);

    requisition.release(releaserId);

    assertStatusChangeExistsAndAuthorIdMatches(requisition, RequisitionStatus.RELEASED,
        releaserId);
  }

  @Test
  public void shouldProperlyRecognizeDeletableRequisition() {
    Requisition requisition = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    assertTrue(requisition.isDeletable());

    requisition.setStatus(RequisitionStatus.REJECTED);
    assertTrue(requisition.isDeletable());

    requisition.setStatus(RequisitionStatus.SKIPPED);
    assertTrue(requisition.isDeletable());

    requisition.setStatus(RequisitionStatus.SUBMITTED);
    assertTrue(requisition.isDeletable());

    requisition.setStatus(RequisitionStatus.AUTHORIZED);
    assertFalse(requisition.isDeletable());

    requisition.setStatus(RequisitionStatus.IN_APPROVAL);
    assertFalse(requisition.isDeletable());

    requisition.setStatus(RequisitionStatus.APPROVED);
    assertFalse(requisition.isDeletable());

    requisition.setStatus(RequisitionStatus.RELEASED);
    assertFalse(requisition.isDeletable());
  }

  @Test
  public void shouldFindLatestStatusChange() {
    // given
    List<StatusChange> statusChanges = Arrays.asList(
        StatusChange.newStatusChange(requisition, UUID.randomUUID()),
        StatusChange.newStatusChange(requisition, UUID.randomUUID()),
        StatusChange.newStatusChange(requisition, UUID.randomUUID())
    );
    ZonedDateTime expectedDate = ZonedDateTime.now();
    ZonedDateTime dt = expectedDate;
    for (StatusChange statusChange : statusChanges) {
      statusChange.setCreatedDate(dt);
      dt = dt.minusDays(1);
    }
    requisition.setStatusChanges(statusChanges);

    // when
    StatusChange latestStatusChange = requisition.getLatestStatusChange();

    // then
    assertEquals(expectedDate, latestStatusChange.getCreatedDate());
  }

  @Test
  public void shouldSetPreviousStatusChangeId() {
    // given
    UUID expectedId = UUID.randomUUID();
    StatusChange firstStatusChange = StatusChange.newStatusChange(requisition, UUID.randomUUID());
    firstStatusChange.setId(expectedId);
    firstStatusChange.setCreatedDate(ZonedDateTime.now());
    requisition.setStatusChanges(Collections.singletonList(firstStatusChange));

    // when
    StatusChange statusChange = StatusChange.newStatusChange(requisition, UUID.randomUUID());

    // then
    assertEquals(expectedId, statusChange.getPreviousStatusChangeId());
  }

  @Test
  public void shouldSetIdealStockAmountForLineItems() {
    // given
    final UUID productId1 = UUID.randomUUID();
    final UUID productId2 = UUID.randomUUID();
    final UUID commodityTypeId = UUID.randomUUID();

    ApprovedProductDto product1 = mockApprovedProduct(productId1);
    product1
        .getOrderable()
        .setIdentifiers(ImmutableMap.of(COMMODITY_TYPE_IDENTIFIER, commodityTypeId.toString()));

    ApprovedProductDto product2 = mockApprovedProduct(productId2);

    Map<UUID, Integer> idealStockAmounts = Maps.newHashMap();
    idealStockAmounts.put(commodityTypeId, 1000);

    // when
    Requisition req = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    req.initiate(template, asList(product1, product2), emptyList(), 0, null,
        idealStockAmounts, UUID.randomUUID(), emptyMap());

    // then
    List<RequisitionLineItem> lineItems = req.getRequisitionLineItems();

    assertEquals(2, lineItems.size());
    assertThat(req.findLineByProductId(productId1).getIdealStockAmount(), is(1000));
    assertThat(req.findLineByProductId(productId2).getIdealStockAmount(), is(nullValue()));
  }

  @Test
  public void shouldSetStockOnHandForLineItems() {
    // given
    final UUID productId1 = UUID.randomUUID();
    final UUID productId2 = UUID.randomUUID();

    ApprovedProductDto product1 = mockApprovedProduct(productId1);
    ApprovedProductDto product2 = mockApprovedProduct(productId2);

    Map<UUID, Integer> orderableSoh = Maps.newHashMap();
    orderableSoh.put(productId1, 1000);

    // when
    Requisition req = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    req.initiate(template, asList(product1, product2), emptyList(), 0, null,
        emptyMap(), UUID.randomUUID(), orderableSoh);

    // then
    List<RequisitionLineItem> lineItems = req.getRequisitionLineItems();

    assertEquals(2, lineItems.size());
    assertThat(req.findLineByProductId(productId1).getStockOnHand(), is(1000));
    assertThat(req.findLineByProductId(productId2).getStockOnHand(), is(nullValue()));
  }

  @Test
  public void shouldNotSetLineItemsForEmergencyRequisition() {
    // given
    final UUID productId1 = UUID.randomUUID();
    final UUID productId2 = UUID.randomUUID();

    ApprovedProductDto product1 = mockApprovedProduct(productId1);
    ApprovedProductDto product2 = mockApprovedProduct(productId2);

    // when
    Requisition req = createRequisitionWithStatusOf(RequisitionStatus.INITIATED);
    req.setEmergency(true);

    req.initiate(template, asList(product1, product2), emptyList(), 0, null,
        emptyMap(), UUID.randomUUID(), new HashMap<>());

    // then
    List<RequisitionLineItem> lineItems = req.getRequisitionLineItems();
    assertThat(lineItems, hasSize(0));
    assertThat(req.findLineByProductId(productId1), is(nullValue()));
    assertThat(req.findLineByProductId(productId2), is(nullValue()));
  }

  private Requisition updateWithDatePhysicalCountCompleted(boolean updateStockDate) {
    RequisitionTemplate requisitionTemplate = mock(RequisitionTemplate.class);
    this.requisition.setTemplate(requisitionTemplate);

    Requisition requisition = new Requisition();
    requisition.setDatePhysicalStockCountCompleted(
        new DatePhysicalStockCountCompleted(LocalDate.now()));
    this.requisition.updateFrom(requisition, Collections.emptyList(), updateStockDate);

    return requisition;
  }

  private Requisition createRequisitionWithStatusOf(RequisitionStatus status) {
    return new Requisition(UUID.randomUUID(), programId, UUID
        .randomUUID(), status, false);
  }

  private void assertStatusChangeExistsAndAuthorIdMatches(Requisition requisition, 
      RequisitionStatus status, UUID authorId) {
    Optional<StatusChange> change = requisition.getStatusChanges().stream()
        .filter(statusChange -> statusChange.getStatus() == status)
        .findFirst();
    assertTrue(change.isPresent());
    assertEquals(authorId, change.get().getAuthorId());
  }

  private void setUpGetTotalCost(BigDecimal fullSupplyCost, BigDecimal nonFullSupplyCost) {
    CurrencyUnit currency = CurrencyUnit.of(CurrencyConfig.CURRENCY_CODE);

    RequisitionLineItem fullSupplyItem = getRequisitionLineItem(false, true);
    fullSupplyItem.setTotalCost(Money.of(currency, fullSupplyCost));

    RequisitionLineItem nonFullSupplyItem = getRequisitionLineItem(false, false);
    nonFullSupplyItem.setTotalCost(Money.of(currency, nonFullSupplyCost));

    requisition.getRequisitionLineItems().clear();
    requisition.getRequisitionLineItems().add(fullSupplyItem);
    requisition.getRequisitionLineItems().add(nonFullSupplyItem);
  }

  private void setUpTestUpdatePacksToShip(OrderableDto productMock, long packsToShip) {
    requisitionLineItem.setPacksToShip(packsToShip);
    productMock.setId(requisitionLineItem.getOrderableId());
    setUpValidRequisitionTemplate();
  }

  private void prepareForTestAdjustedConcumptionTotalCostAndAverageConsumption() {
    requisitionLineItem = new RequisitionLineItem();
    requisition.setRequisitionLineItems(Collections.singletonList(requisitionLineItem));

    PowerMockito.spy(LineItemFieldsCalculator.class);
    when(LineItemFieldsCalculator
        .calculateAdjustedConsumption(requisitionLineItem, MONTHS_IN_PERIOD)
    ).thenReturn(ADJUSTED_CONSUMPTION);
    when(LineItemFieldsCalculator.calculateTotalCost(requisitionLineItem, CURRENCY_UNIT))
        .thenReturn(TOTAL_COST);
    when(LineItemFieldsCalculator
        .calculateAverageConsumption(Collections.singletonList(ADJUSTED_CONSUMPTION)))
        .thenReturn(AVERAGE_CONSUMPTION);

    when(template.isColumnInTemplateAndDisplayed(any())).thenReturn(true);
    requisition.setTemplate(template);
  }

  private void prepareForTestAverageConsumption(
      List<Integer> adjustedConsumptions) {
    UUID orderableId = UUID.randomUUID();
    requisitionLineItem = new RequisitionLineItem();
    requisitionLineItem.setOrderableId(orderableId);
    requisitionLineItem.setPreviousAdjustedConsumptions(adjustedConsumptions);
    requisition.setRequisitionLineItems(Collections.singletonList(requisitionLineItem));

    PowerMockito.spy(LineItemFieldsCalculator.class);
    when(LineItemFieldsCalculator
        .calculateAdjustedConsumption(requisitionLineItem, MONTHS_IN_PERIOD)
    ).thenReturn(ADJUSTED_CONSUMPTION);

    setUpValidRequisitionTemplate();
  }

  private void setUpValidRequisitionTemplate() {
    when(template.isColumnInTemplateAndDisplayed(RequisitionLineItem.ADJUSTED_CONSUMPTION))
        .thenReturn(true);
    when(template.isColumnInTemplateAndDisplayed(RequisitionLineItem.AVERAGE_CONSUMPTION))
        .thenReturn(true);
    requisition.setTemplate(template);
  }

  private RequisitionLineItem mockReqLine(Requisition requisition, UUID productId,
                           int stockOnHand) {
    RequisitionLineItem item = mock(RequisitionLineItem.class);
    when(item.getOrderableId()).thenReturn(productId);
    when(item.getStockOnHand()).thenReturn(stockOnHand);

    when(requisition.findLineByProductId(productId)).thenReturn(item);
    return item;
  }

  private ApprovedProductDto mockApprovedProduct(UUID orderableId) {
    return new ApprovedProductDtoDataBuilder()
        .withOrderable(
            new OrderableDtoDataBuilder()
                .withId(orderableId)
                .withProgramOrderable(programId, true)
                .build())
        .build();
  }

  private SupervisoryNodeDto mockSupervisoryParentNode(UUID parentId) {
    SupervisoryNodeDto parentNode = mock(SupervisoryNodeDto.class);
    when(parentNode.getId()).thenReturn(parentId);
    return parentNode;
  }

  private RequisitionLineItem getRequisitionLineItem(Boolean skipped) {
    RequisitionLineItem item = new RequisitionLineItem();
    item.setSkipped(skipped);
    item.setNonFullSupply(false);
    item.setId(UUID.randomUUID());
    return item;
  }

  private RequisitionLineItem getRequisitionLineItem(boolean skipped, boolean fullSupply) {
    RequisitionLineItem item = getRequisitionLineItem(skipped);
    item.setNonFullSupply(!fullSupply);
    return item;
  }

  private Requisition getRequisition(RequisitionLineItem notSkipped, RequisitionLineItem skipped) {
    Requisition requisition = new Requisition();
    requisition.setRequisitionLineItems(Lists.newArrayList(notSkipped, skipped));
    return requisition;
  }
}