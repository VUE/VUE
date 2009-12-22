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

package tufts.vue;

import tufts.Util;
import tufts.vue.gui.*;
import tufts.vue.gui.formattingpalette.ButtonlessComboBoxUI;


import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.TreeUI;
import javax.swing.table.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;
import javax.swing.border.*;

import edu.tufts.vue.preferences.implementations.ShowAgainDialog;
import edu.tufts.vue.preferences.ui.tree.VueTreeUI;

/**
 * Provides a panel that displays the PathwayTable with note panel
 * editable view of currently selected pathway or item on pathway, as
 * well as controls for navigating through the pathway, and
 * adding/removing new pathways, adding/removing items to the
 * pathways.
 *
 * @see PathwayTable
 * @see PathwayTableModel
 * @see LWPathwayList
 * @see LWPathway
 *
 * @author  Daisuke Fujiwara
 * @author  Scott Fraize
 * @version $Revision: 1.139 $ / $Date: 2009-12-22 18:17:38 $ / $Author: sfraize $
 */

public class PathwayPanel extends JPanel
    implements ActionListener
{    
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(PathwayPanel.class);

    private Frame mParentFrame;
    
    private final VueButton btnAddSlide = new VueButton("presentationDialog.button.makeSlides",this);
    private final VueButton btnMergeInto = new VueButton("presentationDialog.button.mergeInto",this);
    private final VueButton btnLiveMap = new VueButton("presentationDialog.button.liveMap", this);
    
    //edit
    private final VueButton btnEditSlides = new VueButton("presentationDialog.button.preview", this);
  //  private final VueButton btnPreviewFull = new VueButton("presentationDialog.button.previewFull", this);
       
    //master slide
    //private final VueButton btnMasterSlide = new VueButton("presentationDialog.button.masterSlide",this);    													
    private final VueButton btnRefresh = new VueButton("presentationDialog.button.refresh",this);
    
    //new    
    private final VueButton btnPresentationCreate = new VueButton("presentationDialog.button.add",this);        
    private final VueButton btnPresentationDelete = new VueButton("presentationDialog.button.delete",this);
    

    //filter
    private final JToggleButton btnPathwayOnly = new VueButton.Toggle("presentationDialog.button.viewAll",this);
    
    
    //map view
//     private ImageDropDown btnShowSlides = new ImageDropDown(VueResources.getImageIcon("presentationDialog.button.showSlides.raw"),
//                                                             VueResources.getImageIcon("presentationDialog.button.showNodes.raw"),
//                                                             VueResources.getImageIcon("presentationDialog.button.showSlides.disabled"));
    // hack for now as single button just to get this working:
    private final JToggleButton btnShowSlides = new VueButton.Toggle("presentationDialog.button.showSlides",this);
   // private final JToggleButton btnShowSlides2 = new VueButton.Toggle("presentationDialog.button.showSlides",this);    
    
    //playback mode
    //private final ImageDropDown btnPlayMaps = new ImageDropDown(VueResources.getImageIcon("presentationDialog.button.playMap.raw"),
    //                                                      VueResources.getImageIcon("presentationDialog.button.playSlides.raw"),
    //                                                      VueResources.getImageIcon("presentationDialog.button.playSlides.disabled"));
    
    private final VueButton btnPlay = new VueButton("presentationDialog.button.play",this);
    // if have a real action to fire, can always just do this:
    //private final VueButton btnPlay = new VueButton("presentationDialog.button.play", Actions.LaunchPresentation);
                                            
    //Section Labels for the top
    private JLabel lblCreateSlides = new JLabel(VueResources.getString("presentationDialog.createslides.label"));
    private JLabel lblEditSlides = new JLabel(VueResources.getString("presentationDialog.editslides.label"));
    private JLabel lblViewSlide = new JLabel(VueResources.getString("presentationDialog.viewslide.label"));
    private JLabel lblNew = new JLabel(VueResources.getString("presentationDialog.new.label"));    
    //private JLabel lblFilter = new JLabel(VueResources.getString("presentationDialog.filter.label"));
    private JLabel lblRefresh = new JLabel(VueResources.getString("presentationDialog.refresh.label"));
    //private JLabel lblMapView = new JLabel(VueResources.getString("presentationDialog.mapview.label"));
    //private JLabel lblMapView = new JLabel("Slide Icons");
    //private JLabel lblPlayback = new JLabel(VueResources.getString("presentationDialog.playback.label"));
    private JLabel lblPresentationBackground = new JLabel(VueResources.getString("presentationDialog.presentationBackground.label"));
    private ColorMenuButton bkgrndColorButton = null;

    public PathwayTable mPathwayTable;
    private PathwayTableModel mTableModel;
      
    
    private JLabel pathLabel;           // updated for current PathwayTable selection
    private JLabel pathElementLabel;    // updated for current PathwayTable selection
    private JTextArea notesArea;        // updated for current PathwayTable selection
    
    private LWPathway.Entry mSelectedEntry;
    private boolean mNoteKeyWasPressed = false;

    private static final ShowAgainDialog sad = null;
    private final Color BGColor = new Color(241, 243, 246);
 
    //MK - Despite these not being used on the presentation window anymore they are still
    //referenced by the pathway tool so they're sticking around for now.
//     private static final Action path_rewind = new PlayerAction("pathway.control.rewind");
//     private static final Action path_backward = new PlayerAction("pathway.control.backward");
//     private static final Action path_forward = new PlayerAction("pathway.control.forward");
//     private static final Action path_last = new PlayerAction("pathway.control.last");

  //  private final JTabbedPane tabbedPane = new JTabbedPane();

    private static PathwayPanel Singleton;
    private static DeleteSlideDialog dsd = null;
    
    public PathwayPanel(Frame parent) 
    {
        Singleton = this;
        dsd = new DeleteSlideDialog(parent);                  
    	//DISABLE THE NOTES BUTTONS FOR NOW UNTIL WE FIGURE OUT WHAT THEY DO -MK
    	Icon i =VueResources.getIcon("presentationDialog.button.viewAll.raw");
    	addToolTips();
    //	btnAnnotateSlide.setEnabled(false);
    //	btnAnnotatePresentation.setEnabled(false);
    	btnMergeInto.setEnabled(false);
    	//btnPlayMaps.setEnabled(false);
    	//btnLiveMap.setEnabled(false);
    	//btnPreviewFull.setEnabled(false);
    	btnShowSlides.setEnabled(true);
    	btnShowSlides.setSelected(true);
    //	btnShowSlides2.setEnabled(true);
    //	btnShowSlides2.setSelected(true);
    	//btnPlayMaps.setEnabled(false);
    //	btnPlaySlides.setEnabled(false);
//    	btnDisplayAsMap.setEnabled(false);
  //  	btnDisplayAsText.setEnabled(false);
    	//END    	

        Color[] bkColors = VueResources.getColorArray("prsntBkgrndColorValues");
        //String[] textColorNames = VueResources.getStringArray("textColorNames");
        bkgrndColorButton = new ColorMenuButton(bkColors, true);

        LWPathway.setShowSlides(btnShowSlides.isSelected());
    	
        //Font defaultFont = new Font("Helvetica", Font.PLAIN, 12);
        //Font highlightFont = new Font("Helvetica", Font.BOLD, 12);
        final Font defaultFont = getFont();
        final Font boldFont = defaultFont.deriveFont(Font.BOLD);
        final Font smallFont = defaultFont.deriveFont((float) boldFont.getSize()-2);
        final Font smallBoldFont = smallFont.deriveFont(Font.BOLD);
    
        mParentFrame = parent;
        setBorder(new EmptyBorder(4, 4, 7, 4));

        //-------------------------------------------------------
        // Set up the PathwayTableModel, PathwayTable & Listeners
        //-------------------------------------------------------

        mTableModel = new PathwayTableModel();
        mPathwayTable = new PathwayTable(mTableModel);
        
        mPathwayTable.setBackground(BGColor);
        
   
        notesArea = new JTextArea("");
        notesArea.setColumns(5);
        notesArea.setWrapStyleWord(true);
        notesArea.setAutoscrolls(true);
        notesArea.setLineWrap(true);
        notesArea.setBackground(Color.white);
        notesArea.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) { mNoteKeyWasPressed = true; }
                public void keyReleased(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_ENTER) ensureNotesSaved(); }
            });
        notesArea.addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent e) {
                    if (DEBUG.PATHWAY) System.out.println("PathwayPanel.notesArea     focusLost to " + e.getOppositeComponent());
                    ensureNotesSaved();
                }
                public void focusGained(FocusEvent e) {
                    if (DEBUG.PATHWAY) System.out.println("PathwayPanel.notesArea focusGained from " + e.getOppositeComponent());
                }
            });


        JPanel noteLabelPanel = new VueUtil.JPanelAA();
        JLabel notesLabel = new JLabel(VueResources.getString("jlabel.nodes"));
        //notesLabel.setFont(smallFont);
        noteLabelPanel.setLayout(new BoxLayout(noteLabelPanel, BoxLayout.X_AXIS));
        noteLabelPanel.add(notesLabel);
        noteLabelPanel.add(pathLabel = new JLabel(""));
        noteLabelPanel.add(pathElementLabel = new JLabel(""));
        pathLabel.setFont(smallBoldFont);
        pathElementLabel.setFont(smallFont);
        pathElementLabel.setForeground(Color.red.darker());

     //   JPanel notesPanel = new JPanel(new BorderLayout(0,0));
     //   notesPanel.add(noteLabelPanel, BorderLayout.NORTH);
    //    notesPanel.setBorder(new EmptyBorder(7,0,0,0));
    //    notesPanel.add(new JScrollPane(notesArea), BorderLayout.CENTER);


        
        //-------------------------------------------------------
        // Layout for the table components 
        //-------------------------------------------------------
        
        GridBagLayout bag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(bag);
                        
        c.insets = new Insets(1,35,1,1);                
        c.weightx = 1.0; // make sure everything can fill to width
        c.anchor=GridBagConstraints.WEST;
        c.gridx=0;
        c.gridy=0;
        c.gridheight=1;
        c.gridwidth=1;
        add(btnPresentationCreate,c);
        
        c.insets = new Insets(1,1,1,1);                
        c.weightx = 1.0; // make sure everything can fill to width
        c.anchor=GridBagConstraints.WEST;
        c.gridx=1;
        c.gridy=0;
        c.gridheight=1;
        c.gridwidth=1;
        JLabel createPres = new JLabel(VueResources.getString("presentationDiaaog.presentationName.text"));
        //createPres.setFont(smallFont);
        createPres.setFont(tufts.vue.gui.GUI.LabelFace);
        add(createPres,c);


        
        c.insets = new Insets(1,1,1,1);                
        c.weightx = 1.0; // make sure everything can fill to width
        c.anchor=GridBagConstraints.EAST;
        c.gridx=2;
        c.gridy=0;
        c.gridheight=1;
        c.gridwidth=1;
        JLabel lblPlay = new JLabel(VueResources.getString("presentationDialog.playback.label"));
        lblPlay.setFont(tufts.vue.gui.GUI.LabelFace);
        lblPlay.setLabelFor(btnPlay);
        
        add(btnPlay,c);
        if (VUE.isApplet())
        	btnPlay.setEnabled(false);
        c.insets = new Insets(1,1,1,35);        
        c.gridwidth = GridBagConstraints.REMAINDER; // put everything in one column
        c.weightx = 1.0; // make sure everything can fill to width
        c.anchor=GridBagConstraints.EAST;        
        c.gridheight=1;
        c.gridwidth=1;
        c.gridy=0;
        c.gridx=3;
        //lblPlay.setFont(smallFont);
        add(lblPlay,c);
        if (VUE.isApplet())
        	lblPlay.setEnabled(false);
        
       
        c.anchor=GridBagConstraints.CENTER;
        //-------------------------------------------------------
        // add pathway create/delete/lock control panel
        //-------------------------------------------------------

        c.fill = GridBagConstraints.HORIZONTAL;
        //bag.setConstraints(pathwayMasterPanel, c);
        //add(pathwayMasterPanel);        
        //JPanel playbackPanel = new JPanel();
        JPanel slidePanel = new JPanel();
        
        //buildPlaybackPanel(playbackPanel);
        buildSlidePanel(slidePanel);
        //tabbedPane.add(VueResources.getString("presentationDialog.slideTab.title"), slidePanel);
        //tabbedPane.add(VueResources.getString("presentationDialog.playbackTab.title"), playbackPanel);
  
    //    setLayout(new BorderLayout());
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.add(slidePanel);
        p.setBorder(new RoundedBorder());
        c.insets = new Insets(1,0,0,4);
        c.gridheight=1;
        c.gridwidth=4;
        c.gridy=1;
        c.gridx=0;
        add(p,c);
        c.insets = new Insets(8,1,1,1);
        //-------------------------------------------------------
        // add the PathwayTable
        //-------------------------------------------------------

        c.fill = GridBagConstraints.BOTH;
        c.weighty = 2.5;
        JScrollPane tablePane = new JScrollPane(mPathwayTable);
        tablePane.setPreferredSize(new Dimension(getWidth(), 180));
        bag.setConstraints(tablePane, c);
        c.gridheight=1;
        c.gridwidth=4;
        c.gridy=2;
        c.gridx=0;
        add(tablePane,c);
        
        JPanel enclosingPanel = new JPanel();
        enclosingPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy=0;
        gbc.gridx=0;
        gbc.gridwidth=1;
        gbc.anchor=GridBagConstraints.WEST;
        gbc.fill=GridBagConstraints.BOTH;
        gbc.insets = new Insets(0,0,0,0);
        lblPresentationBackground.setFont(smallFont);
        enclosingPanel.add(lblPresentationBackground,gbc);
        
        gbc.anchor=GridBagConstraints.WEST;     
        gbc.gridx=1;
        if (VUE.getActiveMap() == null)        	
        	bkgrndColorButton.selectValue(new Color(32,32,32));
        else
        	bkgrndColorButton.selectValue(VUE.getActiveMap().getPresentationBackgroundValue());
        bkgrndColorButton.setPropertyKey(LWKey.PresentationColor);
        bkgrndColorButton.addPropertyChangeListener( new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) 
            {
            
            	 if (e instanceof LWPropertyChangeEvent == false)
                     return;
            	 Color color = null;
                
                 color= (Color) e.getNewValue();
                
                                
                
                final String colorString = "#" + Integer.toHexString(color.getRGB()).substring(2);
                //System.out.println(colorString); 
                VUE.getActiveMap().setPresentationBackgroundValue(color);
            }
        });
        
        tufts.vue.VUE.addActiveListener(tufts.vue.LWMap.class,new ActiveListener()
        {

			public void activeChanged(ActiveEvent e) 
			{
				//System.out.println("ACTIVE MAP CHANGED");
				if (e== null || e.active == null)
					return;
				LWMap map = (LWMap)e.active;
				bkgrndColorButton.selectValue(map.getPresentationBackgroundValue());
			}
        }); 
        //gbc.insets = new Insets(0,0,0,300);                       
        enclosingPanel.add(bkgrndColorButton,gbc);
        gbc.anchor=GridBagConstraints.EAST;
        //gbc.insets = new Insets(0,300,0,0);
        gbc.gridx=2;
        gbc.weightx=9.0;
        gbc.fill=GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(0,0,0,0);
        btnPresentationDelete.setBorderPainted(false);
        btnPresentationDelete.setContentAreaFilled(false);
        btnPresentationDelete.setBorder(BorderFactory.createEmptyBorder());
        enclosingPanel.add(btnPresentationDelete,gbc);
        
        c.gridheight=1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth=4;
        c.gridy=4;
        c.weighty=0.05;
        c.gridx=0;
        c.anchor=GridBagConstraints.WEST;    
        //enclosingPanel.setBackground(Color.red);
        add(enclosingPanel,c);
        
        //-------------------------------------------------------
        // Add the notes panel
        //-------------------------------------------------------
       /* c.anchor=GridBagConstraints.CENTER;
        c.insets = new Insets(1,1,1,1);
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        c.gridheight=1;
        c.gridwidth=4;
        c.gridy=4;
        c.gridx=0;*/
      //  notesPanel.setPreferredSize(new Dimension(getWidth(), 80));
      //  bag.setConstraints(notesPanel, c);
       // add(notesPanel);

        //-------------------------------------------------------
        // Disable all that need to be
        //-------------------------------------------------------
        
        updateEnabledStates();

        //-------------------------------------------------------
        // Set up the listeners
        //-------------------------------------------------------                        
        VUE.addActiveListener(LWPathway.class, this);
        VUE.addActiveListener(LWPathway.Entry.class, this);
        
        VUE.getSelection().addListener(new LWSelection.Listener() {
                public void selectionChanged(LWSelection s) {
//                     final LWPathway curPath = getSelectedPathway();
//                     if (s.size() == 1 && s.first().inPathway(curPath)) {
//                         if (DEBUG.Enabled)
//                             System.out.println(Util.TERM_RED
//                                                + "PathwayPanel skipping unsafe current path index update of "
//                                                + curPath
//                                                + Util.TERM_CLEAR);
//                         //curPath.setIndex(curPath.firstIndexOf(s.first()));
//                     } else
                        updateEnabledStates();
                }
            }     
        );          
    }
    
 /*   public void paint(Graphics g)
    {
    	super.paint(g);
    	System.out.println("master panel :" + masterPanel.getSize().toString());
    	System.out.println("new panel :" + newPanel.getSize().toString());
    }*/
    
    class RoundedBorder extends AbstractBorder {
    	private final static int MARGIN = 4;
     
    	public void paintBorder( Component c, Graphics g, int x, int y, int width, int height ) {
    		g.setColor(Color.gray);
    		g.drawRoundRect( x, y, width - 1, height - 1, MARGIN*2, MARGIN*2);
    	}
     
    	public Insets getBorderInsets( Component c ) {
    		return new Insets( 2,2,2,2);
    	}
     
    	public Insets getBorderInsets( Component c, Insets insets ) {
    		insets.left = insets.top = insets.right = insets.bottom = MARGIN;
    		return insets;
    	}
         
    }
    public static DeleteSlideDialog getDeleteSlideDialog()
    {
    	return dsd;
    }
    private void addToolTips()
    {
    	String baseProp = "presentationDialog.button.";    	
    	final String helpTextHeader = VueResources.getString("dockWindow.helpTextHeader");
        final String helpTextFooter = VueResources.getString("dockWindow.helpTextFooter");
        
    	btnAddSlide.setToolTipText(helpTextHeader+ VueResources.getString(baseProp+"makeSlides.tooltip") + helpTextFooter);
        btnMergeInto.setToolTipText(helpTextHeader+ VueResources.getString(baseProp+"mergeInto.tooltip") + helpTextFooter);
        btnLiveMap.setToolTipText(helpTextHeader+ VueResources.getString(baseProp+"liveMap.tooltip") + helpTextFooter);
        
        //edit
        btnEditSlides.setToolTipText(helpTextHeader+ VueResources.getString(baseProp+"preview.tooltip") + helpTextFooter);
        
        //new    
        btnPresentationCreate.setToolTipText(helpTextHeader+ VueResources.getString(baseProp+"add.tooltip") + helpTextFooter);        
        btnPresentationDelete.setToolTipText(helpTextHeader+ VueResources.getString(baseProp+"delete.tooltip") + helpTextFooter);
        

        //filter
        btnPathwayOnly.setToolTipText(helpTextHeader+ VueResources.getString(baseProp+"viewAll.tooltip") + helpTextFooter);
        
        // hack for now as single button just to get this working:
        btnShowSlides.setToolTipText(helpTextHeader+ VueResources.getString(baseProp+"showNodes.tooltip") + helpTextFooter);
        btnPlay.setToolTipText(helpTextHeader+ VueResources.getString(baseProp+"play.tooltip") + helpTextFooter);               	
    }
    
    private void buildSlidePanel(JPanel slidePanel)
    {
    	slidePanel.setLayout(new BoxLayout(slidePanel,BoxLayout.X_AXIS));
    	JPanel createSlidePanel = new JPanel();
    	JPanel editPanel = new JPanel();
    	JPanel viewPanel = new JPanel();
    	JPanel refreshPanel = new JPanel();
    	//JPanel newPanel = new JPanel();	
    	//JPanel deletePanel = new JPanel();
    	
    	DividerPanel p1 = new DividerPanel(25);
       // DividerPanel p2 = new DividerPanel(25);
        DividerPanel p3 = new DividerPanel(25);
        DividerPanel p4 = new DividerPanel(25);
        
        
        java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();
        
        createSlidePanel.setLayout(new GridBagLayout());
        editPanel.setLayout(new GridBagLayout());
        viewPanel.setLayout(new GridBagLayout());
        refreshPanel.setLayout(new GridBagLayout());
        //newPanel.setLayout(new GridBagLayout());
        //deletePanel.setLayout(new GridBagLayout());
        
        lblCreateSlides.setFont(VueResources.getFont("node.icon.font"));
        lblEditSlides.setFont(VueResources.getFont("node.icon.font"));
        lblRefresh.setFont(VueResources.getFont("node.icon.font"));
        lblViewSlide.setFont(VueResources.getFont("node.icon.font"));
        lblNew.setFont(VueResources.getFont("node.icon.font"));
        
        
        //START CREATE SLIDE PANEL
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        gbConstraints.gridwidth = 0;
        gbConstraints.gridheight = 1;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        createSlidePanel.add(lblCreateSlides,gbConstraints);
        
        gbConstraints.gridx=0;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.gridy=1;
        gbConstraints.anchor=GridBagConstraints.WEST;
        gbConstraints.fill=GridBagConstraints.NONE;
        createSlidePanel.add(btnAddSlide,gbConstraints);
    
        gbConstraints.gridx=1;
        gbConstraints.gridy=1;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.anchor=GridBagConstraints.WEST;
        createSlidePanel.add(btnMergeInto,gbConstraints);
        
        gbConstraints.gridx=2;
        gbConstraints.gridy=1;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.anchor=GridBagConstraints.WEST;
        createSlidePanel.add(btnLiveMap,gbConstraints);
        //END CREATE SLIDE PANEL
        
        //START EDIT PANEL
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        gbConstraints.gridwidth = 0;
        gbConstraints.gridheight = 1;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        gbConstraints.fill=GridBagConstraints.NONE;        
        editPanel.add(lblEditSlides,gbConstraints);
         
        gbConstraints.gridx=0;
        gbConstraints.gridy=1;       
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.anchor=GridBagConstraints.WEST;
        editPanel.add(btnEditSlides,gbConstraints);
        
       // gbConstraints.gridx=1;
       // gbConstraints.gridy=1;
       // gbConstraints.gridwidth = 1;
       // gbConstraints.gridheight = 1;
       // gbConstraints.fill=GridBagConstraints.NONE;
       // gbConstraints.anchor=GridBagConstraints.WEST;
     //   editPanel.add(btnMasterSlide,gbConstraints);
        //END EDIT PANEL
        
        //START VIEW PANEL        
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        gbConstraints.gridwidth = 0;
        gbConstraints.gridheight = 1;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        
        viewPanel.add(lblViewSlide,gbConstraints);
        
        gbConstraints.insets = new Insets(0,0,0,3);
        gbConstraints.gridx=0;
        gbConstraints.gridy=1;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
                
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.anchor=GridBagConstraints.WEST;
        viewPanel.add(btnPathwayOnly,gbConstraints);     
        
        
        gbConstraints.insets = new Insets(0,3,0,0);
        gbConstraints.gridx=1;
        gbConstraints.gridy=1;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.anchor=GridBagConstraints.WEST;
        viewPanel.add(btnShowSlides,gbConstraints);  
        if (VUE.isApplet())
        	btnShowSlides.setVisible(false);
        //END MASTER PANEL
        gbConstraints.insets = new Insets(0,0,0,0);
        
        //START NEW PANEL        
        
        gbConstraints.gridx=0;
        gbConstraints.gridy=0;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.insets = new Insets(7,0,0,0);
        gbConstraints.anchor=GridBagConstraints.WEST;
        refreshPanel.add(lblRefresh,gbConstraints);
        
        gbConstraints.gridx=0;
        gbConstraints.gridy=1;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.insets = new Insets(3,0,6,0);
        gbConstraints.anchor=GridBagConstraints.WEST;
        refreshPanel.add(btnRefresh,gbConstraints);                
    /*    
        gbConstraints.gridx=1;
        gbConstraints.gridy=1;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.insets = new Insets(3,5,6,0);
        newPanel.add(btnPresentationDelete,gbConstraints);
        //END DELETE PANEL        
      */  
        slidePanel.add(Box.createHorizontalStrut(1));
        slidePanel.add(createSlidePanel);
        slidePanel.add(Box.createHorizontalStrut(1));
        slidePanel.add(p1);
        slidePanel.add(Box.createHorizontalStrut(1));
        slidePanel.add(editPanel);                
        slidePanel.add(Box.createHorizontalStrut(1));
        slidePanel.add(p3);
        slidePanel.add(Box.createHorizontalStrut(3));
        slidePanel.add(refreshPanel);
        slidePanel.add(p4);
        slidePanel.add(Box.createHorizontalStrut(1));
        slidePanel.add(viewPanel);
        //slidePanel.add(Box.createHorizontalStrut(5));
        //slidePanel.add(masterPanel);        
        //slidePanel.add(p4);
        slidePanel.add(Box.createHorizontalStrut(5));                
    //    slidePanel.add(newPanel);        
        //slidePanel.add(p2);
        //slidePanel.add(Box.createHorizontalStrut(1));
       // slidePanel.add(deletePanel);
       // slidePanel.add(Box.createHorizontalStrut(1));        
       
       
        return;
    }
    
       
  /*  private void buildPlaybackPanel(JPanel presentationPanel)
    {
    	presentationPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    	JPanel filterPanel = new JPanel();
    	JPanel mapViewPanel = new JPanel();
    	JPanel playBackPanel = new JPanel();
    	    	
    	DividerPanel p1 = new DividerPanel(25);
        DividerPanel p2 = new DividerPanel(25);
                
        java.awt.GridBagConstraints gbConstraints = new java.awt.GridBagConstraints();
        
        filterPanel.setLayout(new GridBagLayout());
        mapViewPanel.setLayout(new GridBagLayout());
        playBackPanel.setLayout(new GridBagLayout());
        
        lblFilter.setFont(VueResources.getFont("node.icon.font"));
        lblMapView.setFont(VueResources.getFont("node.icon.font"));
        lblPlayback.setFont(VueResources.getFont("node.icon.font"));        
        
        
        //START FILTER PANEL
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        gbConstraints.gridwidth = 0;
        gbConstraints.gridheight = 1;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        gbConstraints.weightx=0;
        gbConstraints.weighty=0;
        filterPanel.add(lblFilter,gbConstraints);
        
        gbConstraints.gridx=0;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.gridy=1;
        gbConstraints.fill=GridBagConstraints.BOTH;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        filterPanel.add(btnPathwayOnly,gbConstraints);            
        //END FILTER PANEL
        
        //START MAP VIEW PANEL
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        gbConstraints.gridwidth = 0;
        gbConstraints.gridheight = 1;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        gbConstraints.fill=GridBagConstraints.NONE;        
        gbConstraints.weightx=0;
        gbConstraints.weighty=0;
        //gbConstraints.insets = new Insets(4,15,4,15);
        mapViewPanel.add(lblMapView,gbConstraints);
        
        gbConstraints.gridx=0;
        gbConstraints.gridy=1;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        mapViewPanel.add(btnShowSlides,gbConstraints);    
        //END MAP VIEW PANEL
        
        //START PLAYBACK PANEL        
        gbConstraints.gridx = 0;
        gbConstraints.gridy = 0;
        gbConstraints.gridwidth = 0;
        gbConstraints.gridheight = 1;
        gbConstraints.fill=GridBagConstraints.NONE;
        gbConstraints.anchor=GridBagConstraints.NORTHWEST;
        gbConstraints.weightx=0;
        gbConstraints.weighty=0;
        playBackPanel.add(lblPlayback,gbConstraints);
        
       // gbConstraints.gridx=0;
       // gbConstraints.gridy=1;
       // gbConstraints.gridwidth = 1;
       // gbConstraints.gridheight = 1;
       // gbConstraints.fill=GridBagConstraints.NONE;
       // gbConstraints.anchor=GridBagConstraints.NORTHWEST;
      //  playBackPanel.add(btnPlayMaps,gbConstraints);
        
        gbConstraints.gridx=0;
        gbConstraints.gridy=1;
        gbConstraints.gridwidth = 1;
        gbConstraints.gridheight = 1;
        gbConstraints.insets = new Insets(0,6,0,0);
        gbConstraints.fill=GridBagConstraints.NONE;
        playBackPanel.add(btnPlay,gbConstraints);
        //END PLAYBACK PANEL
        
       
        presentationPanel.add(Box.createHorizontalStrut(1));
        presentationPanel.add(filterPanel);
        presentationPanel.add(Box.createHorizontalStrut(1));
        presentationPanel.add(p1);
        presentationPanel.add(Box.createHorizontalStrut(1));
        presentationPanel.add(mapViewPanel);                
        presentationPanel.add(Box.createHorizontalStrut(1));
        presentationPanel.add(p2);
        presentationPanel.add(Box.createHorizontalStrut(5));
        presentationPanel.add(playBackPanel);        
        presentationPanel.add(Box.createHorizontalStrut(5));                
                                                                         	
        return;
    }
   */ 

    class ImageDropDown extends JPanel {
        
    	ImageIcon[] images;        
        private JComboBox comboList;
        ImageIcon disabledIcon = null;
        public ImageDropDown(ImageIcon icon1, ImageIcon icon2,ImageIcon icon3) {
            super(new BorderLayout());
            
            disabledIcon = icon3;
            
            //Load the pet images and create an array of indexes.
            images = new ImageIcon[2];
            Integer[] intArray = new Integer[2];
            
            intArray[0] = new Integer(0);
            intArray[1] = new Integer(1);
                
            images[0] = icon1;
            images[1] = icon2;                                                      

            //Create the combo box.
            
            comboList = new JComboBox(intArray);
            ComboBoxRenderer renderer= new ComboBoxRenderer();
            renderer.setPreferredSize(new Dimension(20, 20));
            comboList.setRenderer(renderer);
            comboList.setMaximumRowCount(3);
            comboList.setUI(new ButtonlessComboBoxUI());
            comboList.setOpaque(false);
            comboList.setBorder(BorderFactory.createEmptyBorder());
            comboList.setBackground(this.getBackground());
            //Lay out the demo.
            setOpaque(true);
            add(comboList, BorderLayout.PAGE_START);
          //  this.setPreferredSize(new Dimension(comboList.getWidth()-10,comboList.getHeight()));            
        }

        public void setEnabled(boolean enabled)
        {
        	comboList.setEnabled(enabled);
        }
        public JComboBox getComboBox()
        {
        	return comboList;
        }
        
        class ComboBoxRenderer extends JLabel
                               implements ListCellRenderer {
            private Font uhOhFont;

            public ComboBoxRenderer() {
                setOpaque(true);
                setHorizontalAlignment(CENTER);
                setVerticalAlignment(CENTER);
                
            }

            /*
             * This method finds the image and text corresponding
             * to the selected value and returns the label, set up
             * to display the text and image.
             */
            public Component getListCellRendererComponent(
                                               JList list,
                                               Object value,
                                               int index,
                                               boolean isSelected,
                                               boolean cellHasFocus) {
                //Get the selected index. (The index param isn't
                //always valid, so just use the value.)
                int selectedIndex = ((Integer)value).intValue();

                /*if (isSelected) {
                    setBackground(list.getSelectionBackground());
                    setForeground(list.getSelectionForeground());
                } else {
                    setBackground(list.getBackground());
                    setForeground(list.getForeground());
                }
                */
                
                //Set the icon and text.  If icon was null, say so.
                ImageIcon icon = images[selectedIndex];
                setIcon(icon);
                if (!comboList.isEditable())
                	setIcon(disabledIcon);
      
                return this;
            }
        }
//      default offsets for drawing popup arrow via code
        public int mArrowSize = 3;
        public int mArrowHOffset  = -9;
        public int mArrowVOffset = -7;
        public void paint(Graphics g)
        {
        	super.paint(g);
        	
        	        // draw popup arrow
                    Color saveColor = g.getColor();
                    g.setColor( Color.black);
        			
                    int w = getWidth();
                    int h = getHeight();
        			
                    int x1 = w + mArrowHOffset;
                    int y = h + mArrowVOffset;
                    int x2 = x1 + (mArrowSize * 2) -1;
        			
                    //((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    //RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    for(int i=0; i< mArrowSize; i++) { 
                        g.drawLine(x1,y,x2,y);
                        x1++;
                        x2--;
                        y++;
                    }
                    g.setColor( saveColor);
              }
        
    }
    
    
    public void activeChanged(ActiveEvent e, LWPathway.Entry entry) {
     	//updateTextAreas(entry);
        // todo: don't need to always do this: can track if active entry has actually changed
        updateEnabledStates();
        
    }
    
    public void activeChanged(ActiveEvent e, LWPathway pathway) {

        if (pathway == null) {
            if (exclusiveDisplay != null)
                ClearFilter(exclusiveDisplay.getMap());
            return;
        }

        if (btnPathwayOnly.isSelected()) {
            toggleHideEverythingButCurrentPathway(true);
            toggleHideEverythingButCurrentPathway(false);
        }
    }

    private void ensureNotesSaved() {
        if (mNoteKeyWasPressed && mSelectedEntry != null) {
            mNoteKeyWasPressed = false; // do this first or callback in updateTextAreas will cause 2 different setter calls
            mSelectedEntry.setNotes(notesArea.getText());
            VUE.getUndoManager().mark();
        }
    }

    /** Returns the currently selected pathway.  As currently
     * selected must always be same as VUE globally selected,
     * we just return that. */
    private LWPathway getSelectedPathway() {
        return VUE.getActivePathway();
    }

//     public static class PlaybackToolPanel extends JPanel
//     {
//         public PlaybackToolPanel() {
//             super(new GridLayout(1, 4, 1, 0));

//             add(new VueButton(path_rewind));
//             add(new VueButton(path_backward));
//             add(new VueButton(path_forward));
//             add(new VueButton(path_last));
//         }
//     }

//     private static class PlayerAction extends AbstractAction
//     {
//         PlayerAction(String name) {
//             // as we're to be used by a VueButton, store the key
//             // as the action command, not the name, as we don't
//             // want it to show up as the button label
//             putValue(Action.ACTION_COMMAND_KEY, name);
//         }
        
//         public void actionPerformed(ActionEvent e)
//         {
//             LWPathway pathway = VUE.getActivePathway();
//             if (pathway == null)
//                 return;
//             String cmd = e.getActionCommand();
//                  if (cmd.endsWith("rewind"))    pathway.setFirst();
//             else if (cmd.endsWith("backward"))  pathway.setPrevious();
//             else if (cmd.endsWith("forward"))   pathway.setNext();
//             else if (cmd.endsWith("last"))      pathway.setLast();
//             else
//                 throw new IllegalArgumentException(this + " " + e);
                 
//                  //VUE.getUndoManager().mark(); // the above stuff not worth having undoable
//         }

//         private static void setAllEnabled(boolean t) {
//             path_rewind.setEnabled(t);
//             path_backward.setEnabled(t);
//             path_forward.setEnabled(t);
//             path_last.setEnabled(t);
//         }
//     }

    public void actionPerformed(ActionEvent e)
    {
        final Object btn = e.getSource();
        final LWPathway pathway = getSelectedPathway();

        if (DEBUG.PATHWAY) System.out.println(this + " " + e);

        if (pathway == null && btn != btnPresentationCreate)
            return;
        if (btn == btnEditSlides)
        {
        	/*if (VUE.getSlideDock().isShowing())
        		VUE.getSlideDock().setVisible(false);
        	else
        	{
        		VUE.getSlideDock().setVisible(true);
        		VUE.getSlideViewer().showSlideMode();
        	}*/
        	/*long now = System.currentTimeMillis();
    		MapMouseEvent mme = new MapMouseEvent(new MouseEvent(VUE.getActiveViewer(),
                MouseEvent.MOUSE_CLICKED,
                now,
                5,5,5,5,
                false));
    		pathway.getCurrentEntry().getSlide().doZoomingDoubleClick(mme);*/
        	//Actions.PreviewOnMap.actionPerformed(e);
        	if (pathway.getCurrentEntry() != null && pathway.getCurrentEntry().getSlide() != null)
        		Actions.EditSlide.act(pathway.getCurrentEntry().getSlide());
        	else
        		Actions.EditMasterSlide.act(pathway.getMasterSlide());
        }    
        else if (btn == btnPlay)
        {
            Actions.LaunchPresentation.fire(this, e);
        }
        else if (btn == btnRefresh)
        {
            final LWPathway.Entry entry = VUE.getActiveEntry();
            if (entry != null && entry.getSlide().canSync() && entry.getSlide().numChildren() == 0) {
                Actions.SyncToSlide.fire(this, e);
            } else {
        	setFocusable(false);
        	new SyncDialog(mParentFrame, getLocationOnScreen()).setVisible(true);
        	setFocusable(true);
            }
        }
   /*     else if (btn == btnMasterSlide)
        {
        	if (VUE.getSlideDock().isShowing())
        		VUE.getSlideDock().setVisible(false);
        	else
        	{
        		VUE.getSlideDock().setVisible(true);
        		VUE.getSlideViewer().showMasterSlideMode();
        	}
        }*/
        else if (btn == btnAddSlide)  { 
            if (!pathway.isOpen())
                pathway.setOpen(true);
            pathway.add(VUE.getSelection().iterator());
        }
        else if (btn == btnMergeInto)  {
            final LWComponent node = pathway.createMergedNode(VUE.getSelection());
            node.setLocation(VUE.getActiveViewer().getLastMousePressMapPoint());
            VUE.getActiveViewer().getMap().add(node);
            pathway.add(node);
        }
        else if (btn == btnLiveMap)  {
            LWPortal portal = LWPortal.create();
            portal.setCenterAt(VUE.getActiveViewer().getVisibleMapCenter());
            pathway.getMap().add(portal);
            pathway.getMap().sendToBack(portal);
            pathway.add(portal);
            pathway.getUndoManager().mark("New " + portal.getComponentTypeLabel()); 
        }
      //  else if (btn == btnElementUp)   { pathway.moveCurrentUp(); }
      //  else if (btn == btnElementDown) { pathway.moveCurrentDown(); }

        else if (btn == btnPresentationDelete)   
        {
        	Log.info("Current " + pathway.getCurrentEntry());
        	if (pathway.getCurrentEntry() == null)
        	{
        			final Object[] defaultOrderButtons = {VueResources.getString("optiondialog.deletepathway.cancel"),VueResources.getString("optiondialog.deletepathway.delete")};
        			
        			final Object[]	macOrderButtons = {VueResources.getString("optiondialog.deletepathway.delete"),VueResources.getString("optiondialog.deletepathway.cancel")};
        			
        		   int response = VueUtil.option
                   (VUE.getDialogParent(),
                    VueResources.getString("optiondialog.deletepathway.message") ,         
                    VueResources.getString("optiondialog.deletepathway.title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    (Util.isMacPlatform() ? macOrderButtons : defaultOrderButtons),             
                    VueResources.getString("optiondialog.deletepathway.cancel")
                    );
        		if (Util.isMacPlatform() ? response == 0 : response == 1)
        		{
        			//delete the pathway
        			deletePathway(pathway);
        		}
        	}
        	else
        	{
        		//delete the current entry
//        		 This is a heuristic to try and best guess what the user might want to
                // actually remove.  If nothing in selection, and we have a current pathway
                // index/element, remove that current pathway element.  If one item in
                // selection, also remove whatever the current element is (which ideally is
                // usually also the selection, but if it's different, we want to prioritize
                // the current element hilighted in the PathwayTable).  If there's MORE than
                // one item in selection, do a removeAll of everything in the selection.
                // This removes ALL instances of everything in selection, so that, for
                // instance, a SelectAll followed by pathway delete is guaranteed to empty
                // the pathway entirely.

                if (pathway.getCurrentIndex() >= 0 && VUE.ModelSelection.size() < 2) {
                
                	  
                      java.awt.Point p = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
                      p.x -= dsd.getWidth() / 2;
                      p.y -= dsd.getHeight() / 2;
                      dsd.setLocation(p);
                      if (dsd.showAgain())
                      {
                      	dsd.setVisible(true);
                      }
                      
                      if (dsd.getOkCanel())
                    	  pathway.remove(pathway.getCurrentIndex());
                } else {
                    java.awt.Point p = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
                    p.x -= dsd.getWidth() / 2;
                    p.y -= dsd.getHeight() / 2;
                    dsd.setLocation(p);
                    if (dsd.showAgain())
                    {
                    	dsd.setVisible(true);
                    }
                    
                    if (dsd.getOkCanel())
                    	pathway.remove(VUE.getSelection().iterator());
                }
        	}
        //	deletePathway(pathway); 
        }
        else if (btn == btnPresentationCreate)   {
        	setFocusable(false);
        	new PathwayDialog(mParentFrame, mTableModel, getLocationOnScreen()).setVisible(true);
        	setFocusable(true);
        	}
     //   else if (btn == btnLockPresentation)     { pathway.setLocked(!pathway.isLocked()); }
        else if (btn == btnPathwayOnly) {
            toggleHideEverythingButCurrentPathway(!btnPathwayOnly.isSelected());
        } else if (btn == btnShowSlides) {

            Actions.ToggleSlideIcons.fire(this, e);
            
        }
        else {
            Log.warn(this + ": Unhandled action: " + e);
        }

        VUE.getUndoManager().mark();
    }

    void updateShowSlidesButton() {
        btnShowSlides.setSelected(LWPathway.isShowingSlideIcons());
        ActiveInstance.getHandler(LWPathway.Entry.class).redeliver(this);
        //btnShowSlides.firePropertyChange("showSlidesChange", true, false);
    }

    public static PathwayPanel getInstance() {
        return Singleton;
    }

    public static void TogglePathwayExclusiveFilter() {
        boolean doFilter = !Singleton.btnPathwayOnly.isSelected();

        // TODO: really, this combination of code wants to be in LWMap,
        // using Filter objects -- we want to make sure an established
        // filter is always tied to a map...  The LWPathway
        // could provide a Filter instance that knows how
        // to filter everything but it's contents

        // Instead of remembering a pathway ("exclusiveDisplay"),
        // we should remember the filter.

        // Also, this should handle the undo of the deletion
        // of a pathway that had a filter established: be 
        // able to handle this case, and the architecture
        // will be right.
        
//         if (doFilter)
//             FilterForPathway(VUE.getActivePathway());
//         else
//             ClearFilter(VUE.getActiveMap(), null);
        
        Singleton.toggleHideEverythingButCurrentPathway(!doFilter);
        Singleton.btnPathwayOnly.setSelected(doFilter);
    }

    private static void ClearFilter(LWMap map) {
        ClearFilter(map, map.getAllDescendents());
    }
    
    private static void ClearFilter(LWMap map, Iterable<LWComponent> allNodes)
    {
        for (LWPathway pathway : map.getPathwayList()) 
            pathway.setFiltered(false);

        for (LWComponent c : allNodes)
            c.setFiltered(false);
    }
    

    private LWPathway exclusiveDisplay;
    private synchronized void toggleHideEverythingButCurrentPathway(boolean clearFilter)
    {
        final LWPathway pathway = VUE.getActivePathway();
        final LWMap map = pathway.getMap();
        
        if (pathway == null || map == null)
            return;

        // We have to use the FILTER flag, in case a pathway member
        // is the child of another node (that isn't on the pathway),
        // so that when the parent "hides" because it's not on the
        // pathway, it doesn't also hide the child.

        // This code is a bit complicated, as it both sets a pathway
        // to be exclusively shown, or toggles it if it already is.

        // As we only support one global "filter" at a time,
        // we first we de-filter (show) everything on the map.

        final Collection<LWComponent> allNodes = map.getAllDescendents();

        if (DEBUG.Enabled) Log.debug("toggle; clear=" + clearFilter + "; all=" + Util.tags(allNodes));

        for (LWComponent c : allNodes)
            c.setFiltered(false);
        
        if (exclusiveDisplay == pathway || clearFilter) {
            // We're toggling: just leave everything visible (de-filtered) in the map
            exclusiveDisplay = null;
            clearFilter = true;
        }

        if (!clearFilter) {

            // We're exclusively showing the current pathway: hide (filter) everything
            // that isn't in it.  Currently, any child of an LWComponent that is on a
            // pathway, is also considered on that pathway for display purposes.
            
            filterAllOutsidePathway(map, pathway);
            exclusiveDisplay = pathway;
            
            // Include anything that intersects a portal:
            
            for (LWPathway.Entry entry : pathway.getEntries()) {
                if (entry.isPortal()) {
                    final java.awt.geom.Rectangle2D portalRect = entry.node.getBounds();
                    for (LWComponent c : allNodes) {
                        // (portalShape.intersects(c.getBounds())
                        if (c.intersects(portalRect)) {
                            if (c instanceof LWLink && ((LWLink)c).isConnectedTo(entry.node)) {
                                // a link connected to the portal will always intersect, but
                                // we really don't want to see it as it's rare the shape of
                                // the link is actually going to pass through the body of
                                // the portal
                                continue;
                            }
                            c.setFiltered(false);
                        }
                    }
                }
            }
        }


//         // Now we make sure the Pathway objects themselves
//         // have their filter flag properly set.

//         for (LWPathway path : map.getPathwayList()) {
//             if (clearFilter)
//                 path.setFiltered(false);
//             else
//                 path.setFiltered(path != pathway);
//         }
        
        pathway.notify(this, LWKey.Repaint);
        //pathway.notify(this, "pathway.exclusive.display");
    }

    //private void filterAllOutsidePathway(Iterable<LWComponent> iterable, LWPathway pathway) {
    private void filterAllOutsidePathway(LWComponent node, LWPathway pathway) {
        
        if (DEBUG.Enabled) Log.debug("filter against " + pathway + ": " + node); 

        for (LWComponent c : node.getChildren()) {
            if (c.inPathway(pathway)) {
                // this means we will NOT filter all the children of this component also --
                // they are considered part of the visible pathway.
                continue;
            }
            c.setFiltered(true);
            if (c.hasChildren())
                filterAllOutsidePathway(c, pathway);
        }
    }
    
   
    private void updateAddRemoveActions()
    {
        if (DEBUG.PATHWAY&&DEBUG.META) System.out.println(this + " updateAddRemoveActions");
        
        LWPathway path = getSelectedPathway();
        
        if (path == null || path.isLocked()) {
            btnAddSlide.setEnabled(false);
            btnLiveMap.setEnabled(false);
           // btnDeleteSlide.setEnabled(false);
            btnPresentationDelete.setEnabled(false);
            notesArea.setEnabled(false);
            return;
        }

        btnLiveMap.setEnabled(true);
        notesArea.setEnabled(true);
        btnPresentationDelete.setEnabled(true);
        
        boolean removeDone = false;
        LWSelection selection = VUE.ModelSelection;
        
        // if any viable index, AND path is open so you can see
        // it selected, enable the remove button.
        if (path.getCurrentIndex() >= 0 && path.isOpen()) {
           //btnDeleteSlide.setEnabled(true);
            removeDone = true;
        }
            
        if (selection.size() > 0 && !(selection.first() instanceof LWPathway)) {
            
            // Above instanceof check: pathways are single-select
            // only, so if it's in the selection, it should be first,
            // and there shouldn't be anything else.  We don't ever
            // want to be able to add the pathway to itself!
            // 2007-09-03 -- LWPathway objects not selectable (only master slide),
            // check should now be redundant.

            if (selection.size() == 1)
                btnAddSlide.setEnabled(LWPathway.isPathwayAllowed(selection.first()));
            else
                btnAddSlide.setEnabled(true);
            
            if (!removeDone) {
                // if at least one element in selection is on current path,
                // enable remove.  Theoretically should only get here if
                // pathway is closed.
                boolean enabled = false;
                Iterator i = selection.iterator();
                while (i.hasNext()) {
                    LWComponent c = (LWComponent) i.next();
                    if (c.inPathway(path)) {
                        if (DEBUG.PATHWAY) System.out.println(this + " in selection enables remove: " + c + " on " + path);
                        enabled = true;
                        break;
                    }
                }
               // btnDeleteSlide.setEnabled(enabled);
            }
        } else {
            btnAddSlide.setEnabled(false);
            //if (!removeDone)
                //btnDeleteSlide.setEnabled(false);
        }
        btnMergeInto.setEnabled(selection.size() > 1);
    }

    public void updateEnabledStates()
    {        if (DEBUG.PATHWAY&&DEBUG.META) System.out.println(this + " updateEnabledStates");
        
        updateAddRemoveActions();

        doUpdateMapViewDependentActions();
        
        btnPathwayOnly.setEnabled(VUE.getActivePathway() != null);
        
        if (VUE.getActivePathway() != null)
        {
        	if (VUE.getActivePathway().getCurrentEntry() != null)
        		btnEditSlides.setEnabled(!VUE.getActivePathway().getCurrentEntry().isPortal());
        	else
        		btnEditSlides.setEnabled(true);
        }
        else
        	btnEditSlides.setEnabled(false);
//         final LWPathway pathway = VUE.getActivePathway();
//         if (pathway != null && pathway.getCurrentEntry() != null) {
//             btnRefresh.setEnabled(!pathway.getCurrentEntry().isMapView());
//         } else {
//             btnRefresh.setEnabled(false);
//         }

//         if (pathway != null && pathway.length() > 1) {
//             boolean atFirst = pathway.atFirst();
//             boolean atLast = pathway.atLast();
//              path_rewind.setEnabled(!atFirst);
//              path_backward.setEnabled(!atFirst);
//              path_forward.setEnabled(!atLast);
//              path_last.setEnabled(!atLast);
// //            if (pathway.isLocked()) {
// //                btnElementUp.setEnabled(false);
// //                btnElementDown.setEnabled(false);
// //            } else {
// //                btnElementUp.setEnabled(!atFirst);
// //                btnElementDown.setEnabled(!atLast);
// //            }
//         } else {
//             PlayerAction.setAllEnabled(false);
//     //        btnElementUp.setEnabled(false);
//     //        btnElementDown.setEnabled(false);
//         }
//        btnPathwayOnly.setEnabled(pathway != null);
        
      //  btnLockPresentation.setEnabled(pathway != null);
    }

    private void doUpdateMapViewDependentActions() {
        final LWPathway pathway = VUE.getActivePathway();

        if (pathway != null && pathway.getCurrentEntry() != null) {
            btnRefresh.setEnabled(!pathway.getCurrentEntry().isMapView());
        } else {
            btnRefresh.setEnabled(false);
        }
    }

    public static void updateMapViewDependentActions() {
        Singleton.doUpdateMapViewDependentActions();
    }
    
    /** Delete's a pathway and all it's contents */
    public void deletePathway(LWPathway p) {
        // We only ever delete the current pathway, and if's
        // exclusively displayed, make sure to undo the filter.
        // TODO: handle for undo: is critical for undo of the pathway create!
        if (exclusiveDisplay != null)
            ClearFilter(exclusiveDisplay.getMap());
        VUE.getActiveMap().getPathwayList().remove(p);
    }

    private void setSelectedEntry(LWPathway.Entry entry) {
        mSelectedEntry = entry;
    }

    
//     private boolean mTrailingNoteSave;
//     private void updateTextAreas(LWPathway.Entry entry)
//     {
//         if (DEBUG.PATHWAY||DEBUG.META)
//             System.out.println(this + " updateTextAreas: " + entry + ", skipping="+mTrailingNoteSave);
        
//         if (mTrailingNoteSave)
//             return;
        
//         try {

//             // Save any unsaved changes before re-setting the labels.  This is backup
//             // lazy-save as workaround for java focusLost limitation.

//             // We also wrap this in a loop spoiler because if notes do get saved at this
//             // point, we'll come back here with an update event, and we want to ignore
//             // it as we're switching to a new note anyway.  Ideally, focusLost on the
//             // notesArea would have already handled this, but unfortunately java
//             // delivers that event LAST, after the new focus component has gotten and
//             // handled all it's events, and if it was the PathwayTable selecting another
//             // curent node to display, this code is needed to be sure the note gets
//             // saved.
            
//             mTrailingNoteSave = true;
//             ensureNotesSaved(); 
//         } finally {
//             mTrailingNoteSave = false;
//         }
    
//         if (entry == null) {
//             pathLabel.setText("");
//             pathElementLabel.setText("");
//             notesArea.setText("");
//             setSelectedEntry(null);
//             return;
//         }

//         String pathText = entry.pathway.getLabel();
//         String entryText;

//         if (entry.isPathway()) {
//             entryText = "";
//         } else {
//             pathText += ": ";
//             if (DEBUG.PATHWAY) pathText += "(" + (entry.index()+1) + ") ";
//             entryText = entry.getLabel();
//         }

//         pathLabel.setText(pathText);
//         pathElementLabel.setText(entryText);
//         notesArea.setText(entry.getNotes());
        
//         mNoteKeyWasPressed = false;
//         setSelectedEntry(entry);
//     }

    public static void main(String[] args) {
        System.out.println("PathwayPanel:main");
        DEBUG.Enabled = DEBUG.INIT = true;
        VUE.init(args);
        //VueUtil.displayComponent(new PlaybackToolPanel());
    }
    

    public String toString() {
        return "PathwayPanel[" + VUE.getActivePathway() + "]";
    }
    
}
