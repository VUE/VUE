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
   // final String XML_MAPPING = VUE.CASTOR_XML_MAPPING;
    private Marshaller marshaller = null;
    
    public HTMLConversion() {
    }
    
    public HTMLConversion(String label) {
        super(label);
        putValue(Action.SHORT_DESCRIPTION,label);
    }
    
    public void actionPerformed(ActionEvent ae) {
        System.out.println("Performing HTML Conversion:" + ae.getActionCommand());
        
        //ActionUtil.marshallMap(new File(xmlFileName));
        
        File result = ActionUtil.selectFile("Save As HTML", "html");
        if(!result.equals(null)) convert(result);
    }
    
    public void convert(File result){
        System.out.println("in convert..................");
        
        ActionUtil.marshallMap(new File(xmlFileName));
        TransformerFactory tfactory = TransformerFactory.newInstance();
        
        try
        {
            InputStream xslInput = new FileInputStream( xslFileName );
            StreamSource xslSource = new StreamSource( xslInput );
                
            StreamSource xmlSource = new StreamSource( xmlFileName );
            
            Templates templates = tfactory.newTemplates( xslSource );
        
            
            
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
}