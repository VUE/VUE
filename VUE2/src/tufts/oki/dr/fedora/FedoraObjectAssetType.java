/*
 * FedoraObjectAssetType.java
 *
 * Created on October 10, 2003, 5:23 PM
 */

package tufts.oki.dr.fedora;

import java.io.*;
import java.util.*;
import java.util.prefs.Preferences;

/**
 *
 * @author  akumar03
 */

public class FedoraObjectAssetType extends osid.shared.Type {   
    private Vector infoStructures;
    private DR dr;
    private String id;
    private String type ="TUFTS_STD_IMAGE";

    /** Creates a new instance of FedoraObjectAssetType */
    public FedoraObjectAssetType(DR dr,String type) throws osid.dr.DigitalRepositoryException {
        super("Fedora_Asset","tufts.edu",type);
        this.dr = dr;
        this.type = type;
        this.id = type;
        //out("CONSTRUCTED with dr " + dr + " " + dr.getDisplayName());;
        this.infoStructures = loadInfoStructures(dr);
    }
    
    public String getType() {
        return this.type;
    }
    
    public osid.dr.InfoStructureIterator getInfoStructures()
        throws osid.dr.DigitalRepositoryException
    {
        return (osid.dr.InfoStructureIterator) new InfoStructureIterator(infoStructures);
    }
    
    private Vector loadInfoStructures(DR dr)
        throws osid.dr.DigitalRepositoryException
    {
        Vector infoStructures = new Vector();
        try {
            Preferences prefs = DR.loadPreferences(dr.getConfiguration());
            String infoStructureString = prefs.get(type,"fedora-system:3");
            Vector infoStructureVector = FedoraUtils.stringToVector(infoStructureString);
            Iterator i = infoStructureVector.iterator();
            while (i.hasNext()) {
                String strBehaviorInfoStructure = (String)i.next();
                Vector infoPartVector = FedoraUtils.stringToVector(prefs.get(strBehaviorInfoStructure,""));
                out("infoStructure " + strBehaviorInfoStructure
                    + " infoPartString " + prefs.get(strBehaviorInfoStructure,"")
                    + " count=" + infoPartVector.capacity());
                infoStructures.add(new BehaviorInfoStructure(dr,strBehaviorInfoStructure,infoPartVector.iterator()));
            }
        } catch (Exception ex) {
            throw new osid.dr.DigitalRepositoryException("DR.loadInfoStructure  "+ex);
        }
        return infoStructures;
    }

    private void out(String s)
        throws osid.dr.DigitalRepositoryException
    {
        System.out.println("FOAT[" + dr.getDisplayName() + ", " + type + "] " + s);
    }
    
    public String toString() {
        return getClass().getName()+" id:"+id;
    }
        
}
