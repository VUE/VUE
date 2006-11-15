/*
 * TestRepository.java
 *
 * Created on April 7, 2006, 4:45 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

/**
 *
 * @author akumar03
 */
package edu.tufts.osidimpl.repository.fedora_2_0;

import junit.framework.TestCase;

public class TestRepository extends TestCase {
    
    public void testManager() {
        try {
            org.osid.OsidContext context = new org.osid.OsidContext();
            org.osid.repository.RepositoryManager repositoryManager = (org.osid.repository.RepositoryManager)org.osid.OsidLoader.getManager(
                    "org.osid.repository.RepositoryManager",
                    "edu.tufts.osidimpl.repository.fedora_2_0",
                    context,
                    new java.util.Properties());
            
            
        } catch(Throwable t) {
            t.printStackTrace();
        }
        try {
            org.osid.OsidContext context = new org.osid.OsidContext();
            java.util.Properties properties = new java.util.Properties();
            properties.setProperty("fedora20ConfigurationFilename","fedora.xml");
            properties.setProperty("fedora20DisplayName","Tufts Digital Library");
            properties.setProperty("fedora20Address","dl.tufts.edu");
            properties.setProperty("fedora20Port","8080");
            properties.setProperty("fedora20UserName","test");
            properties.setProperty("fedora20Password","test");
            org.osid.repository.RepositoryManager repositoryManager = (org.osid.repository.RepositoryManager)org.osid.OsidLoader.getManager("org.osid.repository.RepositoryManager",
                    "edu.tufts.osidimpl.repository.fedora_2_0",
                    context,
                    properties);
            
            org.osid.repository.RepositoryIterator ri = repositoryManager.getRepositories();
            int assetCount =0;
            int recordCount =0;
            int partCount = 0;
            while (ri.hasNextRepository()) {
                org.osid.repository.Repository repository = ri.nextRepository();
                System.out.println("Next repository " + repository.getDisplayName() + " " + repository.getId().getIdString());
                org.osid.shared.TypeIterator ti = repository.getSearchTypes();
                while (ti.hasNextType()) System.out.println(ti.nextType().getKeyword());
                System.out.println("starting search");
                org.osid.repository.AssetIterator assetIterator = repository.getAssetsBySearch("Jumbo",new Type("mit.edu","search","keyword"),null);
                while (assetIterator.hasNextAsset()) {
                    assetCount++;
                    org.osid.repository.Asset asset = assetIterator.nextAsset();
                    System.out.println("Asset " + asset.getDisplayName());
                    System.out.println("Description " + asset.getDescription());
                    System.out.println("Id " + asset.getId().getIdString());
                    
                    org.osid.repository.RecordIterator recordIterator = asset.getRecords();
                    while (recordIterator.hasNextRecord()) {
                        recordCount++;
                        org.osid.repository.Record sourceRecord = recordIterator.nextRecord();
                        org.osid.repository.PartIterator partIterator = sourceRecord.getParts();
                        while (partIterator.hasNextPart()) {
                            partCount++;
                            org.osid.repository.Part sourcePart = partIterator.nextPart();
                            if (sourcePart.getValue() != null) {
                                System.out.println();
                                System.out.println("Part Type: " + sourcePart.getPartStructure().getType().getKeyword());
                                System.out.println("Value: " + sourcePart.getValue());
                            }
                        }
                    }
                }
                System.out.println();
            }
            System.out.println("Searched Tufts Digital Library for 'jumbo'");
            System.out.println("Returned "+assetCount+" assets, "+recordCount+" records, "+partCount+" parts");
        } catch (Throwable t) {
            t.printStackTrace();
        }
        
        
    }
    
    /** Creates a new instance of TestRepository */
    public TestRepository() {
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
    }
    
}
