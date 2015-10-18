

package edu.harvard.iq.dataverse;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 *
 * @author Leonid Andreev
 */
@Table( uniqueConstraints = @UniqueConstraint(columnNames={"foreignMetadataFormatMapping_id","foreignFieldXpath"}) 
      , indexes = {@Index(columnList="foreignmetadataformatmapping_id")
		, @Index(columnList="foreignfieldxpath")
		, @Index(columnList="parentfieldmapping_id")})
@NamedQueries({
  @NamedQuery( name="ForeignMetadataFieldMapping.findByPath",
               query="SELECT fmfm FROM ForeignMetadataFieldMapping fmfm WHERE fmfm.foreignMetadataFormatMapping.name=:formatName AND fmfm.foreignFieldXPath=:xPath")  
})
@Entity
public class ForeignMetadataFieldMapping implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    @ManyToOne(cascade = CascadeType.MERGE)
    private ForeignMetadataFormatMapping foreignMetadataFormatMapping;

    @Column(name = "foreignFieldXPath", columnDefinition = "TEXT")
    private String foreignFieldXPath;
    
    @Column(name = "datasetfieldName", columnDefinition = "TEXT")
    private String datasetfieldName;    

    @OneToMany(mappedBy = "parentFieldMapping", cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    private Collection<ForeignMetadataFieldMapping> childFieldMappings;
        
    @ManyToOne(cascade = CascadeType.MERGE)
    private ForeignMetadataFieldMapping parentFieldMapping;
    
    private boolean isAttribute;
    
    /* getters/setters: */

    public ForeignMetadataFormatMapping getForeignMetadataFormatMapping() {
        return foreignMetadataFormatMapping;
    }

    public void setForeignMetadataFormatMapping(ForeignMetadataFormatMapping foreignMetadataFormatMapping) {
        this.foreignMetadataFormatMapping = foreignMetadataFormatMapping;
    }
    
    public String getForeignFieldXPath() {
        return foreignFieldXPath;
    }

    public void setForeignFieldXPath(String foreignFieldXPath) {
        this.foreignFieldXPath = foreignFieldXPath;
    }
    
    public String getDatasetfieldName() {
        return datasetfieldName;
    }

    public void setDatasetfieldName(String datasetfieldName) {
        this.datasetfieldName = datasetfieldName;
    }
    
    
    public Collection<ForeignMetadataFieldMapping> getChildFieldMappings() {
        return this.childFieldMappings;
    }

    public void setChildFieldMappings(Collection<ForeignMetadataFieldMapping> childFieldMappings) {
        this.childFieldMappings = childFieldMappings;
    }
    
    /*
    public Collection<ForeignMetadataFieldMapping> getAttributeMappings() {
        return this.attributeMappings;
    }

    public void setAttributeMappings(Collection<ForeignMetadataFieldMapping> attributeMappings) {
        this.attributeMappings = attributeMappings;
    }
    */
    
    
    public ForeignMetadataFieldMapping getParentFieldMapping() {
        return parentFieldMapping;
    }

    public void setParentFieldMapping(ForeignMetadataFieldMapping parentFieldMapping) {
        this.parentFieldMapping = parentFieldMapping;
    }
    
    public boolean isAttribute() {
        return isAttribute;
    }

    public void setIsAttribute(boolean isAttribute) {
        this.isAttribute = isAttribute;
    }
    
    /* logical: */
    
    public boolean isChild() {
        return this.parentFieldMapping != null;        
    }    
    
    public boolean HasChildren() {
        return !this.childFieldMappings.isEmpty();
    }
    
    /*
    public boolean HasAttributes() {
        return !this.attributeMappings.isEmpty();
    }
    */

    public boolean HasParent() {
        return this.parentFieldMapping != null;
    }
    /* overrides: */ 

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ForeignMetadataFieldMapping)) {
            return false;
        }
        ForeignMetadataFieldMapping other = (ForeignMetadataFieldMapping) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.ForeignMetadataFieldMapping[ id=" + id + " ]";
    }
    
}
