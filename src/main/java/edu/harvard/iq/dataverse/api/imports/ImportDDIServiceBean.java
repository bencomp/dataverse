package edu.harvard.iq.dataverse.api.imports;

import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.api.dto.*;  
import edu.harvard.iq.dataverse.api.dto.FieldDTO;
import edu.harvard.iq.dataverse.api.dto.MetadataBlockDTO;
import edu.harvard.iq.dataverse.api.imports.ImportUtil.ImportType;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLInputFactory;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author ellenk
 */
@Stateless
public class ImportDDIServiceBean {
    public static final String SOURCE_DVN_3_0 = "DVN_3_0";
    
    public static final String NAMING_PROTOCOL_HANDLE = "hdl";
    public static final String NAMING_PROTOCOL_DOI = "doi";
    public static final String AGENCY_HANDLE = "handle";
    public static final String AGENCY_DOI = "DOI";
    public static final String REPLICATION_FOR_TYPE = "replicationFor";
    public static final String VAR_WEIGHTED = "wgtd";
    public static final String VAR_INTERVAL_CONTIN = "contin";
    public static final String VAR_INTERVAL_DISCRETE = "discrete";
    public static final String CAT_STAT_TYPE_FREQUENCY = "freq";
    public static final String VAR_FORMAT_TYPE_NUMERIC = "numeric";
    public static final String VAR_FORMAT_SCHEMA_ISO = "ISO";
    

    public static final String EVENT_START = "start";
    public static final String EVENT_END = "end";
    public static final String EVENT_SINGLE = "single";

    public static final String LEVEL_DVN = "dvn";
    public static final String LEVEL_DV = "dv";
    public static final String LEVEL_STUDY = "study";
    public static final String LEVEL_FILE = "file";
    public static final String LEVEL_VARIABLE = "variable";
    public static final String LEVEL_CATEGORY = "category";

    public static final String NOTE_TYPE_UNF = "VDC:UNF";
    public static final String NOTE_SUBJECT_UNF = "Universal Numeric Fingerprint";

    public static final String NOTE_TYPE_TERMS_OF_USE = "DVN:TOU";
    public static final String NOTE_SUBJECT_TERMS_OF_USE = "Terms Of Use";

    public static final String NOTE_TYPE_CITATION = "DVN:CITATION";
    public static final String NOTE_SUBJECT_CITATION = "Citation";

    public static final String NOTE_TYPE_VERSION_NOTE = "DVN:VERSION_NOTE";
    public static final String NOTE_SUBJECT_VERSION_NOTE= "Version Note";

    public static final String NOTE_TYPE_ARCHIVE_NOTE = "DVN:ARCHIVE_NOTE";
    public static final String NOTE_SUBJECT_ARCHIVE_NOTE= "Archive Note";

    public static final String NOTE_TYPE_ARCHIVE_DATE = "DVN:ARCHIVE_DATE";
    public static final String NOTE_SUBJECT_ARCHIVE_DATE= "Archive Date";
    
    public static final String NOTE_TYPE_EXTENDED_METADATA = "DVN:EXTENDED_METADATA";

    public static final String NOTE_TYPE_LOCKSS_CRAWL = "LOCKSS:CRAWLING";
    public static final String NOTE_SUBJECT_LOCKSS_PERM = "LOCKSS Permission";

    public static final String NOTE_TYPE_REPLICATION_FOR = "DVN:REPLICATION_FOR";
    private XMLInputFactory xmlInputFactory = null;
    private ImportType importType;
     
    @EJB CustomFieldServiceBean customFieldService;
   
    @EJB DatasetFieldServiceBean datasetFieldService;
    
      
    public DatasetDTO doImport(ImportType importType, String xmlToParse) throws XMLStreamException, ImportException {
         this.importType=importType;
        xmlInputFactory = javax.xml.stream.XMLInputFactory.newInstance();
        xmlInputFactory.setProperty("javax.xml.stream.isCoalescing", java.lang.Boolean.TRUE); DatasetDTO datasetDTO = this.initializeDataset();

        // Read docDescr and studyDesc into DTO objects.
        Map fileMap = mapDDI(xmlToParse, datasetDTO);
        if (!importType.equals(ImportType.MIGRATION)) {
                  // For migration, this filemetadata is copied in a separate SQL step
        }
        return datasetDTO;
    }
    
    public void importFileMetadata(DatasetVersion dv, String xmlToParse) {
        
    } 
    
    

    public Map mapDDI(String xmlToParse, DatasetDTO datasetDTO) throws XMLStreamException, ImportException {

        Map filesMap = new HashMap();
        StringReader reader = new StringReader(xmlToParse);
        XMLStreamReader xmlr = null;
        XMLInputFactory xmlFactory = javax.xml.stream.XMLInputFactory.newInstance();
        xmlr = xmlFactory.createXMLStreamReader(reader);
        processDDI(xmlr, datasetDTO, filesMap);

        return filesMap;
    }
   
 
    public Map mapDDI(File ddiFile,  DatasetDTO datasetDTO ) throws ImportException {
        FileInputStream in = null;
        XMLStreamReader xmlr = null;
        Map filesMap = new HashMap();

        try {
            in = new FileInputStream(ddiFile);
            xmlr =  xmlInputFactory.createXMLStreamReader(in);
            processDDI( xmlr,  datasetDTO , filesMap );
        } catch (FileNotFoundException ex) {
            Logger.getLogger("global").log(Level.SEVERE, null, ex);
            throw new EJBException("ERROR occurred in mapDDI: File Not Found!");
        } catch (XMLStreamException ex) {
            Logger.getLogger("global").log(Level.SEVERE, null, ex);
            throw new EJBException("ERROR occurred in mapDDI.", ex);
        } finally {
            try {
                if (xmlr != null) { xmlr.close(); }
            } catch (XMLStreamException ex) {}

            try {
                if (in != null) { in.close();}
            } catch (IOException ex) {}
        }

        return filesMap;
    }
    
    private void processDDI( XMLStreamReader xmlr, DatasetDTO datasetDTO, Map filesMap) throws XMLStreamException, ImportException {
       
        // make sure we have a codeBook
        //while ( xmlr.next() == XMLStreamConstants.COMMENT ); // skip pre root comments
        xmlr.nextTag();
        xmlr.require(XMLStreamConstants.START_ELEMENT, null, "codeBook");

        // Some DDIs provide an ID in the <codeBook> section.
        // We are going to treat it as just another otherId.
        // (we've seen instances where this ID was the only ID found in
        // in a harvested DDI).

        String codeBookLevelId = xmlr.getAttributeValue(null, "ID");
        
        // (but first we will parse and process the entire DDI - and only 
        // then add this codeBook-level id to the list of identifiers; i.e., 
        // we don't want it to be the first on the list, if one or more
        // ids are available in the studyDscr section - those should take 
        // precedence!)
        // In fact, we should only use these IDs when no ID is available down 
        // in the study description section!      
        
        processCodeBook(xmlr,  datasetDTO, filesMap);
        MetadataBlockDTO citationBlock = datasetDTO.getDatasetVersion().getMetadataBlocks().get("citation");
     
         if (codeBookLevelId != null && !"".equals(codeBookLevelId)) {
            if (citationBlock.getField("otherId")==null) {
                // this means no ids were found during the parsing of the 
                // study description section. we'll use the one we found in 
                // the codeBook entry:
                FieldDTO otherIdValue = FieldDTO.createPrimitiveFieldDTO("otherIdValue", codeBookLevelId);
                FieldDTO otherId = FieldDTO.createCompoundFieldDTO("otherId", otherIdValue);
                citationBlock.getFields().add(otherId);
                
            }
        }
         if (importType.equals(ImportType.HARVEST)) {
            datasetDTO.getDatasetVersion().setVersionState(VersionState.RELEASED);
       
         }
        

    }
     public DatasetDTO initializeDataset() {
        DatasetDTO  datasetDTO = new DatasetDTO();
        DatasetVersionDTO datasetVersionDTO = new DatasetVersionDTO();
        datasetDTO.setDatasetVersion(datasetVersionDTO);
        HashMap<String, MetadataBlockDTO> metadataBlocks = new HashMap<>();
        datasetVersionDTO.setMetadataBlocks(metadataBlocks);
        
        datasetVersionDTO.getMetadataBlocks().put("citation", new MetadataBlockDTO());
        datasetVersionDTO.getMetadataBlocks().get("citation").setFields(new ArrayList<FieldDTO>());
        datasetVersionDTO.getMetadataBlocks().put("socialscience", new MetadataBlockDTO());
        datasetVersionDTO.getMetadataBlocks().get("socialscience").setFields(new ArrayList<FieldDTO>());
        datasetVersionDTO.getMetadataBlocks().put("geospatial", new MetadataBlockDTO());
        datasetVersionDTO.getMetadataBlocks().get("geospatial").setFields(new ArrayList<FieldDTO>());
        
        return datasetDTO;
        
    }
       // Read the XMLStream, and populate datasetDTO and filesMap
       private void processCodeBook( XMLStreamReader xmlr, DatasetDTO datasetDTO, Map filesMap) throws XMLStreamException, ImportException {
         for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("docDscr".equals(xmlr.getLocalName())) {
                    processDocDscr(xmlr, datasetDTO);
                }
                else if ("stdyDscr".equals(xmlr.getLocalName())) {
                    processStdyDscr(xmlr, datasetDTO);
                }
                else if ("fileDscr".equals(xmlr.getLocalName()) && !importType.equals(ImportType.MIGRATION)) {
                    // EMK TODO: add this back in for ImportType.NEW
                    // processFileDscr(xmlr, datasetDTO, filesMap);
                    
                }
                  else if ("otherMat".equals(xmlr.getLocalName()) && !importType.equals(ImportType.MIGRATION) ) {
                    // EMK TODO: add this back in
                    // processOtherMat(xmlr, studyVersion);
                }

            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("codeBook".equals(xmlr.getLocalName())) return;
            }
        }
    }
       
  private void processDocDscr(XMLStreamReader xmlr, DatasetDTO datasetDTO) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                
                   if ("IDNo".equals(xmlr.getLocalName()) && StringUtil.isEmpty(datasetDTO.getIdentifier()) ) {
                    // this will set a StudyId if it has not yet been set; it will get overridden by a metadata
                    // id in the StudyDscr section, if one exists
                    if ( AGENCY_HANDLE.equals( xmlr.getAttributeValue(null, "agency") ) ) {
                        parseStudyIdHandle( parseText(xmlr), datasetDTO );
                    } 
                // EMK TODO: we need to save this somewhere when we add harvesting infrastructure 
                } /*else if ( xmlr.getLocalName().equals("holdings") && StringUtil.isEmpty(datasetDTO..getHarvestHoldings()) ) {
                    metadata.setHarvestHoldings( xmlr.getAttributeValue(null, "URI") );
                }*/
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("docDscr".equals(xmlr.getLocalName())) return;
            }
        }
    } 
     private String parseText(XMLStreamReader xmlr) throws XMLStreamException {
        return parseText(xmlr,true);
     }

     private String parseText(XMLStreamReader xmlr, boolean scrubText) throws XMLStreamException {
        String tempString = getElementText(xmlr);
        if (scrubText) {
            tempString = tempString.trim().replace('\n',' ');
        }
        return tempString;
     }
     private String parseDate (XMLStreamReader xmlr, String endTag) throws XMLStreamException {
        String date = xmlr.getAttributeValue(null, "date");
        if (date == null) {
            date = parseText(xmlr);
        }
        return date;
    } 
 /* We had to add this method because the ref getElementText has a bug where it
     * would append a null before the text, if there was an escaped apostrophe; it appears
     * that the code finds an null ENTITY_REFERENCE in this case which seems like a bug;
     * the workaround for the moment is to comment or handling ENTITY_REFERENCE in this case
     */
    private String getElementText(XMLStreamReader xmlr) throws XMLStreamException {
        if(xmlr.getEventType() != XMLStreamConstants.START_ELEMENT) {
            throw new XMLStreamException("parser must be on START_ELEMENT to read next text", xmlr.getLocation());
        }
        int eventType = xmlr.next();
        StringBuffer content = new StringBuffer();
        while(eventType != XMLStreamConstants.END_ELEMENT ) {
            if(eventType == XMLStreamConstants.CHARACTERS
            || eventType == XMLStreamConstants.CDATA
            || eventType == XMLStreamConstants.SPACE
            /* || eventType == XMLStreamConstants.ENTITY_REFERENCE*/) {
                content.append(xmlr.getText());
            } else if(eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
                || eventType == XMLStreamConstants.COMMENT
                || eventType == XMLStreamConstants.ENTITY_REFERENCE) {
                // skipping
            } else if(eventType == XMLStreamConstants.END_DOCUMENT) {
                throw new XMLStreamException("unexpected end of document when reading element text content");
            } else if(eventType == XMLStreamConstants.START_ELEMENT) {
                throw new XMLStreamException("element text content may not contain START_ELEMENT", xmlr.getLocation());
            } else {
                throw new XMLStreamException("Unexpected event type "+eventType, xmlr.getLocation());
            }
            eventType = xmlr.next();
        }
        return content.toString();
    }
    
    private void processStdyDscr(XMLStreamReader xmlr, DatasetDTO datasetDTO) throws XMLStreamException, ImportException {
        
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("citation".equals(xmlr.getLocalName())) processCitation(xmlr, datasetDTO);
                else if ("stdyInfo".equals(xmlr.getLocalName())) processStdyInfo(xmlr, datasetDTO.getDatasetVersion());
                else if ("method".equals(xmlr.getLocalName())) processMethod(xmlr, datasetDTO.getDatasetVersion());
                
                else if ("dataAccs".equals(xmlr.getLocalName())) processDataAccs(xmlr, datasetDTO.getDatasetVersion());
                  
             else if ("othrStdyMat".equals(xmlr.getLocalName())) processOthrStdyMat(xmlr, datasetDTO.getDatasetVersion());
                else if ("notes".equals(xmlr.getLocalName())) processNotes(xmlr, datasetDTO.getDatasetVersion());
                
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("stdyDscr".equals(xmlr.getLocalName())) return;
            }
        }
    }
    private void processOthrStdyMat(XMLStreamReader xmlr, DatasetVersionDTO dvDTO) throws XMLStreamException {
        List<HashSet<FieldDTO>> publications = new ArrayList<>();
        boolean replicationForFound = false;
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("relMat".equals(xmlr.getLocalName())) {
                    // this code is still here to handle imports from old DVN created ddis
                    if (!replicationForFound && REPLICATION_FOR_TYPE.equals(xmlr.getAttributeValue(null, "type"))) {
                        if (!SOURCE_DVN_3_0.equals(xmlr.getAttributeValue(null, "source"))) {
                            // this is a ddi from pre 3.0, so we should add a publication
                          /*  StudyRelPublication rp = new StudyRelPublication();
                             metadata.getStudyRelPublications().add(rp);
                             rp.setMetadata(metadata);
                             rp.setText( parseText( xmlr, "relMat" ) );
                             rp.setReplicationData(true);
                             replicationForFound = true;*/
                            HashSet<FieldDTO> set = new HashSet<>();
                            addToSet(set, DatasetFieldConstant.publicationCitation, parseText(xmlr, "relMat"));
                            if (!set.isEmpty()) {
                                publications.add(set);
                            }
                            if (!publications.isEmpty())
                                getCitation(dvDTO).addField(FieldDTO.createMultipleCompoundFieldDTO(DatasetFieldConstant.publication, publications));
                        }
                    } else {

                        List<String> relMaterial = new ArrayList<String>();
                        relMaterial.add(parseText(xmlr, "relMat"));
                        getCitation(dvDTO).addField(FieldDTO.createMultiplePrimitiveFieldDTO(DatasetFieldConstant.relatedMaterial, relMaterial));
                    }
                }  
                 else if ("relStdy".equals(xmlr.getLocalName())) {
                    List<String> relStudy = new ArrayList<String>();
                    relStudy.add(parseText(xmlr, "relStdy"));
                    getCitation(dvDTO).addField(FieldDTO.createMultiplePrimitiveFieldDTO(DatasetFieldConstant.relatedDatasets, relStudy));
                 }  else if ("relPubl".equals(xmlr.getLocalName())) {
                    HashSet<FieldDTO> set = new HashSet<>();

                    // call new parse text logic
                    Object rpFromDDI = parseTextNew(xmlr, "relPubl");
                    if (rpFromDDI instanceof Map) {
                        Map rpMap = (Map) rpFromDDI;
                        addToSet(set, DatasetFieldConstant.publicationCitation, (String) rpMap.get("text"));
                        addToSet(set, DatasetFieldConstant.publicationIDNumber, (String) rpMap.get("idNumber"));
                        addToSet(set, DatasetFieldConstant.publicationURL, (String) rpMap.get("url"));
                        if (rpMap.get("idType")!=null) {
                            set.add(FieldDTO.createVocabFieldDTO(DatasetFieldConstant.publicationIDType, ((String) rpMap.get("idType")).toLowerCase()));
                        }
                   //    rp.setText((String) rpMap.get("text"));
                        //   rp.setIdType((String) rpMap.get("idType"));
                        //   rp.setIdNumber((String) rpMap.get("idNumber"));
                        //   rp.setUrl((String) rpMap.get("url"));
                        // TODO: ask about where/whether we want to save this 
                        //  if (!replicationForFound && rpMap.get("replicationData") != null) {
                        //    rp.setReplicationData(true);
                        ///    replicationForFound = true;
                        //  }
                    } else {
                        addToSet(set, DatasetFieldConstant.publicationCitation, (String) rpFromDDI);
                        //   rp.setText( (String) rpFromDDI );
                    }
                    publications.add(set);
                    if (!publications.isEmpty()) {
                        getCitation(dvDTO).addField(FieldDTO.createMultipleCompoundFieldDTO(DatasetFieldConstant.publication, publications));
                    }

                } else if ("otherRefs".equals(xmlr.getLocalName())) {

                    List<String> otherRefs = new ArrayList<String>();
                    otherRefs.add(parseText(xmlr, "otherRefs"));
                    getCitation(dvDTO).addField(FieldDTO.createMultiplePrimitiveFieldDTO(DatasetFieldConstant.otherReferences, otherRefs));

                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {

                if ("othrStdyMat".equals(xmlr.getLocalName())) {
                    return;
                }
            }
        }
    }
     private void processCitation(XMLStreamReader xmlr, DatasetDTO datasetDTO) throws XMLStreamException, ImportException {
        DatasetVersionDTO dvDTO = datasetDTO.getDatasetVersion();
        MetadataBlockDTO citation=datasetDTO.getDatasetVersion().getMetadataBlocks().get("citation");
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("titlStmt".equals(xmlr.getLocalName())) processTitlStmt(xmlr, datasetDTO);
                else if ("rspStmt".equals(xmlr.getLocalName())) processRspStmt(xmlr,citation);
                else if ("prodStmt".equals(xmlr.getLocalName())) processProdStmt(xmlr,citation);
                else if ("distStmt".equals(xmlr.getLocalName())) processDistStmt(xmlr,citation);
                else if ("serStmt".equals(xmlr.getLocalName())) processSerStmt(xmlr,citation);
                else if ("verStmt".equals(xmlr.getLocalName())) processVerStmt(xmlr,dvDTO);
                else if ("notes".equals(xmlr.getLocalName())) {
                    String _note = parseNoteByType( xmlr, NOTE_TYPE_UNF );
                    if (_note != null) {
                        datasetDTO.getDatasetVersion().setUNF( parseUNF( _note ) );
                    } else {
                      
                       processNotes(xmlr,dvDTO);
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("citation".equals(xmlr.getLocalName())) return;
            }
        }
    }
     
 
   /**
    * 
    * 
    * @param xmlr
    * @param citation
    * @throws XMLStreamException 
    */  
   private void processStdyInfo(XMLStreamReader xmlr, DatasetVersionDTO dvDTO) throws XMLStreamException {
       List<HashSet<FieldDTO>> descriptions = new ArrayList<>();
      
       for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("subject".equals(xmlr.getLocalName())) {
                             processSubject(xmlr, getCitation(dvDTO));
                } else if ("abstract".equals(xmlr.getLocalName())) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set,"dsDescriptionDate", xmlr.getAttributeValue(null, "date"));
                    addToSet(set,"dsDescriptionValue",  parseText(xmlr, "abstract"));
                    if (!set.isEmpty()) {
                        descriptions.add(set);
                    }
                    
                } else if ("sumDscr".equals(xmlr.getLocalName())) processSumDscr(xmlr, dvDTO);
            
                 else if ("notes".equals(xmlr.getLocalName())) processNotes(xmlr,dvDTO);
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("stdyInfo".equals(xmlr.getLocalName())) {
                    if (!descriptions.isEmpty()) {
                        getCitation(dvDTO).getFields().add(FieldDTO.createMultipleCompoundFieldDTO("dsDescription", descriptions));
                    }
                    return;
                }
            }
        }
    } 
    private void processSubject(XMLStreamReader xmlr, MetadataBlockDTO citation) throws XMLStreamException {
        List<HashSet<FieldDTO>> keywords = new ArrayList<>();
        List<HashSet<FieldDTO>> topicClasses = new ArrayList<>();
          for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
              
                if ("keyword".equals(xmlr.getLocalName())) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set,"keywordVocabulary", xmlr.getAttributeValue(null, "vocab"));     
                    addToSet(set, "keywordVocabularyURI", xmlr.getAttributeValue(null, "vocabURI") );
                    addToSet(set,"keywordValue", parseText(xmlr));
                    if (!set.isEmpty()) {
                        keywords.add(set);
                    }
                } else if ("topcClas".equals(xmlr.getLocalName())) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set,"topicClassVocab", xmlr.getAttributeValue(null, "vocab"));         
                    addToSet(set,"topicClassVocabURI", xmlr.getAttributeValue(null, "vocabURI") );
                    addToSet(set,"topicClassValue",parseText(xmlr)); 
                    if (!set.isEmpty()) {
                        topicClasses.add(set);
                    }
                    
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("subject".equals(xmlr.getLocalName())) {
                    if (!keywords.isEmpty()) {
                        citation.getFields().add(FieldDTO.createMultipleCompoundFieldDTO("keyword", keywords));
                    }
                    if (!topicClasses.isEmpty()) {
                        citation.getFields().add(FieldDTO.createMultipleCompoundFieldDTO("topicClassification", topicClasses));
                    }
                    return;
                }
            } else {
              //      citation.getFields().add(FieldDTO.createPrimitiveFieldDTO( "subject",xmlr.getElementText()));
               
            }
        }
    }
    
    /**
     * Process the notes portion of the DDI doc -- if there is one
     * Return a formatted string
     * 
     * @param xmlr
     * @return 
     */
    private String formatNotesfromXML(XMLStreamReader xmlr) throws XMLStreamException{
        
        if (xmlr==null){
            throw new NullPointerException("XMLStreamReader xmlr cannot be null");
        }
        //System.out.println("formatNotesfromXML");
        // Initialize array of strings
        List<String> noteValues = new ArrayList<String>();
        String attrVal;

        // Check for "subject"
        attrVal = xmlr.getAttributeValue(null, "subject");
        if (attrVal != null){
            noteValues.add("Subject: " + attrVal);
        }
        
        // Check for "type"
        attrVal = xmlr.getAttributeValue(null, "type");
        if (attrVal != null){
            noteValues.add("Type: " + attrVal);
        }
        
        // Add notes, if they exist
        attrVal = parseText(xmlr, "notes");
        if ((attrVal != null) && (!attrVal.isEmpty())){
            noteValues.add("Notes: " + attrVal);
        }        
        
        // Nothing to add
        if (noteValues.isEmpty()){
            //System.out.println("nuthin'");
            return null;
        }
        
        //System.out.println(StringUtils.join(noteValues, " ") + ";");
        return StringUtils.join(noteValues, " ") + ";";

        /*
        Examples of xml:
        <notes type="Statistics" subject="Babylon"> </notes>
        <notes type="Note Type" subject="Note Subject">Note Text</notes>
        <notes type="Note Type 2" subject="Note Subject 2">Note Text 2</notes>
        <notes>Note Text 3</notes>
        */
        
        /*
        // Original, changed b/c of string 'null' appearing in final output
        String note = " Subject: "+xmlr.getAttributeValue(null, "subject")+" "
        + " Type: "+xmlr.getAttributeValue(null, "type")+" "
        + " Notes: "+parseText(xmlr, "notes")+";";
        addNote(note, dvDTO);
       */
    }
    
    
    private void processNotes (XMLStreamReader xmlr, DatasetVersionDTO dvDTO) throws XMLStreamException {
        
        String formattedNotes = this.formatNotesfromXML(xmlr);
        
        if (formattedNotes != null){
            this.addNote(formattedNotes, dvDTO);
        }
    }
    
    private void addNote(String noteText, DatasetVersionDTO dvDTO ) {
        MetadataBlockDTO citation = getCitation(dvDTO);
        FieldDTO field = citation.getField("notesText");
        if (field==null) {
            field = FieldDTO.createPrimitiveFieldDTO("notesText", "");
            citation.getFields().add(field);
        }
        String noteValue = field.getSinglePrimitive();
        noteValue+= noteText;
        field.setSinglePrimitive(noteValue);
    }
  
    private void processSumDscr(XMLStreamReader xmlr, DatasetVersionDTO dvDTO) throws XMLStreamException {
        List<String> geoUnit = new ArrayList<>();
        List<String> unitOfAnalysis = new ArrayList<>();
        List<String> universe = new ArrayList<>();
        List<String> kindOfData = new ArrayList<>();
        List<HashSet<FieldDTO>> geoBoundBox = new ArrayList<>();
        List<HashSet<FieldDTO>> geoCoverages = new ArrayList<>();
        FieldDTO timePeriodStart = null;
        FieldDTO timePeriodEnd = null;
        FieldDTO dateOfCollectionStart = null;
        FieldDTO dateOfCollectionEnd = null;

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("timePrd".equals(xmlr.getLocalName())) {
                    
                    String eventAttr = xmlr.getAttributeValue(null, "event");
                    if (eventAttr == null || EVENT_SINGLE.equalsIgnoreCase(eventAttr) || EVENT_START.equalsIgnoreCase(eventAttr)) {
                        timePeriodStart = FieldDTO.createPrimitiveFieldDTO("timePeriodCoveredStart", parseDate(xmlr, "timePrd"));
                    } else if (EVENT_END.equals(eventAttr)) {
                        timePeriodEnd = FieldDTO.createPrimitiveFieldDTO("timePeriodCoveredEnd", parseDate(xmlr, "timePrd"));
                    }                   
                } else if ("collDate".equals(xmlr.getLocalName())) {
                    String eventAttr = xmlr.getAttributeValue(null, "event");
                    if (eventAttr == null || EVENT_SINGLE.equalsIgnoreCase(eventAttr) || EVENT_START.equalsIgnoreCase(eventAttr)) {
                        dateOfCollectionStart = FieldDTO.createPrimitiveFieldDTO("dateOfCollectionStart", parseDate(xmlr, "collDate"));
                    } else if (EVENT_END.equals(eventAttr)) {
                        dateOfCollectionEnd = FieldDTO.createPrimitiveFieldDTO("dateOfCollectionEnd", parseDate(xmlr, "collDate"));
                    }
                   
                } else if ("nation".equals(xmlr.getLocalName())) {
                     HashSet<FieldDTO> set = new HashSet<>();
                    set.add(FieldDTO.createVocabFieldDTO("country", parseText(xmlr)));
                    geoCoverages.add(set);
                } else if ("geogCover".equals(xmlr.getLocalName())) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    set.add(FieldDTO.createPrimitiveFieldDTO("otherGeographicCoverage", parseText(xmlr)));
                    geoCoverages.add(set);
                } else if ("geogUnit".equals(xmlr.getLocalName())) {
                    geoUnit.add(parseText(xmlr));
                } else if ("geoBndBox".equals(xmlr.getLocalName())) {
                    geoBoundBox.add(processGeoBndBox(xmlr));
                } else if ("anlyUnit".equals(xmlr.getLocalName())) {
                    unitOfAnalysis.add(parseText(xmlr, "anlyUnit"));
                } else if ("universe".equals(xmlr.getLocalName())) {
                    universe.add(parseText(xmlr, "universe"));
                } else if ("dataKind".equals(xmlr.getLocalName())) {
                    kindOfData.add(parseText(xmlr));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("sumDscr".equals(xmlr.getLocalName())) {
                    if (timePeriodStart!=null || timePeriodEnd!=null) {
                        getCitation(dvDTO).addField(FieldDTO.createMultipleCompoundFieldDTO("timePeriodCovered", timePeriodStart, timePeriodEnd));
                    }
                    if (dateOfCollectionStart!=null || dateOfCollectionEnd!=null) {
                        getCitation(dvDTO).addField(FieldDTO.createMultipleCompoundFieldDTO("dateOfCollection", dateOfCollectionStart, dateOfCollectionEnd));
                    }
                  
                    if (!geoUnit.isEmpty()) {
                        getGeospatial(dvDTO).addField(FieldDTO.createMultiplePrimitiveFieldDTO("geographicUnit", geoUnit));
                    }
                    if (!unitOfAnalysis.isEmpty()) {
                        getSocialScience(dvDTO).addField(FieldDTO.createMultiplePrimitiveFieldDTO("unitOfAnalysis", unitOfAnalysis));
                    }
                    if (!universe.isEmpty()) {
                        getSocialScience(dvDTO).addField(FieldDTO.createMultiplePrimitiveFieldDTO("universe", universe));
                    }
                    if (!kindOfData.isEmpty()) {
                        getCitation(dvDTO).addField(FieldDTO.createMultiplePrimitiveFieldDTO("kindOfData", kindOfData));
                    }
                    if (!geoCoverages.isEmpty()) {
                        getGeospatial(dvDTO).addField(FieldDTO.createMultipleCompoundFieldDTO("geographicCoverage", geoCoverages));
                    }
                    if (!geoBoundBox.isEmpty()) {
                        getGeospatial(dvDTO).addField(FieldDTO.createMultipleCompoundFieldDTO("geographicBoundingBox", geoBoundBox));
                    }
                    return ;
                }
            }
        }
    }
    
    
    
 private HashSet<FieldDTO> processGeoBndBox(XMLStreamReader xmlr) throws XMLStreamException {
       HashSet<FieldDTO> set = new HashSet<>();

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("westBL".equals(xmlr.getLocalName())) {
                    addToSet(set,"westLongitude", parseText(xmlr));
                } else if ("eastBL".equals(xmlr.getLocalName())) {
                     addToSet(set,"eastLongitude", parseText(xmlr));
               } else if ("southBL".equals(xmlr.getLocalName())) {
                     addToSet(set,"southLongitude", parseText(xmlr));
               } else if ("northBL".equals(xmlr.getLocalName())) {
                      addToSet(set,"northLongitude", parseText(xmlr));
              }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("geoBndBox".equals(xmlr.getLocalName())) break;
            }
        }
        return set;
    }
   private void processMethod(XMLStreamReader xmlr, DatasetVersionDTO dvDTO ) throws XMLStreamException, ImportException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("dataColl".equals(xmlr.getLocalName())) {
                    processDataColl(xmlr, dvDTO);
                } else if ("notes".equals(xmlr.getLocalName())) {
                   
                    String noteType = xmlr.getAttributeValue(null, "type");
                    if (NOTE_TYPE_EXTENDED_METADATA.equalsIgnoreCase(noteType) ) {
                        processCustomField(xmlr, dvDTO);                       
                    } else {
                        addNote("Subject: Study Level Error Note, Notes: "+ parseText( xmlr,"notes" ) +";", dvDTO);
                       
                    }
                } else if ("anlyInfo".equals(xmlr.getLocalName())) {
                    processAnlyInfo(xmlr, getSocialScience(dvDTO));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("method".equals(xmlr.getLocalName())) return;
            }
        }
    }
   
    private void processCustomField(XMLStreamReader xmlr, DatasetVersionDTO dvDTO) throws XMLStreamException, ImportException {
        String subject = xmlr.getAttributeValue(null, "subject");
        if (!subject.isEmpty()) {
            // Syntax of subject attribute:
            // TEMPLATE:Contains Custom Fields;FIELD:Customfield1
            // first parse by semicolon
            String template = subject.substring(subject.indexOf(":") + 1, subject.indexOf(";"));
            String sourceField = subject.substring(subject.lastIndexOf(":") + 1);
            String fieldValue = parseText(xmlr);
            
            CustomFieldMap map = customFieldService.findByTemplateField(template.trim(), sourceField.trim());
            
            if (map == null) {               
               throw new ImportException("Did not find mapping for template: "+template+", sourceField: "+sourceField);
            }
            if (map.getTargetDatasetField().endsWith("#IGNORE")) {
                // if the target field is #IGNORE, that means we don't want to
                // copy this field from 3.6 to 4.0
                return;
            }
           
            // 1. Get datasetFieldType for the targetField
            // 2. find the metadatablock for this field type
            // 3. If this metadatablock doesn't exist in DTO, create it
            // 4. add field to mdatadatablock
            DatasetFieldType dsfType = datasetFieldService.findByName(map.getTargetDatasetField());
            if (dsfType == null) {
                throw new ImportException("Did not find datasetField for target: " + map.getTargetDatasetField());
            }
            String metadataBlockName = dsfType.getMetadataBlock().getName();
            MetadataBlockDTO customBlock = dvDTO.getMetadataBlocks().get(metadataBlockName);
            if (customBlock == null) {
                customBlock = new MetadataBlockDTO();
                customBlock.setDisplayName(metadataBlockName);
                dvDTO.getMetadataBlocks().put(metadataBlockName, customBlock);
            }
            if (dsfType.isChild()) {
                handleChildField(customBlock, dsfType, fieldValue);
            } else {
                if (dsfType.isAllowMultiples()) {
                    List<String> valList = new ArrayList<>();
                    valList.add(fieldValue);
                    if (dsfType.isAllowControlledVocabulary()) {
                        customBlock.addField(FieldDTO.createMultipleVocabFieldDTO(dsfType.getName(), valList));
                    } else if (dsfType.isPrimitive()) {
                        customBlock.addField(FieldDTO.createMultiplePrimitiveFieldDTO(dsfType.getName(), valList));
                    } else {
                        throw new ImportException("Unsupported custom field type: " + dsfType);
                    }
                } else {
                    if (dsfType.isAllowControlledVocabulary()) {
                        customBlock.addField(FieldDTO.createVocabFieldDTO(dsfType.getName(), fieldValue));
                    } else if (dsfType.isPrimitive()) {
                        customBlock.addField(FieldDTO.createPrimitiveFieldDTO(dsfType.getName(), fieldValue));
                    } else {
                        throw new ImportException("Unsupported custom field type: " + dsfType);
                    }
                }
            }
        }
    }
    
    private void handleChildField(MetadataBlockDTO customBlock, DatasetFieldType dsfType, String fieldValue) throws ImportException {
        DatasetFieldType parent = dsfType.getParentDatasetFieldType();

        // Create child Field
        FieldDTO child = null;
        if (dsfType.isAllowControlledVocabulary()) {
            child = FieldDTO.createVocabFieldDTO(dsfType.getName(), fieldValue);
        } else if (dsfType.isPrimitive()) {
            child = FieldDTO.createPrimitiveFieldDTO(dsfType.getName(), fieldValue);
        } else {
            throw new ImportException("Unsupported custom child field type: " + dsfType);
        }
        // Create compound field with this child as its only element
        FieldDTO compound = null;
        if (parent.isAllowMultiples()) {
            compound = FieldDTO.createMultipleCompoundFieldDTO(parent.getName(), child);
        } else {
            compound = FieldDTO.createCompoundFieldDTO(parent.getName(), child);
        }
        customBlock.addField(compound);
        
    }
   
    private void processSources(XMLStreamReader xmlr, MetadataBlockDTO citation) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                // citation dataSources
                String parsedText;
                if ("dataSrc".equals(xmlr.getLocalName())) {
                    parsedText = parseText( xmlr, "dataSrc" );
                    if (!parsedText.isEmpty()) {
                        citation.addField(FieldDTO.createMultiplePrimitiveFieldDTO("dataSources", Arrays.asList(parsedText)));
                    }
                    // citation originOfSources
                } else if ("srcOrig".equals(xmlr.getLocalName())) {
                     parsedText = parseText( xmlr, "srcOrig" );
                    if (!parsedText.isEmpty()) {
                   citation.getFields().add(FieldDTO.createPrimitiveFieldDTO("originOfSources", parsedText));
                    }
                     // citation characteristicOfSources
                } else if ("srcChar".equals(xmlr.getLocalName())) {
                    parsedText = parseText( xmlr, "srcChar" );
                    if (!parsedText.isEmpty()) {
                        citation.getFields().add(FieldDTO.createPrimitiveFieldDTO("characteristicOfSources", parsedText));
                    }
                     // citation accessToSources
                } else if ("srcDocu".equals(xmlr.getLocalName())) {
                    parsedText = parseText( xmlr, "srcDocu" );
                    if (!parsedText.isEmpty()) {                    
                        citation.getFields().add(FieldDTO.createPrimitiveFieldDTO("accessToSources", parsedText));
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("sources".equals(xmlr.getLocalName())) return;
            }
        }
    }
   private void processAnlyInfo(XMLStreamReader xmlr, MetadataBlockDTO socialScience) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                // socialscience responseRate
                if ("respRate".equals(xmlr.getLocalName())) {
                    socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("responseRate", parseText( xmlr, "respRate" )));
                // socialscience samplingErrorEstimates    
                } else if ("EstSmpErr".equals(xmlr.getLocalName())) {
                   socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("samplingErrorEstimates", parseText( xmlr, "EstSmpErr" )));
                // socialscience otherDataAppraisal    
                } else if ("dataAppr".equals(xmlr.getLocalName())) {
                   socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("otherDataAppraisal", parseText( xmlr, "dataAppr" )));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("anlyInfo".equals(xmlr.getLocalName())) return;
            }
        }
    }

    private void processDataColl(XMLStreamReader xmlr, DatasetVersionDTO dvDTO) throws XMLStreamException {
        MetadataBlockDTO socialScience =getSocialScience(dvDTO);
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                //timeMethod
                if ("timeMeth".equals(xmlr.getLocalName())) {
                  socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("timeMethod", parseText( xmlr, "timeMeth" )));
               } else if ("dataCollector".equals(xmlr.getLocalName())) {
                   socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("dataCollector", parseText( xmlr, "dataCollector" )));
                // frequencyOfDataCollection    
                } else if ("frequenc".equals(xmlr.getLocalName())) {
                  socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("frequencyOfDataCollection", parseText( xmlr, "frequenc" )));
                //samplingProcedure
                } else if ("sampProc".equals(xmlr.getLocalName())) {
                  socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("samplingProcedure", parseText( xmlr, "sampProc" )));
                //targetSampleSize
                } else if ("targetSampleSize".equals(xmlr.getLocalName())) {
                  processTargetSampleSize(xmlr, socialScience);
                    //devationsFromSamplingDesign
                } else if ("deviat".equals(xmlr.getLocalName())) {
                   socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("deviationsFromSampleDesign", parseText( xmlr, "deviat" )));
                 // collectionMode
                } else if ("collMode".equals(xmlr.getLocalName())) {
                  socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("collectionMode", parseText( xmlr, "collMode" )));                      
                //researchInstrument
                } else if ("resInstru".equals(xmlr.getLocalName())) {
                   socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("researchInstrument", parseText( xmlr, "resInstru" )));
                } else if ("sources".equals(xmlr.getLocalName())) {
                    processSources(xmlr,getCitation(dvDTO));
                } else if ("collSitu".equals(xmlr.getLocalName())) {
                     socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("dataCollectionSituation", parseText( xmlr, "collSitu" )));
                } else if ("actMin".equals(xmlr.getLocalName())) {
                      socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("actionsToMinimizeLoss", parseText( xmlr, "actMin" )));
                } else if ("ConOps".equals(xmlr.getLocalName())) {
                       socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("controlOperations", parseText( xmlr, "ConOps" )));
                } else if ("weight".equals(xmlr.getLocalName())) {
                        socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("weighting", parseText( xmlr, "weight" )));
                } else if ("cleanOps".equals(xmlr.getLocalName())) {
                       socialScience.getFields().add(FieldDTO.createPrimitiveFieldDTO("cleaningOperations", parseText( xmlr, "cleanOps" )));
                 }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("dataColl".equals(xmlr.getLocalName())) return;
            }
        }
    }

    private void processTargetSampleSize(XMLStreamReader xmlr, MetadataBlockDTO socialScience) throws XMLStreamException {
        FieldDTO sampleSize=null;
        FieldDTO sampleSizeFormula=null;
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("sampleSize".equals(xmlr.getLocalName())) {
                    sampleSize = FieldDTO.createPrimitiveFieldDTO("targetSampleActualSize",  parseText( xmlr, "sampleSize" ));
                } else if ("sampleSizeFormula".equals(xmlr.getLocalName())) {
                    sampleSizeFormula = FieldDTO.createPrimitiveFieldDTO("targetSampleSizeFormula", parseText( xmlr, "sampleSizeFormula" ));
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("targetSampleSize".equals(xmlr.getLocalName())) {
                    if (sampleSize!=null || sampleSizeFormula!=null) {
                        socialScience.getFields().add(FieldDTO.createCompoundFieldDTO("targetSampleSize", sampleSize,sampleSizeFormula));
                    }
                    return;
                }
            }
        }

    }
    /*
    EMK TODO:  In DVN 3.6, users were allowed to enter their own version date, and in addition the app assigned a version date when
     the version is released.  So DDI's that we have to migrate, we can see this:
    <verStmt>
		<version date="2004-04-04">1</version>
	</verStmt>
	<verStmt source="DVN">
		<version date="2014-05-21" type="RELEASED">1</version>
	</verStmt>
    Question:  what to do with these two different dates?  Need to review with Eleni
    Note: we should use the verStmt with source="DVN" as the 'official' version statement
    DDI's that we are migrating should have one and only one DVN version statement
    */
    private void processVerStmt(XMLStreamReader xmlr, DatasetVersionDTO dvDTO) throws XMLStreamException {
        if (importType.equals(ImportType.MIGRATION) || importType.equals(ImportType.HARVEST)) {        
             if (!"DVN".equals(xmlr.getAttributeValue(null, "source"))) {
            for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if ("version".equals(xmlr.getLocalName())) {
                        addNote("Version Date: "+ xmlr.getAttributeValue(null, "date"),dvDTO); 
                        addNote("Version Text: "+ parseText(xmlr),dvDTO);
                    } else if ("notes".equals(xmlr.getLocalName())) { processNotes(xmlr, dvDTO); }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    if ("verStmt".equals(xmlr.getLocalName())) return;
                }
            }
        } else {
            // this is the DVN version info; get version number for StudyVersion object
            for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
                 if (event == XMLStreamConstants.START_ELEMENT) {
                    if ("version".equals(xmlr.getLocalName())) {
                        dvDTO.setReleaseDate(xmlr.getAttributeValue(null, "date")); 
                        String versionState =xmlr.getAttributeValue(null,"type");
                        if (versionState!=null ) {
                            if("ARCHIVED".equals(versionState)) {
                                versionState="RELEASED";
                            } else if ("IN_REVIEW".equals(versionState)) {
                                versionState = DatasetVersion.VersionState.DRAFT.toString();
                                dvDTO.setInReview(true);
                            }
                            dvDTO.setVersionState(Enum.valueOf(VersionState.class, versionState));  
                        }                                  
                        parseVersionNumber(dvDTO,parseText(xmlr));
                     }
                } else if(event == XMLStreamConstants.END_ELEMENT) {
                    if ("verStmt".equals(xmlr.getLocalName())) return;
                }
            }
        }  
            
        }
        if (importType.equals(ImportType.NEW)) {
            // If this is a new, Draft version, versionNumber and minor versionNumber are null.
            dvDTO.setVersionState(VersionState.DRAFT);
        } 
    }
    
      private void processDataAccs(XMLStreamReader xmlr, DatasetVersionDTO dvDTO) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
             if (event == XMLStreamConstants.START_ELEMENT) {
                if ("setAvail".equals(xmlr.getLocalName())) {
                    processSetAvail(xmlr, dvDTO);
                } else if ("useStmt".equals(xmlr.getLocalName())) {
                    processUseStmt(xmlr, dvDTO);
                } else if ("notes".equals(xmlr.getLocalName())) {
                    String noteType = xmlr.getAttributeValue(null, "type");
                    if (NOTE_TYPE_TERMS_OF_USE.equalsIgnoreCase(noteType) ) {
                        if ( LEVEL_DV.equalsIgnoreCase(xmlr.getAttributeValue(null, "level"))) {
                            dvDTO.setTermsOfUse(parseText(xmlr, "notes"));
                        }
                    } else {
                        processNotes(xmlr, dvDTO);
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("dataAccs".equals(xmlr.getLocalName())) {
                    return;
                }
            }
        }
    }
    
    private void processSetAvail(XMLStreamReader xmlr, DatasetVersionDTO dvDTO) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("accsPlac".equals(xmlr.getLocalName())) {
                    dvDTO.setDataAccessPlace( parseText( xmlr, "accsPlac" ) );
                } else if ("origArch".equals(xmlr.getLocalName())) {
                    dvDTO.setOriginalArchive( parseText( xmlr, "origArch" ) );
                } else if ("avlStatus".equals(xmlr.getLocalName())) {
                    dvDTO.setAvailabilityStatus( parseText( xmlr, "avlStatus" ) );
                } else if ("collSize".equals(xmlr.getLocalName())) {
                    dvDTO.setSizeOfCollection(parseText( xmlr, "collSize" ) );
                } else if ("complete".equals(xmlr.getLocalName())) {
                    dvDTO.setStudyCompletion( parseText( xmlr, "complete" ) );
                } else if ("notes".equals(xmlr.getLocalName())) {
                    processNotes( xmlr, dvDTO );
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("setAvail".equals(xmlr.getLocalName())) return;
            }
        }
    }

    private void processUseStmt(XMLStreamReader xmlr, DatasetVersionDTO dvDTO) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("confDec".equals(xmlr.getLocalName())) {
                    dvDTO.setConfidentialityDeclaration( parseText( xmlr, "confDec" ) );
                } else if ("specPerm".equals(xmlr.getLocalName())) {
                    dvDTO.setSpecialPermissions( parseText( xmlr, "specPerm" ) );
                } else if ("restrctn".equals(xmlr.getLocalName())) {
                    dvDTO.setRestrictions( parseText( xmlr, "restrctn" ) );
                } else if ("contact".equals(xmlr.getLocalName())) {
                    dvDTO.setContactForAccess( parseText( xmlr, "contact" ) );
                } else if ("citReq".equals(xmlr.getLocalName())) {
                    dvDTO.setCitationRequirements( parseText( xmlr, "citReq" ) );
                } else if ("deposReq".equals(xmlr.getLocalName())) {
                    dvDTO.setDepositorRequirements( parseText( xmlr, "deposReq" ) );
                } else if ("conditions".equals(xmlr.getLocalName())) {
                    dvDTO.setConditions( parseText( xmlr, "conditions" ) );
                } else if ("disclaimer".equals(xmlr.getLocalName())) {
                    dvDTO.setDisclaimer( parseText( xmlr, "disclaimer" ) );
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("useStmt".equals(xmlr.getLocalName())) return;
            }
        }
    }
   /**
    * Separate the versionNumber into two parts - before the first '.' 
    * is the versionNumber, and after is the minorVersionNumber.
    * If no minorVersionNumber exists, set to "0".
    * @param dvDTO
    * @param versionNumber 
    */
    private void parseVersionNumber(DatasetVersionDTO dvDTO, String versionNumber) {
        int firstIndex = versionNumber.indexOf('.');
        if (firstIndex == -1) {
            dvDTO.setVersionNumber(Long.parseLong(versionNumber));
            dvDTO.setMinorVersionNumber("0");
        } else {
            dvDTO.setVersionNumber(Long.parseLong(versionNumber.substring(0, firstIndex - 1)));
            dvDTO.setMinorVersionNumber(versionNumber.substring(firstIndex + 1));
        }
       

    }
   
   private void processSerStmt(XMLStreamReader xmlr, MetadataBlockDTO citation) throws XMLStreamException {
          FieldDTO seriesName=null;
          FieldDTO seriesInformation=null;
          for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("serName".equals(xmlr.getLocalName())) {
                   seriesName = FieldDTO.createPrimitiveFieldDTO("seriesName", parseText(xmlr));
                  
                } else if ("serInfo".equals(xmlr.getLocalName())) {
                    seriesInformation=FieldDTO.createPrimitiveFieldDTO("seriesInformation", parseText(xmlr) );
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("serStmt".equals(xmlr.getLocalName())) {
                    citation.getFields().add(FieldDTO.createCompoundFieldDTO("series",seriesName,seriesInformation ));
                    return;
                }
            }
        }
    }

    private void processDistStmt(XMLStreamReader xmlr, MetadataBlockDTO citation) throws XMLStreamException {
        List<HashSet<FieldDTO>> distributors = new ArrayList<>();
        List<HashSet<FieldDTO>> datasetContacts = new ArrayList<>();
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("distrbtr".equals(xmlr.getLocalName())) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set, "distributorAbbreviation", xmlr.getAttributeValue(null, "abbr"));
                    addToSet(set, "distributorAffiliation", xmlr.getAttributeValue(null, "affiliation"));

                    Map<String, String> distDetails = parseCompoundText(xmlr, "distrbtr");
                    addToSet(set, "distributorName", distDetails.get("name"));
                    addToSet(set, "distributorURL", distDetails.get("url"));
                    addToSet(set, "distributorLogoURL", distDetails.get("logo"));
                    distributors.add(set);

                } else if ("contact".equals(xmlr.getLocalName())) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set, "datasetContactEmail", xmlr.getAttributeValue(null, "email"));
                    addToSet(set, "datasetContactAffiliation", xmlr.getAttributeValue(null, "affiliation"));
                    addToSet(set, "datasetContactName", parseText(xmlr));
                    datasetContacts.add(set);

                } else if ("depositr".equals(xmlr.getLocalName())) {
                    Map<String, String> depDetails = parseCompoundText(xmlr, "depositr");
                    citation.getFields().add(FieldDTO.createPrimitiveFieldDTO("depositor", depDetails.get("name")));
                } else if ("depDate".equals(xmlr.getLocalName())) {
                    citation.getFields().add(FieldDTO.createPrimitiveFieldDTO("dateOfDeposit", parseDate(xmlr, "depDate")));

                } else if ("distDate".equals(xmlr.getLocalName())) {
                         citation.getFields().add(FieldDTO.createPrimitiveFieldDTO("distributionDate", parseDate(xmlr, "distDate")));

                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("distStmt".equals(xmlr.getLocalName())) {
                    if (!distributors.isEmpty()) {
                        citation.addField(FieldDTO.createMultipleCompoundFieldDTO("distributor", distributors));
                    }
                    if (!datasetContacts.isEmpty()) {
                        citation.addField(FieldDTO.createMultipleCompoundFieldDTO("datasetContact", datasetContacts));
                    }
                    return;
                }
            }
        }
    }
    private void processProdStmt(XMLStreamReader xmlr, MetadataBlockDTO citation) throws XMLStreamException {
        List<HashSet<FieldDTO>> producers = new ArrayList<>();
        List<HashSet<FieldDTO>> grants = new ArrayList<>();
        List<HashSet<FieldDTO>> software = new ArrayList<>();

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("producer".equals(xmlr.getLocalName())) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set,"producerAbbreviation", xmlr.getAttributeValue(null, "abbr"));
                    addToSet(set,"producerAffiliation", xmlr.getAttributeValue(null, "affiliation"));
                    
                    Map<String, String> prodDetails = parseCompoundText(xmlr, "producer");
                    addToSet(set,"producerName", prodDetails.get("name"));
                    addToSet(set,"producerURL", prodDetails.get("url" ));
                    addToSet(set,"producerLogoURL", prodDetails.get("logo"));
                    if (!set.isEmpty())
                        producers.add(set);
                } else if ("prodDate".equals(xmlr.getLocalName())) {
                    citation.getFields().add(FieldDTO.createPrimitiveFieldDTO("productionDate", parseDate(xmlr, "prodDate")));
                } else if ("prodPlac".equals(xmlr.getLocalName())) {
                    citation.getFields().add(FieldDTO.createPrimitiveFieldDTO("productionPlace", parseDate(xmlr, "prodPlac")));
                } else if ("software".equals(xmlr.getLocalName())) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set,"softwareVersion", xmlr.getAttributeValue(null, "version"));
                    addToSet(set,"softwareName", xmlr.getAttributeValue(null, "version"));
                    if (!set.isEmpty()) {
                        software.add(set);
                    }

                    //TODO: ask Gustavo "fundAg"?TO
                } else if ("fundAg".equals(xmlr.getLocalName())) {
                    // save this in contributorName - member of compoundFieldContributor
                    //    metadata.setFundingAgency( parseText(xmlr) );
                } else if ("grantNo".equals(xmlr.getLocalName())) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set,"grantNumberAgency", xmlr.getAttributeValue(null, "agency"));
                    addToSet(set,"grantNumberValue", parseText(xmlr));
                    if (!set.isEmpty()){
                        grants.add(set);
                    }

                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("prodStmt".equals(xmlr.getLocalName())) {
                    if (!software.isEmpty()) {
                        citation.addField(FieldDTO.createMultipleCompoundFieldDTO("software", software));
                    }
                    if (!grants.isEmpty()) {
                        citation.addField(FieldDTO.createMultipleCompoundFieldDTO("grantNumber", grants));
                    } 
                    if (!producers.isEmpty()) {
                        citation.getFields().add(FieldDTO.createMultipleCompoundFieldDTO("producer", producers));
                    }
                    return;
                }
            }
        }
    }
    
   private void processTitlStmt(XMLStreamReader xmlr, DatasetDTO datasetDTO) throws XMLStreamException, ImportException {
       MetadataBlockDTO citation = datasetDTO.getDatasetVersion().getMetadataBlocks().get("citation");
       List<HashSet<FieldDTO>> otherIds = new ArrayList<>();
       
       for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("titl".equals(xmlr.getLocalName())) {
                    FieldDTO field = FieldDTO.createPrimitiveFieldDTO("title", parseText(xmlr));
                    citation.getFields().add(field);
                } else if ("subTitl".equals(xmlr.getLocalName())) {
                  FieldDTO field = FieldDTO.createPrimitiveFieldDTO("subtitle", parseText(xmlr));
                   citation.getFields().add(field);
                } else if ("altTitl".equals(xmlr.getLocalName())) {
                  FieldDTO field = FieldDTO.createPrimitiveFieldDTO("alternativeTitle", parseText(xmlr));
                   citation.getFields().add(field);
                } else if ("IDNo".equals(xmlr.getLocalName())) {
                    if ( AGENCY_HANDLE.equals( xmlr.getAttributeValue(null, "agency") ) ) {
                        parseStudyIdHandle( parseText(xmlr), datasetDTO );
                    } else if ( AGENCY_DOI.equals( xmlr.getAttributeValue(null, "agency") ) ) {
                        parseStudyIdDOI( parseText(xmlr), datasetDTO );
                    } else {
                        HashSet<FieldDTO> set = new HashSet<>();
                        addToSet(set,"otherIdAgency", xmlr.getAttributeValue(null, "agency"));
                        addToSet(set,"otherIdValue", parseText(xmlr));
                        if(!set.isEmpty()){
                            otherIds.add(set);
                        }
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("titlStmt".equals(xmlr.getLocalName())) {
                    if (!otherIds.isEmpty()) {
                        citation.addField(FieldDTO.createMultipleCompoundFieldDTO("otherId", otherIds));
                    }
                    return;
                }
            }
        }
    }
   private void processRspStmt(XMLStreamReader xmlr, MetadataBlockDTO citation) throws XMLStreamException {
     
       List<HashSet<FieldDTO>> authors = new ArrayList<>();
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("AuthEnty".equals(xmlr.getLocalName())) {
                    HashSet<FieldDTO> set = new HashSet<>();
                    addToSet(set,"authorAffiliation", xmlr.getAttributeValue(null, "affiliation"));
                    addToSet(set,"authorName", parseText(xmlr));
                    if (!set.isEmpty()) {
                        authors.add(set);
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("rspStmt".equals(xmlr.getLocalName())) {
                    if (!authors.isEmpty()) {
                        FieldDTO author = FieldDTO.createMultipleCompoundFieldDTO("author", authors);
                        citation.getFields().add(author);
                    }
                  
                    return;
                }
            }
        }
    }
   private Map<String,String> parseCompoundText (XMLStreamReader xmlr, String endTag) throws XMLStreamException {
        Map<String,String> returnMap = new HashMap<String,String>();
        String text = "";

        while (true) {
            int event = xmlr.next();
            if (event == XMLStreamConstants.CHARACTERS) {
                if (text != "") { text += "\n";}
                text += xmlr.getText().trim().replace('\n',' ');
            } else if (event == XMLStreamConstants.START_ELEMENT) {
                if ("ExtLink".equals(xmlr.getLocalName())) {
                    String mapKey  = ("image".equalsIgnoreCase( xmlr.getAttributeValue(null, "role") ) || "logo".equalsIgnoreCase(xmlr.getAttributeValue(null, "title")))? "logo" : "url";
                    returnMap.put( mapKey, xmlr.getAttributeValue(null, "URI") );
                    parseText(xmlr, "ExtLink"); // this line effectively just skips though until the end of the tag
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals(endTag)) break;
            }
        }

        returnMap.put( "name", text );
        return returnMap;
    }
   
    private String parseText(XMLStreamReader xmlr, String endTag) throws XMLStreamException {
         return (String) parseTextNew(xmlr,endTag);
     }
     
     
     private Object parseTextNew(XMLStreamReader xmlr, String endTag) throws XMLStreamException {
        String returnString = "";
        Map returnMap = null;

        while (true) {
            if (!"".equals(returnString)) { returnString += "\n";}
            int event = xmlr.next();
            if (event == XMLStreamConstants.CHARACTERS) {
                returnString += xmlr.getText().trim().replace('\n',' ');
           } else if (event == XMLStreamConstants.START_ELEMENT) {
                if ("p".equals(xmlr.getLocalName())) {
                    returnString += "<p>" + parseText(xmlr, "p") + "</p>";
                } else if ("emph".equals(xmlr.getLocalName())) {
                    returnString += "<em>" + parseText(xmlr, "emph") + "</em>";
                } else if ("hi".equals(xmlr.getLocalName())) {
                    returnString += "<strong>" + parseText(xmlr, "hi") + "</strong>";
                } else if ("ExtLink".equals(xmlr.getLocalName())) {
                    String uri = xmlr.getAttributeValue(null, "URI");
                    String text = parseText(xmlr, "ExtLink").trim();
                    returnString += "<a href=\"" + uri + "\">" + ( StringUtil.isEmpty(text) ? uri : text) + "</a>";
                } else if ("list".equals(xmlr.getLocalName())) {
                    returnString += parseText_list(xmlr);
                } else if ("citation".equals(xmlr.getLocalName())) {
                    if (SOURCE_DVN_3_0.equals(xmlr.getAttributeValue(null, "source")) ) {
                        returnMap = parseDVNCitation(xmlr);
                    } else {
                        returnString += parseText_citation(xmlr);
                    }
                } else {
                    throw new EJBException("ERROR occurred in mapDDI (parseText): tag not yet supported: <" + xmlr.getLocalName() + ">" );
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if (xmlr.getLocalName().equals(endTag)) break;
            }
        }
        
        if (returnMap != null) {
            // this is one of our new citation areas for DVN3.0
            return returnMap;
        }
      
        // otherwise it's a standard section and just return the String like we always did
        return returnString.trim();
    }
     
    private String parseNoteByType(XMLStreamReader xmlr, String type) throws XMLStreamException {
        if (type.equalsIgnoreCase(xmlr.getAttributeValue(null, "type"))) {
            return parseText(xmlr);
        } else {
            return null;
        }
    }
  private String parseText_list (XMLStreamReader xmlr) throws XMLStreamException {
        String listString = null;
        String listCloseTag = null;

        // check type
        String listType = xmlr.getAttributeValue(null, "type");
        if ("bulleted".equals(listType) ){
            listString = "<ul>\n";
            listCloseTag = "</ul>";
        } else if ("ordered".equals(listType) ) {
            listString = "<ol>\n";
            listCloseTag = "</ol>";
        } else {
            // this includes the default list type of "simple"
            throw new EJBException("ERROR occurred in mapDDI (parseText): ListType of types other than {bulleted, ordered} not currently supported.");
        }

        while (true) {
            int event = xmlr.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("itm".equals(xmlr.getLocalName())) {
                    listString += "<li>" + parseText(xmlr,"itm") + "</li>\n";
                } else {
                    throw new EJBException("ERROR occurred in mapDDI (parseText): ListType does not currently supported contained LabelType.");
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("list".equals(xmlr.getLocalName())) break;
            }
        }

        return (listString + listCloseTag);
    }

    private String parseText_citation (XMLStreamReader xmlr) throws XMLStreamException {
        String citation = "<!--  parsed from DDI citation title and holdings -->";
        boolean addHoldings = false;
        String holdings = "";

        while (true) {
            int event = xmlr.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("titlStmt".equals(xmlr.getLocalName())) {
                    while (true) {
                        event = xmlr.next();
                        if (event == XMLStreamConstants.START_ELEMENT) {
                            if ("titl".equals(xmlr.getLocalName())) {
                                citation += parseText(xmlr);
                            }
                        } else if (event == XMLStreamConstants.END_ELEMENT) {
                            if ("titlStmt".equals(xmlr.getLocalName())) break;
                        }
                    }
                } else if ("holdings".equals(xmlr.getLocalName())) {
                    String uri = xmlr.getAttributeValue(null, "URI");
                    String holdingsText = parseText(xmlr);

                    if ( !StringUtil.isEmpty(uri) || !StringUtil.isEmpty(holdingsText)) {
                        holdings += addHoldings ? ", " : "";
                        addHoldings = true;

                        if ( StringUtil.isEmpty(uri) ) {
                            holdings += holdingsText;
                        } else if ( StringUtil.isEmpty(holdingsText) ) {
                            holdings += "<a href=\"" + uri + "\">" + uri + "</a>";
                        } else {
                            // both uri and text have values
                            holdings += "<a href=\"" + uri + "\">" + holdingsText + "</a>";
                        }
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("citation".equals(xmlr.getLocalName())) break;
            }
        }

        if (addHoldings) {
            citation += " (" + holdings + ")";
        }

        return citation;
    }
  
    private String parseUNF(String unfString) {
        if (unfString.contains("UNF:")) {
            return unfString.substring( unfString.indexOf("UNF:") );
        } else {
            return null;
        }
    }
  
    private Map parseDVNCitation(XMLStreamReader xmlr) throws XMLStreamException {
        Map returnValues = new HashMap();
        
        while (true) {
            int event = xmlr.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
               if ("IDNo".equals(xmlr.getLocalName())) {
                    returnValues.put("idType", xmlr.getAttributeValue(null, "agency") );
                    returnValues.put("idNumber", parseText(xmlr) );                   
               }
                else if ("biblCit".equals(xmlr.getLocalName())) {
                    returnValues.put("text", parseText(xmlr) );                   
                }
                else if ("holdings".equals(xmlr.getLocalName())) {
                    returnValues.put("url", xmlr.getAttributeValue(null, "URI") );                 
                }
                else if ("notes".equals(xmlr.getLocalName())) {
                    if (NOTE_TYPE_REPLICATION_FOR.equals(xmlr.getAttributeValue(null, "type")) ) {
                        returnValues.put("replicationData", new Boolean(true));
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("citation".equals(xmlr.getLocalName())) break;
            }
        } 
        
        return returnValues;
    }    
     
   private void parseStudyIdHandle(String _id, DatasetDTO datasetDTO)  {

        int index1 = _id.indexOf(':');
        int index2 = _id.indexOf('/');
        if (index1==-1) {
            throw new EJBException("Error parsing (Handle) IdNo: "+_id+". ':' not found in string");
        } else {
            datasetDTO.setProtocol(_id.substring(0,index1));
        }
        if (index2 == -1) {
            throw new EJBException("Error parsing (Handle) IdNo: "+_id+". '/' not found in string");

        } else {
            datasetDTO.setAuthority(_id.substring(index1+1, index2));
        }
        datasetDTO.setDoiSeparator("/");
        datasetDTO.setProtocol("hdl");
        datasetDTO.setIdentifier(_id.substring(index2+1));
    }

    private void parseStudyIdDOI(String _id, DatasetDTO datasetDTO) throws ImportException{
        int index1 = _id.indexOf(':');
        int index2 = _id.lastIndexOf('/');
        if (index1==-1) {
            throw new EJBException("Error parsing (DOI) IdNo: "+_id+". ':' not found in string");
        }  
       
        if (index2 == -1) {
            throw new ImportException("Error parsing (DOI) IdNo: "+_id+". '/' not found in string");

        } else {
               datasetDTO.setAuthority(_id.substring(index1+1, index2));
        }
        datasetDTO.setProtocol("doi");
        datasetDTO.setDoiSeparator("/");
       
        datasetDTO.setIdentifier(_id.substring(index2+1));
    }
    // Helper methods
    private MetadataBlockDTO getCitation(DatasetVersionDTO dvDTO) {
        return dvDTO.getMetadataBlocks().get("citation");
    }
    
    private MetadataBlockDTO getGeospatial(DatasetVersionDTO dvDTO) {
        return dvDTO.getMetadataBlocks().get("geospatial");
    }
    
  private MetadataBlockDTO getSocialScience(DatasetVersionDTO dvDTO) {
        return dvDTO.getMetadataBlocks().get("socialscience");
    }
      
    
    private void addToSet(HashSet<FieldDTO> set, String typeName, String value ) {
        if (value!=null && !value.trim().isEmpty()) {
            set.add(FieldDTO.createPrimitiveFieldDTO(typeName, value));
        }
    }
    
    

    private void processFileDscr(XMLStreamReader xmlr, DatasetDTO datasetDTO, Map filesMap) throws XMLStreamException {
        FileMetadataDTO fmdDTO = new FileMetadataDTO();
        
        datasetDTO.getDatasetVersion().getFileMetadatas().add(fmdDTO);

        //StudyFile sf = new OtherFile(studyVersion.getStudy()); // until we connect the sf and dt, we have to assume it's an other file
        // as an experiment, I'm going to do it the other way around:
        // assume that every fileDscr is a subsettable file now, and convert them
        // to otherFiles later if no variables are referencing it -- L.A.


     //   TabularDataFile sf = new TabularDataFile(studyVersion.getStudy()); 
        DataFileDTO dfDTO = new DataFileDTO();
        DataTableDTO dtDTO = new DataTableDTO();
        dfDTO.getDataTables().add(dtDTO);
        fmdDTO.setDataFile(dfDTO);
        datasetDTO.getDataFiles().add(dfDTO);
       
        // EMK TODO: ask Gustavo about this property
     //   dfDTO.setFileSystemLocation( xmlr.getAttributeValue(null, "URI"));
        String ddiFileId = xmlr.getAttributeValue(null, "ID");

        /// the following Strings are used to determine the category

        String catName = null;
        String icpsrDesc = null;
        String icpsrId = null;

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("fileTxt".equals(xmlr.getLocalName())) {
                    String tempDDIFileId = processFileTxt(xmlr, fmdDTO, dtDTO);
                    ddiFileId = ddiFileId != null ? ddiFileId : tempDDIFileId;
                }
                else if ("notes".equals(xmlr.getLocalName())) {
                    String noteType = xmlr.getAttributeValue(null, "type");
                    if (NOTE_TYPE_UNF.equalsIgnoreCase(noteType) ) {
                        String unf = parseUNF( parseText(xmlr) );
                        dfDTO.setUNF(unf);
                        dtDTO.setUnf(unf);
                    } else if ("vdc:category".equalsIgnoreCase(noteType) ) {
                        catName = parseText(xmlr);
                    } else if ("icpsr:category".equalsIgnoreCase(noteType) ) {
                        String subjectType = xmlr.getAttributeValue(null, "subject");
                        if ("description".equalsIgnoreCase(subjectType)) {
                            icpsrDesc = parseText(xmlr);
                        } else if ("id".equalsIgnoreCase(subjectType)) {
                            icpsrId = parseText(xmlr);
                        }
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {// </codeBook>
                if ("fileDscr".equals(xmlr.getLocalName())) {
                    // post process
                    if (fmdDTO.getLabel() == null || "".equals(fmdDTO.getLabel().trim())) {
                        fmdDTO.setLabel("file");
                    }

                    fmdDTO.setCategory(determineFileCategory(catName, icpsrDesc, icpsrId));


                    if (ddiFileId != null) {
                        List filesMapEntry = new ArrayList();
                        filesMapEntry.add(fmdDTO);
                        filesMapEntry.add(dtDTO);
                        filesMap.put( ddiFileId, filesMapEntry);
                    }

                    return;
                }
            }
        }
    }
    
     private String determineFileCategory(String catName, String icpsrDesc, String icpsrId) {
        if (catName == null) {
            catName = icpsrDesc;

            if (catName != null) {
                if (icpsrId != null && !"".equals(icpsrId.trim())) {
                    catName = icpsrId + ". " + catName;
                }
            }
        }

        return (catName != null ? catName : "");
    }
  /**
     * sets fmdDTO.label, fmdDTO.description, fmdDTO.studyfile.subsettableFileType
     * @param xmlr
     * @param fmdDTO
     * @param dtDTO
     * @return fmdDTO.label (ddiFileId)
     * @throws XMLStreamException 
     */
    private String processFileTxt(XMLStreamReader xmlr, FileMetadataDTO fmdDTO, DataTableDTO dtDTO) throws XMLStreamException {
        String ddiFileId = null;
        DataFileDTO dfDTO = fmdDTO.getDataFile();

        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("fileName".equals(xmlr.getLocalName())) {
                    ddiFileId = xmlr.getAttributeValue(null, "ID");
                    fmdDTO.setLabel( parseText(xmlr) );
                    /*sf.setFileType( FileUtil.determineFileType( fmdDTO.getLabel() ) );*/

                } else if ("fileCont".equals(xmlr.getLocalName())) {
                    fmdDTO.setDescription( parseText(xmlr) );
                }  else if ("dimensns".equals(xmlr.getLocalName())) processDimensns(xmlr, dtDTO);
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                if ("fileTxt".equals(xmlr.getLocalName())) {
                    // Now is the good time to determine the type of this subsettable
                    // file (now that the "<dimensns>" section has been parsed, we 
                    // should know whether it's a tab, or a fixed field:
                    String subsettableFileType = "application/octet-stream"; // better this than nothing!
                    if ( dtDTO.getRecordsPerCase() != null )  {
                        subsettableFileType="text/x-fixed-field";
                    } else {
                        subsettableFileType="text/tab-separated-values";
                    }        
                    //EMK TODO: ask Gustavo & Leonid what should be used here instead of setFileType
              //      dfDTO.setFileType( subsettableFileType );
                    
                    return ddiFileId;
                }
            }
        }
        return ddiFileId;
    }  
    
  /**
    * Set dtDTO. caseQuantity, varQuantity, recordsPerCase
    * @param xmlr
    * @param dtDTO
    * @throws XMLStreamException 
    */
    private void processDimensns(XMLStreamReader xmlr, DataTableDTO dtDTO) throws XMLStreamException {
        for (int event = xmlr.next(); event != XMLStreamConstants.END_DOCUMENT; event = xmlr.next()) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("caseQnty".equals(xmlr.getLocalName())) {
                    try {
                        dtDTO.setCaseQuantity( new Long( parseText(xmlr) ) );
                    } catch (NumberFormatException ex) {}
                } else if ("varQnty".equals(xmlr.getLocalName())) {
                    try{
                        dtDTO.setVarQuantity( new Long( parseText(xmlr) ) );
                    } catch (NumberFormatException ex) {}
                } else if ("recPrCas".equals(xmlr.getLocalName())) {
                    try {
                        dtDTO.setRecordsPerCase( new Long( parseText(xmlr) ) );
                    } catch (NumberFormatException ex) {}
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {// </codeBook>
                if ("dimensns".equals(xmlr.getLocalName())) return;
            }
        }
    }
    
}

