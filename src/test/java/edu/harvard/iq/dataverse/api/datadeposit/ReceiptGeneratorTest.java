/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.Dataset;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.swordapp.server.DepositReceipt;

/**
 * Tests for good SWORD receipts.
 * @author ben
 */
public class ReceiptGeneratorTest {
    
    public ReceiptGeneratorTest() {
    }
    
    @Before
    public void setUp() {
    }

    /**
     * Expect a complete receipt for a submitted dataset. Some requirements
     * follow from the SWORD components outside the Dataverse code, like:
     * "We have to include an "edit" IRI or else we get
     * NullPointerException in getAbderaEntry at
     * https://github.com/swordapp/JavaServer2.0/blob/sword2-server-1.0/src/main/java/org/swordapp/server/DepositReceipt.java#L52
     *
     * Do we want to support a replaceMetadata of dataverses? Probably not.
     * Let's do that with the native API.
     *
     * Typically, we only operate on the "collection" IRI for dataverses, to
     * create a dataset."
     */
    @Ignore
    @Test
    public void testCompleteDatasetReceiptGeneration() {
        ReceiptGenerator rg = new ReceiptGenerator();
        String baseUrl = "test";
        
        String protocol = "hdl";
        String authority = "auth";
        String identifier = "dIdentifier";
        String globalId = "hdl:auth/dIdentifier";
        Dataset dataset = new Dataset();
        dataset.setProtocol(protocol);
        dataset.setAuthority(authority);
        dataset.setIdentifier(identifier);
        // GlobalId is composed of above parts. Tested elsewhere too.
        assertEquals(globalId, dataset.getGlobalId());
        
        
        
        DepositReceipt dr = rg.createDatasetReceipt(baseUrl, dataset);
        
        
        assertEquals(baseUrl + "/edit/study/" + globalId, dr.getEditIRI());
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Expect an error generating a receipt for an incomplete submitted dataset.
     *
     */
    @Ignore
    @Test
    public void testIncompleteDatasetReceiptGeneration() {
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Expect a complete receipt for a submitted dataverse. Some requirements
     * follow from the SWORD components outside the Dataverse code, like:
     * "We have to include an "edit" IRI or else we get
     * NullPointerException in getAbderaEntry at
     * https://github.com/swordapp/JavaServer2.0/blob/sword2-server-1.0/src/main/java/org/swordapp/server/DepositReceipt.java#L52
     *
     * Do we want to support a replaceMetadata of dataverses? Probably not.
     * Let's do that with the native API.
     *
     * Typically, we only operate on the "collection" IRI for dataverses, to
     * create a dataset."
     */
    @Ignore
    @Test
    public void testCompleteDataverseReceiptGeneration() {
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Expect an error generating a receipt for an incomplete submitted dataverse.
     * Some requirements
     * follow from the SWORD components outside the Dataverse code, like:
     * "We have to include an "edit" IRI or else we get
     * NullPointerException in getAbderaEntry at
     * https://github.com/swordapp/JavaServer2.0/blob/sword2-server-1.0/src/main/java/org/swordapp/server/DepositReceipt.java#L52
     *
     * Do we want to support a replaceMetadata of dataverses? Probably not.
     * Let's do that with the native API.
     *
     * Typically, we only operate on the "collection" IRI for dataverses, to
     * create a dataset."
     */
    @Ignore
    @Test
    public void testIncompleteDataverseReceiptGeneration() {
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
