/*
 * XMLView.java
 *
 * Created on June 11, 2003, 4:49 PM
 */

package tufts.vue.action;

/**
 *
 * @author  Jay Briedis
 */

import javax.swing.*;
import java.awt.event.*;
import java.io.*;

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import java.io.OutputStreamWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import org.w3c.dom.Document;

import javax.xml.transform.dom.DOMSource;

import javax.xml.transform.stream.StreamResult;

import tufts.vue.*;


public class XMLView extends AbstractAction{ 
    
    final String XML_MAPPING = VUE.CASTOR_XML_MAPPING;
    private static  String fileName = "default.xml";
    private Marshaller marshaller = null;
   
    public XMLView() {
    }
 
    public XMLView(String label) {
        super(label);
        putValue(Action.SHORT_DESCRIPTION,label);
    }       
    
    public void actionPerformed(ActionEvent e)
    {
        System.out.println("Action["+e.getActionCommand()+"] invoked...");
        try {             
            marshaller = getMarshaller();
            marshaller.marshal(tufts.vue.VUE.getActiveMap());
        }catch(Exception ex) {
            System.out.println(ex);
        }
        System.out.println("saving to default.xml complete...");
        
        //create html view from default.xml
        File xmlFile = new File(fileName);
        JTextArea xmlArea = new JTextArea();
        xmlArea.setText("");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        org.w3c.dom.Document xmlDoc = null;
        try{
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            xmlDoc = documentBuilder.parse(fileName);
        }catch(Exception ex){
            System.out.println("problem creating dom object from xml file: "+ex);
        }
        System.out.println("created parsed DOM document");
        try {
            xmlArea.setText(docToString(xmlDoc));
        }
        catch (TransformerException te){
            System.out.println("transformer exception: "+te);
        }
        System.out.println("created a text doc to display in viewer");
        
        JScrollPane pane = new JScrollPane(xmlArea);
        VUE.tabbedPane.addTab("Default.xml", pane);
        VUE.tabbedPane.setSelectedIndex(VUE.tabbedPane.getComponentCount()-1);

        System.out.println("Action["+e.getActionCommand()+"] completed.");
    }
    
    private String docToString(org.w3c.dom.Document xmlDoc) throws TransformerException{
        
        DOMSource domSource=new DOMSource(xmlDoc);
        
        StringWriter stringWriter=new StringWriter();
        StreamResult result = new StreamResult(stringWriter);
        
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        
        transformer.setOutputProperty("indent","yes");
        transformer.transform(domSource, result);        
        return stringWriter.toString();   
    }
 
    private Marshaller getMarshaller()
    {
        if (this.marshaller == null) {
            Mapping mapping = new Mapping();
            try {
                this.marshaller = new Marshaller(new FileWriter(fileName));
                mapping.loadMapping(XML_MAPPING);
                marshaller.setMapping(mapping);
            } catch (Exception e) {
                System.err.println("SaveAction.getMarshaller: " + e);
            }
        }
        return this.marshaller;
    }
}
