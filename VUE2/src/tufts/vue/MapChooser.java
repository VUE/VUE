/*
 * MapChooser.java
 *
 * Created on January 4, 2007, 5:31 PM
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
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2006
 * Tufts University. All rights reserved.</p>
 *
 *
 */

/*
 * * MapChooser.java
 *
 * Created on January 4, 2007, 5:31 PM
 *
 */

package tufts.vue;

import junit.extensions.ActiveTestSuite;
import tufts.vue.action.ActionUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * Map Chooser
 *
 *
 * @author dhelle01
 */
public class MapChooser extends JDialog implements ActionListener{
    
    public static final int CMPUB_WIDTH = 500;
    public static final int CMPUB_HEIGHT = 250;
    public static final int CMPUB_X_LOCATION = 300;
    public static final int CMPUB_Y_LOCATION = 300;
    public static final String generateMessage = "Generate a connectivity matrix from:";
    public static final String browseMessage =   "Browse to map:                      ";
    
    private JPanel locationPanel = null;
    private JButton cancelButton = null;
    private JButton generateButton = null;
    private JPanel buttonPanel = null;
    private JComboBox choice = null;
    private JLabel lineLabel = null;
    private JPanel browsePanel = null;
    private JLabel browseLabel = null;
    private JPanel leftBrowsePanel = null;
    private JPanel rightBrowsePanel = null;
    private JTextField file = null;
    private JButton browseButton = null;
    private JButton attachButton = null;
    private File selectedFile = null;
    
    private LWMap selectedMap = null;
   
    public MapChooser(Frame owner,String title) {
        super(owner,title);
        setLocation(CMPUB_X_LOCATION,CMPUB_Y_LOCATION);
        setModal(true);
        setSize(CMPUB_WIDTH,CMPUB_HEIGHT);
        
        cancelButton = new JButton("Cancel");
        generateButton = new JButton("Generate");
        browseButton = new JButton("browse");
        attachButton = new JButton("attach");
        cancelButton.addActionListener(this);
        generateButton.addActionListener(this);
        browseButton.addActionListener(this);
        attachButton.setEnabled(false);
        attachButton.addActionListener(this);
        
        setUpLocationPanel();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(locationPanel);
        choice.addActionListener(this);
        
        addWindowListener(new WindowAdapter(){
           public void windowClosing(WindowEvent e)
           {
             setSelectedMap(null);   
           }
        });
        
        setSelectedMap(VUE.getActiveMap());
        selectedFile = new File(VueUtil.getCurrentDirectoryPath() + getSelectedMap().toString() + ".txt");
        setResizable(false);
        setVisible(true);
    }
    
    public void setUpLocationPanel()
    {
        locationPanel = new JPanel();
        
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        locationPanel.setLayout(gridBag);
        JLabel info = new JLabel(VueResources.getIcon("smallInfo"));
        info.setToolTipText("Create Connectivity Matrix Help Here - TBD");
        JLabel chooseLabel = new JLabel(generateMessage);
        browseLabel = new JLabel(browseMessage);
        String[] choices = {"selected map","map in a local folder"};
        choice = new JComboBox(choices);
        file = new JTextField(20);
        PolygonIcon lineIcon = new PolygonIcon(Color.DARK_GRAY);
        lineIcon.setIconWidth(CMPUB_WIDTH-40);
        lineIcon.setIconHeight(1);
        browsePanel = new JPanel();
        leftBrowsePanel = new JPanel();
        rightBrowsePanel = new JPanel();
        leftBrowsePanel.setLayout(new BoxLayout(leftBrowsePanel,BoxLayout.Y_AXIS));
        rightBrowsePanel.setLayout(new BoxLayout(rightBrowsePanel,BoxLayout.X_AXIS));
        leftBrowsePanel.add(browseLabel);
        leftBrowsePanel.add(file);
        rightBrowsePanel.add(browseButton);
        rightBrowsePanel.add(attachButton);
        browsePanel.setLayout(new BoxLayout(browsePanel,BoxLayout.X_AXIS));
        lineLabel = new JLabel(lineIcon);
        buttonPanel = new JPanel();
        buttonPanel.add(cancelButton);
        buttonPanel.add(generateButton);
        
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(info,c);
        locationPanel.add(info);
        
        c.insets = new Insets(20,5,5,2);
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.RELATIVE;
        gridBag.setConstraints(chooseLabel,c);
        locationPanel.add(chooseLabel);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor=GridBagConstraints.EAST;
        gridBag.setConstraints(choice,c);
        locationPanel.add(choice);
        
        gridBag.setConstraints(browsePanel,c);
        locationPanel.add(browsePanel);
        
        gridBag.setConstraints(lineLabel,c);
        locationPanel.add(lineLabel);

        gridBag.setConstraints(buttonPanel,c);
        locationPanel.add(buttonPanel);        
    }
    
    
    public LWMap getSelectedMap()
    {
        return selectedMap;
    }
    
    public File getSelectedFile()
    {
        return selectedFile;
    }
    
    public void setSelectedMap(LWMap map)
    {
        selectedMap = map;
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == cancelButton)
        {
            setSelectedMap(null);
            this.dispose();
        }
        if(e.getSource() == generateButton)
        {
            this.dispose();
        }
        if(e.getSource() == choice)
        {
            if(choice.getSelectedIndex()==0)
            {
              browsePanel.remove(leftBrowsePanel);
              browsePanel.remove(rightBrowsePanel);
              generateButton.setEnabled(true);
              file.setText("");
              setSelectedMap(VUE.getActiveMap());
              selectedFile = new File(VueUtil.getCurrentDirectoryPath() + getSelectedMap().toString() + ".txt");
              locationPanel.validate();
            }
            if(choice.getSelectedIndex()==1)
            {
              browsePanel.add(leftBrowsePanel);
              browsePanel.add(rightBrowsePanel);
              generateButton.setEnabled(false);
              locationPanel.validate();
            }
        }
        if(e.getSource() == browseButton)
        {
            try
            {
              file.setText("");
              generateButton.setEnabled(false);
              //selectedFile = ActionUtil.openFile("Choose map file","vue");
              JFileChooser chooseFile = new JFileChooser();
              chooseFile.showDialog(this,"Choose map file");
              selectedFile = chooseFile.getSelectedFile();
              file.setText(selectedFile.getName());
              browseButton.setEnabled(false);
              attachButton.setEnabled(true);
            }
            catch(Exception ex)
            {
              ex.printStackTrace();   
            }
        }
        if(e.getSource() == attachButton)
        {
            try
            {
                setSelectedMap(ActionUtil.unmarshallMap(selectedFile));
                browseButton.setEnabled(true);
                attachButton.setEnabled(false);
                generateButton.setEnabled(true);
            }
            catch(Exception ex)
            {
                ex.printStackTrace();   
            }
    
        }
        
    }   
    
}
