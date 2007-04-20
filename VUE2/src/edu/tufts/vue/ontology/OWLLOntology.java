/*
 * OWLLOntology.java
 *
 * Created on April 20, 2007, 12:26 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author akumar03
 */
package edu.tufts.vue.ontology;

public class OWLLOntology {
    
    /** Creates a new instance of OWLLOntology */
    public OWLLOntology() {
    }
    
    /**
     * @param args the command line arguments
     */
   public org.osid.shared.Type getType() {
       return OntologyType.OWL_TYPE;
   }
    
}
