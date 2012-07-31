/*
* Copyright 2003-2010 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

/**
 *
 * @author akumar03
 */
package edu.tufts.vue.metadata;

import java.util.*;
import java.net.*;
import java.io.*;

import edu.tufts.vue.ontology.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.ontology.*;

import tufts.Util;
import tufts.vue.DEBUG;

public class CategoryModel extends ArrayList<edu.tufts.vue.ontology.Ontology>
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(CategoryModel.class);
    
    public static final String CUSTOM_METADATA_FILE = tufts.vue.VueUtil.getDefaultUserFolder()+File.separator+tufts.vue.VueResources.getString("metadata.custom.file");
    public static final String ONT_SEPARATOR = "#";
    
    // note: the next two constants may not be in sync with the actual
    // properties file -- see code in loader thread below
    // which attempts to detect the problem (and prints a warning)
    public static final int DUBLIN_CORE = 1;
    public static final int VRA = 2;
    
    private boolean ontologiesLoaded = false;
    
    private String[] defaultOntologyUrls;
    
    int ontTypesCount = 0; // is never updated on remove, tho access API method appears to have never been used
    private static final Map<URL,edu.tufts.vue.ontology.Ontology> ontCache = new HashMap<URL,edu.tufts.vue.ontology.Ontology>(); // is never cleared
    
    private edu.tufts.vue.ontology.Ontology customOntology;
    public CategoryModel() {
        Log.debug("Creating Category Model");
        Runnable ontologyLoader = new Runnable()
        {
          public void run()
          {
            loadDefaultVUEOntologies();
            loadCustomOntology(false);
            
            /*if(defaultOntologyUrls[VRA].toString().indexOf("/edu/tufts/vue/metadata/vra_core_3.rdf") == -1)
            {
                System.out.println("warning: may be removing incorrect VRA url " + defaultOntologyUrls[VRA] );
            }
            removeDefaultOntology(VRA);*/
            
            edu.tufts.vue.preferences.implementations.MetadataSchemaPreference
                    msp =
                    edu.tufts.vue.preferences.implementations.MetadataSchemaPreference.getInstance();
            
            if(!msp.getDublinCoreValue())
                removeDefaultOntology(DUBLIN_CORE);
            
            if(!msp.getVRAValue())
                removeDefaultOntology(VRA);
            
            ontologiesLoaded = true;
          }
        };
        Thread loader = new Thread(ontologyLoader);
        loader.start();
    }

    // Actually, the way to do this is to hash in the Ontology, just by label/id, and we can lookup
    // the Ontology by OntType.base (hashed also if we like).  Leaving this in for debug.
    @Override public boolean add(edu.tufts.vue.ontology.Ontology o) {
        if (DEBUG.Enabled) Log.debug("add[" + size() + "] " + o.getBase() + " " + Util.tags(o.getLabel()));
        scanOntTypes(o);
        return super.add(o);
    }
    @Override public edu.tufts.vue.ontology.Ontology set(int index, edu.tufts.vue.ontology.Ontology o) {
        if (DEBUG.Enabled) Log.debug("set[" + index + "] " + o.getBase() + " " + Util.tags(o.getLabel()));
        // impl is messy: will add empty ontologies with numbered names to replace ones removed
        // if (o.getBase() == null) Util.printStackTrace(); 
        scanOntTypes(o);
        return super.set(index, o);
    }
    @Override public boolean remove(Object _o) {
        edu.tufts.vue.ontology.Ontology o = (edu.tufts.vue.ontology.Ontology) _o;
        if (DEBUG.Enabled) Log.debug("remove " + o.getBase() + " " + Util.tags(o.getLabel()));
        // todo: flush cache
        return super.remove(o);
    }
    private void scanOntTypes(edu.tufts.vue.ontology.Ontology o) {
        // ontTypesCount += o.getOntTypes().size(); // won't need: can ask ontTypeByKey.size()
        for (OntType ontType : o.getOntTypes()) {
            if (DEBUG.PAIN || DEBUG.DATA) Log.info(ontType);
            // OntType old = ontTypeByKey.put(ontType.getAsKey(), ontType);
            // if (old != null) Log.warn("duplicate ontType key: " + old.getAsKey());
            // else Log.info("cached: " + ontType);
        }
    }
        
    
    public boolean isLoaded() {
        return ontologiesLoaded;
    }
    
    public void loadDefaultVUEOntologies() {
        defaultOntologyUrls = tufts.vue.VueResources.getStringArray("metadata.load.files");
        for(int i =0;i<defaultOntologyUrls.length;i++) {
            try {
                loadOntology(/*tufts.vue.VueResources.getBundle().getClass()*/tufts.vue.VueResources.class.getResource(defaultOntologyUrls[i]));
            } catch(Throwable t) {
                Log.error("Problem loading metadata: "+defaultOntologyUrls[i]+" Error:"+t.getMessage());
            }
        }
    }
    
    public void loadDefaultVUEOntology(int location) {
        defaultOntologyUrls = tufts.vue.VueResources.getStringArray("metadata.load.files");
        //for(int i =0;i<defaultOntologyUrls.length;i++) {
            try {
                loadOntology(location,tufts.vue.VueResources.class.getResource(defaultOntologyUrls[location]));
            } catch(Throwable t) {
                Log.error("Problem loading metadata: "+defaultOntologyUrls[location]+" Error:"+t.getMessage());
            }
        //}
    }
  
    public void loadOntology(int index, URL url) {
        if(ontCache.get(url) == null) {
            edu.tufts.vue.ontology.Ontology ontology = new RDFSOntology(url);
            ontTypesCount += ontology.getOntTypes().size();
            set(index, ontology);
            ontCache.put(url, ontology);
        } else {
            set(index, ontCache.get(url));
        }
    }
    
    public void loadOntology(URL url) {
        if(ontCache.get(url) == null) {
            edu.tufts.vue.ontology.Ontology ontology = new RDFSOntology(url);
            ontTypesCount += ontology.getOntTypes().size();
            add(ontology);
            ontCache.put(url,ontology);
        } else {
            // Weird: if already loaded, we add it again to the end?  Was this a cut/paste fail from above?
            add(ontCache.get(url));
        }
    }
    
    public int getOntTypesSize() {
        return ontTypesCount;
    }
    
    public OntType addCustomCategory(String name) {
        final OntType ontType = new OntType();
        ontType.setLabel(name);
        ontType.setBase(customOntology.getBase());
        ontType.setId(customOntology.getBase()+name);
        customOntology.getOntTypes().add(ontType);
        return ontType;
    }
    
    public OntType addCustomCategory(String name,OntType value) {
        OntType ontType = new OntType();
        ontType.setLabel(name);
        ontType.setBase(customOntology.getBase());
        ontType.setId(customOntology.getBase()+name);
        if (customOntology.getOntTypes().indexOf(value) > -1)
            customOntology.getOntTypes().set(customOntology.getOntTypes().indexOf(value),ontType);  
        //customOntology.getOntTypes().add(ontType);
        return ontType;
    }
    
    public void removeCustomCategory(OntType ontType) {
        customOntology.getOntTypes().remove(ontType);
    }
    
    public void removeDefaultOntology(int location)
    {
        if(location == DUBLIN_CORE)
        {
            //remove(defaultOntologyUrls[DUBLIN_CORE]);
            edu.tufts.vue.ontology.Ontology dcOntology = ontCache.get(tufts.vue.VueResources.getBundle().getClass().getResource(defaultOntologyUrls[DUBLIN_CORE]));
            RDFSOntology emptyOntology = new RDFSOntology();
            set(DUBLIN_CORE,emptyOntology);
            //set(dcOntologyLocation,emptyOntology);
            //remove(dcOntology);
        }
        
        if(location == VRA)
        {
            edu.tufts.vue.ontology.Ontology vraOntology = ontCache.get(tufts.vue.VueResources.getBundle().getClass().getResource(defaultOntologyUrls[VRA]));
            RDFSOntology emptyOntology = new RDFSOntology();
            set(VRA,emptyOntology);
            // VRALocation...
            remove(vraOntology);
        }
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
            RDFWriter writer = m.getWriter();
            writer.setProperty("allowBadURIs","true");
            writer.write(m,new BufferedWriter(new FileWriter(CUSTOM_METADATA_FILE)),customOntology.getBase());
        }catch(Throwable t) {
            Log.error("Problem saving custom metadata - Error:"+t.getMessage());
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
            File test = new File(CUSTOM_METADATA_FILE);
            URL url = null;
            URI uri = null;
            try
            { 
              uri = test.toURI();
              //System.out.print("Category Model load -- uri " + uri);
              url = uri.toURL();
            }
            catch(Exception e)
            {
                System.out.println("Category Model load -- inner exception: " + e);
            }
            customOntology = new RDFSOntology();
            customOntology.setBase(url.toString());
            add(customOntology);
            if (t.getCause() instanceof FileNotFoundException) {
                Log.info("no custom meta-data: " + t.getCause().getMessage());
            } else {
                Log.info("Didn't load custom metadata, probably just hasn't been created yet - users can enter custom "
                         + "metadata through the info window GUI to create file in user directory, if needed. " 
                         + "Additional details on specific condition follow: "+t.getMessage());
            }
        }
    }
}
