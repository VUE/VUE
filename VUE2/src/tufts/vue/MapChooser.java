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
 * * MapChooser.java
 *
 * Author: dhelle01
 *
 * Created on January 4, 2007, 5:31 PM
 *
 */

package tufts.vue;

//import junit.extensions.ActiveTestSuite;
import tufts.vue.action.ActionUtil;
import tufts.vue.gui.VueFileChooser;
import tufts.vue.gui.VueLabel;


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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class MapChooser extends JDialog implements ActionListener{
    
    public static final int CMPUB_WIDTH = 500;
    public static final int CMPUB_HEIGHT = 350;
    public static final int CMPUB_X_LOCATION = 300;
    public static final int CMPUB_Y_LOCATION = 300;
    public static final String generateMessage = VueResources.getString("dialog.connanalysis");
    public static final String browseMessage =  VueResources.getString("dialog.browsmap");
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
        
        cancelButton = new JButton(VueResources.getString("dialog.connanalysis.cancel"));
        generateButton = new JButton(VueResources.getString("dialog.connanalysis.generate"));
        browseButton = new JButton(VueResources.getString("dialog.connanalysis.browse"));
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
        getRootPane().setDefaultButton(generateButton);
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
        VueLabel info = new VueLabel();
        info.setIcon(VueResources.getIcon("helpIcon.raw"));
        info.setToolTipText(VueResources.getString("ConnectivityMatrix.helpText"));
        JLabel chooseLabel = new JLabel(generateMessage,JLabel.RIGHT);
        browseLabel = new JLabel(browseMessage,JLabel.CENTER);
        String[] choices = {VueResources.getString("dialog.connanalysis.activemap"),VueResources.getString("dialog.connanalysis.mapinfolder")};
        choice = new JComboBox(choices);
        file = new JTextField(2);
        PolygonIcon lineIcon = new PolygonIcon(new Color(153,153,153));
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
        c2.insets = new Insets(0,0,0,10);
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
        c.insets = new Insets(0,0,0,10);
        gridBag.setConstraints(info,c);
        locationPanel.add(info);
        
        c.insets = new Insets(0,0,15,10);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.RELATIVE;
        gridBag.setConstraints(chooseLabel,c);
        locationPanel.add(chooseLabel);
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(choice,c);
        locationPanel.add(choice);
        
        c.weightx = 1.0;
        //c.insets = new Insets(0,0,0,10);
        gridBag.setConstraints(browsePanel,c);
        locationPanel.add(browsePanel);
        
        c.insets = new Insets(0,0,0,10);
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
                    
                    LWMap m = getSelectedMap();
                    
                    // connectivity matrix
                    ConnectivityMatrix matrix = new ConnectivityMatrix(m);
                    String sMatrix = matrix.toString();
                    File temp = File.createTempFile("ConnectivityMatrixTemp",".txt");
                    String fileName = temp.getAbsolutePath();
                    PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
                    writer.write(sMatrix);
                    writer.close();
                    VueUtil.openURL(FILE_URL + fileName);
                    
                    //metadata
                    /*PropertyMap props = m.getMetadata();
                    java.util.Collection c = props.keySet();
                    java.util.Iterator i = c.iterator();
                    System.out.println("about to output metadata: ");
                    while(i.hasNext())
                    {
                        Object key = i.next();
                        System.out.println("mc: metadata key: " + key);
                        System.out.println("mc: value for key: " + props.get(key));
                    }
                    System.out.println("done outputting metadata"); */
                    
                    int nodeCount = m.getChildList().size();
                    
                    java.util.Iterator<LWComponent> i = m.getNodeIterator();
                    
                    
                    int jmax = 0;
                    while(i.hasNext())
                    {
                        LWComponent c = i.next();
                        tufts.vue.filter.NodeFilter filter = c.getNodeFilter();
                        int rows = filter.getRowCount();
                        if(rows>jmax)
                            jmax = rows;
                    }
                    
                    java.util.Iterator<LWComponent> i2 = m.getNodeIterator();
                    
                    String categories = "";
                    String[][] metadata = new String[nodeCount][jmax];
                    int ni = 0;
                    while(i2.hasNext())
                    {
                        LWComponent c = i2.next();
                        tufts.vue.filter.NodeFilter filter = c.getNodeFilter();
                        int rows = filter.getRowCount();
                        int cols = filter.getColumnCount();
                        for(int j=0;j<rows;j++)
                        {
                            for(int k=0;k<cols;k++)
                            {   
                                //System.out.print(filter.getValueAt(j,k) +",");

                                if(k==0)
                                {
                                  categories += filter.getValueAt(j,k);
                                  if(j!=(rows-1))
                                  {
                                    categories+=",";
                                  }
                                }
                                
                                if(k==1)
                                {
                                  metadata[ni][j] = filter.getValueAt(j,k).toString();
                                }
                                
                            }
                            
                            //System.out.println("\n");
                                                        
                        }
                        
                        if(ni<nodeCount-1)
                        {
                           categories += "\n";
                        }
                        ni++;
                    }
                    
                    System.out.println("mc: categories " + categories );
                    
                    if(categories.length() > 0)
                    {
                      File temp2 = File.createTempFile("MetadataCategoriesTemp",".txt");
                      String fileName2 = temp2.getAbsolutePath();
                      PrintWriter writer2 = new PrintWriter(new BufferedWriter(new FileWriter(fileName2)));
                      writer2.write(categories);
                      writer2.close();
                      //VueUtil.openURL(FILE_URL + fileName2);
                    }
                    
                    String md = "";
                    
                    for(int ji = 0;ji<jmax;ji++)
                    {
                        for(int ki=0;ki<nodeCount;ki++)
                        {
                            if(metadata[ki][ji]!=null)
                            {
                                md += metadata[ki][ji];
                            }
                            if(ki!=(nodeCount-1))
                                md +=",";
                        }
                        if(ji < jmax-1)
                            md += System.getProperty("line.separator");
                    }
                    
                    if(md.length() > 0)
                    {
                      File temp3 = File.createTempFile("MetadataValueTemp",".txt");
                      String fileName3 = temp3.getAbsolutePath();
                      PrintWriter writer3 = new PrintWriter(new BufferedWriter(new FileWriter(fileName3)));
                      writer3.write(md);
                      writer3.close();
                      //VueUtil.openURL(FILE_URL + fileName3);
                    }
                    
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
              getRootPane().setDefaultButton(browseButton);
              pack();
            }
        }
        if(e.getSource() == browseButton)
        {
            try
            {
              file.setText("");
              generateButton.setEnabled(false);
              VueFileChooser chooseFile = VueFileChooser.getVueFileChooser();
              chooseFile.setFileFilter(new VueFileFilter(VueFileFilter.VUE_DESCRIPTION));
              chooseFile.showDialog(this,VueResources.getString("dialog.select.title"));
              selectedFile = chooseFile.getSelectedFile();
              if(selectedFile!=null)
              {    
                setSelectedMap(ActionUtil.unmarshallMap(selectedFile));
                file.setText(selectedFile.getName());
                getRootPane().setDefaultButton(generateButton);
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
