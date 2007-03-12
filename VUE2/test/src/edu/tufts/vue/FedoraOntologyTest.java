/*
 * FedoraOntologyTest.java
 *
 * Created on March 8, 2007, 11:38 AM
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
 *
 * @author akumar03
 */


package edu.tufts.vue;


import junit.framework.TestCase;
import java.net.*;
import tufts.vue.*;
import tufts.vue.action.*;
import java.io.*;
import java.util.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.util.iterator.*;


import edu.tufts.vue.ontology.*;
public class FedoraOntologyTest extends TestCase{
    public static final String ONTO_URL = "http://www.fedora.info/definitions/1/0/fedora-relsext-ontology.rdfs";
    /**
    public void testFedoraOntologyRead() {
        try {
            OntModel m = ModelFactory.createOntologyModel();
            m.read(ONTO_URL);
            System.out.println("Read ontology:"+m.toString());
            StmtIterator i = m.listStatements();
            while(i.hasNext()) {
                com.hp.hpl.jena.rdf.model.Statement stmt = i.nextStatement();
                com.hp.hpl.jena.rdf.model.Resource res = stmt.getSubject();
                Object obj = stmt.getObject();
                System.out.println("Stmt - subject:"+res+ " predicate:"+stmt.getPredicate().getLocalName()+" class:"+obj.getClass());
                if(obj instanceof com.hp.hpl.jena.rdf.model.Resource){
                    com.hp.hpl.jena.rdf.model.Resource r = (com.hp.hpl.jena.rdf.model.Resource) obj;
                    System.out.println("Resource - Name: "+r.getURI() );
                }
                
                
            }
            System.out.println("LIST ONTO PROPERTIES======");
        //    OntClass a1 = m.getOntClass( NS + "A" );
         //   Iterator i1 = a1.listDeclaredProperties();
            
            ExtendedIterator ei = m.listNamedClasses();
            while(ei.hasNext()) {
                OntClass  oc = (OntClass) ei.next();
                System.out.println("Ontc - "+oc);
            }
            
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    **/
    
    public void testFedoraOntology() {
        edu.tufts.vue.ontology.Ontology ont = OntManager.getFedoraOntologyWithStyles();
        System.out.println("Ontology"+ont);
    }
}
