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
import java.util.regex.*;
import java.util.ArrayList;
import java.awt.Font;
import java.awt.Color;
import javax.swing.text.StyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

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
    
    //final String XML_MAPPING = VUE.CASTOR_XML_MAPPING;
    private static  String fileName = "default.xml";
    private Marshaller marshaller = null;
    private XMLTextPane xmlArea = null;
    private StyledDocument doc = null;
    private ArrayList braces = null;
    
    
    public XMLView() {
    }
 
    public XMLView(String label) {
        super(label);
        putValue(Action.SHORT_DESCRIPTION,label);
    }
    
    class XMLTextPane extends JTextPane implements Scrollable{
        public boolean getScrollableTracksViewportWidth() {
             return false;
         }
        
    }
    
    public void actionPerformed(ActionEvent e)
    {   
        System.out.println("Action["+e.getActionCommand()+"] invoked...");
        
        if(VUE.getTabbedPane().getSelectedComponent() instanceof MapViewer) {

            //call marshaller in ActionUtil
            ActionUtil.marshallMap(new File(fileName));
            
            //create html view from default.xml
            File xmlFile = new File(fileName);
            xmlArea = new XMLTextPane();
            doc = (StyledDocument)xmlArea.getDocument();
            
            xmlArea.setText("");
            xmlArea.setEditable(false);
            xmlArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            org.w3c.dom.Document xmlDoc = null;
            try{
                DocumentBuilder documentBuilder = factory.newDocumentBuilder();
                xmlDoc = documentBuilder.parse(fileName);
            }catch(Exception ex){
                System.out.println("problem creating dom object from xml file: "+ex);
            }
            
            try {
                xmlArea.setText(docToString(xmlDoc));
            }
            catch (TransformerException te){
                System.out.println("transformer exception: "+te);
            }
            
            JScrollPane pane = new JScrollPane(xmlArea);
            
            String mapName = VUE.getActiveMap().getLabel();
            JTabbedPane tabPane = VUE.getTabbedPane();
            for(int i = 0; i < tabPane.getComponentCount(); i++){
                if(tabPane.getTitleAt(i).equals(mapName +".xml")){
                    VUE.getTabbedPane().setSelectedIndex(i);
                    return;
                }
            }

            VUE.getTabbedPane().addTab(mapName+".xml", pane);
            VUE.getTabbedPane().setSelectedIndex(VUE.getTabbedPane().getComponentCount()-1);
            //setAttributes();
            
            pane.repaint();
            System.out.println("Action["+e.getActionCommand()+"] completed.");
        }
    }
    /*
    private void setAttributes(){
        SimpleAttributeSet att = new SimpleAttributeSet();
        StyleConstants.setForeground(att, Color.blue);
        for(int i = 0; i < braces.size(); i++)
            doc.setCharacterAttributes(Integer.parseInt((String)braces.get(i)), 1, att, false);
    }*/
    
    private String docToString(org.w3c.dom.Document xmlDoc) throws TransformerException{
        
        DOMSource domSource=new DOMSource(xmlDoc);
        
        StringWriter stringWriter = new StringWriter();
        StreamResult result = new StreamResult(stringWriter);
        
        
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        
        transformer.transform(domSource, result);        
        
        Pattern p = Pattern.compile("</|<|>");
        String lookUp = stringWriter.toString();
        Matcher m = p.matcher(lookUp);
        StringBuffer sb = new StringBuffer();
        int indent = -2;
        //braces = new ArrayList(); 
        while(m.find()){
            String group = m.group();
            String tab = "";
            String tabInd = "          ";
            
            if(group.equals("</")){
                
                for(int i = 0; i < indent; i++)
                    tab += tabInd;
                indent--;
                m.appendReplacement(sb, "\n" + tab + "</");
            }
            else if(group.equals("<")){
                indent++;
                for(int i = 0; i < indent; i++)
                    tab += tabInd;
                
                m.appendReplacement(sb, "\n" + tab + "<");
            }
            else if(group.equals(">")){
                indent++;
                for(int i = 0; i < indent; i++)
                    tab += tabInd;
                if(m.end() < lookUp.length()){
                    Pattern pat = Pattern.compile("\\w|#");
                    Matcher match = pat.matcher(lookUp.substring(m.end(), m.end()+1));
                    boolean b = match.matches();

                    if(b)
                    {   m.appendReplacement(sb, ">\n" + tab);
                        
                    }
                    else{
                        m.appendReplacement(sb, ">");
                        
                    }indent--;
                }
            }
            else if(group.equals(">#")){
                indent++;
                for(int i = 0; i < indent; i++)
                    tab += tabInd;
                
                m.appendReplacement(sb, ">\n" + tab + "     #" );
                indent--;
            }
            else if(group.equals(">\\w")){
                indent++;
                for(int i = 0; i < indent; i++)
                    tab += tabInd; 
                
                m.appendReplacement(sb, ">\n" + tab + "     $1" );
                indent--;
            }
        }
        m.appendTail(sb);
        
        return sb.toString();
    }
}
