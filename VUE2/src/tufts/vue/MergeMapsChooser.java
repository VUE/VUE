
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
 * MergeMapsChooser.java
 *
 * Created on January 10, 2007, 11:14 AM
 *
 * @author Daniel J. Heller
 */

package tufts.vue;

import edu.tufts.vue.compare.ui.BaseMapChoiceSelector;
import java.awt.geom.Point2D;
import tufts.vue.action.ActionUtil;
import tufts.vue.gui.VueButton;
import tufts.vue.gui.DockWindow;
import tufts.vue.gui.VueFileChooser;

import edu.tufts.vue.compare.ConnectivityMatrix;
import edu.tufts.vue.compare.VoteAggregate;
import edu.tufts.vue.compare.Util;
import edu.tufts.vue.compare.WeightAggregate;
import edu.tufts.vue.compare.ui.WeightVisualizationSettingsPanel;

import edu.tufts.vue.style.*;

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
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
implements ActiveListener<LWMap>, ActionListener,ChangeListener,LWComponent.Listener
{
    
    private static DockWindow p;
    private LWMap activeMap;
    
    private JPanel selectPanelHolder;
    private SelectPanel sp;
    private File selectedBaseFile;
    
    private JPanel basePanel;
    private JPanel baseBrowsePanel;
    private JButton baseBrowseButton;
    private JTextField baseFileField;
    //private JComboBox baseChoice;
    private BaseMapChoiceSelector baseChoice;
    private LWMap baseMap;
    private JPanel buttonPane;
    private JButton generate;
    
    private JPanel vizPane;
    private JPanel vizPanel;
    private JLabel vizLabel;
    private JComboBox vizChoice;
    private JCheckBox filterChoice;
    private JPanel votePanel;
    private WeightVisualizationSettingsPanel weightPanel;
    private JSlider nodeThresholdSlider;
    private boolean nodeChangeProgrammatic;
    private boolean mousePressed;
    private JLabel percentageDisplay;
    private JSlider linkThresholdSlider;
    private JLabel linkPercentageDisplay;
    private List<Double> intervalBoundaries;
    
    private List<LWMap> mapList = new ArrayList<LWMap>();
    
    private HashMap <LWMap,SelectPanel> selectPanels = new HashMap<LWMap,SelectPanel>();
    
    private GridBagLayout baseGridBag;
    private JLabel baseLabel;
      
    private JTabbedPane vueTabbedPane = VUE.getTabbedPane();
    
    private JButton closeButton = new JButton("Close");
    private JButton undoButton = new JButton("Undo");
    private int undoCount;
    
    public final static String ALL_TEXT = "All maps currently opened";
    public final static String LIST_TEXT = "Browse to maps";
    public final static String SELECT_MESSAGE = "Select Maps to merge:";
    
    public final static String defineThresholdMessage = "Define threshold for nodes and links:";
    
    public final int ALL_OPEN_CHOICE = 0;
    public final int FILE_LIST_CHOICE = 1;
    
    public final int BASE_FROM_LIST = 0;
    public final int BASE_FROM_BROWSE = 1;
    
    public final String otherString = "other";
    public static final String BASE_SELECT_STRING = "Select";
    public static final String BASE_OTHER_STRING = "Other";
    
    public final int TAB_BORDER_SIZE = 20;
    
    
    //public static final MMCKey KEY_NODE_CHANGE = new MMCKey("nodeThresholdSliderValue", "integer");
    //public static final MMCKey KEY_LINK_CHANGE = new MMCKey("linkThresholdSliderValue", "integer");
    
    public static final MMCKey KEY_NODE_CHANGE = new MMCKey("nodeThresholdSliderValue");
    public static final MMCKey KEY_LINK_CHANGE = new MMCKey("linkThresholdSliderValue");
    // will require a refill (recreate) for weight maps as well..
    // public static final MMCKey KEY_FILTER_CHANGE = new MMCKey("filterChoiceChange");
    
    //$
     //static int c = 0;
    //$
   
    public MergeMapsChooser() 
    {
        
        closeButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
              //setVisible(false);
              p.dispose();
              setDockWindow(null);
              //p.dispose();
            }
        });
        
        undoButton.addActionListener(new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
             //VUE.getUndoManager().undo();
             VUE.getActiveMap().getUndoManager().undo();
             if(VUE.getActiveMap() instanceof LWMergeMap)
             {
                LWMergeMap am = (LWMergeMap)VUE.getActiveMap();
                am.setNodeThresholdSliderValue(nodeThresholdSlider.getValue());
                am.setLinkThresholdSliderValue(linkThresholdSlider.getValue());
                //am.setFilterOnBaseMap(filterChoice.isSelected());
             }
             if(undoCount == 1)
             {
                VUE.getActiveMap().getUndoManager().flush();
                undoButton.setEnabled(false);
                undoCount = 0;
             }
             else
             {
                undoCount--;
             }
          }
        });
        
        vueTabbedPane.addChangeListener(this);
        loadDefaultStyle();
        VUE.addActiveListener(LWMap.class, this);
        setLayout(new BorderLayout());
        buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        generate = new JButton("Generate");
        //buttonPane.add(generate);
        buttonPane.add(closeButton);
        buttonPane.add(undoButton);
        buttonPane.add(generate);
        undoButton.setEnabled(false);
        
        JTabbedPane mTabbedPane = new JTabbedPane();
        VueResources.initComponent(mTabbedPane,"tabPane");
        
        selectPanelHolder = new JPanel();
        sp = new SelectPanel();
        //provides a fast means for reloading LWMergeMap settings
        //if re-enabled should be done only for LWMergeMap
        //default behavior is to keep settings loaded when plain
        //LWMap is selected so as to be able to resuse these settings
        /*if(activeMap != null)
        {    
          selectPanels.put(getActiveMap(),sp);
        }*/
        selectPanelHolder.add(sp);
        mTabbedPane.addTab("Select Maps",selectPanelHolder);
        setUpBasePanel();
        mTabbedPane.addTab("Base Map",basePanel);
        setIntervalBoundaries();
        setUpVizPanel();
        vizPane = new JPanel();
        BoxLayout vizPaneLayout = new BoxLayout(vizPane,BoxLayout.Y_AXIS);
        vizPane.setLayout(vizPaneLayout);
        vizPane.add(vizPanel);
        vizPane.add(votePanel);
        mTabbedPane.addTab("Visualization Settings",vizPane);
        
        add(BorderLayout.CENTER,mTabbedPane);
        add(BorderLayout.SOUTH,buttonPane);
        setActiveMap(VUE.getActiveMap());
        
        vizChoice.addActionListener(this);

        generate.addActionListener(this);
        
        //actually weight panel settings .. populates the weight visualization panel gui
        if(VUE.getActiveMap() instanceof LWMergeMap)
        {
          refreshSettings((LWMergeMap)VUE.getActiveMap());
        }
        else
        {
            refreshSettings();
        }
        
        validate();
        
        //todo: would like window to adjust size based on needs of current
        // panel, likely would have to place stub for larger panels and
        // swap the real panel instance back in on tab select.
        //if(p!=null)
            //p.pack();
        
        setVisible(true);
    }
    
    public void refreshSettings()
    {
        weightPanel.loadDefaultStyles();
        weightPanel.loadDefaultSettings();  
    }
    
    public static void loadDefaultStyle()
    {
        StyleReader.readStyles("compare.weight.css");
    }
    
    public static void setDockWindow(DockWindow d)
    {
        p = d;
        if(p!=null)
        {    
          p.setResizeEnabled(false);
        }
    }
    
    public static DockWindow getDockWindow()
    {
        return p;
    }   
    
    public void setUpBasePanel()
    {
        baseGridBag = new GridBagLayout();
        GridBagConstraints baseConstraints = new GridBagConstraints();
        basePanel = new JPanel();
        
        //$
          //if(p!=null)
         //{
           basePanel.addMouseListener(new java.awt.event.MouseAdapter() {
              //static int c =0;
               
              public void mouseEntered(java.awt.event.MouseEvent me)
              {
                 // System.out.println("basePanel mouse entered: " + (c++) + " me: "+ me);
                  if(baseChoice != null)
                  {
                      baseChoice.updateUI();
                  }
              }
          });
          //}
        //$
        
        int b = TAB_BORDER_SIZE;
        basePanel.setBorder(BorderFactory.createEmptyBorder(b,b,b,b));
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
        baseLabel = new JLabel(baseMessage);

        //String[] choices = {VUE.getActiveMap().getLabel(),"other"};
        //Object[] choices = {BASE_SELECT_STRING,BASE_OTHER_STRING};
        //Object[] choices = {new javax.swing.JSeparator()};
        
        
        PolygonIcon lineIcon = new PolygonIcon(new Color(153,153,153));
        lineIcon.setIconWidth(75);
        lineIcon.setIconHeight(1);
        //JLabel lineLabel = new JLabel(lineIcon);
        //Object[] choices = {BASE_SELECT_STRING,lineIcon,BASE_OTHER_STRING};
        
        
        refreshBaseChoices();
       // baseChoice = new JComboBox(choices);
        
        //$
          //baseChoice = new JComboBox();        
          //baseChoice.addItem(BASE_SELECT_STRING);
          baseChoice = new BaseMapChoiceSelector();
        //$


       //baseChoice.addItem(BASE_OTHER_STRING);
        //baseChoice.setRenderer(new MapChoiceCellRenderer());
        JLabel helpLabel = new JLabel(VueResources.getIcon("helpIcon.raw"),JLabel.LEFT);
        baseConstraints.fill = GridBagConstraints.HORIZONTAL;
        baseGridBag.setConstraints(baseLabel,baseConstraints);
        baseInnerPanel.add(baseLabel);
        baseGridBag.setConstraints(baseChoice,baseConstraints);
        baseInnerPanel.add(baseChoice);
        baseConstraints.weightx = 1.0;
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
        baseBrowsePanel = new JPanel()
        {
            public Dimension getMaximumSize()
            {
                 return new Dimension(400,30);
            }
        };
        GridBagLayout baseBrowseGridBag = new GridBagLayout();
        baseBrowsePanel.setLayout(baseGridBag);
        GridBagConstraints baseBrowseConstraints = new GridBagConstraints();
        JLabel basePanelMapLabel = new JLabel("Map:",JLabel.RIGHT)
        {
            public Dimension getPreferredSize()
            {
                return baseLabel.getPreferredSize();
            }
        };
        //JLabel basePanelMapLabel = new JLabel("Select base map:"); 
        baseBrowseConstraints.fill = GridBagConstraints.HORIZONTAL;
        baseGridBag.setConstraints(basePanelMapLabel,baseBrowseConstraints);
        baseBrowsePanel.add(basePanelMapLabel);
        baseFileField = new JTextField(10);
        baseBrowseConstraints.weightx = 1.0;
        baseGridBag.setConstraints(baseFileField,baseBrowseConstraints);
        baseBrowsePanel.add(baseFileField);
        baseBrowseButton = new JButton("Browse");
        baseBrowseConstraints.weightx = 0.0;
        baseGridBag.setConstraints(baseBrowseButton,baseBrowseConstraints);
        baseBrowsePanel.add(baseBrowseButton);
    }
    
    public void refreshBaseChoices()
    {
        
        //$
          if(baseChoice != null)
          {
          //  baseChoice.refreshModel();
            //baseChoice.contentsChanged(null);
            //baseChoice.addNotify();
            //baseChoice.invalidate();
            //baseChoice.doLayout();
            /*int oldIndex = baseChoice.getSelectedIndex();
            if(baseChoice.getSelectedIndex()==0)
              baseChoice.setSelectedItem(baseChoice.getModel().getElementAt(0));
            else
              baseChoice.setSelectedItem(baseChoice.getModel().getElementAt(1));
            baseChoice.setSelectedItem(baseChoice.getModel().getElementAt(oldIndex));*/
              
            //baseChoice.setEnabled(false);
            //baseChoice.setEnabled(true);
            //baseChoice.repaint();
            //baseChoice.revalidate();
            
              
              //baseChoice.updateUI();
          }
        //$
        
        /*boolean otherSelected = false;
        if( baseChoice.getSelectedItem() != null && baseChoice.getSelectedItem().equals(otherString) )
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
        } */
    }
    
    public int getBaseMapSelectionType()
    {
        if(baseChoice.getSelectedIndex()==0)
        {
            return BASE_FROM_LIST;
        }
        else
        {
            return BASE_FROM_BROWSE;
        }
    }
    
    public File getBaseMapFile()
    {
        if(!baseChoice.getSelectedItem().equals(otherString))
        {
            LWMap baseMap = null;
            if(baseChoice.getSelectedItem() instanceof LWMap)
            {
                baseMap = (LWMap)baseChoice.getSelectedItem();
                // really should be checking if base map is saved at this point?
                // possibly not as much of an issue once baseMap is saved in LWMergeMap file
                return baseMap.getFile();
            }
        }
        else
        {
          return selectedBaseFile;
        }
        
        return null;
    }
    
    public void setIntervalBoundaries()
    {
        intervalBoundaries = new ArrayList<Double>();
        for(int vai = 0;vai<6;vai++)
        {
            double va =  20*vai + 0.5;
            intervalBoundaries.add(new Double(va));
        } 
    }
    
    public void setUpVizPanel()
    {
        vizPanel = new JPanel()
        {
            public Dimension getMaximumSize()
            {
                 return new Dimension(400,30);
            }
        };
        GridBagLayout vizLayout = new GridBagLayout();
        GridBagConstraints vizConstraints = new GridBagConstraints();
        vizPanel.setLayout(vizLayout);
        vizLabel = new JLabel("Select a visualization mode:");
        String[] vizChoices = {"Vote","Weight"};
        vizChoice = new JComboBox(vizChoices);
        filterChoice = new JCheckBox("Filter on Base Map?");
        /* filterChoice.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent me)
            {
                mousePressed = true;
            }
        }); */
        //$
        
          //$
           // vizLabel.setOpaque(true);
           // vizLabel.setBackground(Color.CYAN);
          //$
        
          vizConstraints.fill = GridBagConstraints.BOTH;
          vizConstraints.anchor = GridBagConstraints.EAST;
          //vizConstraints.weightx = 0.0;
        //$
        vizLayout.setConstraints(vizLabel,vizConstraints);
        vizPanel.add(vizLabel);
        //$
           vizConstraints.anchor = GridBagConstraints.WEST;
           vizConstraints.weightx = 1.0;
        //$
        vizLayout.setConstraints(vizChoice,vizConstraints);
        vizPanel.add(vizChoice);
        vizConstraints.gridwidth = GridBagConstraints.REMAINDER;
        JLabel helpLabel = new JLabel(VueResources.getIcon("helpIcon.raw"),JLabel.LEFT);
        vizLayout.setConstraints(helpLabel,vizConstraints);
        vizPanel.add(helpLabel);
        vizLayout.setConstraints(filterChoice,vizConstraints);
        //vizPanel.add(filterChoice);
        int b = TAB_BORDER_SIZE;
        vizPanel.setBorder(BorderFactory.createEmptyBorder(b,b,b,b));
        
        votePanel = new JPanel();
        /*{
          public Dimension getPreferredSize()
          {
              return new Dimension(400,600);
          }
        };*/
        
        
        weightPanel = new WeightVisualizationSettingsPanel(this);
        GridBagLayout voteLayout = new GridBagLayout();
        GridBagConstraints voteConstraints = new GridBagConstraints();
        votePanel.setLayout(voteLayout);
        JLabel defineThresholdMessageLabel = new JLabel(defineThresholdMessage);
        //defineThresholdMessageLabel.setBorder(BorderFactory.createEmptyBorder(0,0,20,0));
        
        JPanel moreLessLabel = new JPanel();
        /*{
          public Dimension getPreferredSize()
          {
              return nodeThresholdSlider.getSize();
          }
        };*/
        
        JLabel moreLabel = new JLabel("<< more",JLabel.LEFT);
        JLabel lessLabel = new JLabel("less >>",JLabel.RIGHT);
        
        //JLabel moreLabel = new JLabel("<< more nodes",JLabel.LEFT);
        //moreLabel.setFont(new Font("Courier",Font.PLAIN,10));
        //JLabel lessLabel = new JLabel("less nodes >>",JLabel.RIGHT);
        //lessLabel.setFont(new Font("Courier",Font.PLAIN,10));
        
        moreLessLabel.setLayout(new BorderLayout());
        moreLessLabel.add(BorderLayout.WEST,moreLabel);
        moreLessLabel.add(BorderLayout.EAST,lessLabel);
        
        nodeThresholdSlider = new JSlider(0,100,50);
        nodeThresholdSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent me)
            {
                mousePressed = true;
            }
        });
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
        
        
        //$
           nodeThresholdSlider.setBorder(javax.swing.BorderFactory.createEmptyBorder(5,5,5,5));
        //$
        
        JLabel nodeLabel = new JLabel("Nodes:");
        
        //voteConstraints.gridwidth = GridBagConstraints.REMAINDER;
        //$
          voteConstraints.gridwidth = 2;
          voteConstraints.gridx = 0;
          voteConstraints.gridy = 0;
        //$
        voteConstraints.insets = new Insets(0,0,20,0);
        voteConstraints.anchor = GridBagConstraints.NORTHWEST;
        //voteConstraints.weightx = 1.0;
        voteLayout.setConstraints(defineThresholdMessageLabel,voteConstraints);
        votePanel.add(defineThresholdMessageLabel);
        
        //voteConstraints.weightx =1.0;
        voteConstraints.anchor = GridBagConstraints.CENTER;
        voteConstraints.fill = GridBagConstraints.HORIZONTAL;
        voteConstraints.insets = new Insets(0,0,0,0);
        //$
          voteConstraints.gridwidth = 1;
          voteConstraints.gridx = 1;
          voteConstraints.gridy = 1;
        //$
        voteLayout.setConstraints(moreLessLabel,voteConstraints);
        //xvotePanel.add(moreLessLabel);
        
        //$
          voteConstraints.gridx = 0;
          voteConstraints.gridy = 2;
        //$
        voteConstraints.fill = GridBagConstraints.NONE;
        voteConstraints.anchor = GridBagConstraints.WEST;
        //voteConstraints.weightx = 1.0;
        voteConstraints.insets= new java.awt.Insets(0,40,0,0);
        //voteConstraints.gridwidth = GridBagConstraints.RELATIVE;
        voteConstraints.gridwidth = 1;
        //$
           //nodeLabel.setOpaque(true);
           //nodeLabel.setBackground(Color.YELLOW);
           voteConstraints.gridx = 0;
           voteConstraints.gridy = 2;
        //$
        voteLayout.setConstraints(nodeLabel,voteConstraints);
        votePanel.add(nodeLabel);
        voteConstraints.insets = new java.awt.Insets(0,0,0,0);
        //$
          voteConstraints.gridx = 1;
          voteConstraints.gridy = 2;
        //$
        voteLayout.setConstraints(nodeThresholdSlider,voteConstraints);

        votePanel.add(nodeThresholdSlider);
        /*String nodePercentageText = "";
        if(nodeThresholdSlider.getValue()<10)
        {
            nodePercentageText = nodeThresholdSlider.getValue() + "%  ";
        }
        if(nodeThresholdSlider.getValue()<100)
        {
            nodePercentageText = nodeThresholdSlider.getValue() + "% ";
        }
        else
        {
            nodePercentageText = nodeThresholdSlider.getValue() + "%";
        }
        percentageDisplay = new JLabel("");
        percentageDisplay.setText(nodePercentageText);*/
        percentageDisplay = new JLabel(nodeThresholdSlider.getValue() + "%")
        {
           public Dimension getPreferredSize()
           {
               return (new JLabel("100%").getPreferredSize());
           }
        };
        //have created methods below to turn this on and off (so that changes during setup don't affect the map)
        //boolean method could be used to turn this on (if not already on) from outside this constructor
        nodeThresholdSlider.addChangeListener(this);
        //voteConstraints.gridwidth = GridBagConstraints.REMAINDER;
        //$
          voteConstraints.gridx = 2;
          voteConstraints.gridy = 2;
        //$
        voteConstraints.insets = new java.awt.Insets(0,0,0,40);
        voteLayout.setConstraints(percentageDisplay,voteConstraints);
        votePanel.add(percentageDisplay);
        
        linkThresholdSlider = new JSlider(0,100,50);
        linkThresholdSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent me)
            {
                mousePressed = true;
            }
        });
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
        
        //$
           linkThresholdSlider.setBorder(javax.swing.BorderFactory.createEmptyBorder(5,5,5,5));
        //$
        
        JLabel linkPanel = new JLabel("Links:");
        voteConstraints.gridwidth = 1;
        voteConstraints.insets= new java.awt.Insets(0,40,0,0);
        //$
           voteConstraints.gridx = 0;
           voteConstraints.gridy = 3;
        //$
        voteLayout.setConstraints(linkPanel,voteConstraints);
        votePanel.add(linkPanel);
        voteConstraints.insets= new java.awt.Insets(0,0,0,0);
        //$
           voteConstraints.gridx = 1;
           voteConstraints.gridy = 3;
        //$
        voteLayout.setConstraints(linkThresholdSlider,voteConstraints);
        votePanel.add(linkThresholdSlider);
        linkPercentageDisplay = new JLabel(linkThresholdSlider.getValue()+"%")
        {
           public Dimension getPreferredSize()
           {
               return (new JLabel("100%").getPreferredSize());
           }
        };
        linkThresholdSlider.addChangeListener(this);
        voteConstraints.insets = new java.awt.Insets(0,0,0,40);
        //voteConstraints.gridwidth = GridBagConstraints.REMAINDER;
        //$
           voteConstraints.gridx = 2;
           voteConstraints.gridy = 3;
        //$
        voteLayout.setConstraints(linkPercentageDisplay,voteConstraints);
        votePanel.add(linkPercentageDisplay);
        
        voteConstraints.gridx = 0;
        voteConstraints.gridy = 4;
        voteLayout.setConstraints(filterChoice,voteConstraints);
        vizPanel.add(filterChoice);
        
        //votePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5,5,5,5));
    }
    
    public void startListeningToChanges()
    {
        nodeThresholdSlider.addChangeListener(this);
        linkThresholdSlider.addChangeListener(this);
    }
    
    public void stopListeningToChanges()
    {
        nodeThresholdSlider.removeChangeListener(this);
        linkThresholdSlider.removeChangeListener(this);
    }
    
    // didn't quite compile, but maybe we don't this right now..
    /*public boolean isListeningToChanges()
    {
        ChangeListener[] listeners = nodeThresholdSlider.getChangeListeners();
        List<ChangeListener> listenersList = List.asList(listeners);
        boolean isListening = listenersList.contains(this);
        return isListening;
    }*/
    
    public void LWCChanged(LWCEvent e)
    {
        //nodeThresholdSlider.setValue(e.oldValue)
        //System.out.println("mmc: LWCChanged -- old value: " + e.getOldValue());
        //System.out.println("mmc: LWCChanged -- source: " + e.getSource());
        //System.out.println("mmc: LWCChanged -- component: " + e.getComponent());
    }
    
    public void stateChanged(ChangeEvent e)
    {
        if(e.getSource()==nodeThresholdSlider)
        {
            
            /*String nodePercentageText = "";
            if(nodeThresholdSlider.getValue()<10)
            {
              nodePercentageText = nodeThresholdSlider.getValue() + "%  ";
            }
            if(nodeThresholdSlider.getValue()<100)
            {
              nodePercentageText = nodeThresholdSlider.getValue() + "% ";
            }
            else
            {
              nodePercentageText = nodeThresholdSlider.getValue() + "%";
            }
            percentageDisplay.setText(nodePercentageText); */
            
            percentageDisplay.setText(nodeThresholdSlider.getValue() + "%");
            
            // todo: make LWMergeMap a changelistener in future -- waiting on 
            // focus problems with activeMap
            
            if(getActiveMap() instanceof LWMergeMap)
            {
                if(!nodeThresholdSlider.getValueIsAdjusting() && mousePressed == true)
                {
                    
                   /*if(nodeChangeProgrammatic)
                   {
                       System.out.println("mmc: node change programmatic");
                       nodeChangeProgrammatic = false;
                       return;
                   }*/
                    
                    mousePressed = false;
                    
                    // this created a new window with new map..
                    //generateMergeMap();
                                        
                   LWMergeMap am = (LWMergeMap)getActiveMap();
                   
                   //am.getNodeThresholdValueStack().push(am.getNodeThresholdSliderValue());
                   
                   KEY_NODE_CHANGE.setMMC(this);
                   LWCEvent nodeEvent = new LWCEvent(am,am,KEY_NODE_CHANGE,new Integer(am.getNodeThresholdSliderValue()));
                   am.setNodeThresholdSliderValue(nodeThresholdSlider.getValue());
                   am.notifyProxy(nodeEvent);
                   //am.getUndoManager().processEvent(nodeEvent,false);
                   //am.getUndoManager().mark("recorded event");
                   
                   //System.out.println("mmc: about to call recreateVoteMerge: nodeThresholodSlider.getValue(): " + nodeThresholdSlider.getValue());
                   am.recreateVoteMerge();
                   //VUE.getUndoManager().mark("Merge ReCalculate");
                   am.getUndoManager().mark("Merge Recalculate");
                   undoButton.setEnabled(true);
                   undoCount++;

                }
            }
           
        }
        if(e.getSource()==linkThresholdSlider)
        {
            linkPercentageDisplay.setText(linkThresholdSlider.getValue() + "%");
            
            
            if(getActiveMap() instanceof LWMergeMap)
            {
                if(!linkThresholdSlider.getValueIsAdjusting() && mousePressed == true)
                {
                    
                    
                   mousePressed = false;
                    
                                        
                   LWMergeMap am = (LWMergeMap)getActiveMap();
                   
                   KEY_LINK_CHANGE.setMMC(this);
                   LWCEvent nodeEvent = new LWCEvent(am,am,KEY_LINK_CHANGE,new Integer(am.getLinkThresholdSliderValue()));
                   am.setLinkThresholdSliderValue(linkThresholdSlider.getValue());
                   am.notifyProxy(nodeEvent);

                   am.recreateVoteMerge();

                   am.getUndoManager().mark("Merge Recalculate");
                   undoButton.setEnabled(true);
                   undoCount++;

                }
            }

        }
        if(e.getSource()==vueTabbedPane)
        {
            String selectString = "Select";
        
            boolean noMapsLoaded = (vueTabbedPane.getModel().getSelectedIndex() == -1);
        
            //System.out.println("MergeMapsChooser, state changed on vueTabbedPane: no maps loaded?: " + noMapsLoaded);
            //System.out.println("MergeMapsChooser, state changed on vueTabbedPane, getTabCount: " + vueTabbedPane.getTabCount());
            //System.out.println("MergeMapsChooser, state changed on vueTabbedPane, VUE.getActiveMap().getLabel() " + VUE.getActiveMap().getLabel());
            //System.out.println("MergeMapsChooser, state changed on vueTabbedPane: e: " + e);
            //System.out.println("MergeMapsChooser, state changed on vueTabbedPane, getTitleAt(0): " + vueTabbedPane.getTitleAt(0));
            //System.out.println("MergeMapsChooser, state changed on vueTabbedPane, VUE.openMapCount(): " + VUE.openMapCount());
            //System.out.println("MergeMapsChooser, state changed on vueTabbedPane, VUE.getActiveViewer(): " + VUE.getActiveViewer());
            //System.out.println("MergeMapsChooser, state changed on vueTabbedPane, find tab with map of active map: " + vueTabbedPane.indexOfComponent(VUE.getActiveMap()));
            //System.out.println("MergeMapsChooser, state changed on vueTabbedPane, selected Tab: " + vueTabbedPane.getModel().getSelectedIndex());
            //System.out.println("MergeMapsChooser, state changed on vueTabbedPane, #of components: " + vueTabbedPane.getComponents().length);
            
            //$
            /*
            if(noMapsLoaded)
            {
              baseChoice.removeAllItems();
              baseChoice.addItem(selectString);
              if(baseBrowsePanel==null)
              {
                  setUpBasePanelBrowse();
              }
              basePanel.add(baseBrowsePanel);
              repaint();
              return;
            }   
            else
            {
              if(baseChoice.getSelectedObjects() == null)
              {        
                  refreshBaseChoices();
              }
              baseChoice.removeItem(selectString);
            } */
            //$
            //creates class cast exceptions and deadlock...
            //VUE.setActiveViewer((MapViewer)VUE.getTabbedPane().getSelectedComponent());
            
        }
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if(p==null)
            return;
        if(e.getSource()==baseChoice)
        {
          System.out.println("MMC: action event on base choice: " + e);
          System.out.println("MMC: action event on base choice - selected Item: " + baseChoice.getSelectedItem());
          if(baseChoice.getItemCount() == 0 || baseChoice.getSelectedItem() == null )
              return;
         // if(baseChoice.getSelectedItem().equals("other"));
          if(baseChoice.getSelectedItem().equals(BaseMapChoiceSelector.OTHER_STRING))
            {    
              System.out.println("MMC: other selected in base choice");
              
              if(baseBrowsePanel==null)
              {
                  setUpBasePanelBrowse();
              }
              basePanel.add(baseBrowsePanel);
              
              //basePanel.revalidate();
              //basePanel.repaint();
              repaint();
              /*if(p!=null)
              {
                p.pack();
              }*/
            }
            if(!baseChoice.getSelectedItem().equals(BaseMapChoiceSelector.OTHER_STRING))
            {
              basePanel.remove(baseBrowsePanel);
              //basePanel.revalidate();
              //basePanel.repaint();
              repaint();
              /*if(p!=null)
              {
                p.pack();
              }*/
            }
        }
        if(e.getSource() == baseBrowseButton)
        {
            VueFileChooser choose = VueFileChooser.getVueFileChooser();
            choose.setFileFilter(new VueFileFilter(VueFileFilter.VUE_DESCRIPTION));
            choose.showDialog(this,"Set Base Map");
            selectedBaseFile = choose.getSelectedFile();
            if(selectedBaseFile != null)
            {    
              baseFileField.setText(selectedBaseFile.getName());
              try
              {        
                baseMap = ActionUtil.unmarshallMap(selectedBaseFile);
                baseChoice.setUserSelectedMap(baseMap);
              }
              catch(Exception ex)
              {
                ex.printStackTrace();
              } 
            }
        }
        if(e.getSource() == vizChoice)
        {
            if(vizChoice.getSelectedItem().equals("Weight"))
            {
                vizPane.remove(votePanel);
                vizPane.add(weightPanel);
                validate();
                repaint();
                if(p!=null && !p.isRolledUp())
                {
                   p.pack();
                }
            }
            else
            {
                vizPane.remove(weightPanel);
                vizPane.add(votePanel);
                validate();
                repaint();
                if(p!=null && !p.isRolledUp())
                {
                    p.pack();
                }
            }
        }
        if(e.getSource() == generate)
        {
            
            generateMergeMap();
            
            
        }
    }
    
    /*
     *
     * creates LWMergeMap and saves setting data appropriate to GUI
     * choices then calls appropriate Merge Creation method
     * new LWMergeMap is currently displayed by merge creation method.
     *
     */
    public void generateMergeMap()
    {
            // create new map
            LWMergeMap map = new LWMergeMap(LWMergeMap.getTitle());
            
            //set selection settings
            //really should probably default to file but 
            map.setMapListSelectionType(sp.getMapListSelectionType());
            
            //todo: send all maps not just the active ones.
            // this is for file based selection.
            sp.fillMapList();
            //System.out.println("mmc: " + sp.getMapList().size());
            map.setMapList(sp.getMapList());
            //System.out.println("mmc: map list size in generate merge map " + map.getMapList().size());
            
            //todo: switch to saving the maps themselves even when from files originally.
            // or, at very least, change these to filenames..
            map.setMapFileList(sp.getMapFileList());
            
            //set base map settings
            map.setBaseMapSelectionType(getBaseMapSelectionType());
            map.setBaseMapFile(getBaseMapFile());
            
            //loadBaseMap here? currently baseMap is either open and will
            //be saved with the merge map or has already been loaded through
            //GUI
            //map.setBaseMap(baseMap);
            
            
            //set visualization settings
            map.setVisualizationSelectionType(vizChoice.getSelectedIndex());
            try
            { 
              if(vizChoice.getSelectedIndex()==1)
                map.setStyleMapFile(StyleMap.saveToUniqueUserFile());
              //System.out.println("mmc: " + map.getStyleMapFile());
              //System.out.prinltn("userfolder: ");
            }
            catch(Exception ex)
            {
              ex.printStackTrace();  
            }
            map.setFilterOnBaseMap(filterChoice.isSelected());
            //map.setVoteThresholds();
            //map.setWeightStyle();
            
            //load maps --should this be done as needed instead of always from file?
            //-- how to determine which maps are intended to be loaded? From list
            // of LWMaps in dialog?
            
            //merge maps map object
            
            if(vizChoice.getSelectedIndex() == 0)
            {
              sp.generate(map);
              //mapList.clear();
            }
            else
            {
             

               //Object baseMapObject = baseChoice.getModel().getSelectedItem();
               //if(baseMapObject instanceof LWMap)
               //  baseMap = (LWMap)baseMapObject;
               
               baseMap = baseChoice.getSelectedMap();
            
               if(baseMap == null)
               {
                   VueUtil.alert("Base Map not set","Base Map");
                   return;
               } 
                
                
               //System.out.println("MMC: generate base map -- baseMap: " + baseMap);
               
               
               map.setBaseMap(baseMap);
               
               //map.setUserOrigin(VUE.getActiveViewer().getOriginX(),VUE.getActiveViewer().getOriginY());
               //map.set
               createWeightedMerge(map);
               MapViewer v = VUE.displayMap(map);
               
               tufts.vue.LWCEvent event = new tufts.vue.LWCEvent(v,map,new LWComponent.Key("Merge Map Displayed"));
               v.LWCChanged(event);
               
               
               //v.grabVueApplicationFocus("New Merge Map",null);
               //ZoomTool.setZoomFit();
               // creates class cast exception? (should be MapScrollPane apparently, really need
               // an awkward run-time check..) Also, doesn't seem neccesary... (real problem
               // is base map showing incorrectly until mouse over map)
               //VUE.setActiveViewer((MapViewer)(VUE.getTabbedPane().getSelectedComponent()));
               mapList.clear();
                
            } // */ to top of if/else
    }
    
    public Dimension getVizLabelPreferredSize()
    {
        return vizLabel.getPreferredSize();
    }
    
    public int getInterval(double score)
    {
        Iterator<Double> i = intervalBoundaries.iterator();
        int count = 0;
        while(i.hasNext())
        {
            if(score < i.next().doubleValue())
                return count;
            count ++;
        }
        return 0;
    }
    
    
    public void addMergeNodesForMap(LWMergeMap mergeMap,LWMap map,WeightAggregate weightAggregate,List<Style> styles)
    {
        
        
        
        
           Iterator children = map.getNodeIterator();
           //Iterator children = map.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
           
           
           
           while(children.hasNext()) {
             LWNode comp = (LWNode)children.next();
             boolean repeat = false;
             //if(map.findByID(comp.getChildList(),Util.getMergeProperty(comp)) != null)
             if(mergeMap.nodeAlreadyPresent(comp))
             {
               repeat = true;
             }
             LWNode node = (LWNode)comp.duplicate();
             
             
             //$
               /*List childList = node.getChildList();
               //node.removeChildren(childList.iterator());
               Iterator i = childList.iterator();
               while(i.hasNext())
               {
                   //((LWComponent)i.next()).setVisible(false);
                   
                   LWComponent childComp = (LWComponent)i.next();
                   if(childComp instanceof LWNode)
                   {    
                     LWNode child = (LWNode)childComp;
                     double score = 100*weightAggregate.getNodeCount(Util.getMergeProperty(child))/weightAggregate.getCount();
                     if(score>100)
                     {
                        score = 100;
                     }
                     if(score<0)
                     {
                        score = 0;
                     }
                     //System.out.println("mmc: score: " + score);
                     //System.out.println("mmc: getInterval(score): " + getInterval(score));
                     Style currStyle = styles.get(getInterval(score)-1);
                     //System.out.println("Weighted Merge Demo: " + currStyle + " score: " + score);
                     child.setFillColor(Style.hexToColor(currStyle.getAttribute("background")));
                   }
               }*/
             //$
             
             //System.out.println("Weighted Merge: counts : " + Util.getMergeProperty(node) + ":" + weightAggregate.getNodeCount(Util.getMergeProperty(node)) +
             //                   " " + weightAggregate.getCount());
             /*double score = 100*weightAggregate.getNodeCount(Util.getMergeProperty(node))/weightAggregate.getCount();
             if(score>100)
             {
               score = 100;
             }
             if(score<0)
             {
               score = 0;
             }
             //System.out.println("mmc: score: " + score);
             //System.out.println("mmc: getInterval(score): " + getInterval(score));
             Style currStyle = styles.get(getInterval(score)-1);
             //System.out.println("Weighted Merge Demo: " + currStyle + " score: " + score);
             node.setFillColor(Style.hexToColor(currStyle.getAttribute("background")));*/
             if(!repeat)
             {    
               mergeMap.addNode(node);
             }
             //map.addNode(node);
             
             
             // actually need an intermediate map with all nodes about to be added? or mergeMap may
             // already have styled elements...
             //Iterator mergeMapChildren = mergMap.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();     
             
           }
    }
    
    // todo: replace with LWMergeMap method -- addMergeNodesFromSourceMap, already
    // used by recreateVoteMerge method in LWMergeMap and by slider in this Class...
    public void addMergeNodesForMap(LWMergeMap mergeMap,LWMap map,VoteAggregate voteAggregate)
    {
           Iterator children = map.getNodeIterator();    
           while(children.hasNext()) {
             LWNode comp = (LWNode)children.next();
             boolean repeat = false;
             //if(map.findByID(comp.getChildList(),Util.getMergeProperty(comp)) != null)
             if(mergeMap.nodeAlreadyPresent(comp))
             {
               repeat = true;
             }
             
             if(voteAggregate.isNodeVoteAboveThreshold(Util.getMergeProperty(comp)) ){
                   LWNode node = (LWNode)comp.duplicate();
                   if(!repeat)
                   {
                     mergeMap.addNode(node);
                   }
             }         
             
           }
    }
    
    public void createWeightedMerge(LWMergeMap map)
    {
        
        //System.out.println("mmc: createWeightedMerge mapList size: " + mapList.size() );
        //System.out.println("mmc: createWeightedMerge LWMerge Map mapList size: " + map.getMapList().size() );
        
        ArrayList<ConnectivityMatrix> cms = new ArrayList<ConnectivityMatrix>();
        Iterator<LWMap> i = map.getMapList().iterator();
        while(i.hasNext())
        {
          cms.add(new ConnectivityMatrix(i.next()));
        }
        
        ArrayList<Style> nodeStyles = new ArrayList<Style>();
        ArrayList<Style> linkStyles = new ArrayList<Style>();
        
        for(int si=0;si<5;si++)
        {
            nodeStyles.add(StyleMap.getStyle("node.w" + (si +1)));
        }
        
        for(int lsi=0;lsi<5;lsi++)
        {
            linkStyles.add(StyleMap.getStyle("link.w" + (lsi +1)));
        }
        
        //System.out.println("mmc: create weight aggregate");
        WeightAggregate weightAggregate = new WeightAggregate(cms);
        
        addMergeNodesForMap(map,baseMap,weightAggregate,nodeStyles);
        
        if(!filterChoice.isSelected())
        {
          Iterator<LWMap> maps = map.getMapList().iterator();
          while(maps.hasNext())
          {
            LWMap m = maps.next();
            if(m!=baseMap)
            {
                addMergeNodesForMap(map,m,weightAggregate,nodeStyles);
            }
          }
        }
        
        //apply styles here to make sure they get applied for all sub nodes (what
        // about sub links?)
        // todo: use applyCSS(style) -- need to plug in formatting panel
        Iterator children = map.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
        
        while(children.hasNext())
        {
             LWComponent comp = (LWComponent)children.next();
             if(comp instanceof LWNode)
             {
                  LWNode node = (LWNode)comp;
                  double score = 100*weightAggregate.getNodeCount(Util.getMergeProperty(node))/weightAggregate.getCount();
                  if(score>100)
                  {
                    score = 100;
                  }
                  if(score<0)
                  {
                    score = 0;
                  }
                  //System.out.println("mmc: score: " + score);
                  //System.out.println("mmc: getInterval(score): " + getInterval(score));
                  Style currStyle = nodeStyles.get(getInterval(score)-1);
                  //System.out.println("Weighted Merge Demo: " + currStyle + " score: " + score);
                  node.setFillColor(Style.hexToColor(currStyle.getAttribute("background")));
             }
        }
        //compute and create nodes in Merge Map, apply just background style for now
        /* Iterator children = baseMap.getNodeIterator();
        while(children.hasNext()) {
           LWNode comp = (LWNode)children.next();
           LWNode node = (LWNode)comp.duplicate();
           //System.out.println("Weighted Merge Demo: counts : " + node.getRawLabel() + ":" + weightAggregate.getNodeCount(node.getRawLabel()) + " " + weightAggregate.getCount());
           double score = 100*weightAggregate.getNodeCount(Util.getMergeProperty(node))/weightAggregate.getCount();
           if(score>100)
           {
               score = 100;
           }
           if(score<0)
           {
               score = 0;
           }
           //System.out.println("mmc: score: " + score);
           //System.out.println("mmc: getInterval(score): " + getInterval(score));
           Style currStyle = styles.get(getInterval(score)-1);
           //System.out.println("Weighted Merge Demo: " + currStyle + " score: " + score);
           node.setFillColor(Style.hexToColor(currStyle.getAttribute("background")));
           map.addNode(node);
        }*/
        
        
        
        //compute and create links in Merge Map
        //Iterator children1 = map.getNodeIterator();
        Iterator<LWComponent> children1 = map.getAllDescendents(LWComponent.ChildKind.PROPER).iterator();
        while(children1.hasNext()) {
           LWComponent comp1 = children1.next();
           if(comp1 instanceof LWImage)
               continue;
           LWNode node1 = (LWNode)comp1;
           Iterator children2 = map.getNodeIterator();
           while(children2.hasNext()) {
               LWNode node2 = (LWNode)children2.next();
               if(node2 != node1) {
                  int c = weightAggregate.getConnection(Util.getMergeProperty(node1),Util.getMergeProperty(node2));
                  
                  //$
                    // don't know if link already drawn.. need to keep track or explicitly check for link..
                    //int c2 = weightAggregate.getConnection(Util.getMergeProperty(node2),Util.getMergeProperty(node2));
                  //$
                  
                  if(c >0) {
                    double score = 100*c/weightAggregate.getCount();
                    if(score > 100)
                    {
                        score = 100;
                    }
                    if(score < 0)
                    {
                        score = 0;
                    }
                    Style currLinkStyle = linkStyles.get(getInterval(score)-1);
                    //System.out.println("Weighted Merge Demo: " + currLinkStyle + " score: " + score);
                    LWLink link = new LWLink(node1,node2);
                    link.setStrokeColor(Style.hexToColor(currLinkStyle.getAttribute("background")));
                    //also add label to link if present? (will be nonunique perhaps.. might make sense for voting?)
                    map.addLink(link);
                  }
               }
           }
        }
    }
    
    // this is really "fill vote merge map" as the map must already be instantiated
    // recreatevotemerge in LWMergeMap should be able to use this method
    // with a call to "clear merge map" that currently resides in that function
    // performed first. -- todo: move to "fill/clear" methodology as this functionality
    // moves completely to LWMergeMap -- should also allow dynamic switch from weight
    // to merge -- perhaps name this "fill *as* vote merge map"
    public void createVoteMerge(LWMergeMap map)
    {
        ArrayList<ConnectivityMatrix> cms = new ArrayList<ConnectivityMatrix>();
        
        // why not map.getMapList()? is something wrong here?... 3/15/2007-- lets try it
        // (beware the ides of march!)
        Iterator<LWMap> i = map.getMapList().iterator(); // /*map.getMapList()*/mapList.iterator();
        while(i.hasNext())
        {
          cms.add(new ConnectivityMatrix(i.next()));
        }
        VoteAggregate voteAggregate= new VoteAggregate(cms);
        
        // todo: get these from map in order to move this function to LWMergeMap.
        voteAggregate.setNodeThreshold((double)(nodeThresholdSlider.getValue()/100.0));
        voteAggregate.setLinkThreshold((double)(linkThresholdSlider.getValue()/100.0));
        
        //compute and create nodes in Merge Map
        
        addMergeNodesForMap(map,baseMap,voteAggregate);
        
        if(!filterChoice.isSelected())
        {
          Iterator<LWMap> maps = map.getMapList().iterator();
          while(maps.hasNext())
          {
            LWMap m = maps.next();
            if(m!=baseMap)
            {
                addMergeNodesForMap(map,m,voteAggregate);
            }
          }
        }
        
        /*Iterator children = baseMap.getNodeIterator();
        while(children.hasNext()) {
           LWComponent comp = (LWComponent)children.next();
           if(voteAggregate.isNodeVoteAboveThreshold(Util.getMergeProperty(comp)) ){
                   LWNode node = (LWNode)comp.duplicate();
                   map.addNode(node);
           }
        }*/
        
        //compute and create links in Merge Map
        Iterator children1 = map.getNodeIterator();
        while(children1.hasNext()) {
           LWNode node1 = (LWNode)children1.next();
           Iterator children2 = map.getNodeIterator();
           while(children2.hasNext()) {
               LWNode node2 = (LWNode)children2.next();
               if(node2 != node1) {
                  boolean addLink = voteAggregate.isLinkVoteAboveThreshold(Util.getMergeProperty(node1),Util.getMergeProperty(node2));
                  if(addLink) {
                     map.addLink(new LWLink(node1,node2));
                  }
               }
           }
        }
        
    }
    
    public void refreshSettings(final LWMergeMap map)
    {
        // no longer needed now that links are persisted in xml file? what
        // about for maps saved before update? Probably still works as
        // the data is in the file? todo: ask scott fraize
        // (starting 3/15/2007 this was created exceptions on dynamic slider change..
        //map.recalculateLinks();
        
        sp.setMapListSelectionType(map.getMapListSelectionType());
        
        stopListeningToChanges();
        
        // dialog level map list needed for?... maybe no longer
        // needed since starting saving source maps along with map?
        mapList = map.getMapList();
       
        // need to also populate select panel... use LWMergeMap as Model
        // when load? model can just be a list of LWMaps at this point..
        
        // base map settings
        selectedBaseFile = map.getBaseMapFile();
        baseMap = map.getBaseMap();
        
        //$
        /*
        if(map.getBaseMapSelectionType() == BASE_FROM_LIST)
        {
            // assume base map not open, add to base map list
            //System.out.println("mmc: about to add baseMap: " + baseMap);
            //System.out.println("mmc: for map: " + map.getTitle());
            
            if(baseMap != null)
            {
              baseChoice.addItem(baseMap);
              baseChoice.setSelectedItem(baseMap);
            }
        }
        else // BASE_FROM_BROWSE
        {
            baseChoice.setSelectedItem(otherString);  
            baseFileField.setText(selectedBaseFile.getName());
        }*/
        //$

        // visualization settings
        
        vizChoice.setSelectedIndex(map.getVisualizationSelectionType());
        
        filterChoice.setSelected(map.getFilterOnBaseMap());
        
        nodeThresholdSlider.setValue(map.getNodeThresholdSliderValue());
        percentageDisplay.setText(map.getNodeThresholdSliderValue()+"%");
        linkThresholdSlider.setValue(map.getLinkThresholdSliderValue());
        linkPercentageDisplay.setText(map.getLinkThresholdSliderValue()+"%");
        try
        {
          if(vizChoice.getSelectedIndex()==1)
          {
            StyleMap.readFromUniqueUserFile(map.getStyleMapFile());
          }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
 
                   // from actionPerformed to fix bug -- need to either call actionPerformed, plug
            // method into drop down box change event somehow or make a delegate method
            // and call in both places
            if(vizChoice.getSelectedItem().equals("Weight"))
            {
                vizPane.remove(votePanel);
                vizPane.add(weightPanel);
                validate();
                if(p!=null && !p.isRolledUp())
                {
                    p.pack();
                }
            }
            else
            {
                vizPane.remove(weightPanel);
                vizPane.add(votePanel);
                validate();
                if(p!=null && !p.isRolledUp())
                {
                    p.pack();
                }
            }
        
        
        //actually loads current styles and settings -- *fix names* -- probably only should load
        //defaults (using StyleReader) if there is no style loaded at all
        // *however, doesn't yet provide proper style load unless weight panel already open...*
        weightPanel.loadDefaultStyles();
        weightPanel.loadDefaultSettings();
        
        //$
          //if(p!=null)
          //{
           // p.pack();
          //}
        //$
        
 
        startListeningToChanges();   
 
    }
    
    public void setActiveMap(LWMap map)
    {
        String selectString = "Select";
        
        boolean noMapsLoaded = !(VUE.getLeftTabbedPane().getAllMaps().hasNext());
        
        //$
        /*
        if(noMapsLoaded)
        {
            baseChoice.removeAllItems();
            baseChoice.addItem(selectString);
            return;
        }   
        else
        {
            baseChoice.removeItem(selectString);
        }*/
        //$
        
        /*if(map==null)
        {
            return;
        }*/
        LWMap previousMap = activeMap;
        if(previousMap !=null)
        {
          previousMap.removeLWCListener(this);
        }
        activeMap = map;
        
        
        //map.addLWCListener(this);
        
        //$
          //map.addLWCListener(this,KEY_NODE_CHANGE);
          //map.addLWCListener(this,KEY_LINK_CHANGE);
        //$
        
        refreshBaseChoices();

        if(map instanceof LWMergeMap)
        {
           refreshSettings((LWMergeMap)map);
           //activeMap = previousMap; //? good fail safe? Might want to check previous map as well
                                      // and default to first non Merge Map if is merge map
                                      // what if no non merge maps are visible? Use empty non visible map?
                                      // note: generate button should likely be disabled in this case anyway
        }
        
        if(map instanceof LWMergeMap)
        {
           //disable for now (this is confusing in term of base map set
           //to non active map, now that it is no longer dynamically set)
           // also: a bug(?) in tab selection still sometimes refuses to reenable
           // the button without a click in a non merge map
           // generate.setEnabled(false);
        }
        else
        {
            generate.setEnabled(true);
        }

        //selectPanelHolder.remove(sp);
        //if(selectPanels.containsKey(activeMap))
        //{
           // maintains run-time settings even if not merge map
           // sp = (SelectPanel)selectPanels.get(activeMap);
        //}
        //else if(activeMap instanceof LWMergeMap)
        //{
        //    if(previousMap!=null)
        //    {
              //sp = (SelectPanel)selectPanels.get(previousMap);
        //    }
        //    else
        //    {
                
                //sp = new SelectPanel();
        //    }
        //}
        //else
        //{
            // retains run-time settings even for non merge maps
            //sp = new SelectPanel();
        //}
        
        //selectPanelHolder.add(sp);
        //selectPanels.put(activeMap,sp);
        //selectPanelHolder.repaint();
        /*if(p!=null)
        {    
          p.pack();
        }*/

    }
    
    public LWMap getActiveMap()
    {
        return activeMap;
    }
    
    public void activeChanged(ActiveEvent<LWMap> e)
    {
        //System.out.println("Merge Maps Chooser: active map changed " + map.getLabel());
        

        
        setActiveMap(e.active);
        
        
        /*if(p!=null)
        {
            if(p.isRolledUp())
            {
                //p.getHiddenFrame().repaint();
                //p.getHiddenFrame().setBounds(300,300,300,300);
                //p.repaint();
                p.showRolledUp();
            }
                
        }*/
    }
    
    public void programmaticNodeSliderChange(int newValue)
    {
        nodeThresholdSlider.setValue(newValue);
    }
    
    public void programmaticLinkSliderChange(int newValue)
    {
        linkThresholdSlider.setValue(newValue);
    }

    public static class MMCKey extends LWComponent.Key
    {
            private MergeMapsChooser mmc;
            private String keyString;
            
            public MMCKey(String keyString)
            {
                super(keyString,LWComponent.KeyType.DATA);
                this.keyString = keyString;
            }
            
            public void setMMC(MergeMapsChooser m)
            {
                mmc = m;
            }
        
            public void setValue(LWComponent c, Object val) {
                System.out.println("mmc: Key Property: " + keyString);
                
                if(keyString.equals("nodeThresholdSliderValue"))
                {
                  System.out.println("KEY_NODE_CHANGE setValue: " + val); 
                  mmc.programmaticNodeSliderChange(((Integer)val).intValue());
                }
                if(keyString.equals("linkThresholdSliderValue"))
                {
                  System.out.println("KEY_LINK_CHANGE setValue: " + val); 
                  mmc.programmaticLinkSliderChange(((Integer)val).intValue());
                }
            }
            public Object getValue(LWComponent c) { System.out.println("mmc: KEY_NODE OR LINK CHANGE getValue"); return 0; }
    };
    
    class SelectPanel extends JPanel implements ActionListener
    {
        private JComboBox choice;
        private JTextField fileField;
        private JButton browseButton;
        private JButton addButton;
        private MapListPanel listPanel;
        //private JButton generateButton;
        
        private JPanel topPanel;
        private JPanel browsePanel;
        private JPanel bottomPanel;
        
        private File selectedFile;
        
        private List<File> mapFileList;
              
        public SelectPanel()
        {
            
            setUpTopPanel();
            setUpBrowsePanel();
            setUpBottomPanel();
           
            setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
            add(topPanel);
            //choice combobox will display browsePanel (see below)
            add(bottomPanel);
            
            int b = TAB_BORDER_SIZE;
            setBorder(BorderFactory.createEmptyBorder(b,b,b,b));
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
        }
        
        public void actionPerformed(ActionEvent e)
        {
            if(e.getSource() == choice)
            {
                if(choice.getSelectedItem().equals(ALL_TEXT))
                {
                    remove(bottomPanel);
                    remove(browsePanel);
                    if(p!=null && !p.isRolledUp())
                    {
                        p.pack();
                    }
                    add(bottomPanel);
                }
                else if(choice.getSelectedItem().equals(LIST_TEXT))
                {
                    remove(bottomPanel);
                    add(browsePanel);
                    if(p!=null && !p.isRolledUp())
                    {
                        p.pack();
                    }
                    add(bottomPanel);
                }
            }
            if(e.getSource() == browseButton)
            {
                VueFileChooser fileChooser = VueFileChooser.getVueFileChooser();
                fileChooser.setFileFilter(new VueFileFilter(VueFileFilter.VUE_DESCRIPTION));
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
            /*if(e.getSource() == generateButton)
            {
               generate();
            }*/
            validate();
            if(p!=null && !p.isRolledUp())
            {
                p.pack();
            }
        }
        
        public void addMap(LWMap map)
        {
            MapListElementPanel mlep = new MapListElementPanel(map);
            mlep.adjustColor(listPanel.getComponentCount());
            listPanel.add(mlep);
            addButton.setEnabled(false);
            fileField.setText(""); 
        }
        
        public void loadMergeSourceMaps(LWMergeMap mergeMap)
        {
            if(mergeMap.getMapListSelectionType() == 0)
            {
               List<LWMap> mapList = mergeMap.getMapList();
               
            }
            else
            {
               //List<File> mapFileList = mergeMap.getMapFileList();
            }
        }
        
        public List<LWMap> getMapList()
        {
            return mapList;
        }
        
        /**
         *
         * todo: files should not be loaded until
         *       generate button is pressed
         *       (new interface already works this way)
         *
         **/
        public List<String> getMapFileList()
        {
            ArrayList<String> stringMapFileList = new ArrayList<String>();
            if(mapFileList != null)
            {    
              Iterator i = mapFileList.iterator();
              while(i.hasNext())
              {
                File file = (File)i.next();
                stringMapFileList.add(file.getAbsolutePath());
              }
            }
            return stringMapFileList;
        }
        
        public void fillMapList()
        {
               mapList.clear();
               
               if(choice.getSelectedItem().equals(ALL_TEXT))
               {
                 Iterator <LWMap> i = VUE.getLeftTabbedPane().getAllMaps();
                 while(i.hasNext())
                 {
                   LWMap m = i.next();
                   if(!(m instanceof LWMergeMap))
                   {
                     mapList.add(m);
                   }
                 }
                 //map.setSelectChoice("all");
               }
               else if(choice.getSelectedItem().equals(LIST_TEXT))
               {
                 ArrayList<LWMap> listPanelMaps = new ArrayList<LWMap> ();
                 mapFileList = new ArrayList<File> ();
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
                 //map.setMapFileList(mapFileList);
                 //map.setActiveMapList(activeFileList);
                 //map.setSelectChoice("list");
               }
        }
        
        public void generate(LWMergeMap map)
        {
            //LWMergeMap map = new LWMergeMap(LWMergeMap.getTitle());
               //fail safe default value for base map is active map
              /* if(baseMap == null)
               {
                 baseMap = VUE.getActiveMap();
               }
               Object baseMapObject = baseChoice.getSelectedItem();
               if(baseMapObject instanceof LWMap)
                 baseMap = (LWMap)baseMapObject;*/
               
               baseMap = baseChoice.getSelectedMap();
            
               if(baseMap == null)
               {
                   VueUtil.alert("Base Map not set","Base Map");
                   return;
               }
            
               map.setBaseMap(baseMap);
               map.setBaseMapFile(baseMap.getFile());
               
               //fillMapList();
               
               if(choice.getSelectedItem().equals(ALL_TEXT))
               {
                 /*Iterator <LWMap> i = VUE.getLeftTabbedPane().getAllMaps();
                 while(i.hasNext())
                 {
                   mapList.add(i.next());
                 }*/
                 map.setSelectChoice("all");
               }
               else if(choice.getSelectedItem().equals(LIST_TEXT))
               {
                 /*ArrayList<LWMap> listPanelMaps = new ArrayList<LWMap> ();
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
                 mapList.addAll(listPanelMaps); */
                 if(getMapFileList().size() == 0)
                 {
                     VueUtil.alert("No Maps Selected","Select Map");
                     return;
                 }
                 map.setMapFileList(getMapFileList());
                 //map.setActiveMapList(activeFileList);
                 map.setSelectChoice("list");
               }

               map.setNodeThresholdSliderValue(nodeThresholdSlider.getValue());
               map.setLinkThresholdSliderValue(linkThresholdSlider.getValue());
               createVoteMerge(map);
               float x = VUE.getActiveViewer().getOriginX();
               float y = VUE.getActiveViewer().getOriginY();
               //map.setUserOrigin(VUE.getActiveViewer().getOriginX(),VUE.getActiveViewer().getOriginY());
               MapViewer v = VUE.displayMap(map);
               tufts.vue.LWCEvent event = new tufts.vue.LWCEvent(v,map,new LWComponent.Key("Merge Map Displayed"));
               v.LWCChanged(event);
               //v.grabVueApplicationFocus("Merge Map",null);
               //VUE.getActiveMap().notifyProxy(event);
               //java.awt.Point p = new java.awt.Point((int)x,(int)y);
               //VUE.getActiveViewer().setLocation(p);
               //map.setUserOrigin(x,y);
               
               //v.fireViewerEvent(MapViewerEvent.PAN);
               //v.repaint();
               
               //v.addNotify();
               
               //v.loadFocal(map);
               //v.revalidate();
               //VUE.getTabbedPane().revalidate();
               //VUE.getTabbedPane().repaint();
               
               /*java.awt.Component[] comps = VUE.getTabbedPane().getComponents();
               
               for(int compc=0;compc<comps.length;compc++)
               {
                   if(comps[compc] instanceof MapViewer)
                   {
                       MapViewer vc = (MapViewer)comps[compc];
                       if(!(vc.getMap() instanceof LWMergeMap))
                       {
                           x = Math.max(x,vc.getOriginX());
                           y=  Math.max(y,vc.getOriginY());
                       }
                   }
               }

               v.setMapOriginOffset(x,y);*/
               
               
               
              // v.getCurrentTool().handleMouseReleased(new tufts.vue.MapMouseEvent());

               //ZoomTool.setZoomFit();
               
               // creates class cast exception? Also, doesn't seem neccesary... (real problem
               // is base map showing incorrectly until mouse over map)
               //VUE.setActiveViewer((MapViewer)VUE.getTabbedPane().getSelectedComponent());
        }
        
        
        public int getMapListSelectionType()
        {
            if(choice.getSelectedIndex()==0)
            {
                return ALL_OPEN_CHOICE;
            }
            //else
            return FILE_LIST_CHOICE;
        }
        
        public void setMapListSelectionType(int choice)
        {
            this.choice.setSelectedIndex(choice);
        }
        
    } 
    
    /*private class BaseMapPanel extends JPanel implements ActionListener
    {
        
        private JComboBox choice;
        private JTextField fileField;
        private JButton browseButton;
        //private JPanel basePanel;
        private JPanel baseBrowsePanel;
        private JButton baseBrowseButton;
        private JTextField baseFileField;
        private JComboBox baseChoice;
        private LWMap baseMap;
        
        public BaseMapPanel()
        {
           setUpPanel();
        }
        
        public void actionPerformed(ActionEvent e)
        {
            
        }
     
        public void setUpPanel()
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

        
    }*/
    
}
