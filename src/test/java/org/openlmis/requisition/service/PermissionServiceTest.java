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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_NO_FOLLOWING_PERMISSION;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_NO_FOLLOWING_PERMISSION_FOR_REQUISITION_UPDATE;
import static org.openlmis.requisition.service.PermissionService.ORDERS_EDIT;
import static org.openlmis.requisition.service.PermissionService.REQUISITION_APPROVE;
import static org.openlmis.requisition.service.PermissionService.REQUISITION_AUTHORIZE;
import static org.openlmis.requisition.service.PermissionService.REQUISITION_CREATE;
import static org.openlmis.requisition.service.PermissionService.REQUISITION_DELETE;
import static org.openlmis.requisition.service.PermissionService.REQUISITION_TEMPLATES_MANAGE;
import static org.openlmis.requisition.service.PermissionService.REQUISITION_VIEW;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.requisition.domain.requisition.Requisition;
import org.openlmis.requisition.domain.requisition.RequisitionStatus;
import org.openlmis.requisition.dto.ReleasableRequisitionDto;
import org.openlmis.requisition.errorhandling.FailureType;
import org.openlmis.requisition.errorhandling.ValidationResult;
import org.openlmis.requisition.repository.RequisitionRepository;
import org.openlmis.requisition.utils.Message;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class PermissionServiceTest {

  @Mock
  private RightAssignmentPermissionValidator rightAssignmentPermissionValidator;

  @Mock
  private RoleAssignmentPermissionValidator roleAssignmentPermissionValidator;

  @Mock
  private RequisitionRepository requisitionRepository;

  @InjectMocks
  private PermissionService permissionService;

  @Mock
  private Requisition requisition;

  private UUID requisitionId = UUID.randomUUID();
  private UUID programId = UUID.randomUUID();
  private UUID facilityId = UUID.randomUUID();
  private UUID supervisoryNodeId = UUID.randomUUID();
  private ReleasableRequisitionDto releasableRequisitionDto = new ReleasableRequisitionDto();
  private List<ReleasableRequisitionDto> releasableDtos = new ArrayList<>();

  @Before
  public void setUp() {
    releasableRequisitionDto.setRequisitionId(requisitionId);
    releasableRequisitionDto.setSupplyingDepotId(facilityId);
    releasableDtos.add(releasableRequisitionDto);

    when(requisition.getId()).thenReturn(requisitionId);
    when(requisition.getProgramId()).thenReturn(programId);
    when(requisition.getFacilityId()).thenReturn(facilityId);
    when(requisition.getSupplyingFacilityId()).thenReturn(facilityId);
    when(requisition.getStatus()).thenReturn(RequisitionStatus.SUBMITTED);
    when(requisition.getSupervisoryNodeId()).thenReturn(supervisoryNodeId);

    when(requisitionRepository.findOne(requisitionId)).thenReturn(requisition);
    when(requisitionRepository.findAll(ImmutableSet.of(requisitionId)))
        .thenReturn(Lists.newArrayList(requisition));
  }

  @Test
  public void canInitRequisition() {
    // given
    hasRight(REQUISITION_CREATE, facilityId, programId, true);

    // expect
    expectValidationSucceeds(permissionService.canInitRequisition(programId, facilityId));
  }

  @Test
  public void cannotInitRequisition() {
    // given
    hasRight(REQUISITION_CREATE, facilityId, programId, false);

    // expect
    expectMissingPermission(permissionService.canInitRequisition(programId, facilityId),
        REQUISITION_CREATE);
  }

  @Test
  public void canUpdateRequisition() {
    // given
    hasRole(REQUISITION_CREATE, true);

    given(requisition.getStatus()).willReturn(RequisitionStatus.INITIATED);

    // expect
    expectValidationSucceeds(permissionService.canUpdateRequisition(requisition));
  }

  @Test
  public void cannotUpdateRequisition() {
    // given
    hasRole(REQUISITION_CREATE, false);

    given(requisition.getStatus()).willReturn(RequisitionStatus.INITIATED);

    // expect
    expectMissingPermissionToUpdate(permissionService.canUpdateRequisition(requisition),
        RequisitionStatus.INITIATED, REQUISITION_CREATE);
  }

  @Test
  public void canSubmitRequisition() {
    // given
    hasRight(REQUISITION_CREATE, facilityId, programId, true);

    // expect
    expectValidationSucceeds(permissionService.canSubmitRequisition(requisition));
  }

  @Test
  public void cannotSubmitRequisition() {
    // given
    hasRight(REQUISITION_CREATE, facilityId, programId, false);

    // expect
    expectMissingPermission(permissionService.canSubmitRequisition(requisition),
        REQUISITION_CREATE);
  }

  @Test
  public void canApproveRequisitionRequisition() {
    // given
    hasRole(REQUISITION_APPROVE, true);

    // expect
    expectValidationSucceeds(permissionService.canApproveRequisition(requisition));
  }

  @Test
  public void cannotApproveRequisition() {
    // given
    hasRole(REQUISITION_APPROVE, false);

    // expect
    expectMissingPermission(permissionService.canApproveRequisition(requisition),
        REQUISITION_APPROVE);
  }

  @Test
  public void canAuthorizeRequisition() {
    // given
    hasRight(REQUISITION_AUTHORIZE, facilityId, programId, true);

    // expect
    expectValidationSucceeds(permissionService.canAuthorizeRequisition(requisition));
  }

  @Test
  public void cannotAuthorizeRequisition() {
    // given
    hasRight(REQUISITION_AUTHORIZE, facilityId, programId, false);

    // expect
    expectMissingPermission(permissionService.canAuthorizeRequisition(requisition),
        REQUISITION_AUTHORIZE);
  }

  @Test
  public void canDeleteInitiatedRequisitionWhenHasCreateRight() {
    // given
    hasRight(REQUISITION_DELETE, facilityId, programId, true);
    hasRight(REQUISITION_CREATE, facilityId, programId, true);

    given(requisition.getStatus()).willReturn(RequisitionStatus.INITIATED);

    // expect
    expectValidationSucceeds(permissionService.canDeleteRequisition(requisition));
  }

  @Test
  public void cannotDeleteInitiatedRequisitionWhenHasNoCreateRight() {
    // given
    hasRight(REQUISITION_DELETE, facilityId, programId, true);
    hasRight(REQUISITION_CREATE, facilityId, programId, false);

    given(requisition.getStatus()).willReturn(RequisitionStatus.INITIATED);

    // expect
    expectMissingPermission(permissionService.canDeleteRequisition(requisition),
        REQUISITION_CREATE);
  }

  @Test
  public void shouldDeleteSkippedRequisitionWhenHasCreateRight() {
    // given
    hasRight(REQUISITION_DELETE, facilityId, programId, true);
    hasRight(REQUISITION_CREATE, facilityId, programId, true);

    given(requisition.getStatus()).willReturn(RequisitionStatus.SKIPPED);

    // expect
    expectValidationSucceeds(permissionService.canDeleteRequisition(requisition));
  }

  @Test
  public void shouldNotDeleteSkippedRequisitionWhenHasNoCreateRight() {
    // given
    hasRight(REQUISITION_DELETE, facilityId, programId, true);
    hasRight(REQUISITION_CREATE, facilityId, programId, false);

    given(requisition.getStatus()).willReturn(RequisitionStatus.SKIPPED);

    // expect
    expectMissingPermission(permissionService.canDeleteRequisition(requisition),
        REQUISITION_CREATE);
  }

  @Test
  public void canDeleteSubmittedRequisitionWhenHasAuthorizeRight() {
    // given
    hasRight(REQUISITION_DELETE, facilityId, programId, true);
    hasRight(REQUISITION_AUTHORIZE, facilityId, programId, true);

    given(requisition.getStatus()).willReturn(RequisitionStatus.SUBMITTED);

    // expect
    expectValidationSucceeds(permissionService.canDeleteRequisition(requisition));
  }

  @Test
  public void cannotDeleteSubmittedRequisitionWhenHasNoAuthorizeRight() {
    // given
    hasRight(REQUISITION_DELETE, facilityId, programId, true);
    hasRight(REQUISITION_AUTHORIZE, facilityId, programId, false);

    given(requisition.getStatus()).willReturn(RequisitionStatus.SUBMITTED);

    // expect
    expectMissingPermission(permissionService.canDeleteRequisition(requisition),
        REQUISITION_AUTHORIZE);
  }

  @Test
  public void cannotDeleteRequisitionWhenHasNoDeleteRight() {
    // given
    hasRight(REQUISITION_DELETE, facilityId, programId, false);

    // expect
    expectMissingPermission(permissionService.canDeleteRequisition(requisition),
        REQUISITION_DELETE);
  }

  @Test
  public void canViewRequisition() {
    // given
    hasRole(REQUISITION_VIEW, true);

    // expect
    expectValidationSucceeds(permissionService.canViewRequisition(requisitionId));
  }

  @Test
  public void cannotViewRequisition() {
    // given
    hasRole(REQUISITION_VIEW, false);

    // expect
    expectMissingPermission(permissionService.canViewRequisition(requisitionId), REQUISITION_VIEW);
  }

  @Test
  public void canConvertToOrder() {
    // given
    hasRight(ORDERS_EDIT, facilityId, true);

    // expect
    expectValidationSucceeds(permissionService.canConvertToOrder(releasableDtos));
  }

  @Test
  public void cannotConvertToOrder() {
    // given
    hasRight(ORDERS_EDIT, facilityId, false);

    // expect
    expectMissingPermission(permissionService.canConvertToOrder(releasableDtos), ORDERS_EDIT);
  }

  @Test
  public void canManageRequisitionTemplate() {
    // given
    hasRight(REQUISITION_TEMPLATES_MANAGE, true);

    // expect
    expectValidationSucceeds(permissionService.canManageRequisitionTemplate());
  }

  @Test
  public void cannotManageRequisitionTemplate() {
    // given
    hasRight(REQUISITION_TEMPLATES_MANAGE, false);

    // expect
    expectMissingPermission(permissionService.canManageRequisitionTemplate(),
        REQUISITION_TEMPLATES_MANAGE);
  }

  private void hasRight(String rightName, boolean hasRight) {
    given(rightAssignmentPermissionValidator
        .hasPermission(new RightAssignmentPermissionValidationDetails(rightName)))
        .willReturn(getValidationResult(rightName, hasRight));
  }

  private void hasRight(String rightName, UUID facilityId, UUID programId, boolean hasRight) {
    given(rightAssignmentPermissionValidator
        .hasPermission(new RightAssignmentPermissionValidationDetails(
            rightName, facilityId, programId)))
        .willReturn(getValidationResult(rightName, hasRight));
  }

  private void hasRight(String rightName, UUID warehouseId, boolean hasRight) {
    given(rightAssignmentPermissionValidator
        .hasPermission(new RightAssignmentPermissionValidationDetails(rightName, warehouseId)))
        .willReturn(getValidationResult(rightName, hasRight));
  }

  private void hasRole(String rightName, boolean hasRight) {
    given(roleAssignmentPermissionValidator
        .hasPermission(new RoleAssignmentPermissionValidationDetails(rightName, requisition)))
        .willReturn(getValidationResult(rightName, hasRight));
  }

  private ValidationResult getValidationResult(String rightName, boolean success) {
    return success
        ? ValidationResult.success()
        : ValidationResult.noPermission(ERROR_NO_FOLLOWING_PERMISSION, rightName);
  }

  private void expectValidationSucceeds(ValidationResult validationResult) {
    assertTrue(validationResult.isSuccess());
  }

  private void expectMissingPermission(ValidationResult result, String rightName) {
    assertTrue(result.hasErrors());
    assertEquals(FailureType.NO_PERMISSION, result.getError().getType());
    assertEquals(new Message(ERROR_NO_FOLLOWING_PERMISSION, rightName),
        result.getError().getMessage());
  }

  private void expectMissingPermissionToUpdate(ValidationResult result, RequisitionStatus status,
                                               String rightName) {
    assertTrue(result.hasErrors());
    assertEquals(FailureType.NO_PERMISSION, result.getError().getType());
    assertEquals(new Message(ERROR_NO_FOLLOWING_PERMISSION_FOR_REQUISITION_UPDATE,
        status.toString(), rightName), result.getError().getMessage());
  }

}
