/*
 * HTMLConversion.java
 *
 * Created on June 5, 2003, 2:39 PM
 */

package tufts.vue.action;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import java.io.OutputStreamWriter;

import tufts.vue.*;

/**
 *
 * @author  Jay Briedis
 */

/**a class which constructs an HTML file based on the data within the concept map*/
public class HTMLConversion extends AbstractAction {
    
    private static  String htmlFileName = "default.html";
    private static  String fileName = "C:\\vueproject\\VUEDevelopment\\src\\default.xml";
    final String XML_MAPPING = VUE.CASTOR_XML_MAPPING;
    private Marshaller marshaller = null;
    
    /** Creates a new instance of HTMLConversion */
    public HTMLConversion() {
    }
    
    /**A constructor */
    public HTMLConversion(String label) {
        super(label);
        putValue(Action.SHORT_DESCRIPTION,label);
    }
    
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        System.out.println("Performing HTML Conversion:" + actionEvent.getActionCommand());
        try {
            marshaller = getMarshaller();
            marshaller.marshal(tufts.vue.VUE.getActiveMap());
            marshaller = null;
        }catch(Exception ex) {
            System.out.println(ex);
        }
        
        
        //convert default.xml to html
        selectHTMLFile();
        String state = "xalan"
            +" -IN default.xml"
            +" -xsl viewHtml.xsl"
            +" -OUT \"" + htmlFileName + "\"";
        try{
            Runtime r = Runtime.getRuntime();
            
            Process p = r.exec( "cmd /c" + state);
            p = null;
            
        } catch (IOException ioe){
            System.err.println("HTMLConversion.actionPerformed error: " +ioe);
        }
        System.out.println("Action["+actionEvent.getActionCommand()+"] performed!");
    }
    
    private Marshaller getMarshaller() {
        if (this.marshaller == null) {
            
            Mapping mapping = new Mapping();
            try {
                this.marshaller = new Marshaller(new FileWriter(fileName));
                mapping.loadMapping(XML_MAPPING);
                marshaller.setMapping(mapping);
            } catch (Exception e) {
                System.err.println("HTMLConversion.getMarshaller: " + e);
            }
        }
        return this.marshaller;
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
            }
            System.out.println("saving to file: "+htmlFileName);
        }catch(Exception ex) {System.out.println(ex);}
    }
}
