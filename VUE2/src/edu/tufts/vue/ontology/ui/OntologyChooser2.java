
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
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import tufts.vue.VueUtil;


/*
 * OntologyChooser2.java
 *
 * Created on June 4, 2007, 2:41 PM
 *
 * @author dhelle01
 */
public class OntologyChooser2 extends javax.swing.JDialog implements java.awt.event.ActionListener {
    
    public static final int FILE = 0;
    public static final int URL = 1;
    
    public static final int ONT_CHOOSER_WIDTH = 500;
    public static final int ONT_CHOOSER_HEIGHT = 400;
    public static final int ONT_CHOOSER_X_LOCATION = 300;
    public static final int ONT_CHOOSER_Y_LOCATION = 300;
    
    //public static final String stepOneMessage = "<html> Step 1 of 2 - Add an <b>ontology</b> </html>";
    //public static final String stepTwoMessage = "<html> Step 2 of 2 - Select a <b>style sheet</b> for the ontology</html>";
    public static final String ontTitle = "Ontology";
    public static final String cssTitle = "Ontology Style Sheet*";
    public static final String browseFileMessage = "Browse to file:";
    //public static final String orMessage = "or";
    public static final String typeURLMessage = "Type in a URL:";
    public static final String styleSheetMessage = "<html>The style sheet can be added later using<br> the \"x\" window </html>";
    
    private int ontologySelectionType;
    private int cssSelectionType;
    
    private JPanel mainPanel = null;
    private JPanel ontBrowsePanel = null;
    private JPanel cssBrowsePanel = null;
    private JPanel ontURLPanel = null;
    private JPanel cssURLPanel = null;
    private JPanel buttonPanel = null;
    private JButton cancelButton = null;
    private JButton nextButton = null;
    private JButton cssBrowseButton = null;
    private JButton ontBrowseButton = null;
    private JLabel ontologyLabel = null;
    private JLabel ontLabel = null;
    private JLabel cssLabel = null;
    private JTextField ontBrowseFileField = null;
    private JTextField cssBrowseFileField = null;
    private JTextField typeOntURLField = null;
    private JTextField typeCssURLField = null;
    private JLabel styleSheetMessageLabel = null;
    
    private OntologyBrowser browser = null;
    
    private File ontFile;
    private File cssFile;
    private String ontURLText;
    private String cssURLText;
    private URL ontURL;
    private URL cssURL;
    
    private JComboBox ontChoice;
    private JComboBox cssChoice;
    
    public OntologyChooser2(java.awt.Frame owner,String title,OntologyBrowser browser) 
    {
        super(owner,title);
        this.browser = browser;
        setLocation(ONT_CHOOSER_X_LOCATION,ONT_CHOOSER_Y_LOCATION);
        setModal(true);
        setSize(ONT_CHOOSER_WIDTH,ONT_CHOOSER_HEIGHT);
        
        String[] choices = {"on the web","in a local folder"};
        
        cssChoice = new JComboBox(choices);
        ontChoice = new JComboBox(choices);
        
        typeOntURLField = new JTextField(20);
        typeCssURLField = new JTextField(20);
        
        cancelButton = new JButton("Cancel");
        nextButton = new JButton("Add");
        ontBrowseButton = new JButton("Browse");
        cssBrowseButton = new JButton("Browse");
        
        cssChoice.addActionListener(this);
        ontChoice.addActionListener(this);
        cancelButton.addActionListener(this);
        nextButton.addActionListener(this);
        ontBrowseButton.addActionListener(this);
        cssBrowseButton.addActionListener(this);
        
        setUpPanels();
        
        getContentPane().setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        getContentPane().add(mainPanel);
        
        setVisible(true);
    }
    
    public void setUpPanels()
    {
        ontLabel = new JLabel(ontTitle);
        cssLabel = new JLabel(cssTitle);
        JLabel info = new JLabel(tufts.vue.VueResources.getIcon("helpIcon.raw"));
        info.setToolTipText("Add an Ontology help here - TBD");
        tufts.vue.PolygonIcon lineIcon = new tufts.vue.PolygonIcon(new java.awt.Color(153,153,153));
        lineIcon.setIconWidth(ONT_CHOOSER_WIDTH-40);
        lineIcon.setIconHeight(1);
        JLabel lineLabel = new JLabel(lineIcon);
        tufts.vue.PolygonIcon lineIcon2 = new tufts.vue.PolygonIcon(new java.awt.Color(153,153,153));
        lineIcon2.setIconWidth(ONT_CHOOSER_WIDTH-40);
        lineIcon2.setIconHeight(1);
        JLabel lineLabel2 = new JLabel(lineIcon2);
        
        mainPanel = new JPanel();
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        mainPanel.setLayout(gridBag);
        
        setUpOntBrowsePanel();
        setUpCssBrowsePanel();
        setUpOntURLPanel();
        setUpCssURLPanel();
        setUpButtonPanel();
        
        c.insets = new Insets(10,10,10,10);
        gridBag.setConstraints(ontLabel,c);
        mainPanel.add(ontLabel);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.EAST;
        gridBag.setConstraints(info,c);
        mainPanel.add(info);
        c.gridwidth = 2;
        JLabel fileLabel = new JLabel("File location: ");
        c.insets = new Insets(10,30,10,10);
        gridBag.setConstraints(fileLabel,c);
        mainPanel.add(fileLabel);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(ontChoice,c);
        mainPanel.add(ontChoice);
        c.gridwidth = 2;
        JLabel typeLabel = new JLabel("Type in a URL: ");
        gridBag.setConstraints(typeLabel,c);
        mainPanel.add(typeLabel,c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(typeOntURLField,c);
        mainPanel.add(typeOntURLField,c);
        c.fill = GridBagConstraints.HORIZONTAL;
        //gridBag.setConstraints(browsePanel,c);
        //mainPanel.add(browsePanel);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(10,10,10,10);
        gridBag.setConstraints(lineLabel,c);
        mainPanel.add(lineLabel);
        gridBag.setConstraints(cssLabel,c);
        mainPanel.add(cssLabel);
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 2;
        JLabel fileLabel2 = new JLabel("File location: ");
        c.insets = new Insets(10,30,10,10);
        gridBag.setConstraints(fileLabel2,c);
        mainPanel.add(fileLabel2);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(cssChoice,c);
        mainPanel.add(cssChoice);
        c.gridwidth = 2;
        JLabel typeLabel2 = new JLabel("Type in a URL: ");
        gridBag.setConstraints(typeLabel2,c);
        mainPanel.add(typeLabel2,c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(typeCssURLField,c);
        mainPanel.add(typeCssURLField,c);
        c.fill = GridBagConstraints.HORIZONTAL;
        gridBag.setConstraints(lineLabel2,c);
        mainPanel.add(lineLabel2);
        c.anchor = GridBagConstraints.WEST;
        gridBag.setConstraints(buttonPanel,c);
        mainPanel.add(buttonPanel);
    }
    
    public void setUpOntBrowsePanel()
    {
       /*JLabel browseLabel = new JLabel(browseFileMessage);
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
       browsePanel.add(typeURLField); */
               
    }
    
    public void setUpCssBrowsePanel()
    {
        
    }
    
    public void setUpOntURLPanel()
    {
        
    }
    
    public void setUpCssURLPanel()
    {
        
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
    
    public static org.osid.shared.Type getOntType(URL ontURL)
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
            //if(step == STEP_ONE)
            //{  
               //System.out.println("1--->2");
               //step = STEP_TWO;
               //stepLabel.setText(stepTwoMessage);
               //styleSheetMessageLabel.setText(styleSheetMessage);
               //cancelButton.setText("Back");
               //nextButton.setText("Finish");
               ontURLText = typeOntURLField.getText();
               //typeURLField.setText("");
               //browseFileField.setText("");
               //System.out.println("end 1---->2");
            //}
            //else
            //if(step == STEP_TWO)
            //{
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
               if(!(typeOntURLField.getText().trim().length()==0))
               {
                   fromURL = true;
                   try
                   {
                       cssURL = new URL(typeCssURLField.getText());
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
                       tufts.vue.gui.Widget w = browser.addTypeList(list,edu.tufts.vue.ontology.Ontology. getLabelFromUrl(ontURL.getFile()));
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
                       tufts.vue.gui.Widget w = browser.addTypeList(list,edu.tufts.vue.ontology.Ontology. getLabelFromUrl(ontURL.getFile()));
                      // browser.getViewer().getList().updateUI();
                       list.loadOntology(ontURL,cssURL,getOntType(ontURL),browser,w); 
               }
               setVisible(false);
               //}
               
               //browser.getViewer().getList().updateUI();
               
               //System.out.println("end 2 finish");
            }
        }
        if(e.getSource() == cancelButton)
        {
            dispose();
        }
        if(e.getSource() == ontBrowseButton)
        {
            JFileChooser ontChooser = new JFileChooser();
            ontChooser.showOpenDialog(tufts.vue.VUE.getDialogParentAsFrame());
            File ontSelectedFile = ontChooser.getSelectedFile();
            if(ontSelectedFile!=null)
            {    
                ontFile = ontSelectedFile;
                ontBrowseFileField.setText(ontSelectedFile.getName());
            }
        }
        if(e.getSource() == cssBrowseButton)
        {
            JFileChooser cssChooser = new JFileChooser();
            cssChooser.showOpenDialog(tufts.vue.VUE.getDialogParentAsFrame());
            File cssSelectedFile = cssChooser.getSelectedFile();
            if(cssSelectedFile!=null)
            {
                cssFile = cssSelectedFile;
                cssBrowseFileField.setText(cssSelectedFile.getName());
            } 
        }
    }
    
}

