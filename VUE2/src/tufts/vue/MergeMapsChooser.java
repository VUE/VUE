
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
 * MergeMapsChooser.java
 *
 * Created on January 10, 2007, 11:14 AM
 *
 * @author dhelle01
 */

package tufts.vue;

import tufts.vue.action.ActionUtil;
import tufts.vue.gui.VueButton;
import tufts.vue.gui.DockWindow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;

public class MergeMapsChooser extends JPanel
implements VUE.ActiveMapListener,ActionListener
{
    
    private DockWindow p;
    private LWMap activeMap;
    private JPanel selectPanel;
    private JComboBox choice;
    private JButton browseButton;
    private JButton addButton;
    private JPanel mapPanel;
    private JScrollPane listScroll;
    private JPanel basePanel;
    private JComboBox baseChoice;
    private JTextField file;
    private File selectedFile;
    private MapListPanel listPanel;
    private JPanel buttonPanel;
    private JButton generateButton;
    
    public final static String ALL_TEXT = "All maps currently opened";
    public final static String LIST_TEXT = "Browse to maps";
   
    public MergeMapsChooser() 
    {
        VUE.addActiveMapListener(this);
        setLayout(new BorderLayout());
        JTabbedPane mTabbedPane = new JTabbedPane();
        VueResources.initComponent(mTabbedPane,"tabPane");
        
        setUpSelectPanel();
        mTabbedPane.addTab("Select Maps",selectPanel);
        setUpBasePanel();
        //base panel logic is not yet quite complete below
        //have to watch for user selection of non active but visible map
        //mTabbedPane.addTab("Base Map",basePanel);
        
        add(BorderLayout.CENTER,mTabbedPane);
        setActiveMap(VUE.getActiveMap());
        validate();
        setVisible(true);
    }
    
    public void setDockWindow(DockWindow d)
    {
        p = d;
        p.setResizeEnabled(false);
    }
    
    public void setUpSelectPanel()
    {
        selectPanel = new JPanel();
        selectPanel.setLayout(new BoxLayout(selectPanel,BoxLayout.Y_AXIS));
        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        
        String selectMessage = "Select Maps to merge:";
        final JLabel selectLabel = new JLabel(selectMessage);
        String [] choices = {ALL_TEXT,LIST_TEXT};
        choice = new JComboBox(choices);
        JLabel helpLabel = new JLabel(VueResources.getIcon("helpIcon.raw"));
        
        JPanel innerPanel = new JPanel()
        {
            public Dimension getMaximumSize()
            {
                if(p!=null && selectLabel.getHeight() > 0)
                 return new Dimension(p.getWidth(),selectLabel.getHeight());
                else
                 return new Dimension(400,30);
            }
        };
        innerPanel.setLayout(gridBag);
        
        choice.addActionListener(this);        
        
        c.weightx = 0.0;
        c.weighty = 1.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        gridBag.setConstraints(selectLabel,c);
        innerPanel.add(selectLabel);
        gridBag.setConstraints(choice,c);
        innerPanel.add(choice);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(helpLabel,c);
        innerPanel.add(helpLabel);
        
        selectPanel.add(innerPanel);
        
        setUpSelectPanelBrowse();
        
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        generateButton = new JButton("Generate Map");
        buttonPanel.add(generateButton);
        selectPanel.add(buttonPanel);
        
        browseButton.addActionListener(this);
        addButton.addActionListener(this);
        generateButton.addActionListener(this);
        
    }
    
    public void setUpSelectPanelBrowse()
    {
        String mapMessage = "Maps:";
        JLabel mapLabel = new JLabel(mapMessage);
        file = new JTextField(15);
        browseButton = new JButton("Browse");
        addButton = new JButton("Add");
        addButton.setEnabled(false);
        
        mapPanel = new JPanel();
                
        mapPanel.add(mapLabel);
        mapPanel.add(file);
        mapPanel.add(browseButton);
        mapPanel.add(addButton);
        
        listPanel = new MapListPanel();
        //listPanel.setLayout(new javax.swing.BoxLayout(listPanel,javax.swing.BoxLayout.Y_AXIS));
        listPanel.setLayout(new java.awt.GridLayout(0,1));
        listScroll = new JScrollPane(listPanel);    
    }
    
    public void setUpBasePanel()
    {
        basePanel = new JPanel();
        String baseMessage = "Select base map:";
        JLabel baseLabel = new JLabel(baseMessage);
        String[] choices = {VUE.getActiveMap().getLabel(),"other"};
        baseChoice = new JComboBox(choices);
        JLabel helpLabel = new JLabel(VueResources.getIcon("helpIcon.raw"));
        basePanel.add(baseLabel);
        basePanel.add(baseChoice);
        basePanel.add(helpLabel);
    }
    
    public void refreshBaseChoices()
    {
        String otherString = "other";
        boolean otherSelected = false;
        if(baseChoice.getSelectedItem() != null && baseChoice.getSelectedItem().equals("other"))
        {
            otherSelected = true;
        }
        baseChoice.removeAllItems();   
        baseChoice.addItem(getActiveMap().getLabel());        
        baseChoice.addItem(otherString);
        java.util.Iterator<LWMap> i = VUE.getLeftTabbedPane().getAllMaps();
        while(i.hasNext()) 
        {
           LWMap map = i.next();
           String id = map.getID();
           String actid = getActiveMap().getID();
           LWMap am = getActiveMap();
           if(map!=am)
               baseChoice.addItem(map.getLabel());
           else
               System.out.println("refresh base choices:" + map);
        }
        if(otherSelected)
        {
            baseChoice.setSelectedItem(otherString);
        }
        basePanel.validate();
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource()==choice)
        {
            if(choice.getSelectedIndex()==0)
            {
                selectPanel.remove(mapPanel);
                selectPanel.remove(listScroll);
                p.pack();
            }
            if(choice.getSelectedIndex()==1)
            {
                if(mapPanel==null)
                {
                    setUpSelectPanelBrowse();
                }
                else
                {
                    selectPanel.remove(buttonPanel);
                    selectPanel.add(mapPanel);
                    selectPanel.add(listScroll);
                    selectPanel.add(buttonPanel);
                }
                p.pack();
            }
        }
        if(e.getSource() == browseButton)
        {
            JFileChooser choose = new JFileChooser();
            choose.showDialog(this,"Add Map");
            selectedFile = choose.getSelectedFile();
            if(selectedFile != null)
            {    
              file.setText(selectedFile.getName());
              addButton.setEnabled(true);
            }
        }
        if(e.getSource() == addButton)
        {
            LWMap map = null;
            try
            {        
              map = ActionUtil.unmarshallMap(selectedFile);
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
            MapListElementPanel mlep = new MapListElementPanel(map);
            mlep.adjustColor(listPanel.getComponentCount());
            listPanel.add(mlep);
            addButton.setEnabled(false);
            file.setText("");
            validate();
        }
        if(e.getSource() == generateButton)
        {
            LWMergeMap map = new LWMergeMap(LWMergeMap.getTitle());
            //temporary local variable until base map tab is working:
            LWMap baseMap = VUE.getActiveMap();
            if(choice.getSelectedItem().equals(ALL_TEXT))
            {
               map.addMaps(VUE.getLeftTabbedPane().getAllMaps());  
            }
            else if(choice.getSelectedItem().equals(LIST_TEXT))
            {
               ArrayList<LWMap> listPanelMaps = new ArrayList<LWMap> ();
               for(int i=0;i<listPanel.getComponentCount();i++)
               {
                   MapListElementPanel mlep = (MapListElementPanel)listPanel.getComponent(i);
                   if(mlep.isActive())
                   {
                     listPanelMaps.add(mlep.getMap());
                   }
               }
               map.addMaps(listPanelMaps.iterator());
               //also temporary until get base map tab working
               if(listPanel.getComponentCount()>0)
               {
                   baseMap = ((MapListElementPanel)listPanel.getComponent(1)).getMap();
               }
            }
            map.setSelectionText((String)choice.getSelectedItem());
            //TBD (see above): change this to reflect base tab
            map.setBaseMap(baseMap);
            map.mergeMaps();
            VUE.displayMap(map);
        }
    }
    
    public void setActiveMap(LWMap map)
    {
        activeMap = map;
        refreshBaseChoices();
    }
    
    public LWMap getActiveMap()
    {
        return activeMap;
    }
    
    public void activeMapChanged(LWMap map)
    {
        setActiveMap(map);
    }
    
}
