/*
 * Dataset.java
 *
 * Created on July 15, 2008, 5:40 PM
 *
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
 *
 */
package edu.tufts.vue.dataset;


import java.util.*;
import java.io.*;
import tufts.vue.*;
import java.net.*;
import edu.tufts.vue.ontology.OntManager;
import edu.tufts.vue.ontology.OWLLOntology;
import edu.tufts.vue.ontology.OntType;
import edu.tufts.vue.ontology.ui.OntologyBrowser;
import edu.tufts.vue.ontology.ui.TypeList;
import edu.tufts.vue.ontology.ui.OntologyChooser;


import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.util.iterator.*;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.iterator.Filter;


public class Dataset {
    
    public static final int MAX_SIZE = tufts.vue.VueResources.getInt("dataset.maxSize");
    public static final int MAX_LABEL_SIZE = 10000;
    String fileName;
    String label;
    ArrayList<String> heading;
    ArrayList<ArrayList<String>> rowList;
    
    String baseClass;
    /** Creates a new instance of Dataset */
    
    Layout layout = new ListRandomLayout(); // this is the default if no layout is set
    public Dataset() {
    }
    
    public void setLayout(Layout layout) {
        this.layout = layout;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public void setHeading(ArrayList<String> heading) {
        this.heading = heading;
    }
    public ArrayList<String> getHeading() {
        return heading;
    }
    
    public String getBaseClass() {
        return baseClass;
    }
    
    public ArrayList<ArrayList<String>> getRowList() {
        return rowList;
    }
    
    public LWMap createMap() throws Exception{
        return layout.createMap(this,getMapName(fileName));
    }
    
    public void createOntology()  {
        System.out.println("Creating owl ontology for: "+fileName);
        String base = VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separator+"test.owl";
        String ontClassLabel = getHeading().get(0);
        baseClass = base+"#"+ontClassLabel;
        try {
            OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_LITE_MEM, null);
            m.createClass(baseClass);
            OWLLOntology ontology = new OWLLOntology();
            
            //ontology.setBase((new File(fileName)).toURI().toURL().toString());
            ontology.equals(base);
            // OntType ontType = new OntType();
            // ontType.setId(ontClassLabel);
            // ontType.setLabel(ontClassLabel);
            // ontology.getOntTypes().add(ontType);
            
            // ontology.applyDefaultStyle();
            //    ontology.write(new FileWriter(base));
            m.write(new FileWriter(base));
            edu.tufts.vue.ontology.OntManager.getOntManager().load();
            TypeList list = new TypeList();
            
            tufts.vue.gui.Widget w = null;
            URL ontURL = new File(base).toURI().toURL();
            w = OntologyBrowser.getBrowser().addTypeList(list, ontology.getLabel(),ontURL);
            //   list.loadOntology(new File(base).toURI().toURL(),ontology.getStyle(),OntologyChooser.getOntType(new URL(ontology.getBase())),OntologyBrowser.getBrowser(),w);
            list.loadOntology(ontURL ,null,OntologyChooser.getOntType(ontURL),OntologyBrowser.getBrowser(),w);
            
            //OntManager.getOntManager().addOntology((new File(fileName)).toURI().toURL(),ontology);
        } catch(Exception ex) {
            System.out.println("Dataset.createOntology :"+ex);
            ex.printStackTrace();
        }
        
        
    }
    
    private  String getMapName(String fileName) {
        String mapName = fileName.substring(fileName.lastIndexOf(File.separator)+1,fileName.length());
        if(mapName.lastIndexOf(".")>0)
            mapName = mapName.substring(0,mapName.lastIndexOf("."));
        if(mapName.length() == 0)
            mapName = "Text Import";
        return mapName;
    }
}
