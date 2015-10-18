/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.authorization;

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author ben
 */
public class DataverseRolePermissionHelperTest {
    private DataverseRolePermissionHelper theHelper;
    
    public DataverseRolePermissionHelperTest() {
    }
    
    @Before
    public void setUp() {
        theHelper = null;
    }

    /**
     * Test of hasFilePermissions method, of class DataverseRolePermissionHelper.
     */
    @Test
    public void testSingleDataversePermissionRole() {
        List<DataverseRole> roles = new ArrayList<>();
        DataverseRole role = new DataverseRole();
        role.setId(1l);
        role.setName("Dataset Adder");
        role.addPermission(Permission.AddDataset);
        roles.add(role);
        theHelper = new DataverseRolePermissionHelper(roles);
        assertFalse("Should not have had file permissions", theHelper.hasFilePermissions(role));
        assertTrue("Should have had dataverse permissions", theHelper.hasDataversePermissions(role));
        assertFalse("Should have had dataset permissions", theHelper.hasDatasetPermissions(role));
    }
    
    /**
     * Test of hasFilePermissions method, of class DataverseRolePermissionHelper.
     */
    @Test
    public void testDualDataversePermissionRole() {
        List<DataverseRole> roles = new ArrayList<>();
        DataverseRole role = new DataverseRole();
        role.setId(1l);
        role.setName("Dataset Adder/Editor");
        role.addPermission(Permission.AddDataset);
        role.addPermission(Permission.EditDataverse);
        roles.add(role);
        
        theHelper = new DataverseRolePermissionHelper(roles);
        assertFalse("Should not have had file permissions", theHelper.hasFilePermissions(role));
        assertTrue("Should have had dataverse permissions", theHelper.hasDataversePermissions(role));
        assertFalse("Should have had dataset permissions", theHelper.hasDatasetPermissions(role));
    }
    
    /**
     * Test of hasFilePermissions method, of class DataverseRolePermissionHelper.
     */
    @Test
    public void testAllPermissionRole() {
        List<DataverseRole> roles = new ArrayList<>();
        DataverseRole role = new DataverseRole();
        role.setId(1l);
        role.setName("Admin");
        role.addPermission(Permission.AddDataverse);
        role.addPermission(Permission.AddDataset);
        role.addPermission(Permission.ViewUnpublishedDataverse);
        role.addPermission(Permission.ViewUnpublishedDataset);
        role.addPermission(Permission.DeleteDatasetDraft);
        role.addPermission(Permission.DeleteDataverse);
        role.addPermission(Permission.DownloadFile);
        role.addPermission(Permission.EditDataverse);
        role.addPermission(Permission.EditDataset);
        role.addPermission(Permission.ManageDatasetPermissions);
        role.addPermission(Permission.ManageDataversePermissions);
        role.addPermission(Permission.PublishDataverse);
        role.addPermission(Permission.PublishDataset);
        roles.add(role);
        
        theHelper = new DataverseRolePermissionHelper(roles);
        assertTrue("Should have had file permissions", theHelper.hasFilePermissions(role));
        assertTrue("Should have had dataverse permissions", theHelper.hasDataversePermissions(role));
        assertTrue("Should have had dataset permissions", theHelper.hasDatasetPermissions(role));
    }
    
    /**
     * Test of hasFilePermissions method, of class DataverseRolePermissionHelper.
     */
    @Test
    public void testNulls() {
        List<DataverseRole> roles = new ArrayList<>();
        theHelper = new DataverseRolePermissionHelper(roles);
        assertFalse("null should not have had file permissions", theHelper.hasFilePermissions((DataverseRole)null));
        assertFalse("null should not have had dataverse permissions", theHelper.hasDataversePermissions((DataverseRole)null));
        assertFalse("null should not have had dataset permissions", theHelper.hasDatasetPermissions((DataverseRole)null));
        assertFalse("null should not have had file permissions", theHelper.hasFilePermissions((Long)null));
        assertFalse("null should not have had dataverse permissions", theHelper.hasDataversePermissions((Long)null));
        assertFalse("null should not have had dataset permissions", theHelper.hasDatasetPermissions((Long)null));
    }
    
}
