/*
 * FedoraObjectAssetType.java
 *
 * Created on October 10, 2003, 5:23 PM
 */

package tufts.oki.dr.fedora;

/**
 *
 * @author  akumar03
 */

import java.io.*;

public class FedoraObjectAssetType extends osid.shared.Type {   
    private java.util.Vector infoStructures = new java.util.Vector();
    private DR dr;
    private String id;
    private String type ="TUFTS_STD_IMAGE";
    /** Creates a new instance of FedoraObjectAssetType */
    public FedoraObjectAssetType(DR dr,String type) throws osid.dr.DigitalRepositoryException {
        super("Fedora_Asset","tufts.edu",type);
        this.dr = dr;
        this.type = type;
        id = type;
        loadInfoStructures();
    }
    
    public String getType() {
        return this.type;
    }
    
    public osid.dr.InfoStructureIterator getInfoStructures() throws osid.dr.DigitalRepositoryException{
            return (osid.dr.InfoStructureIterator)new InfoStructureIterator(infoStructures);
    }
    
    public String toString() {
        return getClass().getName()+" id:"+id;
    }
    
    private void loadInfoStructures() throws  osid.dr.DigitalRepositoryException {
        java.util.prefs.Preferences   prefs = java.util.prefs.Preferences.userRoot().node("/");
        try {
            FileInputStream fis = new FileInputStream(dr.getConfiguration().getFile().replaceAll("%20"," "));
            prefs.importPreferences(fis);
            String infoStructureString = prefs.get(type,"fedora-system:3");
            java.util.Vector infoStructureVector = FedoraUtils.stringToVector(infoStructureString);
            java.util.Iterator i = infoStructureVector.iterator();
            while(i.hasNext()) {
                String strBehaviorInfoStructure = (String)i.next();
                java.util.Vector infoPartVector = FedoraUtils.stringToVector(prefs.get(strBehaviorInfoStructure,""));
                System.out.println("loadInfoStructures : Type = "+type+" infoStructure "+strBehaviorInfoStructure+ " infoPartString "+prefs.get(strBehaviorInfoStructure,"")+ " count ="+infoPartVector.capacity());
                infoStructures.add(new BehaviorInfoStructure(dr,strBehaviorInfoStructure,infoPartVector.iterator()));
            }
        }catch(Exception ex) {
            throw new osid.dr.DigitalRepositoryException("DR.loadInfoStructure  "+ex);
        }
    }
    
}