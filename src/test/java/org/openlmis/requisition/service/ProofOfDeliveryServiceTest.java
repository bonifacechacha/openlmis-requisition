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

package org.openlmis.requisition.service;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.Requisition;
import org.openlmis.requisition.domain.RequisitionStatus;
import org.openlmis.requisition.dto.OrderDto;
import org.openlmis.requisition.dto.ProofOfDeliveryDto;
import org.openlmis.requisition.service.fulfillment.OrderFulfillmentService;
import org.springframework.data.domain.PageImpl;

import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class ProofOfDeliveryServiceTest {

  @Mock
  private OrderFulfillmentService orderFulfillmentService;

  @InjectMocks
  private ProofOfDeliveryService proofOfDeliveryService;

  @Mock
  private Requisition requisition;

  @Mock
  private OrderDto order;

  @Mock
  private ProofOfDeliveryDto proofOfDelivery;

  @Before
  public void setUp() throws Exception {
    when(requisition.getFacilityId()).thenReturn(UUID.randomUUID());
    when(requisition.getProgramId()).thenReturn(UUID.randomUUID());
    when(requisition.getProcessingPeriodId()).thenReturn(UUID.randomUUID());

    when(order.getId()).thenReturn(UUID.randomUUID());
    when(proofOfDelivery.getId()).thenReturn(UUID.randomUUID());
  }

  @Test
  public void shouldReturnNullIfRequisitionWasSkipped() throws Exception {
    when(requisition.getStatus()).thenReturn(RequisitionStatus.SKIPPED);
    assertThat(proofOfDeliveryService.get(requisition), is(nullValue()));
  }

  @Test
  public void shouldReturnNullIfRequisitionHasEmergencyFlag() throws Exception {
    when(requisition.getEmergency()).thenReturn(true);
    assertThat(proofOfDeliveryService.get(requisition), is(nullValue()));
  }

  @Test
  public void shouldReturnNullIfThereIsNoOrders() throws Exception {
    when(orderFulfillmentService.search(
        null, requisition.getFacilityId(), requisition.getProgramId(),
        requisition.getProcessingPeriodId(), null
    )).thenReturn(null);

    assertThat(proofOfDeliveryService.get(requisition), is(nullValue()));
  }

  @Test
  public void shouldReturnNullIfThereIsNoPods() throws Exception {
    when(orderFulfillmentService.search(
        null, requisition.getFacilityId(), requisition.getProgramId(),
        requisition.getProcessingPeriodId(), null
    )).thenReturn(new PageImpl<>(Lists.newArrayList(order)));

    when(orderFulfillmentService.getProofOfDelivery(order.getId()))
        .thenReturn(null);

    assertThat(proofOfDeliveryService.get(requisition), is(nullValue()));
  }

  @Test
  public void shouldReturnRelatedPod() throws Exception {
    when(orderFulfillmentService.search(
        null, requisition.getFacilityId(), requisition.getProgramId(),
        requisition.getProcessingPeriodId(), null
    )).thenReturn(new PageImpl<>(Lists.newArrayList(order)));

    when(orderFulfillmentService.getProofOfDelivery(order.getId()))
        .thenReturn(proofOfDelivery);

    assertThat(proofOfDeliveryService.get(requisition), is(proofOfDelivery));
  }
}
