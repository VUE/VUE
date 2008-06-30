
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

package edu.tufts.vue.ontology.ui;

import edu.tufts.vue.ontology.OntManager;
import edu.tufts.vue.ontology.OntologyType;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import tufts.vue.VueUtil;
import tufts.vue.gui.VueFileChooser;

/*
 * OntologyChooser.java
 *
 * Created on April 4, 2007, 10:38 AM
 *
 * @author dhelle01
 */
public class OntologyChooser extends javax.swing.JDialog implements java.awt.event.ActionListener {
    
    public static final int STEP_ONE = 0;
    public static final int STEP_TWO = 1;
    
    public static final int ONT_CHOOSER_WIDTH = 500;
    public static final int ONT_CHOOSER_HEIGHT = 350;
    public static final int ONT_CHOOSER_X_LOCATION = 300;
    public static final int ONT_CHOOSER_Y_LOCATION = 300;
    
    public static final String stepOneMessage = "<html> Step 1 of 2 - Add an <b>ontology</b> </html>";
    public static final String stepTwoMessage = "<html> Step 2 of 2 - Select a <b>style sheet</b> for the ontology</html>";
    public static final String browseFileMessage = "Browse to file:";
    public static final String orMessage = "or";
    public static final String typeURLMessage = "Type in a URL:";
    public static final String styleSheetMessage = "<html>The style sheet can be added later using<br> the \"x\" window </html>";
    
    private int step = STEP_ONE;
    
    private JPanel mainPanel = null;
    private JPanel browsePanel = null;
    private JPanel buttonPanel = null;
    private JButton cancelButton = null;
    private JButton nextButton = null;
    private JButton browseButton = null;
   // private JButton attachButton = null;
    private JLabel stepLabel = null;
    private JTextField browseFileField = null;
    private JTextField typeURLField = null;
    private JLabel styleSheetMessageLabel = null;
    
    private OntologyBrowser browser = null;
    
    private File ontFile;
    private File cssFile;
    private String ontURLText;
    private String cssURLText;
    private URL ontURL;
    private URL cssURL;
    
    public OntologyChooser(java.awt.Frame owner,String title,OntologyBrowser browser) 
    {
        super(owner,title);
        this.browser = browser;
        setLocation(ONT_CHOOSER_X_LOCATION,ONT_CHOOSER_Y_LOCATION);
        setModal(true);
        setSize(ONT_CHOOSER_WIDTH,ONT_CHOOSER_HEIGHT);
        
        cancelButton = new JButton("Cancel");
        nextButton = new JButton("Next");
        browseButton = new JButton("Browse");
        //attachButton = new JButton("Attach");
        
        cancelButton.addActionListener(this);
        nextButton.addActionListener(this);
        browseButton.addActionListener(this);
        //attachButton.addActionListener(this);
        
        setUpPanels();
        
        getContentPane().setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        getContentPane().add(mainPanel);
        
        setVisible(true);
    }
    
    public void setUpPanels()
    {
        stepLabel = new JLabel(stepOneMessage);
        JLabel info = new JLabel(tufts.vue.VueResources.getIcon("helpIcon.raw"));
        info.setToolTipText("Add an Ontology help here - TBD");
        tufts.vue.PolygonIcon lineIcon = new tufts.vue.PolygonIcon(new java.awt.Color(153,153,153));
        lineIcon.setIconWidth(ONT_CHOOSER_WIDTH-40);
        lineIcon.setIconHeight(1);
        JLabel lineLabel = new JLabel(lineIcon);
        
        mainPanel = new JPanel();
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        mainPanel.setLayout(gridBag);
        
        setUpBrowsePanel();
        setUpButtonPanel();
        
        c.insets = new Insets(10,10,10,10);
        gridBag.setConstraints(stepLabel,c);
        mainPanel.add(stepLabel);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.EAST;
        gridBag.setConstraints(info,c);
        mainPanel.add(info);
        c.fill = GridBagConstraints.HORIZONTAL;
        gridBag.setConstraints(browsePanel,c);
        mainPanel.add(browsePanel);
        gridBag.setConstraints(lineLabel,c);
        mainPanel.add(lineLabel);
        //c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        gridBag.setConstraints(buttonPanel,c);
        mainPanel.add(buttonPanel);
    }
    
    public void setUpBrowsePanel()
    {
       JLabel browseLabel = new JLabel(browseFileMessage);
       JLabel orLabel = new JLabel(orMessage);
       JLabel urlLabel = new JLabel(typeURLMessage);
       browseFileField = new JTextField(10);
       typeURLField = new JTextField(10);
       browsePanel = new JPanel(); 
       GridBagLayout browseGrid = new GridBagLayout();
       GridBagConstraints browseConstraints = new GridBagConstraints();
       browsePanel.setLayout(browseGrid);
       
       browseConstraints.insets = new Insets(5,5,5,5);
       browseGrid.setConstraints(browseLabel,browseConstraints);
       browsePanel.add(browseLabel);
       browseConstraints.weightx = 1.0;
       browseConstraints.fill = GridBagConstraints.HORIZONTAL;
       browseGrid.setConstraints(browseFileField,browseConstraints);
       browsePanel.add(browseFileField);
       browseConstraints.weightx = 0.0;
       browseConstraints.gridwidth = GridBagConstraints.REMAINDER;
       browseGrid.setConstraints(browseButton,browseConstraints);
       browsePanel.add(browseButton);
       //browseConstraints.gridwidth = GridBagConstraints.REMAINDER;
       //browseGrid.setConstraints(attachButton,browseConstraints);
       //browsePanel.add(attachButton);
       browseGrid.setConstraints(orLabel,browseConstraints);
       browsePanel.add(orLabel);
       browseConstraints.gridwidth = 1;
       browseGrid.setConstraints(urlLabel,browseConstraints);
       browsePanel.add(urlLabel);
       browseConstraints.gridwidth = GridBagConstraints.REMAINDER;
       browseGrid.setConstraints(typeURLField,browseConstraints);
       browsePanel.add(typeURLField);
               
    }
    
    public void setUpButtonPanel()
    {
        styleSheetMessageLabel = new JLabel();
        buttonPanel = new JPanel();
        
        GridBagLayout buttonLayout = new GridBagLayout();
        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonPanel.setLayout(buttonLayout);
        
        buttonConstraints.anchor = GridBagConstraints.WEST;
        buttonConstraints.weightx = 1.0;
        buttonLayout.setConstraints(styleSheetMessageLabel,buttonConstraints);
        buttonPanel.add(styleSheetMessageLabel);
        buttonConstraints.weightx = 0.0;
        buttonLayout.setConstraints(cancelButton,buttonConstraints);
        buttonPanel.add(cancelButton);
        buttonLayout.setConstraints(nextButton,buttonConstraints);
        buttonPanel.add(nextButton);
    }
    
    public static org.osid.shared.Type  getOntType(URL ontURL)
    {
        String type = ontURL.toString().substring(ontURL.toString().lastIndexOf(".")+1);
        if(type.equals("rdfs"))
            return edu.tufts.vue.ontology.OntologyType.RDFS_TYPE;
        else
        if(type.equals("owl"))
            return edu.tufts.vue.ontology.OntologyType.OWL_TYPE;
        else
            return edu.tufts.vue.ontology.OntologyType.OWL_TYPE;
    }
    
    public void actionPerformed(java.awt.event.ActionEvent e)
    {
        if(e.getSource() == nextButton)
        {
            if(step == STEP_ONE)
            {  
               //System.out.println("1--->2");
               step = STEP_TWO;
               stepLabel.setText(stepTwoMessage);
               styleSheetMessageLabel.setText(styleSheetMessage);
               cancelButton.setText("Back");
               nextButton.setText("Finish");
               ontURLText = typeURLField.getText();
               typeURLField.setText("");
               browseFileField.setText("");
               //System.out.println("end 1---->2");
            }
            else
            if(step == STEP_TWO)
            {
               //System.out.println("2 finish");
               TypeList list = new TypeList();
               if(ontURLText != null)
               {
                   boolean fromURL = true;
                   try
                   {
                       ontURL = new URL(ontURLText);
                   }
                   catch(MalformedURLException mue)
                   {
                      fromURL = false;
                      //the following dialog can fall behind the chooser and create the (mistaken) 
                      // appearance of deadlock...
                      //VueUtil.alert("Improper URL, try file field instead?","URL Error");
                      System.out.println("OntologyChooser: Improper URL, will try file field instead");
                      try
                      {
                          if(ontFile!=null)
                          {
                            ontURL = ontFile.toURL();
                          }
                      }
                      catch(MalformedURLException mue2)
                      {
                          System.out.println("OntologyChooser: File also produced Malformed URL " + mue2);
                      }
                   }
               if(!(typeURLField.getText().trim().length()==0))
               {
                   fromURL = true;
                   try
                   {
                       cssURL = new URL(typeURLField.getText());
                   }
                   catch(MalformedURLException mue)
                   {
                      fromURL = false;
                      //this dialog can fall behind the chooser and create the appearance
                      //of deadlock...
                      //VueUtil.alert("Improper URL, try file field instead?","URL Error");
                      System.out.println("OntologyChooser: Improper CSS URL, will try file field instead");
                      try
                      {
                          if(cssFile!=null)
                          {
                            cssURL = cssFile.toURL();
                          }
                      }
                      catch(MalformedURLException mue2)
                      {
                          System.out.println("OntologyChooser: CSS File also produced Malformed URL " + mue2);
                      }
                   }
               }
               else if(cssFile!=null)
               {
                       try
                       {
                         cssURL = cssFile.toURL();
                       }
                       catch(MalformedURLException mue)
                       {
                           System.out.println("Malformed URL from file choice: " + mue);
                       }
               }
                   
               if(ontURL!=null && (cssURL == null))
               {
                       //edu.tufts.vue.ontology.Ontology ontology = edu.tufts.vue.ontology.OntManager.getOntManager().readOntology(ontURL,OntologyType.OWL_TYPE);
                       tufts.vue.gui.Widget w = browser.addTypeList(list,edu.tufts.vue.ontology.Ontology. getLabelFromUrl(ontURL.getFile()),ontURL);
                       //edu.tufts.vue.ontology.Ontology ontology = edu.tufts.vue.ontology.OntManager.getOntManager().
                        //                                        readOntology(ontURL,getOntType(ontURL));
                       //list.setModel(new TypeList.OntologyTypeListModel(ontology));
                                                                
                       list.loadOntology(ontURL,cssURL,getOntType(ontURL),browser,w);                                          
                                                                
                       browser.getViewer().getList().clearSelection();
                       browser.getViewer().getList().setSelectedIndex(-1);
                       //browser.getViewer().getList().updateUI();
                       //browser.getViewer().getList().setSelectedValue(ontology,true);
               }
               if(ontURL != null && cssURL!=null)
               {
                       tufts.vue.gui.Widget w = browser.addTypeList(list,edu.tufts.vue.ontology.Ontology. getLabelFromUrl(ontURL.getFile()),ontURL);
                      // browser.getViewer().getList().updateUI();
                       list.loadOntology(ontURL,cssURL,getOntType(ontURL),browser,w); 
               }
               setVisible(false);
               }
               
               //browser.getViewer().getList().updateUI();
               
               //System.out.println("end 2 finish");
            }
        }
        if(e.getSource() == cancelButton)
        {
            if(step == STEP_ONE)
            {
                dispose();
            }
            if(step == STEP_TWO)
            {
              step = STEP_ONE;
              //setTitle("Ontology");
              stepLabel.setText(stepOneMessage);
              styleSheetMessageLabel.setText("");
              cancelButton.setText("Cancel");
              nextButton.setText("Next");
              cssURLText = typeURLField.getText();
              if(ontURLText!=null && !ontURLText.equals(""))
              {
                  typeURLField.setText(ontURLText);
              }
            }
        }
        if(e.getSource() == browseButton)
        {
            VueFileChooser chooser = VueFileChooser.getVueFileChooser();
            chooser.showOpenDialog(tufts.vue.VUE.getDialogParentAsFrame());
            File selectedFile = chooser.getSelectedFile();
            if(selectedFile!=null)
            {
                if(step==STEP_ONE)
                {    
                  ontFile = selectedFile;
                }
                if(step==STEP_TWO)
                {
                  cssFile = selectedFile;
                }
                browseFileField.setText(selectedFile.getName());
            }
        }
    }
    
}
