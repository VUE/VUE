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
