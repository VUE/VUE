
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
    private File selectedBaseFile;
    
    private JPanel basePanel;
    private JPanel baseBrowsePanel;
    private JButton baseBrowseButton;
    private JTextField baseFileField;
    private JComboBox baseChoice;
    private LWMap baseMap;
    private JPanel buttonPane;
    //private JButton generateDemo;
    private JButton generate;
    
    private JPanel vizPane;
    private JPanel vizPanel;
    private JLabel vizLabel;
    private JComboBox vizChoice;
    private JPanel votePanel;
    private WeightVisualizationSettingsPanel weightPanel;
    private JSlider nodeThresholdSlider;
    private JLabel percentageDisplay;
    private JSlider linkThresholdSlider;
    private JLabel linkPercentageDisplay;
    private List<Double> intervalBoundaries;
    
    List<LWMap> mapList = new ArrayList<LWMap>();
    
    private HashMap <LWMap,SelectPanel> selectPanels = new HashMap<LWMap,SelectPanel>();
    
    //$
      private GridBagLayout baseGridBag;
      private JLabel baseLabel;
    //$
      
    private JTabbedPane vueTabbedPane = VUE.getTabbedPane();
    
    public final static String ALL_TEXT = "All maps currently opened";
    public final static String LIST_TEXT = "Browse to maps";
    public final static String SELECT_MESSAGE = "Select Maps to merge:";
    
    public final static String defineThresholdMessage = "Define threshold for nodes and links:";
    
    public final int ALL_OPEN_CHOICE = 0;
    public final int FILE_LIST_CHOICE = 1;
    
    public final int BASE_FROM_LIST = 0;
    public final int BASE_FROM_BROWSE = 1;
    
    public final String otherString = "other";
   
    public MergeMapsChooser() 
    {
        
        //$
          //System.out.println(tufts.vue.VueUtil.getDefaultUserFolder());
        //$
        
        vueTabbedPane.addChangeListener(this);
        loadDefaultStyle();
        VUE.addActiveMapListener(this);
        setLayout(new BorderLayout());
        buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        generate = new JButton("Generate");
        buttonPane.add(generate);
        JTabbedPane mTabbedPane = new JTabbedPane();
        VueResources.initComponent(mTabbedPane,"tabPane");
        
        selectPanelHolder = new JPanel();
        sp = new SelectPanel();
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
        
        //generateDemo.addActionListener(this);
        generate.addActionListener(this);
        
        //actually weight panel settings .. populates the weight visualization panel gui
        if(VUE.getActiveMap() instanceof LWMergeMap)
        {
          //System.out.println("mmc: active map is instanceof LWMergeMap");
          refreshSettings((LWMergeMap)VUE.getActiveMap());
        }
        else
        {
            refreshSettings();
        }
        
        validate();
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
        p.setResizeEnabled(false);
    }
    
    public static DockWindow getDockWindow()
    {
        return p;
    }   
    
    public void setUpBasePanel()
    {
        /*GridBagLayout*/ baseGridBag = new GridBagLayout();
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
        //$
          //baseInnerPanel.setOpaque(true);
          //baseInnerPanel.setBackground(Color.BLUE);
        //$
        baseInnerPanel.setLayout(baseGridBag);
        String baseMessage = "Select base map:";
        baseLabel = new JLabel(baseMessage);
        //$
           //baseLabel.setOpaque(true);
           //baseLabel.setBackground(Color.RED);
        //$
        String[] choices = {VUE.getActiveMap().getLabel(),"other"};
        baseChoice = new JComboBox(choices);
        baseChoice.setRenderer(new MapChoiceCellRenderer());
        JLabel helpLabel = new JLabel(VueResources.getIcon("helpIcon.raw"),JLabel.LEFT);
        baseConstraints.fill = GridBagConstraints.HORIZONTAL;
        baseGridBag.setConstraints(baseLabel,baseConstraints);
        baseInnerPanel.add(baseLabel);
        //baseConstraints.weightx = 1.0;
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
        //baseBrowsePanel.setLayout(baseBrowseGridBag);
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
        //$
           //baseFileField.setText("New Map");
        //$
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
       // String otherString = "other";
        boolean otherSelected = false;
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
                // not as big an issue once baseMap is saved in LWMergeMap file
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
        vizConstraints.gridwidth = GridBagConstraints.REMAINDER;
        vizLayout.setConstraints(vizChoice,vizConstraints);
        vizPanel.add(vizChoice);
        
        votePanel = new JPanel();
        
        //$
          //votePanel.setOpaque(true);
          //votePanel.setBackground(Color.RED);
        //$
        
        weightPanel = new WeightVisualizationSettingsPanel(this);
        GridBagLayout voteLayout = new GridBagLayout();
        GridBagConstraints voteConstraints = new GridBagConstraints();
        votePanel.setLayout(voteLayout);
        JLabel defineThresholdMessageLabel = new JLabel(defineThresholdMessage);
        
        //$
           //defineThresholdMessageLabel.setOpaque(true);
           //defineThresholdMessageLabel.setBackground(Color.BLUE);
        //$
        
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
        
        voteConstraints.gridwidth = GridBagConstraints.REMAINDER;
        voteConstraints.anchor = GridBagConstraints.NORTHWEST;
        //voteConstraints.weightx = 1.0;
        voteLayout.setConstraints(defineThresholdMessageLabel,voteConstraints);
        votePanel.add(defineThresholdMessageLabel);
        voteConstraints.anchor = GridBagConstraints.WEST;
        voteConstraints.weightx = 1.0;
        voteConstraints.insets= new java.awt.Insets(0,40,0,0);
        //voteConstraints.gridwidth = GridBagConstraints.RELATIVE;
        voteConstraints.gridwidth = 1;
        //$
           //nodeLabel.setOpaque(true);
           //nodeLabel.setBackground(Color.YELLOW);
        //$
        voteLayout.setConstraints(nodeLabel,voteConstraints);
        votePanel.add(nodeLabel);
        voteConstraints.insets = new java.awt.Insets(0,0,0,0);
        voteLayout.setConstraints(nodeThresholdSlider,voteConstraints);
        //$
          //nodeThresholdSlider.setOpaque(true);
          //nodeThresholdSlider.setBackground(Color.GREEN);
        //$
        votePanel.add(nodeThresholdSlider);
        percentageDisplay = new JLabel(nodeThresholdSlider.getValue()+ "%");
        nodeThresholdSlider.addChangeListener(this);
        voteConstraints.gridwidth = GridBagConstraints.REMAINDER;
        voteConstraints.insets = new java.awt.Insets(0,0,0,40);
        voteLayout.setConstraints(percentageDisplay,voteConstraints);
        votePanel.add(percentageDisplay);
        
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
        voteConstraints.gridwidth = 1;
        voteConstraints.insets= new java.awt.Insets(0,40,0,0);
        voteLayout.setConstraints(linkPanel,voteConstraints);
        votePanel.add(linkPanel);
        voteConstraints.insets= new java.awt.Insets(0,0,0,0);
        voteLayout.setConstraints(linkThresholdSlider,voteConstraints);
        votePanel.add(linkThresholdSlider);
        linkPercentageDisplay = new JLabel(linkThresholdSlider.getValue()+"%");
        linkThresholdSlider.addChangeListener(this);
        voteConstraints.insets = new java.awt.Insets(0,0,0,40);
        voteConstraints.gridwidth = GridBagConstraints.REMAINDER;
        voteLayout.setConstraints(linkPercentageDisplay,voteConstraints);
        votePanel.add(linkPercentageDisplay);
    }
    
    public void stateChanged(ChangeEvent e)
    {
        if(e.getSource()==nodeThresholdSlider)
        {
            percentageDisplay.setText(nodeThresholdSlider.getValue() + "%");
            // todo: make LWMergeMap a changelistener in future -- waiting on 
            // focus problems with activeMap
            if(getActiveMap() instanceof LWMergeMap)
            {
                if(!nodeThresholdSlider.getValueIsAdjusting())
                {
                    //generateMergeMap();
                    //need to fill map list as well...
                    
                   LWMergeMap am = (LWMergeMap)getActiveMap();
                   am.setNodeThresholdSliderValue(nodeThresholdSlider.getValue());
                   
                   System.out.println("mmc: state changed: active map list size: " + am.getMapList().size());
                    
                   ((LWMergeMap)getActiveMap()).recreateVoteMerge();
                }
            }
        }
        if(e.getSource()==linkThresholdSlider)
        {
            linkPercentageDisplay.setText(linkThresholdSlider.getValue() + "%");
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
            }
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
          // comment back in to see focus error (and to enable browse to base map) 
          if(baseChoice.getItemCount() == 0 || baseChoice.getSelectedItem() == null )
              return;
          if(baseChoice.getSelectedItem().equals("other"));
            {    
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
            if(!baseChoice.getSelectedItem().equals("other"))
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
            JFileChooser choose = new JFileChooser();
            choose.setFileFilter(new VueFileFilter(VueFileFilter.VUE_DESCRIPTION));
            choose.showDialog(this,"Set Base Map");
            selectedBaseFile = choose.getSelectedFile();
            if(selectedBaseFile != null)
            {    
              baseFileField.setText(selectedBaseFile.getName());
              try
              {        
                baseMap = ActionUtil.unmarshallMap(selectedBaseFile);
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
                if(p!=null)
                {
                    p.pack();
                }
            }
            else
            {
                vizPane.remove(weightPanel);
                vizPane.add(votePanel);
                validate();
                if(p!=null)
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
    
    public void generateMergeMap()
    {
            // create new map
            LWMergeMap map = new LWMergeMap(LWMergeMap.getTitle());
            
            //set selection settings
            map.setMapListSelectionType(sp.getMapListSelectionType());
            map.setMapFileList(sp.getMapFileList());
            //todo: send all maps not just the active ones.
            sp.fillMapList();
            System.out.println("mmc: " + sp.getMapList().size());
            map.setMapList(sp.getMapList());
            System.out.println("mmc: map list size: " + map.getMapList().size());
            
            //set base map settings
            map.setBaseMapSelectionType(getBaseMapSelectionType());
            map.setBaseMapFile(getBaseMapFile());
            
            //loadBaseMap here? currently baseMap is either open and will
            //be saved with the merge map or has already been loaded through
            //GUI
            //map.setBaseMap(baseMap);
            //actually set base map below...
            
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
             
               //LWMergeMap map = new LWMergeMap(LWMergeMap.getTitle());
               //fail safe default value for base map is active map
               if(baseMap == null)
               {
                 baseMap = VUE.getActiveMap();
               }
               Object baseMapObject = baseChoice.getSelectedItem();
               if(baseMapObject instanceof LWMap)
                 baseMap = (LWMap)baseMapObject;
               //if(choice.getSelectedItem().equals(ALL_TEXT))
               //{
               //Iterator <LWMap> i = VUE.getLeftTabbedPane().getAllMaps();
               //while(i.hasNext())
               //{
               // mapList.add(i.next());
               //}
               
               //sp.fillMapList();
               mapList = map.getMapList();//= sp.getMapList();
               //need to get from sp
               //map.setSelectChoice("all");
               
               map.setBaseMap(baseMap);
               
               createWeightedMerge(map);
               VUE.displayMap(map);
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
    
    public void createWeightedMerge(LWMergeMap map)
    {
        
        ArrayList<ConnectivityMatrix> cms = new ArrayList<ConnectivityMatrix>();
        Iterator<LWMap> i = mapList.iterator();
        while(i.hasNext())
        {
          cms.add(new ConnectivityMatrix(i.next()));
        }
        
        ArrayList<Style> styles = new ArrayList<Style>();
        ArrayList<Style> linkStyles = new ArrayList<Style>();
        
        for(int si=0;si<5;si++)
        {
            styles.add(StyleMap.getStyle("node.w" + (si +1)));
        }
        
        for(int lsi=0;lsi<5;lsi++)
        {
            linkStyles.add(StyleMap.getStyle("link.w" + (lsi +1)));
        }
        
        //System.out.println("mmc: create weight aggregate");
        WeightAggregate weightAggregate = new WeightAggregate(cms);
        
        //compute and create nodes in Merge Map, apply just background style for now
        Iterator children = baseMap.getNodeIterator();
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
           System.out.println("mmc: score: " + score);
           System.out.println("mmc: getInterval(score): " + getInterval(score));
           Style currStyle = styles.get(getInterval(score)-1);
           //System.out.println("Weighted Merge Demo: " + currStyle + " score: " + score);
           node.setFillColor(Style.hexToColor(currStyle.getAttribute("background")));
           map.addNode(node);
        }
        
        //compute and create links in Merge Map
        Iterator children1 = map.getNodeIterator();
        while(children1.hasNext()) {
           LWNode node1 = (LWNode)children1.next();
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
    
    public void createVoteMerge(LWMergeMap map)
    {
        ArrayList<ConnectivityMatrix> cms = new ArrayList<ConnectivityMatrix>();
        Iterator<LWMap> i = map.getMapList().iterator();
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
           if(voteAggregate.isNodeVoteAboveThreshold(Util.getMergeProperty(comp)) ){
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
                  int c = voteAggregate.getConnection(Util.getMergeProperty(node1),Util.getMergeProperty(node2));
                  if(c >0) {
                     map.addLink(new LWLink(node1,node2));
                  }
               }
           }
        }
        
    }
    
    public void refreshSettings(final LWMergeMap map)
    {
        sp.setMapListSelectionType(map.getMapListSelectionType());
        
        mapList = map.getMapList();
        
        // base map settings
        selectedBaseFile = map.getBaseMapFile();
        baseMap = map.getBaseMap();
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
        }
        

        // visualization settings
        
        vizChoice.setSelectedIndex(map.getVisualizationSelectionType());
        
        nodeThresholdSlider.setValue(map.getNodeThresholdSliderValue());
        linkThresholdSlider.setValue(map.getLinkThresholdSliderValue());
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
                if(p!=null)
                {
                    p.pack();
                }
            }
            else
            {
                vizPane.remove(weightPanel);
                vizPane.add(votePanel);
                validate();
                if(p!=null)
                {
                    p.pack();
                }
            }
        
        
        //actually loads current styles and settings -- *fix names* -- probably only should load
        //defaults (using StyleReader) if there is no style loaded at all
        // *however, doesn't yet provide proper style load unless weight panel already open...*
        weightPanel.loadDefaultStyles();
        weightPanel.loadDefaultSettings();
            
 
    }
    
    public void setActiveMap(LWMap map)
    {
        String selectString = "Select";
        
        boolean noMapsLoaded = !(VUE.getLeftTabbedPane().getAllMaps().hasNext());
        
        if(noMapsLoaded)
        {
            baseChoice.removeAllItems();
            baseChoice.addItem(selectString);
            return;
        }   
        else
        {
            baseChoice.removeItem(selectString);
        }
        /*if(map==null)
        {
            return;
        }*/
        LWMap previousMap = activeMap;
        activeMap = map;
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
            generate.setEnabled(false);
        }
        else
        {
            generate.setEnabled(true);
        }

        //selectPanelHolder.remove(sp);
        if(selectPanels.containsKey(activeMap))
        {
           // maintains run-time settings even if not merge map
           // sp = (SelectPanel)selectPanels.get(activeMap);
        }
        else if(activeMap instanceof LWMergeMap)
        {
            if(previousMap!=null)
            {
              //sp = (SelectPanel)selectPanels.get(previousMap);
            }
            else
            {
                
                //sp = new SelectPanel();
            }
        }
        else
        {
            // retains run-time settings even for non merge maps
            //sp = new SelectPanel();
        }
        
        //selectPanelHolder.add(sp);
        //selectPanels.put(activeMap,sp);
        //selectPanelHolder.repaint();
        if(p!=null)
        {    
          p.pack();
        }

    }
    
    public LWMap getActiveMap()
    {
        return activeMap;
    }
    
    public void activeMapChanged(LWMap map)
    {
        //System.out.println("Merge Maps Chooser: active map changed " + map.getLabel());
        setActiveMap(map);
    }
    
    private class SelectPanel extends JPanel implements ActionListener
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
            }
            if(e.getSource() == browseButton)
            {
                JFileChooser fileChooser = new JFileChooser();
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
            if(p!=null)
            {
                p.pack();
            }
        }
        
        public List<LWMap> getMapList()
        {
            return mapList;
        }
        
        public List<File> getMapFileList()
        {
            return mapFileList;
        }
        
        public void fillMapList()
        {
               mapList.clear();
               
               if(choice.getSelectedItem().equals(ALL_TEXT))
               {
                 Iterator <LWMap> i = VUE.getLeftTabbedPane().getAllMaps();
                 while(i.hasNext())
                 {
                   if(!(i instanceof LWMergeMap))
                   {
                     mapList.add(i.next());
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
               if(baseMap == null)
               {
                 baseMap = VUE.getActiveMap();
               }
               Object baseMapObject = baseChoice.getSelectedItem();
               if(baseMapObject instanceof LWMap)
                 baseMap = (LWMap)baseMapObject;
               
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
                 map.setMapFileList(getMapFileList());
                 //map.setActiveMapList(activeFileList);
                 map.setSelectChoice("list");
               }

               map.setNodeThresholdSliderValue(nodeThresholdSlider.getValue());
               map.setLinkThresholdSliderValue(linkThresholdSlider.getValue());
               createVoteMerge(map);
               VUE.displayMap(map);
               
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
