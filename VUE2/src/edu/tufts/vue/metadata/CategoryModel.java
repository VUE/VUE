/*
 * CategoryModel.java
 *
 * Created on August 14, 2007, 4:39 PM
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
package edu.tufts.vue.metadata;


import java.util.*;
import  edu.tufts.vue.ontology.*;
import java.net.*;
public class CategoryModel extends ArrayList<Ontology>{
    int ontTypesCount = 0;
    private static Map<URL,Ontology> ontCache = new HashMap<URL,Ontology>();
    public CategoryModel() {
        loadDefaultVUEOntologies();
    }
    
    public void loadDefaultVUEOntologies() {
        String[] ontologyUrls = tufts.vue.VueResources.getStringArray("metadata.load.files");
        for(int i =0;i<ontologyUrls.length;i++) {
            try {
                loadOntology(tufts.vue.VueResources.getBundle().getClass().getResource(ontologyUrls[i]));
            } catch(Throwable t) {
                tufts.vue.VUE.Log.error("Problem loading metadata: "+ontologyUrls[i]+" Errror:"+t.getMessage());
            }
        }
    }
    
    public void  loadOntology(URL url) {
        if(ontCache.get(url) == null) {
            Ontology ontology = new RDFSOntology(url);
            ontTypesCount += ontology.getOntTypes().size();
            add(ontology);
            ontCache.put(url,ontology);
        } else {
            add(ontCache.get(url));
        }
    }
    
    public int getOntTypesSize() {
        return ontTypesCount;
    }
    
}
