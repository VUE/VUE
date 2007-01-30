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

/*
 * * MapChooser.java
 *
 * Author: dhelle01
 *
 * Created on January 4, 2007, 5:31 PM
 *
 */

package tufts.vue;

import junit.extensions.ActiveTestSuite;
import tufts.vue.action.ActionUtil;


import edu.tufts.vue.compare.ConnectivityMatrix;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class MapChooser extends JDialog implements ActionListener{
    
    public static final int CMPUB_WIDTH = 500;
    public static final int CMPUB_HEIGHT = 350;
    public static final int CMPUB_X_LOCATION = 300;
    public static final int CMPUB_Y_LOCATION = 300;
    public static final String generateMessage = "Generate a connectivity matrix from:";
    public static final String browseMessage =   "Browse to map:";
    public static final String FILE_URL = "file://";
    private JPanel locationPanel = null;
    private JButton cancelButton = null;
    private JButton generateButton = null;
    private JPanel buttonPanel = null;
    private JComboBox choice = null;
    private JLabel lineLabel = null;
    private JPanel browsePanel = null;
    private JLabel browseLabel = null;
    private JPanel innerBrowsePanel = null;
    private JTextField file = null;
    private JButton browseButton = null;
    private File selectedFile = null;
    private File currentMapDir = null;
    
    private LWMap selectedMap = null;
   
    public MapChooser(Frame owner,String title) {
        super(owner,title);
        setLocation(CMPUB_X_LOCATION,CMPUB_Y_LOCATION);
        setModal(true);
        setSize(CMPUB_WIDTH,CMPUB_HEIGHT);
        
        cancelButton = new JButton("Cancel");
        generateButton = new JButton("Generate");
        browseButton = new JButton("browse");
        cancelButton.addActionListener(this);
        generateButton.addActionListener(this);
        browseButton.addActionListener(this);
        
        setUpLocationPanel();
        getContentPane().setLayout(new FlowLayout());
        getContentPane().add(locationPanel);
        choice.addActionListener(this);
        
        addWindowListener(new WindowAdapter(){
           public void windowClosing(WindowEvent e)
           {
             setSelectedMap(null);   
           }
        });
        
        setSelectedMap(VUE.getActiveMap());
        
        locationPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        pack();
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
        JLabel chooseLabel = new JLabel(generateMessage,JLabel.RIGHT);
        browseLabel = new JLabel(browseMessage,JLabel.CENTER);
        String[] choices = {"selected map","map in a local folder"};
        choice = new JComboBox(choices);
        file = new JTextField(2);
        PolygonIcon lineIcon = new PolygonIcon(Color.DARK_GRAY);
        lineIcon.setIconWidth(CMPUB_WIDTH-40);
        lineIcon.setIconHeight(1);
        browsePanel = new JPanel();
        //browsePanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        browsePanel.setLayout(new BorderLayout());
        innerBrowsePanel = new JPanel();
        lineLabel = new JLabel(lineIcon);
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(cancelButton);
        buttonPanel.add(generateButton);
        
        // innerBrowsePanel gets added and removed in action for choice
        // but do layout here so as to not have to redo the layout
        // each time
        GridBagLayout innerGridBag = new GridBagLayout();
        GridBagConstraints c2 = new GridBagConstraints();
        innerBrowsePanel.setLayout(innerGridBag);
        c2.weightx= 0.0;
        c2.fill = GridBagConstraints.HORIZONTAL;
        innerGridBag.setConstraints(browseLabel,c2);
        innerBrowsePanel.add(browseLabel);
        c2.weightx = 1.0;
        innerGridBag.setConstraints(file,c2);        
        innerBrowsePanel.add(file);
        c2.weightx = 0.0;
        innerGridBag.setConstraints(browseButton,c2);
        innerBrowsePanel.add(browseButton);
        
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(info,c);
        locationPanel.add(info);
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.RELATIVE;
        gridBag.setConstraints(chooseLabel,c);
        locationPanel.add(chooseLabel);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(choice,c);
        locationPanel.add(choice);
        
        c.weightx = 1.0;
        c.insets = new Insets(10,0,0,0);
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
            try
            {
                if(getSelectedMap()!=null)
                {
                    ConnectivityMatrix matrix = new ConnectivityMatrix(getSelectedMap());
                    String sMatrix = matrix.toString();
                    File temp = File.createTempFile("ConnectivityMatrixTemp",".txt");
                    String fileName = temp.getAbsolutePath();
                    PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
                    writer.write(sMatrix);
                    writer.close();
                    VueUtil.openURL(FILE_URL + fileName);
                }
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
            this.dispose();
        }
        if(e.getSource() == choice)
        {
            if(choice.getSelectedIndex()==0)
            {
              browsePanel.remove(innerBrowsePanel);
              generateButton.setEnabled(true);
              file.setText("");
              setSelectedMap(VUE.getActiveMap());
              pack();
            }
            if(choice.getSelectedIndex()==1)
            {
              browsePanel.add(innerBrowsePanel);
              generateButton.setEnabled(false);
              pack();
            }
        }
        if(e.getSource() == browseButton)
        {
            try
            {
              file.setText("");
              generateButton.setEnabled(false);
              JFileChooser chooseFile = new JFileChooser();
              chooseFile.setFileFilter(new VueFileFilter("vue"));
              chooseFile.showDialog(this,"Choose map file");
              selectedFile = chooseFile.getSelectedFile();
              if(selectedFile!=null)
              {    
                setSelectedMap(ActionUtil.unmarshallMap(selectedFile));
                file.setText(selectedFile.getName());
                generateButton.setEnabled(true);
              }
            }
            catch(Exception ex)
            {
              ex.printStackTrace();   
            }
        }
        
    }   
    
}
