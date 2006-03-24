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
 * Save the currently active map.
 *
 * @author akumar03
 * @author Scott Fraize
 */
public class SaveAction extends VueAction
{
    private boolean saveAs = true;
   
    public SaveAction(String label, boolean saveType){
        super(label, null, ":general/Save");
        setSaveAs(saveType);
    }
    
    public SaveAction(String label) {
        this(label, true);
    }
    
    public SaveAction() {
        this("Save", false);
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
    
    private boolean inSave = false;
    public void actionPerformed(ActionEvent e)
    {
        if (inSave) // otherwise rapid Ctrl-S's will trigger multiple dialog boxes
            return;

        try {
            inSave = true;
            System.out.println("Action["+e.getActionCommand()+"] invoked...");
            saveMap(tufts.vue.VUE.getActiveMap(), isSaveAs());
            System.out.println("Action["+e.getActionCommand()+"] completed.");
        } finally {
            inSave = false;
        }
    }

    /**
     * @return true if success, false if not
     */
      
    public static boolean saveMap(LWMap map, boolean saveAs)
    {
        System.out.println("SaveAction.saveMap: " + map);
        
        if (map == null)
            return false;
        
        File file = map.getFile();
        
        if (saveAs || file == null)
            //file = ActionUtil.selectFile("Save Map", "vue");
            file = ActionUtil.selectFile("Save Map", null);
        
        if (file == null)
            return false;
        
        try {
            VUE.activateWaitCursor();
            String name = file.getName().toLowerCase();

            if (name.endsWith(".xml") || name.endsWith(".vue"))
                ActionUtil.marshallMap(file, map);
            
            else if (name.endsWith(".jpeg") || name.endsWith(".jpg"))
                ImageConversion.createActiveMapJpeg(file);
            
            else if (name.endsWith(".svg"))
                new SVGConversion().createSVG(file);
            
            else if (name.endsWith(".pdf"))
                new PDFTransform().convert(file);
            
            else if (name.endsWith(".html"))
                new HTMLConversion().convert(file);
            
            else if (name.endsWith(".imap"))
                new ImageMap().createImageMap(file);
            
            // don't know this as not all the above stuff is passing
            // exceptions on to us!
            System.out.println("Save code completed for " + file);
            return true;

        } catch (Throwable e) {
            Throwable originalException = e;
            if (e.getCause() != null)
                originalException = e.getCause();
            if (originalException instanceof java.io.FileNotFoundException) {
                System.out.println("Save failed: " + originalException);
            } else {
                e.printStackTrace();
            }
            VUE.Log.error("Exception attempting to save file " + file);
            VueUtil.alert("Save failed for \"" + file + "\"", originalException);
        } finally {
            VUE.clearWaitCursor();
        }

        return false;
    }

    public static boolean saveMap(LWMap map) {
        return saveMap(map, false);
    }
    
    public static void main(String args[])
    {
        LWMap map = new LWMap("test");
        map.setFile(new File("test.xml"));
        map.addNode(new LWNode("Test Node"));
        System.err.println("Attempting to save test map " + map);
        DEBUG.Enabled = DEBUG.INIT = true;
        new SaveAction().saveMap(map, false);
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






