package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.branding.BrandingUtilTest;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import java.io.StringReader;
import java.net.URI;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class DatasetFieldTest {
    
    private static final Logger logger = Logger.getLogger(DatasetField.class.getCanonicalName());
    
    // @BeforeAll
    // public static void setUp() {
    //     BrandingUtilTest.setupMocks();
    // }
    
    // @AfterAll
    // public static void tearDown() {
    //     BrandingUtilTest.setupMocks();
    // }

    @Test
    public void testEquality() {
        DatasetField field1 = DatasetField.createNewEmptyDatasetField(MocksFactory.makeDatasetFieldType(), (DatasetVersion) null);
        DatasetField field2 = DatasetField.createNewEmptyDatasetField(MocksFactory.makeDatasetFieldType(), (DatasetVersion) null);
        // DatasetFields without ids or any other data are not equal
        assertFalse(field1.equals(field2));
        // DatasetFields with and without ids are not equal
        field1.setId(1L);
        assertFalse(field1.equals(field2));
        // DatasetFields with different ids are not equal
        field2.setId(2L);
        assertFalse(field1.equals(field2));
        // DatasetFields with the same ids are equal
        field1.setId(2L);
        assertTrue(field1.equals(field2));
    }

    @Test
    public void testCopy() {
        DatasetField field1 = DatasetField.createNewEmptyDatasetField(MocksFactory.makeDatasetFieldType(), (DatasetVersion) null);
        field1.setId(1L);
        DatasetField fieldCopy = field1.copy((DatasetVersion) null);
        assertFalse(field1.equals(fieldCopy));
    }
}