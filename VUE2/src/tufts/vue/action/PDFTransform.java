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

/*
 * PDFTransform.java
 *
 * Created on June 10, 2003, 1:02 PM
 *
 * Class that saves concept map as PDF file
 * Saves selected concept map as XML, then converts it to named PDF
 * This version is performed all in VUE, with no external command line calls
 */

package tufts.vue.action;

//classes from castor jar files
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

import javax.swing.AbstractAction;
import javax.swing.Action;

import java.io.File;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import tufts.vue.*;

/**
 *
 * @author Jay Briedis
 */
/*  This class has been replaced by the itext export code found in 
 * PresentationNotes.java, there were problems including the xml/fo/xsl
 * were not actually present in the JAR, and I think in the end
 * we get abetter map from the itext code.
 * 
 * -Mike
 */
public class PDFTransform extends AbstractAction {
    
    private static  String pdfFileName = "default.pdf";
    private static  String xmlFileName = "default.xml";
    private static  String foFileName = "default.fo";
    private static  String xslFileName = "viewPDF.xsl";
    
    public PDFTransform() {
    }
    
    public PDFTransform(String label) {
        super(label);
        putValue(Action.SHORT_DESCRIPTION,label);
    }
    
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        System.out.println("Performing PDF Conversion:" + actionEvent.getActionCommand());
        
        //ActionUtil.marshallMap(new File(xmlFileName));
        
        File pdfFile = ActionUtil.selectFile("Save As PDF", "pdf");
        if(!pdfFile.equals(null)) convert(pdfFile);
    }
    
    public void convert(File pdfFile){
        
        ActionUtil.marshallMap(new File(xmlFileName));
        //create new instance of transformer factory
        TransformerFactory factory =
            TransformerFactory.newInstance();
        
        //create File objects from file names
        File xslFile = new File(xslFileName);
        File xmlFile = new File(xmlFileName);
        File foFile = new File(foFileName);
        
        
        //create a transformer to hold the converted xml object
        Transformer trans = null;
        try{
            trans = 
            factory.newTransformer(new StreamSource(xslFile));
        }catch(TransformerConfigurationException tce){
            System.out.println("problem creating new transformer: " + tce);
        }
        
        //create an output stream to the default.fo file
        FileOutputStream foOut = null; 
        try{
            foOut = new FileOutputStream(foFile);
        }catch(FileNotFoundException fnfe){
            System.out.println("can't find fo file: " + fnfe);
        }
        
        //transform xml file to fo file to prep for pdf conversion
        try{
            trans.transform(new StreamSource(xmlFile),
            new StreamResult(foOut));
        }catch(TransformerException te){
            System.out.println("problem performing traslation: " + te);
        }
        
        //closing file output stream to default.fo file (no sharing violation)
        try{
            foOut.close();
        }catch(IOException ioe){
            System.out.println("io problems closing fo file: " + ioe);
        }
        
        //get fop version used below as param for renderer
        String version = org.apache.fop.apps.Version.getVersion();
        
        
        //set system prop driver to the sax parser
        System.setProperty("org.xml.sax.driver", 
            "org.apache.xerces.parsers.SAXParser");
        
        //create an xml reader
        try{
            XMLReaderFactory.createXMLReader(); 
        }catch(SAXException se){
            System.out.println("problems creating XML reader: " + se);
        }
        
        //create output stream for pdf file
        FileOutputStream pdfOut =null;
        try{
            pdfOut = new FileOutputStream(pdfFile);
        }catch(FileNotFoundException fnfe){
            System.out.println("can't find pdf file: " + fnfe);
        }
        
        //initalize new driver for rendering with param (fo file source, pdf file)
        org.apache.fop.apps.Driver driver = 
            new org.apache.fop.apps.Driver(
                new org.xml.sax.InputSource(
                foFileName), pdfOut);
        
        driver.setRenderer(1);
        
        //running driver to render fo file to pdf format
        try{
            driver.run();
        }catch(IOException ioe){
            System.out.println("io problems running the driver: " + ioe);
        }catch(Exception fe){
            System.out.println("fop problems running the driver: " + fe);
        }
        
        //closing file output stream to selected pdf file (no sharing violation)
        try{
            pdfOut.close();
        }catch(IOException ioe){
            System.out.println("io problems closing pdf file: " + ioe);
        }
    }
}
