
/*
 *
 * * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 *
 */

package edu.tufts.vue.ontology.action;


import edu.tufts.vue.ontology.ui.TypeList;

/*
 * RDFSOntologyOpenAction.java
 *
 * Created on March 30, 2007, 12:46 PM
 *
 * @author dhelle01
 */
public class RDFSOntologyOpenAction extends tufts.vue.VueAction {
    
    
    /** Creates a new instance of RDFSOntologyOpenAction */
    public RDFSOntologyOpenAction(String label) {
        super(label);
    }
    
    
    /**
     *
     * Use FileChoosers as first step until create windows to read file or 
     * url.
     *
     * DockWindows for seperate instances of the typelist panel seem to need to
     * have unique names for now.
     *
     **/
    public void actionPerformed(java.awt.event.ActionEvent e)
    {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        javax.swing.JFileChooser styleChooser = new javax.swing.JFileChooser();
        chooser.showOpenDialog(tufts.vue.VUE.getActiveViewer());
        java.io.File file = chooser.getSelectedFile();
        styleChooser.showOpenDialog(tufts.vue.VUE.getActiveViewer());
        java.io.File styleFile = styleChooser.getSelectedFile();
        System.out.println("RDFSooa: file, stylefile: " + file + "," + styleFile);
        if(file!=null && styleFile!=null )
        {
          TypeList typeList = new TypeList();
          try
          {
            java.net.URL fileURL = new java.net.URL("file:////"+file.getAbsolutePath());
            java.net.URL cssURL = new java.net.URL("file:////" + styleFile.getAbsolutePath());
              
            System.out.println("RDFS open action: fileURL, cssURL: " + fileURL + "," + cssURL);
            
            typeList.loadOntology(fileURL,
                                  cssURL,
                                  edu.tufts.vue.ontology.OntManager.RDFS,true);
          }
          catch(Exception ex)
          {
              tufts.vue.VueUtil.alert("Couldn't load Ontology or Style","Error");
              System.out.println("Exception in rdfs ontology load: " + ex);
          }
          tufts.vue.gui.DockWindow typeWindow = tufts.vue.gui.GUI.createDockWindow(chooser.getSelectedFile() + ": " + (TypeList.count++),
                                                typeList);
          typeWindow.setLocation(200,100);
          typeWindow.pack();
          typeWindow.setVisible(true);
        }
    }
    
}