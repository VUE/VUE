/*
 * SaveAction.java
 *
 * Created on March 31, 2003, 1:33 PM
 */

package tufts.vue.action;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import tufts.vue.*;

/**
 *
 * @author  akumar03
 *
 */
public class SaveAction extends AbstractAction
{
    //    private static File file = null;
    private boolean saveAs = true;
   
    public SaveAction() {
    }
    
    public SaveAction(String label) {
        super(label);
        putValue(Action.SHORT_DESCRIPTION,label);
    }
    
    public SaveAction(String label, boolean saveType){
        super(label);
        setSaveAs(saveType);
        putValue(Action.SHORT_DESCRIPTION,label);
    }
    
    public boolean isSaveAs() {
        return this.saveAs;
    }
    
    public void setSaveAs(boolean saveAs){
        this.saveAs = saveAs;
    }      
    
    /*    
    public void setFileName(String fileName) {
        file = new File(fileName);
    }
    
    public String getFileName() {
        return file.getAbsolutePath();
    }
    */
    
    public void actionPerformed(ActionEvent e)
    {
        System.out.println("Action["+e.getActionCommand()+"] invoked...");
        
        LWMap map = tufts.vue.VUE.getActiveMap();
        File file = map.getFile();
        
        if (isSaveAs() || file == null)
            file = ActionUtil.selectFile("Save Map", "xml");
        
        if (file != null)
        {
            String name = file.getName().toLowerCase();

            if (name.endsWith(".xml"))
                ActionUtil.marshallMap(file, map);
            
            else if (name.endsWith(".jpeg") || name.endsWith(".jpg"))
                new ImageConversion().createJpeg(file);

            else if (name.endsWith(".svg"))
                new SVGConversion().createSVG(file);

            else if (name.endsWith(".pdf"))
                new PDFTransform().convert(file);

            else if (name.endsWith(".html"))
                new HTMLConversion().convert(file);
            
            System.out.println("Wrote " + file);
        }
            
        System.out.println("Action["+e.getActionCommand()+"] completed.");
    }

    
    /*
    public void actionPerformed_writes_over_other_saved_maps(ActionEvent e)
    {
        System.out.println("Action["+e.getActionCommand()+"] invoked...");
         
        boolean saveCondition = true;
        
        if (isSaveAs() || file == null)
        {
          file = ActionUtil.selectFile("Save Map", "xml");
          
          if (file == null)
              saveCondition = false;
        }
        
        if (saveCondition == true)
        {
            if (file.getName().endsWith(".xml")) {
                LWMap map = tufts.vue.VUE.getActiveMap();
                map.setLabel(file.getName());
                ActionUtil.marshallMap(file, map);
            }
          else if (file.getName().endsWith(".jpeg"))
            new ImageConversion().createJpeg(file);
          
          else if (file.getName().endsWith(".svg"))
            new SVGConversion().createSVG(file);
          
          else if (file.getName().endsWith(".pdf"))
            new PDFTransform().convert(file);
          
          else if (file.getName().endsWith(".html"))
            new HTMLConversion().convert(file);
          
          System.out.println("Saved " + getFileName());
        }
            
        System.out.println("Action["+e.getActionCommand()+"] completed.");
    }
    */

    
}






