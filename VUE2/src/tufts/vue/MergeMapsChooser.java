
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

import junit.extensions.ActiveTestSuite;
import tufts.vue.action.ActionUtil;
import tufts.vue.gui.VueButton;
import tufts.vue.gui.DockWindow;

import edu.tufts.vue.compare.ConnectivityMatrix;
import edu.tufts.vue.compare.VoteAggregate;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class MergeMapsChooser extends JPanel
implements VUE.ActiveMapListener,ActionListener,ChangeListener
{
    
    private static DockWindow p;
    private LWMap activeMap;
    
    private JPanel selectPanelHolder;
    //private JPanel selectPanel;
    private SelectPanel sp;
    //private JComboBox choice;
    //private JButton browseButton;
    //private JButton addButton;
    //private JPanel mapPanel;
    //private JScrollPane listScroll;
    //private JTextField file;
    private File selectedFile;
    //private MapListPanel listPanel;
    //private JPanel buttonPanel;
    //private JButton generateButton;
    
    private JPanel basePanel;
    private JPanel baseBrowsePanel;
    private JButton baseBrowseButton;
    private JTextField baseFileField;
    private JComboBox baseChoice;
    private LWMap baseMap;
    
    private JPanel vizPanel;
    private JSlider nodeThresholdSlider;
    private JLabel percentageDisplay;
    private JSlider linkThresholdSlider;
    private JLabel linkPercentageDisplay;
    
    List<LWMap> mapList = new ArrayList<LWMap>();
    
    private HashMap <LWMap,SelectPanel> selectPanels = new HashMap<LWMap,SelectPanel>();
    
    public final static String ALL_TEXT = "All maps currently opened";
    public final static String LIST_TEXT = "Browse to maps";
    public final static String SELECT_MESSAGE = "Select Maps to merge:";
   
    public MergeMapsChooser() 
    {
        VUE.addActiveMapListener(this);
        setLayout(new BorderLayout());
        JTabbedPane mTabbedPane = new JTabbedPane();
        VueResources.initComponent(mTabbedPane,"tabPane");
        
        //setUpSelectPanel();
        selectPanelHolder = new JPanel();
        sp = new SelectPanel();
        if(activeMap != null)
        {    
          selectPanels.put(getActiveMap(),sp);
        }
        selectPanelHolder.add(sp);
        mTabbedPane.addTab("Select Maps",selectPanelHolder);
        setUpBasePanel();
        mTabbedPane.addTab("Base Map",basePanel);
        setUpVizPanel();
        mTabbedPane.addTab("Visualization Settings",vizPanel);
        
        add(BorderLayout.CENTER,mTabbedPane);
        setActiveMap(VUE.getActiveMap());
        validate();
        setVisible(true);
    }
    
    public static void setDockWindow(DockWindow d)
    {
        p = d;
        p.setResizeEnabled(false);
    }
    
    public static DockWindow getDockWindow()
    {
        return p;
    }
    
    /*public void setUpSelectPanel()
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
        
        JLabel leftPadding = new JLabel()
        {
            public Dimension getPreferredSize()
            {
               return new Dimension(30,30); 
            }
        };
        JLabel rightPadding = new JLabel()
        {
            public Dimension getPreferredSize()
            {
               return new Dimension(30,30); 
            }
        };
        c.weightx = 0.0;
        c.weighty = 1.0;
        gridBag.setConstraints(leftPadding,c);
        c.anchor = GridBagConstraints.WEST;
        gridBag.setConstraints(selectLabel,c);
        innerPanel.add(leftPadding);
        innerPanel.add(selectLabel);
        c.insets = new Insets(0,5,0,0);
        c.anchor = GridBagConstraints.SOUTH;
        gridBag.setConstraints(choice,c);
        innerPanel.add(choice);
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0,5,0,0);
        gridBag.setConstraints(helpLabel,c);
        innerPanel.add(helpLabel);
        c.gridwidth = GridBagConstraints.REMAINDER;
        gridBag.setConstraints(rightPadding,c);
        innerPanel.add(rightPadding);
        
        selectPanel.add(innerPanel);
        
        setUpSelectPanelBrowse();
        
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        generateButton = new JButton("Generate Map");
        buttonPanel.add(generateButton);
        selectPanel.add(buttonPanel);
        
        browseButton.addActionListener(this);
        addButton.addActionListener(this);
        generateButton.addActionListener(this);
        
    }*/
    
    /*public void setUpSelectPanelBrowse()
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
        
        //may be interfereing with listPanel load and deletes with call from event thread
        listPanel = new MapListPanel();
        //listPanel.setLayout(new javax.swing.BoxLayout(listPanel,javax.swing.BoxLayout.Y_AXIS));
        listPanel.setLayout(new java.awt.GridLayout(0,1));
        listScroll = new JScrollPane(listPanel);    
    }*/
    
    public void setUpBasePanel()
    {
        GridBagLayout baseGridBag = new GridBagLayout();
        GridBagConstraints baseConstraints = new GridBagConstraints();
        basePanel = new JPanel();
        basePanel.setLayout(new BoxLayout(basePanel,BoxLayout.Y_AXIS));
        JPanel baseInnerPanel = new JPanel()
        {
            public Dimension getMaximumSize()
            {
                 return new Dimension(400,30);
            }
        };
        baseInnerPanel.setLayout(baseGridBag);
        String baseMessage = "Select base map:";
        JLabel baseLabel = new JLabel(baseMessage);
        String[] choices = {VUE.getActiveMap().getLabel(),"other"};
        baseChoice = new JComboBox(choices);
        baseChoice.setRenderer(new MapChoiceCellRenderer());
        JLabel helpLabel = new JLabel(VueResources.getIcon("helpIcon.raw"));
        baseGridBag.setConstraints(baseLabel,baseConstraints);
        baseInnerPanel.add(baseLabel);
        baseGridBag.setConstraints(baseChoice,baseConstraints);
        baseInnerPanel.add(baseChoice);
        baseConstraints.gridwidth = GridBagConstraints.REMAINDER;
        baseGridBag.setConstraints(helpLabel,baseConstraints);
        baseInnerPanel.add(helpLabel);
        basePanel.add(baseInnerPanel);
        setUpBasePanelBrowse();
        baseChoice.addActionListener(this);
        baseBrowseButton.addActionListener(this);
    }
    
    public void setUpBasePanelBrowse()
    {
        baseBrowsePanel = new JPanel();
        GridBagLayout baseBrowseGridBag = new GridBagLayout();
        baseBrowsePanel.setLayout(baseBrowseGridBag);
        GridBagConstraints baseBrowseConstraints = new GridBagConstraints();
        JLabel basePanelMapLabel = new JLabel("Map:"); 
        baseBrowseConstraints.fill = GridBagConstraints.HORIZONTAL;
        baseBrowseGridBag.setConstraints(basePanelMapLabel,baseBrowseConstraints);
        baseBrowsePanel.add(basePanelMapLabel);
        baseFileField = new JTextField();
        baseBrowseConstraints.weightx = 1.0;
        baseBrowseGridBag.setConstraints(baseFileField,baseBrowseConstraints);
        baseBrowsePanel.add(baseFileField);
        baseBrowseButton = new JButton("Browse");
        baseBrowseConstraints.weightx = 0.0;
        baseBrowseGridBag.setConstraints(baseBrowseButton,baseBrowseConstraints);
        baseBrowsePanel.add(baseBrowseButton);
    }
    
    public void refreshBaseChoices()
    {
        String otherString = "other";
        boolean otherSelected = false;
        if( baseChoice.getSelectedItem() != null && baseChoice.getSelectedItem().equals("other") )
        {
            otherSelected = true;
        }
        Object currentSelection = null;
        if(baseChoice != null && !otherSelected)
        {
            currentSelection = baseChoice.getSelectedItem();
        }
        baseChoice.removeAllItems();
        LWMap activeMap = getActiveMap();
        if(!(activeMap instanceof LWMergeMap))
        {
          baseChoice.addItem(activeMap);
        }
        baseChoice.addItem(otherString);
        java.util.Iterator<LWMap> i = VUE.getLeftTabbedPane().getAllMaps();
        while(i.hasNext()) 
        {
           LWMap map = i.next();
           if(map!=activeMap && !(map instanceof LWMergeMap))
           {
               baseChoice.addItem(map);
           }
        }
        /*if(otherSelected)
        {
            baseChoice.setSelectedItem(otherString);
        }
        else
        if(activeMap instanceof LWMergeMap)
        {
            baseChoice.setSelectedItem(((LWMergeMap)activeMap).getBaseMap());
        }
        else
        {
            baseChoice.setSelectedIndex(0);
        }*/
        
    }
    
    public void setUpVizPanel()
    {
        vizPanel = new JPanel();
        GridBagLayout vizLayout = new GridBagLayout();
        GridBagConstraints vizConstraints = new GridBagConstraints();
        vizPanel.setLayout(vizLayout);
        String[] vizChoices = {"Vote","Weight"};
        JComboBox vizChoice = new JComboBox(vizChoices);
        vizConstraints.gridwidth = GridBagConstraints.REMAINDER;
        vizLayout.setConstraints(vizChoice,vizConstraints);
        vizPanel.add(vizChoice);
        nodeThresholdSlider = new JSlider(0,100,50);
        nodeThresholdSlider.setPaintTicks(true);
        nodeThresholdSlider.setMajorTickSpacing(10);
        nodeThresholdSlider.setPaintLabels(true);
        java.util.Dictionary labels = nodeThresholdSlider.getLabelTable();
        java.util.Enumeration e = labels.elements();
        while(e.hasMoreElements())
        {
            Object label = e.nextElement();
            if(label instanceof javax.swing.JComponent)
            {
                ((javax.swing.JComponent)label).setFont(new Font("Courier",Font.PLAIN,9));
            }
        }
        JLabel nodeLabel = new JLabel("Nodes:");
        vizConstraints.gridwidth = GridBagConstraints.RELATIVE;
        vizLayout.setConstraints(nodeLabel,vizConstraints);
        vizPanel.add(nodeLabel);
        vizLayout.setConstraints(nodeThresholdSlider,vizConstraints);
        vizPanel.add(nodeThresholdSlider);
        percentageDisplay = new JLabel(nodeThresholdSlider.getValue()+ "%");
        nodeThresholdSlider.addChangeListener(this);
        vizConstraints.gridwidth = GridBagConstraints.REMAINDER;
        vizLayout.setConstraints(percentageDisplay,vizConstraints);
        vizPanel.add(percentageDisplay);
        
        linkThresholdSlider = new JSlider(0,100,50);
        linkThresholdSlider.setPaintTicks(true);
        linkThresholdSlider.setMajorTickSpacing(10);
        linkThresholdSlider.setPaintLabels(true);
        java.util.Dictionary linkLabels = linkThresholdSlider.getLabelTable();
        java.util.Enumeration le = linkLabels.elements();
        while(le.hasMoreElements())
        {
            Object linkLabel = le.nextElement();
            if(linkLabel instanceof javax.swing.JComponent)
            {
                ((javax.swing.JComponent)linkLabel).setFont(new Font("Courier",Font.PLAIN,9));
            }
        }
        JLabel linkPanel = new JLabel("Links:");
        vizConstraints.gridwidth = GridBagConstraints.RELATIVE;
        vizLayout.setConstraints(linkPanel,vizConstraints);
        vizPanel.add(linkPanel);
        vizLayout.setConstraints(linkThresholdSlider,vizConstraints);
        vizPanel.add(linkThresholdSlider);
        linkPercentageDisplay = new JLabel(linkThresholdSlider.getValue()+"%");
        linkThresholdSlider.addChangeListener(this);
        vizConstraints.gridwidth = GridBagConstraints.REMAINDER;
        vizLayout.setConstraints(linkPercentageDisplay,vizConstraints);
        vizPanel.add(linkPercentageDisplay);
    }
    
    public void stateChanged(ChangeEvent e)
    {
        if(e.getSource()==nodeThresholdSlider)
        {
            percentageDisplay.setText(nodeThresholdSlider.getValue() + "%");
        }
        if(e.getSource()==linkThresholdSlider)
        {
            linkPercentageDisplay.setText(linkThresholdSlider.getValue() + "%");
        }
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if(p==null)
            return;
        /*if(e.getSource()==choice)
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
        }*/
        /*if(e.getSource() == browseButton)
        {
            JFileChooser choose = new JFileChooser();
            choose.showDialog(this,"Add Map");
            selectedFile = choose.getSelectedFile();
            if(selectedFile != null)
            {    
              file.setText(selectedFile.getName());
              addButton.setEnabled(true);
            }
        }*/
        /*if(e.getSource() == addButton)
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
        }*/
        if(e.getSource()==baseChoice)
        {
          // comment back in to see focus error (and to enable browse to base map)  
          /*  if(baseChoice.getSelectedItem().equals("other"));
            {    
              if(baseBrowsePanel==null)
              {
                  setUpBasePanelBrowse();
              }
              //basePanel.add(baseBrowsePanel);
              //basePanel.revalidate();
              //basePanel.repaint();
              if(p!=null)
              {
                p.pack();
              }
            }
            if(!baseChoice.getSelectedItem().equals("other"))
            {
              //basePanel.remove(baseBrowsePanel);
              //basePanel.revalidate();
              //basePanel.repaint();
              if(p!=null)
              {
                p.pack();
              }
            }*/
        }
        if(e.getSource() == baseBrowseButton)
        {
            JFileChooser choose = new JFileChooser();
            choose.showDialog(this,"SetBase Map");
            File selectedBaseFile = choose.getSelectedFile();
            if(selectedBaseFile != null)
            {    
              baseFileField.setText(selectedBaseFile.getName());
              try
              {        
                baseMap = ActionUtil.unmarshallMap(selectedFile);
              }
              catch(Exception ex)
              {
                ex.printStackTrace();
              } 
            }
        }
        /*if(e.getSource() == generateButton)
        {
            //mapList.clear();
            LWMergeMap map = new LWMergeMap(LWMergeMap.getTitle());
            //fail safe default value for base map is active map
            if(baseMap == null)
            {
              baseMap = VUE.getActiveMap();
            }
            Object baseMapObject = baseChoice.getSelectedItem();
            if(baseMapObject instanceof LWMap)
                baseMap = (LWMap)baseMapObject;
            if(choice.getSelectedItem().equals(ALL_TEXT))
            {
               Iterator <LWMap> i = VUE.getLeftTabbedPane().getAllMaps();
               while(i.hasNext())
               {
                   mapList.add(i.next());
               }
               map.setSelectChoice("all");
            }
            else if(choice.getSelectedItem().equals(LIST_TEXT))
            {
               ArrayList<LWMap> listPanelMaps = new ArrayList<LWMap> ();
               ArrayList<File> mapFileList = new ArrayList<File> ();
               ArrayList<Boolean> activeFileList = new ArrayList<Boolean> ();
               for(int i=0;i<listPanel.getComponentCount();i++)
               {
                   MapListElementPanel mlep = (MapListElementPanel)listPanel.getComponent(i);
                   if(mlep.isActive())
                   {
                     listPanelMaps.add(mlep.getMap());
                   }
                   mapFileList.add(mlep.getMap().getFile());
                   activeFileList.add(new Boolean(mlep.isActive())); 
               }
               mapList.addAll(listPanelMaps);
               map.setMapFileList(mapFileList);
               map.setActiveMapList(activeFileList);
               map.setSelectChoice("list");
            }

            map.setNodeThresholdSliderValue(nodeThresholdSlider.getValue());
            map.setLinkThresholdSliderValue(linkThresholdSlider.getValue());
            mergeMaps(map);
            VUE.displayMap(map);
        } */
    }
    
    public void mergeMaps(LWMergeMap map)
    {
        ArrayList<ConnectivityMatrix> cms = new ArrayList<ConnectivityMatrix>();
        Iterator<LWMap> i = mapList.iterator();
        while(i.hasNext())
        {
          cms.add(new ConnectivityMatrix(i.next()));
        }
        VoteAggregate voteAggregate= new VoteAggregate(cms);
        voteAggregate.setNodeThreshold((double)(nodeThresholdSlider.getValue()/100.0));
        voteAggregate.setLinkThreshold((double)(linkThresholdSlider.getValue()/100.0));
        
        //compute and create nodes in Merge Map
        Iterator children = baseMap.getNodeIterator();
        while(children.hasNext()) {
           LWComponent comp = (LWComponent)children.next();
           if(voteAggregate.isNodeVoteAboveThreshold(comp.getLabel())) {
                   LWNode node = (LWNode)comp.duplicate();
                   map.addNode(node);
           }
        }
        
        //compute and create links in Merge Map
        Iterator children1 = map.getNodeIterator();
        while(children1.hasNext()) {
           LWNode node1 = (LWNode)children1.next();
           Iterator children2 = map.getNodeIterator();
           while(children2.hasNext()) {
               LWNode node2 = (LWNode)children2.next();
               if(node2 != node1) {
                  int c = voteAggregate.getConnection(node1.getLabel(),node2.getLabel());
                  if(c >0) {
                     map.addLink(new LWLink(node1,node2));
                  }
               }
           }
        }
        
    }

    /*public String getALL_TEXT() {
        return ALL_TEXT;
    }*/
    
    public void refreshSettings(final LWMergeMap map)
    {
        /*if(map.getSelectChoice().equals("all"))
        {
            choice.setSelectedIndex(0);
        }
        if(map.getSelectChoice().equals("list"))
        {
            choice.setSelectedIndex(1);   
        }*/
        nodeThresholdSlider.setValue(map.getNodeThresholdSliderValue());
        linkThresholdSlider.setValue(map.getLinkThresholdSliderValue());
    }
    
    public void setActiveMap(LWMap map)
    {
        /*String selectString = "Select";
        if(map==null)
        {
            baseChoice.removeAllItems();
            baseChoice.addItem(SelectString);
            return;
        }   
        else
        {
            baseChoice.removeItem(selectString);
        }*/
        if(map==null)
        {
            return;
        }
        LWMap previousMap = activeMap;
        activeMap = map;
        refreshBaseChoices();

        if(map instanceof LWMergeMap)
        {
           refreshSettings((LWMergeMap)map);
        }

        selectPanelHolder.remove(sp);
        if(selectPanels.containsKey(activeMap))
        {
            sp = (SelectPanel)selectPanels.get(activeMap);
        }
        else if(activeMap instanceof LWMergeMap)
        {
            if(previousMap!=null)
            {
              sp = (SelectPanel)selectPanels.get(previousMap);
            }
            else
            {
                sp = new SelectPanel();
            }
        }
        else
        {
            //selectPanel = new JPanel();
            //setUpSelectPanel();
            sp = new SelectPanel();
        }
        selectPanelHolder.add(sp);
        selectPanels.put(activeMap,sp);
        selectPanelHolder.repaint();
        if(p!=null)
        {    
          p.pack();
        }
        
        System.out.println("Merge Maps Chooser: " + selectPanels.get(activeMap));
        System.out.println("Merge Maps Chooser: " + selectPanels.size());
    }
    
    public LWMap getActiveMap()
    {
        return activeMap;
    }
    
    public void activeMapChanged(LWMap map)
    {
        setActiveMap(map);
    }
    
    private class SelectPanel extends JPanel implements ActionListener
    {
        private JComboBox choice;
        private JTextField fileField;
        private JButton browseButton;
        private JButton addButton;
        private MapListPanel listPanel;
        private JButton generateButton;
        
        private JPanel topPanel;
        private JPanel browsePanel;
        private JPanel bottomPanel;
        
        private File selectedFile;
        
        public SelectPanel()
        {
            
            setUpTopPanel();
            setUpBrowsePanel();
            setUpBottomPanel();
           
            setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
            add(topPanel);
            //choice combobox will display browsePanel (see below)
            add(bottomPanel);
            
            setOpaque(true);
            setBackground(new Color(100,100,255));
        }
        
        public void setUpTopPanel()
        {
            topPanel = new JPanel();
                    
            JLabel messageLabel = new JLabel(SELECT_MESSAGE);
            
            String[] choices = {ALL_TEXT,LIST_TEXT};
            choice = new JComboBox(choices);
            choice.addActionListener(this);
            
            topPanel.add(messageLabel);
            topPanel.add(choice);   
        }
        
        public void setUpBrowsePanel()
        {
            browsePanel = new JPanel();
            GridBagLayout gridBag = new GridBagLayout();
            browsePanel.setLayout(gridBag);
            GridBagConstraints c = new GridBagConstraints();
            
            fileField = new JTextField("");
            fileField.setEnabled(false);
            browseButton = new JButton("Browse");
            addButton = new JButton("Add");
            addButton.setEnabled(false);
            listPanel = new MapListPanel();
       
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            gridBag.setConstraints(fileField,c);
            browsePanel.add(fileField);
            c.weightx = 0.0;
            gridBag.setConstraints(browseButton,c);
            browsePanel.add(browseButton);
            c.weightx = 0.0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            gridBag.setConstraints(addButton,c);
            browsePanel.add(addButton);
            //gridBag.setConstraints(listPanel,c);
            JScrollPane listScroll = new JScrollPane(listPanel);
            gridBag.setConstraints(listScroll,c);
            browsePanel.add(listScroll);
            
            browseButton.addActionListener(this);
            addButton.addActionListener(this);
            
        }
        
        public void setUpBottomPanel()
        {
            bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            generateButton = new JButton("Generate");
            bottomPanel.add(generateButton);
            generateButton.addActionListener(this);
        }
        
        public void actionPerformed(ActionEvent e)
        {
            if(e.getSource() == choice)
            {
                if(choice.getSelectedItem().equals(ALL_TEXT))
                {
                    remove(bottomPanel);
                    remove(browsePanel);
                    if(p!=null)
                    {
                        p.pack();
                    }
                    add(bottomPanel);
                }
                else if(choice.getSelectedItem().equals(LIST_TEXT))
                {
                    remove(bottomPanel);
                    add(browsePanel);
                    if(p!=null)
                    {
                        p.pack();
                    }
                    add(bottomPanel);
                }
                //toggleBackgroundColor();
            }
            if(e.getSource() == browseButton)
            {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.showDialog(this,"Add Map");
                selectedFile = fileChooser.getSelectedFile();       
                if(selectedFile != null)
                {
                    fileField.setText(selectedFile.getName());
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
                fileField.setText(""); 
            }
            if(e.getSource() == generateButton)
            {
               LWMergeMap map = new LWMergeMap(LWMergeMap.getTitle());
               //fail safe default value for base map is active map
               if(baseMap == null)
               {
                 baseMap = VUE.getActiveMap();
               }
               Object baseMapObject = baseChoice.getSelectedItem();
               if(baseMapObject instanceof LWMap)
                 baseMap = (LWMap)baseMapObject;
               if(choice.getSelectedItem().equals(ALL_TEXT))
               {
                 Iterator <LWMap> i = VUE.getLeftTabbedPane().getAllMaps();
                 while(i.hasNext())
                 {
                   mapList.add(i.next());
                 }
                 map.setSelectChoice("all");
               }
               else if(choice.getSelectedItem().equals(LIST_TEXT))
               {
                 ArrayList<LWMap> listPanelMaps = new ArrayList<LWMap> ();
                 ArrayList<File> mapFileList = new ArrayList<File> ();
                 ArrayList<Boolean> activeFileList = new ArrayList<Boolean> ();
                 for(int i=0;i<listPanel.getComponentCount();i++)
                 {
                   MapListElementPanel mlep = (MapListElementPanel)listPanel.getComponent(i);
                   if(mlep.isActive())
                   {
                     listPanelMaps.add(mlep.getMap());
                   }
                   mapFileList.add(mlep.getMap().getFile());
                   activeFileList.add(new Boolean(mlep.isActive())); 
                 }
                 mapList.addAll(listPanelMaps);
                 map.setMapFileList(mapFileList);
                 map.setActiveMapList(activeFileList);
                 map.setSelectChoice("list");
               }

               map.setNodeThresholdSliderValue(nodeThresholdSlider.getValue());
               map.setLinkThresholdSliderValue(linkThresholdSlider.getValue());
               mergeMaps(map);
               VUE.displayMap(map);
            }
            validate();
            if(p!=null)
            {
                p.pack();
            }
        }
        
    }
    
    /*private class BaseMapPanel extends JPanel implements ActionListener
    {
        
        private JComboBox choice;
        private JTextField fileField;
        private JButton browseButton;
        private JButton generateButton;
        
        
        public void actionPerformed(ActionEvent e)
        {
            
        }
    }*/
    
}
