package edu.harvard.iq.dataverse;

// import edu.harvard.iq.dataverse.branding.BrandingUtilTest;
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
        assertNotNull(field.getControlledVocabularyValues());
        assertNotNull(field.getSingleValue());
        assertNotNull(field.getValues());
        assertNotNull(field.getRawValuesList());
        assertNotNull(field.getRawValue());
        assertNotNull(field.getCompoundRawValue());
        assertNotNull(field.getCompoundDisplayValue());
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
}