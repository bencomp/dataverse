package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
// import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
// import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
// import org.junit.jupiter.api.BeforeAll;
// import org.junit.jupiter.api.AfterAll;
import org.junit.Test;

public class DatasetFieldTest {
    
    // private static final Logger logger = Logger.getLogger(DatasetField.class.getCanonicalName());
    
    // @BeforeAll
    // public static void setUp() {
    //     BrandingUtilTest.setupMocks();
    // }
    
    // @AfterAll
    // public static void tearDown() {
    //     BrandingUtilTest.setupMocks();
    // }

    @Test
    public void testInitializedValues() {
        DatasetField field = new DatasetField();
        // These methods always return a non-null value
        assertNotNull(field.getDatasetFieldValues());
        assertTrue(field.getDatasetFieldValues().isEmpty());
        assertNotNull(field.getControlledVocabularyValues());
        assertTrue(field.getControlledVocabularyValues().isEmpty());
        assertNotNull(field.getSingleValue());
        assertNotNull(field.getValues());
        assertTrue(field.getValues().isEmpty());
        assertNotNull(field.getRawValuesList());
        assertTrue(field.getRawValuesList().isEmpty());
        assertNotNull(field.getRawValue());
        assertNotNull(field.getCompoundRawValue());
        assertNotNull(field.getCompoundDisplayValue());
        assertNotNull(field.getValues_nondisplay());
        assertTrue(field.getValues_nondisplay().isEmpty());
        // These methods return null if no data were added
        assertNull(field.getValue());
        assertNull(field.getSingleControlledVocabularyValue());
    }

    @Test
    public void testEqualityWithoutIds() {
        DatasetField field1 = DatasetField.createNewEmptyDatasetField(MocksFactory.makeDatasetFieldType(), new DatasetVersion());
        DatasetField field2 = DatasetField.createNewEmptyDatasetField(MocksFactory.makeDatasetFieldType(), new DatasetVersion());
        // DatasetFields without ids or any other data are not equal
        assertFalse(field1.equals(field2));
    }

    @Test
    public void testEquality() {
        DatasetField field1 = DatasetField.createNewEmptyDatasetField(MocksFactory.makeDatasetFieldType(), new DatasetVersion());
        DatasetField field2 = DatasetField.createNewEmptyDatasetField(MocksFactory.makeDatasetFieldType(), new DatasetVersion());
        // DatasetFields with and without ids are not equal
        field1.setId(1L);
        assertFalse(field1.equals(field2));
        // DatasetFields with different ids are not equal
        field2.setId(2L);
        assertFalse(field1.equals(field2));
        // DatasetFields with the same ids are equal
        field1.setId(2L);
        assertTrue(field1.equals(field2));
        // If the objects are equal, their hashCodes must be equal
        assertEquals(field1.hashCode(), field2.hashCode());
    }

    @Test
    public void testCopy() {
        DatasetField field1 = DatasetField.createNewEmptyDatasetField(MocksFactory.makeDatasetFieldType(), new DatasetVersion());
        field1.setId(1L);
        DatasetField fieldCopy = field1.copy((DatasetVersion) null);
        assertFalse(field1.equals(fieldCopy));
    }

    /**
     * FIXME: Checks existing assumptions only
     */
    @Test
    public void testNeedsTextCleaning() {
        DatasetField field = new DatasetField();
        // a new field has no type, so doesn't need cleaning
        assertFalse(field.needsTextCleaning());
        DatasetFieldType fieldType = new DatasetFieldType();
        field.setDatasetFieldType(fieldType);
        // a new fieldtype has no type, so the field doesn't need cleaning
        assertFalse(field.needsTextCleaning());
        // a field with fieldtype TEXT needs cleaning
        fieldType.setFieldType(FieldType.TEXT);
        assertTrue(field.needsTextCleaning());
        // a field with fieldtype TEXTBOX needs cleaning
        fieldType.setFieldType(FieldType.TEXTBOX);
        assertTrue(field.needsTextCleaning());
        // a field with fieldtype FLOAT needs no cleaning
        fieldType.setFieldType(FieldType.FLOAT);
        assertFalse(field.needsTextCleaning());
    }
}