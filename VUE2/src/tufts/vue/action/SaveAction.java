/*
 * SaveAction.java
 *
 * Created on March 31, 2003, 1:33 PM
 */

package tufts.vue.action;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import java.io.OutputStreamWriter;

import tufts.vue.*;
/**
 *
 * @author  akumar03
 */
public class SaveAction extends AbstractAction
{
    final String XML_MAPPING = "concept_map.xml";
    private static  String fileName = "test.xml";
    private Marshaller marshaller = null;
    //private boolean saveAs = true;
    private String saveType = "save";
    
    /** Creates a new instance of SaveAction */
    
    public SaveAction() {
    }
    
    public SaveAction(String label) {
        super(label);
        putValue(Action.SHORT_DESCRIPTION,label);
    }
    
    public SaveAction(String label, String saveType){
        super(label);
        //setSaveAs(false);
        this.saveType = saveType;   
        putValue(Action.SHORT_DESCRIPTION,label);
    }
    /*
    public boolean isSaveAs() {
        return this.saveAs;
    }
    
    public void setSaveAs(boolean saveAs){
        this.saveAs = saveAs;
    }      
    */
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFileName() {
        return this.fileName;
    }
    
    
    public void actionPerformed(ActionEvent e) {
      if(!saveType.equalsIgnoreCase("html"))
      {
        try {  
    //        if (isSaveAs())
            if(saveType.equalsIgnoreCase("saveAs"))
                selectFile();
            marshaller = getMarshaller();
            marshaller.marshal(tufts.vue.VUE.getActiveMap());
        
        }catch(Exception ex) {
            System.out.println(ex);
        }
        System.out.println("Action["+e.getActionCommand()+"] performed!");
      }
      else {
            System.out.println("request to save as html...");
            //save as html file in current directory
            String output = getOutput();
            //String output = "<HTML><HEAD></HEAD><BODY>XML File........</BODY></HTML>";
            try{
                
                File outputFile = new File("C:\\XmlToHtml.html");
                FileWriter out = new FileWriter(outputFile);
                out.write(output);
                out.close();
                System.out.println("wrote to the file...");
            }catch(IOException ioe){
                System.out.println("Error trying to write to html file: " + ioe);
            }
      }
    }
    
    private String getOutput() {
        String output = "<HTML><HEAD><TITLE>XML TEST FILE</TITLE></HEAD><BODY>";
        output = output + "<b>Concept Map:</b> <p>";
        ConceptMap map = (ConceptMap) tufts.vue.VUE.getActiveMap();
        output = getItemData(output, map);
        
        output = output + "<b>Nodes:</b> <p>";
        java.util.Iterator ni = (java.util.Iterator) map.getNodeIterator();
        int i = 0;
        while( ni.hasNext() ){
            output = output + "&nbsp;<u>Node No."+i+"</u>:<p>"; i++;
            output = getNodeData(output, (Node)ni.next());
        }
        
        output = output + "<b>Links:</b> <p>";
        java.util.Iterator li = (java.util.Iterator) map.getLinkIterator();
        i = 0;
        while( li.hasNext() ){
            output = output + "&nbsp;<u>Link No."+i+"</u>:<p>"; i++;
            output = getLinkData(output, (Link)li.next());
        }
        
        output = output + "<b>Pathways:</b> <p>";
        java.util.Iterator pi = (java.util.Iterator) map.getPathwayIterator();
        i = 0;
        while( pi.hasNext() ){
            output = output + "&nbsp;<u>Pathway No."+i+"</u>:<p>"; i++;
            output = getPathwayData(output, (Pathway)pi.next());
        }
        
        output = output + "</BODY></HTML>";
        return output;
    }
    
    private String getNodeData(String out, Node node){
        out = getItemData(out, node);
        return out;
    }
    
    private String getLinkData(String out, Link link){
        out = getItemData(out, link);
        out = out + "&nbsp;&nbsp;&nbsp;&nbsp;Weight: " + link.getWeight() + "<p>";
        out = out + "&nbsp;&nbsp;&nbsp;&nbsp;Ordered?: " + link.isOrdered() + "<p>";
        out = out + "&nbsp;&nbsp;&nbsp;&nbsp;Fixed?: " + link.isFixed() + "<p>";
        out = out + "&nbsp;&nbsp;&nbsp;&nbsp;Item 1: " + link.getItem1().getLabel() + "<p>";
        out = out + "&nbsp;&nbsp;&nbsp;&nbsp;Item 2: " + link.getItem2().getLabel() + "<p>";
        return out;
    }
    
    private String getPathwayData(String out, Pathway path){
        out = getItemData(out, path);
        out = out + "&nbsp;&nbsp;&nbsp;&nbsp;Weight: " + path.getWeight() + "<p>";
        out = out + "&nbsp;&nbsp;&nbsp;&nbsp;Ordered?: " + path.isOrdered() + "<p>";
        return out;
    }
    
    private String getItemData(String out, MapItem item){
        out = out + "&nbsp;&nbsp;&nbsp;&nbsp;Label: " + item.getLabel() + "<p>";
        out = out + "&nbsp;&nbsp;&nbsp;&nbsp;ID: " + item.getID() + "<p>";
        out = out + "&nbsp;&nbsp;&nbsp;&nbsp;Notes: " + item.getNotes() + "<p>";
        out = out + "&nbsp;&nbsp;&nbsp;&nbsp;MetaData: " + item.getMetaData() + "<p>";
        out = out + "&nbsp;&nbsp;&nbsp;&nbsp;Catagory: " + item.getCategory() + "<p>";
        out = out + "&nbsp;&nbsp;&nbsp;&nbsp;Resource: <a href=\""+item.getResource()+"\">" + item.getResource() +"</a><p>";
        return out;
    }
    
    private void selectFile()
    {
        try {  
          
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Map");
        chooser.setFileFilter(new VueFileFilter());
        if(VueUtil.isCurrentDirectoryPathSet()) 
            chooser.setCurrentDirectory(new File(VueUtil.getCurrentDirectoryPath()));  
        int option = chooser.showDialog(tufts.vue.VUE.frame, "Save");
        if (option == JFileChooser.APPROVE_OPTION) {
            fileName = chooser.getSelectedFile().getAbsolutePath();
            // if they choose nothing, fileName will be null -- detect & abort
            VueUtil.setCurrentDirectoryPath(chooser.getSelectedFile().getParent());
        }
       }catch(Exception ex) {System.out.println(ex);}   
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
                System.err.println("OpenAction.getMarshaller: " + e);
            }
        }
        return this.marshaller;
    }
}
