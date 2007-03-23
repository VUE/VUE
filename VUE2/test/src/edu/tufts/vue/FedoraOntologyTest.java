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
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.iterator.Filter;


import edu.tufts.vue.ontology.*;
public class FedoraOntologyTest extends TestCase{
    public static final String ONTO_URL = "http://www.fedora.info/definitions/1/0/fedora-relsext-ontology.rdfs";
    
    public void oldTestFedoraOntologyRead() {
        try {
            OntModel m = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM_RDFS_INF,null);
            m.read(ONTO_URL);
            System.out.println("Read ontology:"+m.toString());
            StmtIterator i = m.listStatements();
            while(i.hasNext()) {
                com.hp.hpl.jena.rdf.model.Statement stmt = i.nextStatement();
                com.hp.hpl.jena.rdf.model.Resource res = stmt.getSubject();
                Object obj = stmt.getObject();
                //System.out.println("Stmt - subject:"+res+ " predicate:"+stmt.getPredicate().getLocalName()+" class:"+obj.getClass());
                if(obj instanceof com.hp.hpl.jena.rdf.model.Resource){
                    com.hp.hpl.jena.rdf.model.Resource r = (com.hp.hpl.jena.rdf.model.Resource) obj;
                    System.out.println("Resource - Name: "+r.getLocalName() );
                }
                
                
            }
            printIterator(m.listOntProperties()," Ont Properties");
            
            printIterator(m.listNameSpaces(), "Namespaces");
            printIterator(m.listClasses() , "All Classes");
            printIterator(m.listHierarchyRootClasses(),"Hierarchy Root Classes");
            OntProperty c = m.getOntProperty("http://www.fedora.info/definitions/1/0/fedora-relsext-ontology.rdfs#fedoraRelationship");
            System.out.println("Ont Property"+c.getLocalName()+" Comment"+c.getComment(null));
            printIterator(c.listSubProperties(true),"Sub properties");
            new ClassHierarchy().showHierarchy( System.out, m );
            
            System.out.println("---DONE testFedoraOntologyRead---");
            //    OntClass a1 = m.getOntClass( NS + "A" );
            //   Iterator i1 = a1.listDeclaredProperties();
            //OntClass c = m.getOntClass( ONTO_URL+"#fedoraRelationship");
            //ExtendedIterator ei = m.listIndividuals();
            //printIterator(ei,"Sub Classes");
            //     ResIterator iter = m.listSubjectsWithProperty(RDF.type);
            //  while(iter.hasNext()) {
            //       com.hp.hpl.jena.rdf.model.Resource r = iter.nextResource();
            //      System.out.println("Resource - "+r.getLocalName());
            // }
            
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    
    
    public void oldTestFedoraOntology() {
        edu.tufts.vue.ontology.Ontology ont = OntManager.getFedoraOntologyWithStyles();
        System.out.println("Ontology"+ont);
    }
    
    public void testReadFedoraOntology() {
        try {
            edu.tufts.vue.ontology.Ontology ont = OntManager.readOntologyWithStyle(new URL(ONTO_URL),tufts.vue.VueResources.getURL("fedora.ontology.css"),OntManager.RDFS); 
            System.out.println("Fedora Ontology"+ont);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    public static void printIterator(Iterator i, String header) {
        System.out.println(header);
        for(int c = 0; c < header.length(); c++)
            System.out.print("=");
        System.out.println();
        
        if(i.hasNext()) {
            while (i.hasNext())
                System.out.println( i.next() );
        } else
            System.out.println("<EMPTY>");
        
        System.out.println();
    }
    
    public class ClassHierarchy {
        // Constants
        //////////////////////////////////
        
        // Static variables
        //////////////////////////////////
        
        // Instance variables
        //////////////////////////////////
        
        protected OntModel m_model;
        private Map m_anonIDs = new HashMap();
        private int m_anonCount = 0;
        
        
        
        // Constructors
        //////////////////////////////////
        
        // External signature methods
        //////////////////////////////////
        
        /** Show the sub-class hierarchy encoded by the given model */
        public void showHierarchy( PrintStream out, OntModel m ) {
            // create an iterator over the root classes that are not anonymous class expressions
            Iterator i = m.listHierarchyRootClasses()
            .filterDrop( new Filter() {
                public boolean accept( Object o ) {
                    return ((com.hp.hpl.jena.rdf.model.Resource) o).isAnon();
                }} );
                
                while (i.hasNext()) {
                    showClass( out, (OntClass) i.next(), new ArrayList(), 0 );
                }
        }
        
        
        // Internal implementation methods
        //////////////////////////////////
        
        /** Present a class, then recurse down to the sub-classes.
         *  Use occurs check to prevent getting stuck in a loop
         */
        protected void showClass( PrintStream out, OntClass cls, List occurs, int depth ) {
            renderClassDescription( out, cls, depth );
            out.println();
            
            // recurse to the next level down
            if (cls.canAs( OntClass.class )  &&  !occurs.contains( cls )) {
                for (Iterator i = cls.listSubClasses( true );  i.hasNext(); ) {
                    OntClass sub = (OntClass) i.next();
                    
                    // we push this expression on the occurs list before we recurse
                    occurs.add( cls );
                    showClass( out, sub, occurs, depth + 1 );
                    occurs.remove( cls );
                }
            }
        }
        
        
        /**
         * <p>Render a description of the given class to the given output stream.</p>
         * @param out A print stream to write to
         * @param c The class to render
         */
        public void renderClassDescription( PrintStream out, OntClass c, int depth ) {
            indent( out, depth );
            
            if (c.isRestriction()) {
                renderRestriction( out, (Restriction) c.as( Restriction.class ) );
            } else {
                if (!c.isAnon()) {
                    out.print( "Class " );
                    renderURI( out, c.getModel(), c.getURI() );
                    out.print( ' ' );
                } else {
                    renderAnonymous( out, c, "class" );
                }
            }
        }
        
        /**
         * <p>Handle the case of rendering a restriction.</p>
         * @param out The print stream to write to
         * @param r The restriction to render
         */
        protected void renderRestriction( PrintStream out, Restriction r ) {
            if (!r.isAnon()) {
                out.print( "Restriction " );
                renderURI( out, r.getModel(), r.getURI() );
            } else {
                renderAnonymous( out, r, "restriction" );
            }
            
            out.print( " on property " );
            renderURI( out, r.getModel(), r.getOnProperty().getURI() );
        }
        
        /** Render a URI */
        protected void renderURI( PrintStream out, PrefixMapping prefixes, String uri ) {
            out.print( prefixes.shortForm( uri ) );
        }
        
        /** Render an anonymous class or restriction */
        protected void renderAnonymous( PrintStream out, com.hp.hpl.jena.rdf.model.Resource anon, String name ) {
            String anonID = (String) m_anonIDs.get( anon.getId() );
            if (anonID == null) {
                anonID = "a-" + m_anonCount++;
                m_anonIDs.put( anon.getId(), anonID );
            }
            
            out.print( "Anonymous ");
            out.print( name );
            out.print( " with ID " );
            out.print( anonID );
        }
        
        /** Generate the indentation */
        protected void indent( PrintStream out, int depth ) {
            for (int i = 0;  i < depth; i++) {
                out.print( "  " );
            }
        }
        
        
        //==============================================================================
        // Inner class definitions
        //==============================================================================
        
    }
    
    
    
    
}
