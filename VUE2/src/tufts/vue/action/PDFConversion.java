/*
 * PDFConversion.java
 *
 * Created on June 4, 2003, 7:02 PM
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

/**a class which constructs a PDF file based on the data within the concept map*/
public class PDFConversion extends AbstractAction {
    
    private static  String pdfFileName = "";
    private static  String fileName = "default.xml";
    final String XML_MAPPING = VueResources.getString("mapping.lw");
    private Marshaller marshaller = null;
    
    /** Creates a new instance of PDFConversion */
    public PDFConversion() {
    }
    
    /**A constructor */
    public PDFConversion(String label) {
        super(label);
        putValue(Action.SHORT_DESCRIPTION,label);
    }
    
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        System.out.println("Performing PDF Conversion:" + actionEvent.getActionCommand());
        try {
            marshaller = getMarshaller();
            marshaller.marshal(tufts.vue.VUE.getActiveMap());
            marshaller = null;
        }catch(Exception ex) {
            System.out.println(ex);
        }
        
        
        //convert default.xml to pdf
        selectPDFFile();
        String state = "fop"
            +" -xsl view.xsl"
            +" -xml default.xml"
            +" -pdf \"" + pdfFileName + "\"";
        try{
            Runtime r = Runtime.getRuntime();
            
            Process p = r.exec( "cmd /c" + state);
            p = null;
            
        } catch (IOException ioe){
            System.err.println("PDFConversion.actionPerformed error: " +ioe);
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
                System.err.println("PDFConversion.getMarshaller: " + e);
            }
        }
        return this.marshaller;
    }
    
    private void selectPDFFile() {
        String label = VUE.getActiveMap().getLabel();
        try {
            
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save as PDF");
            int option = chooser.showDialog(tufts.vue.VUE.frame, "Save");
            if (option == JFileChooser.APPROVE_OPTION) {
                pdfFileName = chooser.getSelectedFile().getAbsolutePath();
                if(!pdfFileName.endsWith(".pdf")) pdfFileName += ".pdf";
            }
            System.out.println("saving to file: "+pdfFileName);
        }catch(Exception ex) {System.out.println(ex);}
    }
}
