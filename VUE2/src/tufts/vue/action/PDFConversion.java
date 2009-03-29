/*
 * Copyright 2003-2008 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

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

import tufts.vue.*;
import tufts.vue.gui.VueFileChooser;


/**
 *
 * @author  Jay Briedis
 */

/**a class which constructs a PDF file based on the data within the concept map*/
public class PDFConversion extends AbstractAction {
    
    private static  String pdfFileName = "";
    private static  String fileName = "default.xml";
    
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

        ActionUtil.marshallMap(new File(fileName), tufts.vue.VUE.getActiveMap());

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
    
    private void selectPDFFile() {
        String label = VUE.getActiveMap().getLabel();
        try {
            
            VueFileChooser chooser = VueFileChooser.getVueFileChooser();
            chooser.setDialogTitle(VueResources.getString("dialog.saveaspdf.title"));
            int option = chooser.showDialog(tufts.vue.VUE.getDialogParent(), VueResources.getString("dialog.save.title"));
            if (option == VueFileChooser.APPROVE_OPTION) {
                pdfFileName = chooser.getSelectedFile().getAbsolutePath();
                if(!pdfFileName.endsWith(".pdf")) pdfFileName += ".pdf";
            }
            System.out.println("saving to file: "+pdfFileName);
        }catch(Exception ex) {System.out.println(ex);}
    }
}
