
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

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
    private JButton attachButton = null;
    private JLabel stepLabel = null;
    private JTextField browseFileField = null;
    private JTextField typeURLField = null;
    private JLabel styleSheetMessageLabel = null;
    
    public OntologyChooser(java.awt.Frame owner,String title) 
    {
        super(owner,title);
        setLocation(ONT_CHOOSER_X_LOCATION,ONT_CHOOSER_Y_LOCATION);
        setModal(true);
        setSize(ONT_CHOOSER_WIDTH,ONT_CHOOSER_HEIGHT);
        
        cancelButton = new JButton("Cancel");
        nextButton = new JButton("Next");
        browseButton = new JButton("Browse");
        attachButton = new JButton("Attach");
        
        cancelButton.addActionListener(this);
        nextButton.addActionListener(this);
        browseButton.addActionListener(this);
        attachButton.addActionListener(this);
        
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
       
       //$
          //browsePanel.setOpaque(true);
          //browsePanel.setBackground(java.awt.Color.RED);
       //$
       
       browseConstraints.insets = new Insets(5,5,5,5);
       browseGrid.setConstraints(browseLabel,browseConstraints);
       browsePanel.add(browseLabel);
       browseConstraints.weightx = 1.0;
       browseConstraints.fill = GridBagConstraints.HORIZONTAL;
       browseGrid.setConstraints(browseFileField,browseConstraints);
       browsePanel.add(browseFileField);
       browseConstraints.weightx = 0.0;
       browseGrid.setConstraints(browseButton,browseConstraints);
       browsePanel.add(browseButton);
       browseConstraints.gridwidth = GridBagConstraints.REMAINDER;
       browseGrid.setConstraints(attachButton,browseConstraints);
       browsePanel.add(attachButton);
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
        
        //$
           //buttonPanel.setOpaque(true);
           //buttonPanel.setBackground(java.awt.Color.BLUE);
           //cancelButton.setOpaque(false);
           //nextButton.setOpaque(false);
        //$
        
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
    
    public void actionPerformed(java.awt.event.ActionEvent e)
    {
        if(e.getSource() == nextButton)
        {
            if(step == STEP_ONE)
            {    
              step = STEP_TWO;
              //setTitle("CSS");
              stepLabel.setText(stepTwoMessage);
              styleSheetMessageLabel.setText(styleSheetMessage);
              cancelButton.setText("Back");
              nextButton.setText("Finish");
            }
        }
        if(e.getSource() == cancelButton)
        {
            if(step == STEP_TWO)
            {
              step = STEP_ONE;
              //setTitle("Ontology");
              stepLabel.setText(stepOneMessage);
              styleSheetMessageLabel.setText("");
              cancelButton.setText("Cancel");
              nextButton.setText("Next");
            }
        }
    }
    
    public static void main(String[] args)
    {
        OntologyChooser ontoC = new OntologyChooser(null,"Add an Ontology");
        ontoC.pack();
    }
    
}
