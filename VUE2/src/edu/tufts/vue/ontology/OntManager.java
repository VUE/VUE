/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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
package edu.tufts.vue.ontology;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.xml.sax.InputSource;

import tufts.vue.VueResources;

public class OntManager
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(OntManager.class);
    
    public static final String ONT_NOT_FOUND = VueResources.getString("ontology.ontologynotfound");
    public static final String ONT_FILE = tufts.vue.VueResources.getString("ontology.save");
    public static final int RDFS = 0;
    public static final int OWL = 1;
    /** Creates a new instance of OntManager */
    List<Ontology> ontList = Collections.synchronizedList(new ArrayList<Ontology>());
    static OntManager ontManager;
    public OntManager() {
    }
    
    public void populateStyledOntology(URL ontUrl,URL cssUrl,
                                       Ontology ontology)
    {
        /*if(ontology != null && !ontList.contains(ontology)) {
            ontList.add(ontology);
        }*/
        
        org.osid.shared.Type type = null;
        if(ontology instanceof RDFSOntology)
            type = OntologyType.RDFS_TYPE;
        else if(ontology instanceof OWLLOntology)
            type = OntologyType.OWL_TYPE;
        
        if(type == null)
        {
            //ontList.remove(ontology);
            return;
        }
        if(type.isEqual(OntologyType.RDFS_TYPE)) {
            RDFSOntology.populateExistingRDFSOntology(ontUrl,cssUrl,(RDFSOntology)ontology);
        } else if(type.isEqual(OntologyType.OWL_TYPE)) {
            OWLLOntology.populateExistingOWLOntology(ontUrl,cssUrl,(OWLLOntology)ontology);
        }
        else
        {
            //ontList.remove(ontology);
            return;
        }
    }
            
 
    public void addOntology(URL ontUrl,Ontology ontology)
    {
        if(ontology != null && !ontList.contains(ontology)) {
            ontList.add(ontology);
        }
    }
    
    public void populateOntology(URL ontUrl,Ontology ontology)   
    {
        /*if(ontology != null && !ontList.contains(ontology)) {
            ontList.add(ontology);
        }*/
        org.osid.shared.Type type = null;
        if(ontology instanceof RDFSOntology)
            type = OntologyType.RDFS_TYPE;
        else if(ontology instanceof OWLLOntology)
            type = OntologyType.OWL_TYPE;
        
        if(type == null)
        {
            //ontList.remove(ontology);
            return;
        }
        if(type.isEqual(OntologyType.RDFS_TYPE)) {
            RDFSOntology.populateExistingRDFSOntology(ontUrl,(RDFSOntology)ontology);
        } else if(type.isEqual(OntologyType.OWL_TYPE)) {
            OWLLOntology.populateExistingOWLOntology(ontUrl,(OWLLOntology)ontology);
        }
        else
        {
           // ontList.remove(ontology);
            return;
        }
    }
    
    public Ontology readOntologyWithStyle(URL ontUrl,URL cssUrl,org.osid.shared.Type type) {
        Ontology ont = null;
        if(type.isEqual(OntologyType.RDFS_TYPE)) {
            ont =  new RDFSOntology(ontUrl,cssUrl);
        } else if(type.isEqual(OntologyType.OWL_TYPE)) {
            ont = new OWLLOntology(ontUrl,cssUrl);
        }
        if(ont != null && !ontList.contains(ont)) {
            ontList.add(ont);
        }
        return ont;
    }
    
    public Ontology readOntology(URL ontUrl,org.osid.shared.Type type) {
        Ontology ont =null;
        if(type.isEqual(OntologyType.RDFS_TYPE)) {
            ont = new RDFSOntology(ontUrl);
        } else if(type.isEqual(OntologyType.OWL_TYPE)) {
            ont = new OWLLOntology(ontUrl);
        }
        if(ont != null && !ontList.contains(ont)) {
            ontList.add(ont);
        }
        return ont;
    }
    
    public void applyStyleToOntology(URL ontUrl,URL cssUrl) throws Throwable  {
        Ontology ont = getOntology(ontUrl);
        if(ont == null) {
            throw new Exception(ONT_NOT_FOUND);
        }
        ont.applyStyle(cssUrl);
        
    }
    public List<Ontology> getOntList() {
        return ontList;
    }
    
    public void setOntList(List<Ontology> list) {
        this.ontList = list;
    }
    public Ontology getOntology(URL ontUrl) {
        for(Ontology ont: ontList) {
            if(ont.getBase().equalsIgnoreCase(ontUrl.toString())){
                return ont;
            }
        }
        return null;
    }
    
    public void removeOntology(URL ontUrl) {
        for(Ontology ont: ontList) {
            if(ont.getBase().equalsIgnoreCase(ontUrl.toString())){
                ontList.remove(ont);
                return;
            }
        }
    }
    
    public Ontology applyStyle(URL ontUrl, URL cssUrl) {
        return null;
    }
    
    public void save() {
        if(ontManager != null) {
            try {
                File file = new File(tufts.vue.VueUtil.getDefaultUserFolder()+File.separator+ONT_FILE);
                Mapping mapping = new Mapping();
                FileWriter writer = new FileWriter(file);
                Marshaller marshaller = new Marshaller(writer);
                marshaller.setMapping(tufts.vue.action.ActionUtil.getDefaultMapping());
                System.out.println("Marshalling OntManager. Class = "+this.getClass());
                marshaller.marshal(this);
                writer.flush();
                writer.close();
            } catch(Exception ex) {
                Log.error("OntManager.save: "+ex);
            }
        } else
            Log.error("OntManager.save: OntManager not initialized");
        
    }
    
    public void load() {
        try {
            Unmarshaller unmarshaller = tufts.vue.action.ActionUtil.getDefaultUnmarshaller();
            File file = new File(tufts.vue.VueUtil.getDefaultUserFolder()+File.separator+ONT_FILE);
            FileReader reader = new FileReader(file);
            ontManager = (OntManager) unmarshaller.unmarshal(new InputSource(reader));
            loadSavedOntTypes() ;
            reader.close();
        } catch(Exception ex) {
            Log.error("OntManager.save: "+ex);
        }
    }
    public  static OntManager getOntManager() {
        if(ontManager == null) {
            ontManager = new OntManager();
        }
        return ontManager;
    }
    
    private void loadSavedOntTypes() {
        for(Ontology o: ontList) {
            if(o.getStyle() == null)
                o.readAllSupportedOntTypes();
            else
                o.readAllSupportedOntTypesWithCss();
        }
    }
    
}
