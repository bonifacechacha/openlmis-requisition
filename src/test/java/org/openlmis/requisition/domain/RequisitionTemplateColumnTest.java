package org.openlmis.requisition.domain;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openlmis.requisition.exception.RequisitionTemplateColumnException;

public class RequisitionTemplateColumnTest {
  RequisitionTemplateColumn requisitionTemplateColumn;

  @Before
  public void setUp() {
    requisitionTemplateColumn = new RequisitionTemplateColumn();
    requisitionTemplateColumn.setName("Test Column");
  }

  @Test
  public void testShouldChangeLabelOnlyIfValid() throws RequisitionTemplateColumnException {
    requisitionTemplateColumn.setLabel("ValidName");
    Assert.assertEquals("ValidName", requisitionTemplateColumn.getLabel());
    requisitionTemplateColumn.setLabel("New valid name with numbers 123 and spaces ");
    Assert.assertEquals("New valid name with numbers 123 and spaces ",
        requisitionTemplateColumn.getLabel());
  }

  @Test(expected = RequisitionTemplateColumnException.class)
  public void testShouldNotChangeLabelIfInvalid() throws RequisitionTemplateColumnException {
    requisitionTemplateColumn.setLabel("New not valid name with wrong signs: !@#$%^&*()");
    requisitionTemplateColumn.setLabel("!@#$%^&*()");
    requisitionTemplateColumn.setLabel("");
    requisitionTemplateColumn.setLabel(" ");
  }
}
