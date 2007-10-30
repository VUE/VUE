/*
 * -----------------------------------------------------------------------------
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

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
import java.awt.event.ActionEvent;

import tufts.vue.*;

public class HTMLConversion extends AbstractAction
{   
    
    private static  String htmlFileName = "default.html";
    private static  String xmlFileName = "default.xml";
    private static  String xslFileName = "viewHTML.xsl";
    
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