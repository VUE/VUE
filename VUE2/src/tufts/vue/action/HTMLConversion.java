/*
 * HTMLConversion.java
 *
 * Created on June 11, 2003, 10:46 AM
 */

package tufts.vue.action;

/**
 *
 * @author  Jay Briedis
 */
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import java.io.FileInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import java.awt.event.ActionEvent;

import tufts.vue.*;

public class HTMLConversion extends AbstractAction
{   
    
    private static  String htmlFileName = "default.html";
    private static  String xmlFileName = "default.xml";
    private static  String xslFileName = "viewHTML.xsl";
    final String XML_MAPPING = VUE.CASTOR_XML_MAPPING;
    private Marshaller marshaller = null;
    
    public HTMLConversion() {
    }
    
    public HTMLConversion(String label) {
        super(label);
        putValue(Action.SHORT_DESCRIPTION,label);
    }
    
    public void actionPerformed(ActionEvent ae) {
        System.out.println("Performing HTML Conversion:" + ae.getActionCommand());
        
        try {
            marshaller = getMarshaller();
            marshaller.marshal(tufts.vue.VUE.getActiveMap());
            marshaller = null;
        }catch(Exception ex) {
            System.out.println(ex);
        }
        System.out.println("done with converting to xml...");
        selectHTMLFile();
    }
    
    public void convert(){
        System.out.println("in convert..................");
        TransformerFactory tfactory = TransformerFactory.newInstance();
        
        try
        {
            InputStream xslInput = new FileInputStream( xslFileName );
            StreamSource xslSource = new StreamSource( xslInput );
                
            StreamSource xmlSource = new StreamSource( xmlFileName );
            
            Templates templates = tfactory.newTemplates( xslSource );
        
            File result = new File(htmlFileName);
            
            StreamResult out = new StreamResult(result);
            
            Transformer transformer = templates.newTransformer();
    
            transformer.transform( xmlSource, out );
        }
        catch ( IOException ex )
        {
            System.out.println(ex.getMessage());
        }
        catch ( TransformerException ex )
        {
            System.out.println( ex.getMessage() );
        }
        
        System.out.println("finished converting xml to html.");
    }
    
    private void selectHTMLFile() {
        String label = VUE.getActiveMap().getLabel();
        try {
            
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save as HTML");
            int option = chooser.showDialog(tufts.vue.VUE.frame, "Save");
            if (option == JFileChooser.APPROVE_OPTION) {
                htmlFileName = chooser.getSelectedFile().getAbsolutePath();
                if(!htmlFileName.endsWith(".html")) htmlFileName += ".html";
                System.out.println("saving to file: "+htmlFileName);
                convert();
            }     
        }catch(Exception ex) {System.out.println(ex);}
    }
    
    private Marshaller getMarshaller() {
        if (this.marshaller == null) {
            
            Mapping mapping = new Mapping();
            try {
                this.marshaller = new Marshaller(new FileWriter(xmlFileName));
                mapping.loadMapping(XML_MAPPING);
                marshaller.setMapping(mapping);
            } catch (Exception e) {
                System.err.println("HTMLConversion.getMarshaller: " + e);
            }
        }
        return this.marshaller;
    }
}