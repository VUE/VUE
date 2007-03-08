
/*
 *
 * * <p><b>License and Copyright: </b>The contents of this file are subject to the
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
 *
 */

package edu.tufts.vue.ontology;


import com.hp.hpl.jena.rdf.model.*;

/*
 * OntologyReader.java
 *
 * Created on March 6, 2007, 7:37 PM
 *
 * @author dhelle01
 */
public class OntologyReader {
    
    /** Creates a new instance of OntologyReader */
    public OntologyReader() {
    }
    
    public static void main(String[] args)
    {
        Model model = ModelFactory.createDefaultModel();
        RDFReader reader = model.getReader();
        reader.read(model,"file:///Users/dhelle01/test.rdf");
        System.out.println(model.size());
        NodeIterator i = model.listObjects();
        while(i.hasNext())
        {
           System.out.println(i.next());       
        }
        StmtIterator si = model.listStatements();
        while(si.hasNext())
        {
           Statement stmt = (Statement)si.next();
           System.out.println(stmt);
           System.out.println(stmt.getSubject());
           System.out.println(stmt.getResource());
           // stmt.getProperty(RDFS.class
           // or maybe r=model.getResource(), r.getProperty()
        }
    }
}
