/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author ben
 */
public class DatasetPageTest {
    
    public DatasetPageTest() {
    }
    
    @Before
    public void setUp() {
    }

    /**
     * Test of canDownloadFile method, of class DatasetPage.
     */
    @Test
    public void testCanDownloadFile() {
        System.out.println("canDownloadFile");
        FileMetadata fileMetadata = null;
        DatasetPage instance = new DatasetPage();
        boolean result = instance.canDownloadFile(fileMetadata);
        assertFalse(result);
    }

    /**
     * Test of isShapefileType method, of class DatasetPage.
     */
    @Test
    public void testIsShapefileType() {
        System.out.println("isShapefileType");
        FileMetadata fm = null;
        DatasetPage instance = new DatasetPage();
        boolean result = instance.isShapefileType(fm);
        assertFalse(result);
    }

    /**
     * Test of hasMapLayerMetadata method, of class DatasetPage.
     */
    @Test
    public void testHasMapLayerMetadata() {
        System.out.println("hasMapLayerMetadata");
        FileMetadata fm = null;
        DatasetPage instance = new DatasetPage();
        boolean result = instance.hasMapLayerMetadata(fm);
        assertFalse(result);
    }

    /**
     * Test of canSeeMapButtonReminderToPublish method, of class DatasetPage.
     */
    @Test
    public void testCanSeeMapButtonReminderToPublish() {
        System.out.println("canSeeMapButtonReminderToPublish");
        FileMetadata fm = null;
        DatasetPage instance = new DatasetPage();
        boolean result = instance.canSeeMapButtonReminderToPublish(fm);
        assertFalse(result);
    }

    /**
     * Test of canUserSeeMapDataButton method, of class DatasetPage.
     */
    @Test
    public void testCanUserSeeMapDataButton() {
        System.out.println("canUserSeeMapDataButton");
        FileMetadata fm = null;
        DatasetPage instance = new DatasetPage();
        boolean result = instance.canUserSeeMapDataButton(fm);
        assertFalse(result);
    }

    /**
     * Test of canUserSeeExploreWorldMapButton method, of class DatasetPage.
     */
    @Test
    public void testCanUserSeeExploreWorldMapButton() {
        System.out.println("canUserSeeExploreWorldMapButton");
        FileMetadata fm = null;
        DatasetPage instance = new DatasetPage();
        boolean result = instance.canUserSeeExploreWorldMapButton(fm);
        assertFalse(result);
    }
    
}
