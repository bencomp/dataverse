/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.search.SolrField;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author skraffmi
 */
public class DatasetFieldTypeTest {
    
    public DatasetFieldTypeTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testInitializedValues() {
        // Create a sample TEXT field type that doesn't allow multiple values
        DatasetFieldType type = MocksFactory.makeDatasetFieldType();
        assertFalse(type.isAllowControlledVocabulary());
        assertFalse(type.isAdvancedSearchFieldType());
        assertFalse(type.isAllowMultiples());
        assertFalse(type.isChild());
        assertFalse(type.isCompound());
        assertFalse(type.isControlledVocabulary());
        assertFalse(type.isDisplayOnCreate());
        // assertFalse(type.isEscapeOutputText());
        assertFalse(type.isFacetable());
        assertFalse(type.isHasChildren());
        assertFalse(type.isHasParent());
        assertFalse(type.isHasRequiredChildren());
        assertFalse(type.isInclude());
        assertTrue(type.isPrimitive());
        assertFalse(type.isRequired());
        assertFalse(type.isRequiredDV());
        assertFalse(type.isSanitizeHtml());
        assertFalse(type.isSubField());
        // Test constructor initializes childDatasetFieldTypes
        assertNotNull(type.getChildDatasetFieldTypes());
        assertNull(type.getControlledVocabularyValues());
        assertNull(type.getDatasetFieldDefaultValues());
        assertNull(type.getDatasetFields());
        assertNull(type.getDataverseFacets());
        assertNull(type.getDataverseFieldTypeInputLevels());
        assertNull(type.getDescription());
        assertNull(type.getDisplayFormat());
        // Mock sets a name
        assertNotNull(type.getDisplayName());
        assertEquals(0, type.getDisplayOrder());
        // Mock sets an ID
        assertNotNull(type.getId());
        // Without a MetadataBlock, getJsonLDTerm throws a NullPointerException
        // assertNotNull(type.getJsonLDTerm());
        assertNull(type.getListValues());
        assertNull(type.getLocaleDescription());
        // Mock sets a name and title
        assertNotNull(type.getLocaleTitle());
        assertNull(type.getLocaleWatermark());
        assertNull(type.getMetadataBlock());
        assertNotNull(type.getName());
        assertNull(type.getOptionSelectItems());
        assertNull(type.getParentDatasetFieldType());
        assertNull(type.getSearchValue());
        assertNotNull(type.getSolrField());
        assertNotNull(type.getTitle());
        assertNotNull(type.getTmpNullFieldTypeIdentifier());
        assertNull(type.getUri());
        assertNull(type.getValidationFormat());
        assertNull(type.getWatermark());
    }

    /**
     * Test of setInclude method, of class DatasetFieldType.
     */





    /**
     * Test of isSanitizeHtml method, of class DatasetFieldType.
     */
    @Test
    public void testIsSanitizeHtml() {
        System.out.println("isSanitizeHtml");
        DatasetFieldType instance = new DatasetFieldType();
        instance.setFieldType(DatasetFieldType.FieldType.TEXT);
        Boolean result = instance.isSanitizeHtml();
        assertFalse(result);
               
        //if textbox then sanitize - allow tags
        instance.setFieldType(DatasetFieldType.FieldType.TEXTBOX);
        result = instance.isSanitizeHtml();
        assertEquals(true, result);
        
        //if textbox then don't sanitize - allow tags
        instance.setFieldType(DatasetFieldType.FieldType.EMAIL);
        result = instance.isSanitizeHtml();
        assertEquals(false, result);
        
        //URL, too
        instance.setFieldType(DatasetFieldType.FieldType.URL);
        result = instance.isSanitizeHtml();
        assertEquals(true, result);
    }
    
    @Test
    public void testIsEscapeOutputText(){
                System.out.println("testIsEscapeOutputText");
        DatasetFieldType instance = new DatasetFieldType();
        instance.setFieldType(DatasetFieldType.FieldType.TEXT);
        Boolean result = instance.isEscapeOutputText();
        assertTrue(result);
        
        //if Disaplay Format includes a link then don't escape
        instance.setDisplayFormat("'<a target=\"_blank\" href=\"http://www.rcsb.org/pdb/explore/explore.do?structureId=#VALUE\">PDB (RCSB) #VALUE</a>'");
        result = instance.isEscapeOutputText();
        assertFalse(result);  
        
        //if textbox then sanitize - allow tags
        instance.setFieldType(DatasetFieldType.FieldType.TEXTBOX);
        result = instance.isEscapeOutputText();
        assertFalse( result);
        
        //if textbox then don't sanitize - allow tags
        instance.setFieldType(DatasetFieldType.FieldType.EMAIL);
        result = instance.isEscapeOutputText();
        assertTrue(result);
        
        //URL, too
        instance.setFieldType(DatasetFieldType.FieldType.URL);
        result = instance.isEscapeOutputText();
        assertEquals(false, result);
        
    }
    
    @Test
    public void testGetSolrField(){
        
        DatasetFieldType instance = new DatasetFieldType();
        instance.setFieldType(DatasetFieldType.FieldType.DATE);
        SolrField solrField = instance.getSolrField();       
        assertEquals(SolrField.SolrType.DATE, solrField.getSolrType());
        
        instance.setFieldType(DatasetFieldType.FieldType.EMAIL);
        solrField = instance.getSolrField();       
        assertEquals(SolrField.SolrType.EMAIL, solrField.getSolrType());
        DatasetFieldType parent = new DatasetFieldType();
        parent.setAllowMultiples(true);
        instance.setParentDatasetFieldType(parent);
        solrField = instance.getSolrField();
        assertEquals(true, solrField.isAllowedToBeMultivalued());
        
    }


    
}
