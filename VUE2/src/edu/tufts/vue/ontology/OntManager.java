/*
 * OntManager.java
 *
 * Created on March 12, 2007, 10:59 AM
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
package edu.tufts.vue.ontology;

import java.util.*;
import edu.tufts.vue.style.*;

public class OntManager {
    /** Creates a new instance of OntManager */
    public OntManager() {
    }
    
    public static Ontology getFedoraOntologyWithStyles() {
        String[] ontTerms = {"isPartOf","hasPart","isDerivationOf","hasDerivation","isDependentOf","hasDependent","isDescriptionOf","hasDescription","hasEquivalent"};
        int[] ontWeights = {1,1,2,2,3,3,4,4,5};
        String base ="info:fedora/fedora-system:def/relations-external";
        List<OntType> types = new ArrayList<OntType>();
        
        StyleReader.readStyles("fedora.ontology.css");
        
        Ontology ont = new Ontology();
        ont.setBase(base);
        for(int i = 0;i<ontTerms.length;i++) {
            OntType type = new OntType();
            type.setName(ontTerms[i]);
            type.setBase(base);
            //Style style = new LinkStyle(base+":"+ontTerms[i]);
            //style.setAttribute("weight",""+ontWeights[i]);
            Style style = StyleMap.getStyle("link."+base+":"+ontTerms[i]);
            if(style==null)
            {
               System.out.println("OntManager: couldn't load style for " + base+":"+ontTerms[i]);
               style = new LinkStyle(base+":"+ontTerms[i]);
               style.setAttribute("weight",""+ontWeights[i]);
            }
            type.setStyle(style);
            types.add(type);
        }
        ont.setOntTypes(types);
        return ont;
    }
}
