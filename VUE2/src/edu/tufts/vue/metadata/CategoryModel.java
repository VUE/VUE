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

import  edu.tufts.vue.ontology.*;

import java.util.*;
import java.net.*;
import java.io.*;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.ontology.*;

public class CategoryModel extends ArrayList<edu.tufts.vue.ontology.Ontology>{
    public static final String CUSTOM_METADATA_FILE = tufts.vue.VueUtil.getDefaultUserFolder()+File.separator+tufts.vue.VueResources.getString("metadata.custom.file");
    public static final String ONT_SEPARATOR = "#";
    
    private boolean ontologiesLoaded = false;
    
    int ontTypesCount = 0;
    private static Map<URL,edu.tufts.vue.ontology.Ontology> ontCache = new HashMap<URL,edu.tufts.vue.ontology.Ontology>();
    private edu.tufts.vue.ontology.Ontology customOntology;
    public CategoryModel() {
        System.out.println("Creating Category Model");
        Runnable ontologyLoader = new Runnable()
        {
          public void run()
          {
            loadDefaultVUEOntologies();
            loadCustomOntology(false);
            ontologiesLoaded = true;
          }
        };
        Thread loader = new Thread(ontologyLoader);
        loader.start();
    }
    
    public boolean isLoaded()
    {
        return ontologiesLoaded;
    }
    
    public void loadDefaultVUEOntologies() {
        String[] ontologyUrls = tufts.vue.VueResources.getStringArray("metadata.load.files");
        for(int i =0;i<ontologyUrls.length;i++) {
            try {
                loadOntology(tufts.vue.VueResources.getBundle().getClass().getResource(ontologyUrls[i]));
            } catch(Throwable t) {
                tufts.vue.VUE.Log.error("Problem loading metadata: "+ontologyUrls[i]+" Error:"+t.getMessage());
            }
        }
    }
    
    public void  loadOntology(URL url) {
        if(ontCache.get(url) == null) {
            edu.tufts.vue.ontology.Ontology ontology = new RDFSOntology(url);
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
    
    public void addCustomCategory(String name) {
        OntType ontType = new OntType();
        ontType.setLabel(name);
        ontType.setBase(customOntology.getBase());
        ontType.setId(customOntology.getBase()+name);
        customOntology.getOntTypes().add(ontType);
    }
    
    public void removeCustomCategory(OntType ontType) {
        customOntology.getOntTypes().remove(ontType);
    }
    
    public edu.tufts.vue.ontology.Ontology getCustomOntology() {
        return customOntology;
    }
    
    synchronized public void saveCustomOntology() {
        try {
            OntModel m = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM,null);
            for(OntType ontType: customOntology.getOntTypes()) {
                m.createClass(customOntology.getBase()+ONT_SEPARATOR+ontType.getLabel());
            }
            m.write(new BufferedWriter(new FileWriter(CUSTOM_METADATA_FILE)));
        }catch(Throwable t) {
            tufts.vue.VUE.Log.error("Problem saving custom metadata - Error:"+t.getMessage());
            t.printStackTrace();
        }
    }
    private void loadCustomOntology(boolean flag)  {
        try {
            if(customOntology == null && !flag) {
                customOntology = new RDFSOntology(new File(CUSTOM_METADATA_FILE).toURI().toURL());
                ontTypesCount += customOntology.getOntTypes().size();
                add(customOntology);
            } else if(flag) {
                remove(customOntology);
                customOntology = new RDFSOntology(new File(CUSTOM_METADATA_FILE).toURI().toURL());
                ontTypesCount += customOntology.getOntTypes().size();
                add(customOntology);
            }
        } catch(Throwable t) {
            customOntology = new RDFSOntology();
            customOntology.setBase(CUSTOM_METADATA_FILE);
            tufts.vue.VUE.Log.error("Problem loading custom metadata, creating new one - Error:"+t.getMessage());
           
        }
    }
}
