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
    
    final String XML_MAPPING = VUE.CASTOR_XML_MAPPING;
    private static  String fileName = "default.xml";
    private Marshaller marshaller = null;
    private JTextPane xmlArea = null;
    private StyledDocument doc = null;
    private ArrayList braces = null;
    
    
    public XMLView() {
    }
 
    public XMLView(String label) {
        super(label);
        putValue(Action.SHORT_DESCRIPTION,label);
    }
    
    class XMLTextPane extends JTextPane implements Scrollable{
        /*public boolean getScrollableTracksViewportWidth() {
             if (getParent() instanceof JViewport) {
                 JViewport port = (JViewport)getParent();
                 TextUI ui = getUI();
                 int w = port.getWidth();
                 Dimension min = ui.getMinimumSize(this);
                 Dimension max = ui.getMaximumSize(this);
                 if ((w >= min.width) && (w <= max.width)) {
                     return true;
                 }
             }
             return false;
         }*/
        
    }
    
    public void actionPerformed(ActionEvent e)
    {   
        System.out.println("Action["+e.getActionCommand()+"] invoked...");
        
        if(VUE.tabbedPane.getSelectedComponent() instanceof MapViewer) {

            //call marshaller in ActionUtil
            ActionUtil.marshallMap(new File(fileName));
            
            //create html view from default.xml
            File xmlFile = new File(fileName);
            xmlArea = new JTextPane();
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
            
            setAttributes();
            for(int i = 0; i < braces.size(); i++){
                System.out.println(braces.get(i));
            }
            
            JScrollPane pane = new JScrollPane(xmlArea);

            String mapName = VUE.getActiveMap().getLabel();
            JTabbedPane tabPane = VUE.tabbedPane;
            for(int i = 0; i < tabPane.getComponentCount(); i++){
                if(tabPane.getTitleAt(i).equals(mapName +".xml")){
                    System.out.println("in repeat selection.......");
                    VUE.tabbedPane.setSelectedIndex(i);
                    return;
                }
            }

            VUE.tabbedPane.addTab(mapName+".xml", pane);
            VUE.tabbedPane.setSelectedIndex(VUE.tabbedPane.getComponentCount()-1);

            System.out.println("Action["+e.getActionCommand()+"] completed.");
        }
    }
    
    private void setAttributes(){
        SimpleAttributeSet att = new SimpleAttributeSet();
        StyleConstants.setForeground(att, Color.blue);
        for(int i = 0; i < braces.size(); i++)
            doc.setCharacterAttributes(Integer.parseInt((String)braces.get(i)), 1, att, false);
    }
    
    private String docToString(org.w3c.dom.Document xmlDoc) throws TransformerException{
        
        DOMSource domSource=new DOMSource(xmlDoc);
        
        StringWriter stringWriter = new StringWriter();
        StreamResult result = new StreamResult(stringWriter);
        
        
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        
        transformer.setOutputProperty("indent","yes");
        transformer.transform(domSource, result);        
        
        Pattern p = Pattern.compile("</|<");
        Matcher m = p.matcher(stringWriter.toString());
        StringBuffer sb = new StringBuffer();
        int indent = -2;
        braces = new ArrayList(); 
        System.out.println("before find...");
        while(m.find()){
            String group = m.group();
            String tab = "";
            String tabInd = "     ";
            System.out.println("group: " + group);
            
            if(group.equals("</")){
                
                for(int i = 0; i < indent; i++)
                    tab += tabInd;
                indent--;
                m.appendReplacement(sb, tab + "</");
                braces.add(Integer.toString(m.start()));
            }
            else if(group.equals("<")){
                indent++;
                for(int i = 0; i < indent; i++)
                    tab += tabInd;
                
                m.appendReplacement(sb, tab + "<");
                braces.add(Integer.toString(m.start()));
            }
        }
        m.appendTail(sb);
        
        return sb.toString();
    }
}
