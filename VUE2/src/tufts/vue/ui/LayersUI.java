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

package tufts.vue.ui;

import tufts.vue.*;
import tufts.vue.ActiveEvent;
import static tufts.vue.LWComponent.Flag.*;
import static tufts.vue.LWComponent.HideCause.*;
import static tufts.vue.LWComponent.ChildKind;
import static tufts.vue.LWComponent.Order;
import tufts.vue.LWMap.Layer;
import tufts.vue.gui.*;

import tufts.Util;
import static tufts.Util.reverse;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.*;
import javax.swing.border.*;


/**
 * @version $Revision: 1.56 $ / $Date: 2009-02-06 22:35:57 $ / $Author: sraphe01 $
 * @author Scott Fraize
 */
public class LayersUI extends tufts.vue.gui.Widget implements LWComponent.Listener, LWSelection.Listener//, ActionListener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LayersUI.class);

    private static final boolean SCROLLABLE = true;

    private final java.util.List<Row> mRows = new java.util.ArrayList();
    private final JPanel mToolbar = new JPanel();
    private final JPanel mRowList = new JPanel();

    private final AbstractButton mShowAll = new JToggleButton("Show All");

    // PROBLEM: IF WE ALLOW LAYERS IN THE SELECTION, LWComponent.selctedOrParent will
    // start returning TRUE for anything inside the layer.  This screws up
    // hierarchy actions, delete, duplicate, etc.

//     private final LWSelection mSelection = new LWSelection() {
//             @Override
//             protected boolean isSelectable(LWComponent c) {
//                 return c instanceof LWMap.Layer;
//             }
//             @Override
//             protected void postNotify() { /* do nothing */ }
//             @Override
//             public String toString() {
//                 return "LayerSelection[" + paramString() + "]";
//             }
//         };
    
    private LWMap mMap;
    private boolean isDragUnderway;
    private Row mDragRow;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints gBC = new GridBagConstraints();
    private int selectedIndex = -1;
   
    //private static final Collection<VueAction> _selectionWatchers = new java.util.ArrayList();

    private static final Collection<LayerAction> AllLayerActions = new java.util.ArrayList();
    
    //private class LayerAction extends Actions.LWCAction {
    private class LayerAction extends VueAction {

//         @Override
//         protected Collection<VueAction> getSelectionWatchers() {
//             return _selectionWatchers;
//         }
    
        Layer active;
        
        LayerAction(String name, String tip) {
            //super(name, tip, KeyStroke.getKeyStroke(KeyEvent.VK_L, 0), null);
            super(name, tip, null, null);
            AllLayerActions.add(this);
        }
        @Override
        public void actionPerformed(ActionEvent ae) {
            active = getActiveLayer();
            super.actionPerformed(ae);
        }

//         protected java.util.List<LWComponent> selection() {
//             return null;
//         }

        @Override
        protected LWSelection selection() {
            return null;
            //return mSelection;
        }

//         // This is overridden only because VueAction.enabledFor is
//         // *package* private, instead of protected, and we're
//         // not going to change that right now, so we need a
//         // new enabled method we can override.
//         @Override
//         protected void updateEnabled(LWSelection selection) {
//             if (selection == null)
//                 setEnabled(false);
//             else
//                 setEnabled(enabledWith(selection));
//         }
//        boolean enabledWith(LWSelection s) { return s.size() > 0; }
        
        boolean enabledWith(Layer layer) { return layer != null; }

        // this is called at the end of each action execution
        @Override
        protected void updateSelectionWatchers() {
            super.updateSelectionWatchers();
            updateLayerActionEnabled(getActiveLayer());
            
        }

        @Override
        public String getUndoName(ActionEvent e, Throwable exception) {
            String name = super.getUndoName(e, exception) + " Layer";
            //if (selection().only() instanceof Layer && (this == LAYER_DUPLICATE || this == LAYER_DELETE))
            if (active != null && (this == LAYER_DUPLICATE || this == LAYER_DELETE))
                name += " " + Util.quote(active.getDisplayLabel());
            //name += " " + Util.quote(selection().only().getDisplayLabel());
            return name;
        }

        
        
    }

    static void updateLayerActionEnabled(Layer layer) {
        for (LayerAction a : AllLayerActions)
            a.setEnabled(a.enabledWith(layer));
    }

    

    private static int NewLayerCount = 1;

//     private final VueAction
//         LAYER_NEW = new VueAction("New", "Create a new layer", null, null) {
//                 public void act() {
//                     mMap.addLayer("New Layer " + NewLayerCount++);
//                 }
//                 @Override
//                 public String getUndoName() { return "New Layer"; }
//             };
        

    private final LayerAction

        LAYER_NEW = new LayerAction("New", "Create a new layer") {
                @Override
                boolean enabledWith(Layer layer) {
                    return mMap != null;
                }
                public void act() {                	
                    mMap.addLayer("New Layer " + NewLayerCount++);                    
                }
                @Override
                public String getUndoName() { return "New Layer"; }
            },
        
        
        LAYER_DUPLICATE = new LayerAction("Duplicate", "Duplicate Layer") {
//                 public void act() {
//                     for (LWComponent c : reverse(selection()))
//                         mMap.addChild(c.duplicate());
//                 }
                public void act() {
                    final Layer dupe = (Layer) active.duplicate();
                     mMap.addOnTop(active, dupe);
                    setActiveLayer(dupe); // make the new duplicate layer the active layer
                }
            },
        
        LAYER_DELETE = new LayerAction("Delete", "Remove a layer and all of its contents") {
                @Override
                boolean enabledWith(Layer layer) {
                    return mRows.size() > 1;
                }
                @Override
                public void act() {                 	
                	selectedIndex = active.getIndex();
                	mMap.deleteChildPermanently(active); // todo: LWMap should setActiveLayer(null) if active is deleted
                	mMap.setActiveLayer(null);
                    //setActiveLayer(null, true);
                    attemptAlternativeActiveLayer(true); // better if this tried to find the nearest layer, and not check last-active
                    //VUE.getSelection().clearDeleted(); // in case any in delete layer were in selection [no auto-handled in UndoManager]
                }				           
            },
        
        LAYER_MERGE_DOWN = new LayerAction("Merge Down", "Merge into layer below") {
//                 @Override
//                 boolean enabledWith(LWSelection s) {
//                     return s.size() == 1
//                         && s.first() != mRows.get(mRows.size()-1).layer;
//                 }
                @Override
                boolean enabledWith(Layer layer) {
                    return layer != null && indexOf(layer) < mRows.size() - 1;
                }
                
                @Override
                public void act() {
                    //final LWComponent mergingDown = selection().first();
                    final LWContainer mergingDown = active;
                    final Layer below = (Layer) mRows.get(indexOf(mergingDown) + 1).layer;
                    below.takeAllChildren(mergingDown);
                    setActiveLayer(below);
                    mMap.deleteChildPermanently(mergingDown);
                }
            },
            
            LAYER_FILTER = new LayerAction("Filter", "Filter: Hide unselected layers") {
            	boolean flg = true;
                @Override
                boolean enabledWith(Layer layer) {
                    return true;
                }
                @Override
                public void act() {
                	//sheejo     
                	if(flg){	                	
	                	((JButton)mToolbar.getComponent(3)).setBorderPainted(true);
	                	((JButton)mToolbar.getComponent(3)).setContentAreaFilled(true);	
	                	//super.updateSelectionWatchers();	                	
	                	Layer layer = getActiveLayer();
	                	setActiveLayer((Layer) layer, !UPDATE);
	                	VUE.getSelection().setTo(layer.getAllDescendents());
//	                	if (VUE.getSelection() !=null)
//	            			VUE.getSelection().add(layer.getChildren());
//	            		else
//	            			VUE.getSelection().setTo(new LWSelection(layer.getChildren()));    		
	            		          		
	            		
	                	flg = !flg;
                	}else{
                		((JButton)mToolbar.getComponent(3)).setBorderPainted(false);
	                	((JButton)mToolbar.getComponent(3)).setContentAreaFilled(false);
	                	flg = !flg;
                	}
                }
                
            },
            
            LAYER_LOCK = new LayerAction("Lock", "Lock") {
                @Override
                boolean enabledWith(Layer layer) {
                    return true;
                }
                @Override
                public void act() {
                   
                }
            }
                
//         LAYER_MERGE = new LayerAction("Merge", "Merge multiple layers") {
//                 @Override
//                 boolean enabledWith(LWSelection s) { return s.size() > 1; }
//                 @Override
//                 public void act() {

//                     final Layer merged = new LWMap.Layer("Merged");

//                     final ArrayList<LWComponent> mergedChildren = new ArrayList();

//                     for (LWComponent c : reverse(selection())) {
//                         merged.takeAllChildren(c);
//                         mMap.deleteChildPermanently(c);
//                     }

// //                     for (LWComponent c : reverse(selection())) {
// //                         mergedChildren.addAll(c.getChildren());
// //                         if (c instanceof LWContainer)
// //                             ((LWContainer)c).setChildren(null);
// //                         mMap.deleteChildPermanently(c);
// //                         //merged.addChildren(c.getChildren(), LWComponent.ADD_PRESORTED);
// //                         //merged.addAll(c.getChildren());
// //                     }
// //                     merged.setChildren(mergedChildren);
                    
//                     mMap.addChild(merged);
//                 }
//                 @Override
//                 public String getUndoName() { return "Merge Layers"; }
//             },
        
        ;
         
    public LayersUI() {
        super("layers");
        setName("layersUI");        
        //setLayout(new GridBagLayout());

        mToolbar.setName("layersUI.tool");
        mRowList.setName("layersUI.rows");
        
        mRowList.setLayout(new  GridBagLayout());
        mRowList.addMouseListener(RowMouseEnterExitTracker); // doesn't work?
        //To Change the GUI
        
        gBC.fill = GridBagConstraints.HORIZONTAL;
        addButton(LAYER_NEW);
        addButton(LAYER_DUPLICATE);
        //addButton(LAYER_MERGE);
        addButton(LAYER_MERGE_DOWN);
        addButton(LAYER_FILTER);
        //addButton(LAYER_LOCK);
        addButton(LAYER_DELETE);
        gBC.weightx = 0.5;
        gBC.gridx = 1;
        gBC.gridy = 0; 
        JPanel panel = new JPanel(){
        	 public void paintComponent(Graphics g)
        	 {
        		 // call paintComponent to ensure the panel displays correctly
        		 super.paintComponent(g);
        		 g.drawLine(10, 5, 10, 25);         		
        	 }        	
        };  
        panel.setForeground(Color.gray);
        panel.setPreferredSize(new Dimension(10,25));         
        mToolbar.add(panel,gBC);  
//         if (DEBUG.Enabled) {
//             mShowAll.addActionListener(new ActionListener() {
//                     public void actionPerformed(ActionEvent e) {
//                         loadLayers(mMap);
//                     }});
//             addButton(mShowAll);
//             addButton(Actions.Group);
//             addButton(Actions.Undo);
//         }
        mToolbar.setBorder(new SubtleSquareBorder(true));
        add(mToolbar, BorderLayout.NORTH);
        if (SCROLLABLE) {        	
            JScrollPane sp = new JScrollPane(mRowList);
            sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            //sp.setBorder(null);
            add(sp, BorderLayout.CENTER);
            sp.addMouseListener(RowMouseEnterExitTracker); // doesn't always work
        } else {        	
            //mRowList.setSize(300, 40);
            //mRowList.setMaximumSize(new Dimension(300, 40));
            add(mRowList, BorderLayout.NORTH);
        }

//         mSelection.addListener(new LWSelection.Listener() {
//                 public void selectionChanged(LWSelection s) {
//                     LayerAction.updateSelectionWatchers(_selectionWatchers, s);
//                 }
//                 @Override
//                 public String toString() {
//                     return "Handler for " + _selectionWatchers.size() + " " + LayerAction.class.getSimpleName() + "s";
//                 }
//             });

        
        VUE.addActiveListener(LWMap.class, this);
        VUE.getSelection().addListener(this);
        //VUE.addActiveListener(Layer.class, this);
        //VUE.addActiveListener(LWComponent.class, this);
        //setMinimumSize(new Dimension(300,260));      
    }

    private void addButton(Action a) {
        addButton(new JButton(a));
    }
    private void addButton(AbstractButton b) {
        if (b instanceof JToggleButton)
            ;//b.putClientProperty("JButton.buttonType", "roundRect"); // for Mac Leopard Java
        else
        b.putClientProperty("JButton.buttonType", "textured"); // for Mac Leopard Java        
        
//        Font defaultFont = getFont();
//        Font boldFont = defaultFont.deriveFont(Font.BOLD);
//        Font smallFont = defaultFont.deriveFont((float) boldFont.getSize()-2);
//        b.setFont(smallFont);
        b.setFont(tufts.vue.gui.GUI.LabelFace);
        b.setFocusable(false);
        if(b.getAction() == null) {
            ;
        } else if(b.getAction().equals(LAYER_NEW)){
        	
        	b.setText("New Layer");        	
        	b.setIcon(tufts.vue.VueResources.getImageIcon("metadata.editor.add.up"));
        	b.setRolloverEnabled(true);
        	b.setPreferredSize(new Dimension(90,30));
        	//b.setMinimumSize(new Dimension(90,30));
        	b.setRolloverIcon(VueResources.getImageIcon("metadata.editor.add.down"));
        	b.setBorder(BorderFactory.createEmptyBorder(2,5,2,2));
        	b.setHorizontalAlignment(JButton.LEADING); // optional
        	b.setBorderPainted(false);
        	b.setContentAreaFilled(false);
        	gBC.weightx = 0.5;
            gBC.gridx = 0;
            gBC.gridy = 0; 
        }else if(b.getAction().equals(LAYER_DUPLICATE)){    
        	b.setText("");
        	b.setIcon(tufts.vue.VueResources.getImageIcon("layer.duplicate.add"));
        	b.setRolloverEnabled(true);
        	b.setRolloverIcon(VueResources.getImageIcon("layer.duplicate.add.ov"));
        	//b.setBorder(BorderFactory.createEmptyBorder(2,5,2,30));
        	//b.setHorizontalAlignment(JButton.LEADING); // optional
        	b.setBorderPainted(false);
        	b.setContentAreaFilled(false);
        	gBC.weightx = 0.5;
            gBC.gridx = 2;
            gBC.gridy = 0; 
        }else if(b.getAction().equals(LAYER_MERGE_DOWN)){
        	b.setText("");
        	b.setIcon(tufts.vue.VueResources.getImageIcon("layer.merge.add"));
        	b.setRolloverEnabled(true);
        	b.setRolloverIcon(VueResources.getImageIcon("layer.merge.add.ov"));
        	b.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        	//b.setHorizontalAlignment(JButton.LEADING); // optional
        	b.setBorderPainted(false);
        	b.setContentAreaFilled(false);
        	gBC.weightx = 0.5;
        	gBC.gridx = 3;
            gBC.gridy = 0;
        }else if(b.getAction().equals(LAYER_DELETE)){
        	b.setText("");
        	b.setIcon(tufts.vue.VueResources.getImageIcon("ontologicalmembership.delete.up"));
        	b.setRolloverEnabled(true);
        	b.setRolloverIcon(VueResources.getImageIcon("ontologicalmembership.delete.down"));
        	b.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        	//b.setHorizontalAlignment(JButton.LEADING); // optional
        	b.setBorderPainted(false);
        	b.setContentAreaFilled(false);
        	gBC.gridx = 6;
            gBC.gridy = 0;
        }else if(b.getAction().equals(LAYER_FILTER)){        	
        	b.setText("");
        	b.setIcon(tufts.vue.VueResources.getImageIcon("layer.filter.off"));
        	b.setRolloverEnabled(true);
        	b.setRolloverIcon(VueResources.getImageIcon("layer.filter.on"));
        	b.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
        	//b.setHorizontalAlignment(JButton.LEADING); // optional
        	b.setBorderPainted(false);
        	b.setContentAreaFilled(false);
        	gBC.gridx = 4;
            gBC.gridy = 0;
        }/*else if(b.getAction().equals(LAYER_LOCK)){
        	b.setText("");
        	b.setIcon(tufts.vue.VueResources.getImageIcon("lockOpen"));
        	b.setRolloverEnabled(true);
        	b.setRolloverIcon(VueResources.getImageIcon("lock"));
        	b.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        	//b.setHorizontalAlignment(JButton.LEADING); // optional
        	b.setBorderPainted(false);
        	b.setContentAreaFilled(false);
        	gBC.gridx = 5;
            gBC.gridy = 0;
        }  */      
        mToolbar.setLayout(gridbag);
        mToolbar.add(b,gBC);        

        //b.addActionListener(this);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        //setMinimumSize(new Dimension(400,120+mToolbar.getHeight()));
        SwingUtilities.getWindowAncestor(this).addMouseListener(RowMouseEnterExitTracker);
    }

    public void activeChanged(ActiveEvent e, LWMap map) {
        loadMap(map);
    }

//     public void activeChanged(ActiveEvent e, Layer layer) {
//         // used to just call indic
//     }

//     public void activeChanged(ActiveEvent e, LWComponent c) {
//         enableForSingleSelection(c);
//         // for debug / child-list mode:
//         if (mMap != null && !mMap.isLayered())
//             indicateActiveLayers(null);
//     }

    private static final boolean UPDATE = true;
    
    private void setActiveLayer(Layer c) {
        setActiveLayer(c, !UPDATE);
    }

    private void setActiveLayer(final Layer layer, boolean update) {
        //if (DEBUG.Enabled) Log.debug("SET-ACTIVE: " + c);
        if (layer != null)
            mMap.setClientData(Layer.class, "last", mMap.getActiveLayer());
        mMap.setActiveLayer(layer);        
        if (update)
            indicateActiveLayers(null);
        else{
        	LWSelection selection = VUE.getSelection(); 
        	indicateActiveLayers(selection.getParents());
        }
        updateLayerActionEnabled(layer);

//         if (DEBUG.Enabled) {
//             GUI.invokeAfterAWT(new Runnable() { public void run() {
//                 VUE.setActive(Layer.class, LayersUI.this, layer);
//             }});
//         }
    }

    private boolean canBeActive(LWComponent layer) {
        return canBeActive(layer, true);
    }
    private boolean canBeActive(LWComponent layer, boolean checkLocking) {
        if (layer == null || layer.isHidden() || layer.isDeleted())
            return false;
        else if (checkLocking && layer.isLocked() && mRows.size() > 1)
            return false;
        else
            return layer instanceof Layer;
    }

    private static final boolean AUTO_ADJUST_ACTIVE_LAYER = false;
    
    private void attemptAlternativeActiveLayer(boolean isDeleteFlg) {

        //if (!AUTO_ADJUST_ACTIVE_LAYER) return;
    	Layer lastActive = null;
        final Layer curActive = getActiveLayer();
        
        if(!isDeleteFlg){
        	lastActive = mMap.getClientData(Layer.class, "last"); 
        }else{
        	if(selectedIndex > 0)
        		lastActive = (Layer) mMap.getChild(selectedIndex-1);
        	else{
        		lastActive = mMap.getClientData(Layer.class, "last"); 
        	}
        }
       
        if (canBeActive(lastActive)) {        	       	
            setActiveLayer(lastActive, UPDATE);
            return;
        }

        LWComponent fullyOpen = null;
        
        // find the top-most visible and unlocked layer:
        for (Row row : mRows)
            if (canBeActive(row.layer))
                fullyOpen = row.layer;

        if (fullyOpen != null) {
            setActiveLayer((Layer) fullyOpen, UPDATE);
            return;
        }
        
        LWComponent visibleButLocked = null;
        
        // find the top-most visible layer:
        for (Row row : mRows)
            if (canBeActive(row.layer, false))
                visibleButLocked = row.layer;

        if (visibleButLocked != null && (curActive == null || (curActive.isHidden() && curActive.isLocked()))) {
            // only switch to visible but locked if the current active is actually worse off
            setActiveLayer((Layer) visibleButLocked, UPDATE);
        }
    }


    private Layer getActiveLayer() {
        if (mMap == null) {
            Log.warn("getActiveLayer when map is null");
            return null;
        } else {
            return mMap.getActiveLayer();
        }
    }
    
    public void selectionChanged(LWSelection s) {

//        System.err.println("selectionChanged: " + s + "; size=" + s.size() + "; " + Arrays.asList(s.toArray()) + "; parents=" + s.getParents());

        updateGrabEnabledForSelection(s);

//         if (!s.getParents().contains(mMap.getActiveLayer()))
//             for (LWComponent c : s.getParents())
//                 if (c instanceof Layer)
//                     mMap.setActiveLayer(c);

        if (s.getParents().size() == 1)
            setActiveLayer(s.first().getLayer());


//         if (s.size() == 1 && s.first().getLayer() != null) {
//             //if (DEBUG.Enabled) Log.debug("selectionChanged: single selection; activate layer of: " + s.first());
//             setActiveLayer(s.first().getLayer());
//         } else if (s.getParents().size() == 1 && s.first().getParent() instanceof Layer) {
//             //if (DEBUG.Enabled) Log.debug("selectionChanged: one parent in selection; active parent " + s.first().getParent());
//             setActiveLayer((Layer) s.first().getParent());//         }
        
        indicateActiveLayers(s.getParents());

//         // for debug / child-list mode:
//         if (mMap != null && !mMap.isLayered())
//             indicateActiveLayer(null);
    }

    
    private void loadMap(final LWMap map)
    {
        if (DEBUG.Enabled) Log.debug("load map " + map);     
             
        if (mMap == map)
            return;

        if (mMap != null)
            mMap.removeLWCListener(this);
        
        mMap = map;

        //setActiveLayer(map.getActiveLayer());
        loadLayers(map);

        if (map != null) {
            // todo: we should be able to just listen for LWKey.HierarchyChanged, tho
            // this currently is only generated on UNDO's, and hardly anything is
            // currently listenting for it (OutlineViewTree, and some references to "hier.*)
            map.addLWCListener(this);
        }
    }

    //private boolean layerReparentingSeen;

    public void LWCChanged(LWCEvent e) {

        // ignore events from children: just want hierarchy events directly from the map
        // (as we're only interested in changes to map layers)
        
        if (e.key == LWKey.UserActionCompleted) {
//             if (layerReparentingSeen) {
//                 VUE.getSelection().resetStatistics();
//                 updateGrabEnabledForSelection(VUE.getSelection());
//                 layerReparentingSeen = false;
//             }
            repaint(); // repaint the previews
        }
        else if (mShowAll.isSelected() || e.getSource() == mMap) {
            if (e.getName().startsWith("hier.")) {
                loadLayers(mMap);
            }
        }
// [ below now handled by calling selectionChanged at end of grabFromSelection after selection stat reset ]
//         else if (e.getSource() instanceof Layer && e.getName().startsWith("hier.")) {
//             // tho we only really need to track this for changes to any components
//             // that are in the selection, we just track it for everything right
//             // now -- we especially need this to handle updating grab enabled
//             // states when undoing grabs
//             layerReparentingSeen = true;
//         }
    }
    
    private final Color AlphaWhite = new Color(255,255,255,128);
    
    private final Color ActiveBG = new Color(188,212,255);//VueConstants.COLOR_SELECTION.brighter();//VueConstants.COLOR_SELECTION;
    
    private final Color IncludedBG = Util.alphaMix(AlphaWhite, VueConstants.COLOR_SELECTION);
    //private final Color IncludedBG = VueConstants.COLOR_SELECTION.darker();
    //private final Color IncludedBG = new Color(128,128,255,128);
    
    private final Color SelectedBG = VueConstants.COLOR_SELECTION.brighter();
    
    private void indicateActiveLayers(Collection<LWContainer> parents) {
    	
        final Layer activeLayer = getActiveLayer();
        
        if (parents == null) {

            // update the active layer indication based on a change
            // in the active layer -- not a change in a selection
            // this will see to it that only one layer is indicated,
            // and all other layers are not.

            for (Row row : mRows) {
                //row.activeIcon.setEnabled(row.layer == activeLayer);               
                if (row.layer.isSelected() || row.layer == activeLayer){                	
                //if (row.layer.isSelected())
                    row.setBackground(row.layer instanceof Layer ? ActiveBG : SelectedBG);                    
                }
                else 
                    row.setBackground(null);
                
//                if (row.layer == activeLayer) {
//                    row.setBorder(BorderFactory.createLineBorder(Color.red, 2));
//                }
//                else {
//                    row.setBorder(new CompoundBorder(new MatteBorder(1,0,1,0, Color.lightGray),
//                                                     GUI.makeSpace(3,7,3,7)));
//                }
                
            }

        } else {


            // update the active layer indication based on a change
            // in the selection (hilite *any* layers found in the selection)

            final Set<Layer> layersInSelection = new HashSet(parents.size());
            
            // on empty selection, parents will be empty
            
            for (LWComponent c : parents)
                layersInSelection.add(c.getLayer());
            
            for (Row row : mRows) {

                //row.activeIcon.setEnabled(row.layer == activeLayer);
                
                if (row.layer instanceof Layer) {
                    if (layersInSelection.contains(row.layer)) {
                        //row.layer.setSelected(true);
                        if (row.layer == activeLayer){
                            row.setBackground(ActiveBG); 
                            if(layersInSelection.size()>1){
                            	row.setBorder(BorderFactory.createLineBorder(Color.red, 2));
                            }
                        }
                        else{
                            row.setBackground(IncludedBG);
                            row.setBorder(new CompoundBorder(new MatteBorder(1,1,1,1, Color.lightGray),
                                    GUI.makeSpace(3,7,3,7)));
                            //row.setBorder(null);
                        }
                    } else {
                        //row.layer.setSelected(false);
                        if (row.layer == activeLayer){
                            row.setBackground(ActiveBG);
                            if(layersInSelection.size()>1){
                            	row.setBorder(BorderFactory.createLineBorder(Color.red, 2));
                            }
                        }
                        else{
                            row.setBackground(null);
                            row.setBorder(new CompoundBorder(new MatteBorder(1,1,1,1, Color.lightGray),
                                    GUI.makeSpace(3,7,3,7)));
                            //row.setBorder(null);
                        }
                    }
                    
                } else if (row.layer.isSelected()) {
                    row.setBackground(SelectedBG);
                } else{
                    row.setBackground(null);
                }
            }
            
            
        }
        
    }

    private void updateGrabEnabledForSelection(LWSelection s) {

        final Collection<LWContainer> parents = s.getParents();
        //final LWContainer parent0 = parents.isEmpty() ? null : parents.iterator().next();

        boolean disable =
            s.size() < 1
            || s.only() instanceof Layer;

        // todo: to be more precise, could always accumme related parets

        if (!disable) {
            boolean canExtract = false;
            for (LWContainer parent : parents) {
                if (isExtractableParent(parent)) {
                    canExtract = true;
                    break;
                }
            }
            disable = !canExtract;
        }
        
        for (Row row : mRows) {
            if (row.grab == null)
                continue;
            if (disable) {
                row.grab.setEnabled(false);
            } else if (parents.size() == 1 && parents.contains(row.layer)) {
                //Log.debug("DISABLE GRAB IN " + row);
                row.grab.setEnabled(false);
            } else  {
                //Log.debug(" ENABLE GRAB IN " + row);
                row.grab.setEnabled(true);
            }
        }
    }

    private static boolean isExtractableParent(LWContainer parent) {

        return parent instanceof Layer
            || parent instanceof LWMap; // shouldn't happen, but just in case of up-leakage
        
//         if (parent instanceof LWGroup)
//             return false;
//         else if (parent instanceof LWNode)
//             return false;
//         else if (parent instanceof LWSlide)
//             return false;
//         else
//             return true;
    }

    private static boolean layerCanGrab(Layer layer, LWComponent c) {

        final LWContainer parent = c.getParent();

        if (parent == layer)
            return false;
        else
            return isExtractableParent(parent);
        
    }
    
    private void grabFromSelection(Layer layer) {
        final LWSelection selection = VUE.getSelection();
        
        final java.util.List grabbing = new ArrayList();
        
        for (LWComponent c : selection) {
            if (layerCanGrab(layer, c))
                grabbing.add(c);
        }
      
        // todo perf: remove all old layer in one swoop, then add to new
        
        layer.addChildren(grabbing);

        selection.resetStatistics();

        selectionChanged(selection);

        //indicateActiveLayers(selection.getParents());
    }
    

    private void loadLayers(final LWMap map) {

        mRows.clear();
        
        if (map != null) {
            
            // handle in reverse order (top layer on top)
            for (LWComponent layer : reverse(map.getChildren())) {
                mRows.add(produceRow(layer));
                if (mShowAll.isSelected()) {
                    //for (LWComponent c : reverse(layer.getChildren()))
                    for (LWComponent c : reverse(layer.getAllDescendents(ChildKind.PROPER, new ArrayList(), Order.DEPTH)))
                        mRows.add(produceRow(c));
                }
            }

            updateLayerActionEnabled(map.getActiveLayer());
        } else {
            updateLayerActionEnabled(null);
        }
        
        if (!isDragUnderway) {
            updateGrabEnabledForSelection(VUE.getSelection());
            indicateActiveLayers(null);
        }
        
        layoutRows();
    }

    private void layoutRows() {
        layoutRows(mRowList, mRows);
    }
        
    private void layoutRows(final JComponent container, final java.util.List<? extends JComponent> rows)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.weighty = 0; // 1 has all expanding to fill vertical, 0 leaves all at min height
        c.weightx = 1;
        c.gridheight = 1;
        c.gridwidth = 1;
        //c.gridwidth = GridBagConstraints.REMAINDER;
        //c.fill = GridBagConstraints.HORIZONTAL;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.gridx = 0;
        c.gridy = 0;
        
        // Each Row has a top and a bottom border line, so that
        // one is always visible no matter what, but we normally only
        // want to see a single line, so this will let them
        // overlap during standard display (e.g., but not when drag-reordering)
        c.insets = new Insets(0,0,-1,0);

        container.removeAll();

        if (!rows.isEmpty()) {  
            
            for (JComponent row : rows) {            	
                c.insets.left = (((Row)row).layer.getDepth() - 1) * 56; // refactoring: note Row cast
                row.setOpaque(true);
                container.add(row, c);
                c.gridy++;
            }
        
            if (c.weighty == 0) {
                // now add a default vertical expander so the rest of items stay at the top
                c.weighty = 1;
                c.gridy = rows.size() + 1;
                container.add(new JPanel(), c);
            }
        }       
        
        // will property event or DockWindow API: DockWindow controls this, and only polls it on init
        //setMinimumSize(new Dimension(400,40*rows.size())); 
        container.revalidate(); // needed for Tiger (uneeded on Leopard)
        ////if (isVisible()) SwingUtilities.getWindowAncestor(this).pack();
        container.repaint();
    }

    private Row produceRow(final LWComponent layer)
    {
        Row row = layer.getClientData(Row.class);
        if (row != null) {
            return row;
        } else {
            row = new Row(layer);
            layer.setClientData(Row.class, row);
            return row;
        }
    }

    private int indexOf(final LWComponent layer) {
        int i = 0;
        for (Row row : mRows) {
            if (row.layer == layer)
                return i;
            i++;
        }
        return -1;
    }
    

//     private boolean inExclusiveMode() {
//         return fetchExclusiveRow() != null;
//     }

    private Row fetchExclusiveRow() {
        return mMap.getClientData(Row.class, "exclusive");
    }
    private void storeExclusiveRow(Row row) {
        mMap.setClientData(Row.class, "exclusive", row);
    }

    private Layer fetchPreExclusiveLayer() {
        return mMap.getClientData(Layer.class, "pre-exclusive");
    }
    private void storePreExclusiveLayer(Layer layer) {
        mMap.setClientData(Layer.class, "pre-exclusive", layer);
    }
    
    // TODO: if a row is deleted while in exclusive mode, you won't
    // be able to access it's controls if it's un-deleted when NOT
    // in exclusive mode -- you'll need to enter/exit exlusive mode
    // to re-gain access (to visible/lock buttons)

    private void setExclusiveMode(boolean entering, Row exclusiveRow)
    {
        //Log.debug("SET-EXCLUSIVE-MODE " + entering + "; nowExclusive=" + exclusiveRow);
        
        if (entering)
            storePreExclusiveLayer(getActiveLayer());

        for (Row row : mRows) {

            final LWComponent layer = row.layer;

            if (entering) {
                //layer.setFlag(WAS_LOCKED, layer.isLocked());
                //layer.setFlag(WAS_HIDDEN, layer.isHidden(DEFAULT));
            } else {
                layer.setLocked(row.locked.isSelected());
                layer.setHidden(DEFAULT, !row.visible.isSelected());
                //layer.setLocked(layer.hasFlag(WAS_LOCKED));
                //layer.setHidden(DEFAULT, layer.hasFlag(WAS_HIDDEN));
                //layer.clearFlag(WAS_LOCKED);
                //layer.clearFlag(WAS_HIDDEN);
            }

            row.visible.setEnabled(!entering);
            row.locked.setEnabled(!entering);

            if (row == exclusiveRow)
                continue;

            row.label.setEnabled(entering ? false : row.visible.isSelected());
                
            layer.setHidden(LAYER_EXCLUDED, entering);
        }

        if (!entering) {
            setActiveLayer(fetchPreExclusiveLayer(), true);
            storeExclusiveRow(null);
            storePreExclusiveLayer(null);
        }

    }
        
        

    private static class TextEdit extends JTextField implements FocusListener {

        static final boolean TransparentHack = Util.isMacLeopard();

        static final Color Transparent = new Color(0,0,0,0);

        static final Dimension MaxSize = new Dimension(Short.MAX_VALUE, 30);
        // we'd like to use a shorter max-size to allow more room for the layer
        // if you drag the window very big instead of the text-box, but
        // this doesn't work with our hack that allows the variable width
        // info field to not throw off row-to-row column alignment.
        //static final Dimension MaxSize = new Dimension(200, 30);

        // todo perf: these could be static
        final Border activeBorder;
        final Border inactiveBorder;

        final Row row;
        
        public TextEdit(final Row row) {
            this.row = row;            
            setDragEnabled(false);
            setPreferredSize(MaxSize);
            setMaximumSize(MaxSize);
            
            setText(row.layer.getDisplayLabel());
            setToolTipText(row.layer.getDisplayLabel());
            enableEvents(AWTEvent.MOUSE_EVENT_MASK);
            enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);            
            addFocusListener(this);
            addKeyListener(new KeyAdapter() {
                    public void keyPressed(KeyEvent e) {
                        if (DEBUG.KEYS) Log.debug("KEY " + e);
                        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                            //focusLost(null); // will be called again on actual focus-loss; could call setEditable(false);
                            setEditable(false); // rely's on focusLost being generated
                        }
                    }
                });

            row.layer.addLWCListener(new LWComponent.Listener() {
                    public void LWCChanged(LWCEvent e) {

                        final String text = row.layer.getDisplayLabel();

                        if (text.equals(getText())) {
                            
                            // be sure to skip the setText if the text is the same:
                            // setting a text component to the same value tends to
                            // trigger a java bug where setScrollOffset(0) stops
                            // working, and the left-most text stays obscured if the
                            // text is wider than the display area.
                            
                            return;
                        }                        
                        setText(text);
                        setScrollOffset(0);
                        // also do this later just in case: somes helps, tho
                        // Vista and XP still seem to have a hard time with this bug,
                        // but generally only if the model text is set elsewhere,
                        // and we're just handling a value update here.
                        GUI.invokeAfterAWT(new Runnable() { public void run() {
                            setScrollOffset(0);
                        }});
                    }},
                LWKey.Label);

            activeBorder = getBorder();
//             Insets insets = activeBorder.getBorderInsets(this);
//             if (Util.isMacLeopard()) {
//                 // sometimes this is wrong... would be safer to do this in addNotify
//                 insets.left -= 3;
//                 insets.right -= 3;
//             }
//             inactiveBorder = GUI.makeSpace(insets);
            inactiveBorder = GUI.makeSpace(activeBorder.getBorderInsets(this));

            //setOpaque(true);
            setEditable(false);
        }

        private boolean isConstructed() {
            return activeBorder != null;
        }
        
        @Override
        public void setEnabled(boolean enabled) {
            setForeground(enabled ? Color.black : Color.gray);
        }

        @Override
        public void setEditable(boolean edit) {
            if (isConstructed()) 
                makeEditable(edit); // don't do this during default JTextComponent init
            super.setEditable(edit);
        }
        
        public void focusGained(FocusEvent e) {}                    
        public void focusLost(FocusEvent e) {
            setEditable(false);
            setScrollOffset(0);
            row.layer.setLabel(getText().trim());
            // make sure if text is longer than fits into field, we scroll back to 0 at the left
            row.layer.getMap().getUndoManager().mark();
        }

        private void makeEditable(boolean edit) {
            if (DEBUG.Enabled) Log.debug("MAKE EDITABLE " + Util.tags(this) + " " + edit);
            if (edit) {            	
                setFocusable(true);
                setBorder(activeBorder);
                setBackground(Color.white);
                if (!TransparentHack)
                    setOpaque(true);
            } else {            	
                setFocusable(false);
                setBorder(inactiveBorder);
                if (TransparentHack) {
                    setBackground(Transparent);
                } else {
                    setBackground(null);
                    setOpaque(false);
                }
            }
        }

//         @Override
//         public void addNotify() {
//             // horizontal mouse-draggs across a non-edit mode label draw's some selection or
//             // repaints chars with semi-transpareng BG, leading to blocky artifacts -- if this is
//             // caret/hilighter, we should be able to turn it off or change the color, but having
//             // had no success in that, it may just be a repaint issue much harder to fix (e.g.,
//             // sometimes the text itself appears to "bolden" as it repaints)
//             super.addNotify();
//             //getCaret().setVisible(false);
//             //getCaret().setSelectionVisible(false);
//             setSelectionColor(Color.red);
//             setHighlighter(null);
//             //setCaret(null); // we'll get NPE
//         }
        
        @Override
        protected void processEvent(AWTEvent e) {

            // this form of delegation is much simpler than passing everything through
            // our own mouse / mouse motion listeners, and has the added benefit of
            // preventing horizontal cross-text drags from causing the blocky
            // character-level repaint bug (when the background is semi-transparent)
            
            if (!isEditable() && e instanceof MouseEvent) {
                if (e.getID() == MouseEvent.MOUSE_CLICKED) {
                    mouseClicked((MouseEvent)e);
                } else {
                    // This allows mouse press/release/drag events to 
                    // be passed on to the Row as if they happened there.
                    // The coordinate system will be different, but
                    // Row.mouseDragged really only needs the relatve
                    // event-to-event deltas.
                    row.processEventUp(e);
                }
                
            } else
                super.processEvent(e);
        }

        private void mouseClicked(MouseEvent e)
        {
            if (GUI.isDoubleClick(e)) {
                if (DEBUG.MOUSE) Log.debug("DOUBLE CLICK " + this);                
                setEditable(true);
                requestFocus();
            }
        }        

        public String toString() {
            return "TextEdit[" + row + "]";
        }
    }

    private static class Preview extends JPanel {

        final LWComponent layer;

        Preview(LWComponent c) {
            layer = c;
        }
        
        @Override
        public void paintComponent(Graphics _g) {

            //final DrawContext dc = new DrawContext(DEBUG.BOXES ? g.create() : g, layer);
            final DrawContext dc = new DrawContext(_g.create(), layer);
            dc.setAntiAlias(true);
            dc.setPrioritizeSpeed(true);
            dc.setDraftQuality(true);
            dc.setInteractive(false);
                        
            if (layer instanceof Layer == false) {
                //dc.fillBackground(Color.white);
                layer.drawFit(dc, 0);
                return;
            }

            //System.out.println("bounds: " + Util.fmt(getBounds()));
            //System.out.println("  clip: " + Util.fmt(g.getClipRect()));
            
//             if (layer.isVisible()) {
//                 //g.setColor(Color.yellow);
//                 g.setColor(layer.getMap().getFillColor());
//                 ((Graphics2D)g).fill(g.getClipRect());
//             }
                        
            final Rectangle frame = getBounds();

            frame.x = frame.y = 0; // our GC is already offset to Component.getX/getY
            frame.grow(-1, -1);
            //frame.grow(-1, -4); // leave a vertical gap, and a bit of horiz room to prevent clipping at right
                        
            final Point2D.Float offset = new Point2D.Float();
            final Size size = new Size(frame);

            final Rectangle2D.Float allLayerBounds = new Rectangle2D.Float();

            // todo: would be nice if layers cached all their children bounds
            // -- LWMap should be using code for same

            for (LWComponent l : layer.getMap().getChildren())
                LWMap.accruePaintBounds(l.getChildren(), allLayerBounds);
                        
            final double zoom = tufts.vue.ZoomTool
                .computeZoomFit(size,
                                0,
                                allLayerBounds,
                                offset,
                                0.5); // max zoom for preview is 50%
                        
            dc.g.translate(-offset.x + frame.x, -offset.y + frame.y);
            dc.g.scale(zoom, zoom);
            dc.setClipOptimized(false);
            layer.drawZero(dc);

            if (DEBUG.BOXES) {

                // Would be nice if computeZoomFit could also set for us a used
                // viewport size, so we wouldn't have to draw this in the scaled
                // down GC, and it would be easier to create insets.
                dc.setAbsoluteStroke(1);
                //dc.g.setColor(Color.blue);
                //dc.g.draw(allLayerBounds);
                dc.g.setColor(Color.red);
                //Util.grow(allLayerBounds, 5 / (float) zoom);
                //Util.grow(allLayerBounds, 5);
                //allLayerBounds.width -= 1/zoom;
                dc.g.draw(allLayerBounds);
            }
                        

            if (DEBUG.BOXES) {
                final Graphics2D g = (Graphics2D) _g;
                g.setColor(Color.lightGray);
                Rectangle r = getBounds();
                g.drawRect(0,0, r.width-1, r.height-1);
            }

            //g.draw(new Rectangle2D.Float(offset.x,offset.y, allLayerBounds.width, allLayerBounds.height));
                        
            //((Graphics2D)g).draw(frame);
            //((LWContainer)layer).drawChildren(dc);
        }
    }

    private static final Insets LockedInsets = new Insets(4,4,4,0);
    private static final Dimension LayerHeight = new Dimension(0, 38);
    private static final Dimension DefaultHeight = new Dimension(0, 28);

//     private static class MouseTracker extends tufts.vue.MouseAdapter implements Runnable
//     {
//         boolean entered;
//         Row overRow;

//         MouseTracker() {
//             super("layer.row.mouse-tracker");
//         }
        
//         public void mouseEntered(MouseEvent e) {
//             entered = true;
//         }
//         public void mouseExited(MouseEvent e) {
//             entered = false;
//         }

//         // called when mouse-entered happens on Row container
//         public void setRow(Row r) {
//             if (overRow != null && overRow != r)
//                 overRow.rollOff();
//             overRow = r;
//         }
        
//         // called when mouse-exited happens on the Row container
//         public void run() {
//             if (!entered) {
//                 if (DEBUG.FOCUS) Log.debug("MOUSE-TRACKER: no child entered, rolling off for real");
//                 overRow.rollOff();
//             } else {
//                 if (DEBUG.FOCUS) Log.debug("MOUSE-TRACKER: a child was entered, do not roll off row");
//             }
//         }
//     }

    /**
     * Fail-safe mouse enter/exit tracker for Row's.  Only "exits" a Row when a new one
     * is entered, or a parent of all the Rows is exited -- otherwise, if we just rely
     * on standard events, when any child of a Row is mouse-entered, the Row itself is
     * exited, yet this isn't actually rolling-off the row.
     */
    
    private static class MouseTracker extends tufts.vue.MouseAdapter implements Runnable
    {
        Row overRow;

        MouseTracker() {
            super("layer.row.mouse-tracker");
        }
        
        /** MOUSE_ENTERED events on Rows should be forwarded here */
        public void recordMouseEntered(Row row, MouseEvent e) {
            setRow(row);
        }

        public void mouseExited(MouseEvent e) {
            
            // any potential parent of a Row, that could possibly get a mouseExited
            // event, should be re-routed here.  MOUSE_EXITED events can appear very
            // unreliably, so as many parents as possible should be tracking for these
            // events in the hope that at least one of them will get the event, even
            // when the mouse is moving fast.  Even then, we can still miss some.  It
            // appears we may even need to check for WINDOW_LOST_FOCUS events on the top
            // level window for more reliability -- and we must create a listener --
            // there appears to be no Window state we can reliably poll for this!
            
            setRow(null);
        }

        // called when mouse-entered happens on Row container
        void setRow(Row r) {
            if (overRow != null && overRow != r)
                overRow.rollOff();
            overRow = r;
            if (overRow != null)
                overRow.rollOn();
        }

        public void run() {

            // todo: only real fail-safe method of handling this will be to reset a
            // timer each we get here (Row MOUSE_EXITED has happened), and then if after
            // 500ms or so, another Row hasn't been entered, we can roll off the last
            // row.  Tho we should be able to cancel the timer right off and do nothing
            // if overRow has changed since the check was scheduled in the AWT event
            // queue (e.g., right after MOUSE_EXITED, a MOUSE_ENTERED happened on
            // another row, so overRow is different, and until we see another Row
            // MOUSE_EXITED, we don't need the failsafe timer).
            
//             if (overRow != null) {
//                 Window parent = SwingUtilities.getWindowAncestor(overRow);
//                 if (!parent.isFocused()) {
//                     if (DEBUG.FOCUS) Log.debug("rolling off last row as window has lost focus");
//                     setRow(null);
//                 } else if (DEBUG.FOCUS) {
//                     if (DEBUG.FOCUS) Log.debug("parent still focused: " + GUI.name(parent));
//                 }
//             }
        }
    }
    

    private final MouseTracker RowMouseEnterExitTracker = new MouseTracker();
    

    private class Row extends JPanel implements javax.swing.event.MouseInputListener, Runnable {

        final AbstractButton exclusive;
        final AbstractButton visible = new JCheckBox();
        final AbstractButton locked = new JRadioButton();
        //final JLabel activeIcon = new JLabel();

        final JTextField label;
        final JPanel preview;
        final AbstractButton grab;
        
        final LWComponent layer;

        final Color defaultBackground;

        
        Row(final LWComponent layer)
        {
            this.layer = layer;
            label = new TextEdit(this);
            setToolTipText(label.getText());
            setName(layer.toString());
            setLayout(new GridBagLayout());
            setBorder(new CompoundBorder(new MatteBorder(1,0,1,0, Color.lightGray),
                                         GUI.makeSpace(3,7,3,0)));
            if (SCROLLABLE) {
                if (layer instanceof Layer)
                    setPreferredSize(LayerHeight);
                else
                    setPreferredSize(DefaultHeight);
            }
            //setMaximumSize(new Dimension(Short.MAX_VALUE, 64)); // no effect
            //setMinimumSize(new Dimension(150, 100)); // no effect

            addMouseListener(this);
            addMouseMotionListener(this);
            
            if (layer instanceof Layer)
                defaultBackground = Color.white;// Changed for Background
            else
                defaultBackground = Color.white; // debug/test case
            setBackground(defaultBackground);
            
            if (true) {
                // looks a bit messy w/current icons, but more informative
                visible.setName("layer.visible");
                visible.setIcon(VueResources.getImageIcon("pathwayOff"));
                visible.setSelectedIcon(VueResources.getImageIcon("pathwayOn"));
                // need a bigger and/or colored icon -- to tough to see
                locked.setName("layer.locked");
                locked.setIcon(VueResources.getImageIcon("lockOpen"));
                locked.setSelectedIcon(VueResources.getImageIcon("lock"));
                locked.setMargin(LockedInsets);
                locked.setBorder(GUI.makeSpace(1,5,5,1)); // no effect
            }
            
            locked.setSelected(layer.isLocked());
            locked.setBorderPainted(layer.isLocked());
            locked.setOpaque(false);
            locked.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        //locked.setBorderPainted(locked.isSelected());
                        layer.setLocked(locked.isSelected());
                        if (layer == getActiveLayer() && !canBeActive(layer))
                            if (AUTO_ADJUST_ACTIVE_LAYER) attemptAlternativeActiveLayer(false);
                    }});
            

            visible.setSelected(layer.isVisible());
            visible.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        layer.setVisible(visible.isSelected());
                        locked.setEnabled(layer.isVisible());                        
                        label.setEnabled(layer.isVisible());                        
                        if (layer == getActiveLayer() && !canBeActive(layer))
                            if (AUTO_ADJUST_ACTIVE_LAYER) attemptAlternativeActiveLayer(false);
                            
                    }});

            label.setEnabled(layer.isVisible());            
            
            if (layer instanceof Layer) {

                exclusive = new JRadioButton();
                exclusive.setName("exclusive");
                exclusive.setToolTipText("Quick-Edit");
                exclusive.setBorderPainted(false);
                exclusive.setIcon(VueResources.getIcon(VUE.class, "images/quickFocus_ov.png"));
                exclusive.setFocusable(false);
                exclusive.setOpaque(false);
                exclusive.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            exclusive.setBorderPainted(exclusive.isSelected());
                            Row.this.setExclusive(exclusive.isSelected());
                        }});
            
                //grab = new JButton("Grab");
                //grab.setFont(VueConstants.SmallFont);
                grab = new JRadioButton();                
                grab.setName("grab");
                grab.setToolTipText("Move selection to this layer");
                grab.setBorderPainted(false);
                grab.setIcon(VueResources.getIcon(VUE.class, "images/grab_ov.png"));
                grab.setFocusable(false); // FYI, no help on ignoring mouse-motion
                grab.setOpaque(false);
                

//                     // todo: use icon-button version when ready to go -- may
//                     // want to use a VueButton
//                     grab = new JButton();
//                     grab.setBorderPainted(false);
//                     // todo: update when Melanie creates new icon for this
//                     grab.setIcon(VueResources.getIcon(VUE.class, "images/hand_open.png"));
//                     grab.putClientProperty("JButton.buttonType", "textured");
//                     grab.putClientProperty("JButton.sizeVariant", "tiny");
                
                grab.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            if (VUE.getSelection().size() > 0) {
                                grabFromSelection((Layer)layer);
                                VUE.getUndoManager().mark("Move To Layer " + Util.quote(layer.getLabel()));
                            }
                        }});
            } else {
                exclusive = null;
                grab = null;
            }

            
            final JLabel info = new JLabel()
                //{ public Dimension getMinimumSize() { return GUI.ZeroSize; } }
                ;
            info.setMinimumSize(new Dimension(40,30));
            if (layer.supportsChildren()) {
                
                // This might slow down undo of some large-set operations in large maps,
                // such as grabs, as auto-reparenting will currently de-parent each
                // child separately, issuing an event for each.  (All hierarchy events,
                // however, are merged into a single one for undo/redo for each parent).
                
                // Note: depends on Layer having permitZombieEvent(e) return
                // true, otherwise won't update correctly on undo.
                final LWComponent.Listener countListener
                    = new LWComponent.Listener() {
                        public void LWCChanged(LWCEvent e) {
                            //if (DEBUG.Enabled) Log.debug("UPDATING " + Row.this + " " + e);
                            String counts = "";
                            final int nChild = layer.numChildren();
                            final int allChildren = layer.getDescendentCount();
                                
                            if (nChild > 0)
                                counts += nChild;
                            if (allChildren != nChild)
                                counts += "/" + allChildren;
                            
                            if(counts.length()==0){
                            	info.setText("");
                            }else{
                            	info.setText("("+counts+")");
                            }
                            //if (DEBUG.Enabled) { Row.this.validate(); GUI.paintNow(Row.this); } // slower
                            // above will usually cause a deadlock tho when dropping images and this UI is visible
                            //if (DEBUG.Enabled) { Row.this.validate(); GUI.paintNow(info); } // faster
                        }};
                countListener.LWCChanged(null); // do the initial set
                layer.addLWCListener(countListener, LWKey.ChildrenAdded, LWKey.ChildrenRemoved);
            }


//             activeIcon.setIcon(VueResources.getIcon(VUE.class, "images/hand_open.png"));
//             // todo perf: only actually need instance of each of these for all rows:
//             activeIcon.setDisabledIcon(new GUI.EmptyIcon(activeIcon.getIcon())); 
//             activeIcon.setBorder(GUI.makeSpace(4,0,0,0));

            
            //final JComponent label = new VueTextField(layer.getLabel());
            // VueTextField impl not useful to us (also not used anywhere)
            // -- we need an impl that works just like VueTextPane, except
            // as a single line of text.
            
            //final JLabel info = new JLabel("(" + layer.numChildren() + " items)");
            
            final GridBagConstraints c = new GridBagConstraints();
            c.weighty = 1; // 1 has all expanding to fill vertical, 0 leaves all at min height
            c.anchor = GridBagConstraints.WEST;
            
//             c.insets.right = 4;
//             add(exclusive, c);

            //add(Box.createHorizontalStrut(5), c);
            c.insets.right = 0;
            add(visible, c);
            
            info.setHorizontalAlignment(SwingConstants.RIGHT);

            if (true) {

                // this magic, setting min-size to zero on the info text to 0, and wrapping
                // it in a container with the label, allows it fill left, shriking the
                // edit label if need-be, but never expanding the size of the two
                // components togehter -- that way, all label-edit + info-text groups
                // in all rows will always have the same width, keeping everything
                // in alignment
                
                info.setMinimumSize(new Dimension(40,23));                
                label.setCaretPosition(0);                
                //info.addMouseListener(RowMouseEnterExitTracker);
                Box box = new Box(BoxLayout.X_AXIS);
                //JPanel box = new JPanel();
                label.setPreferredSize(null); // must remove this, or info gets squished to 0 width
                //label.addMouseListener(RowMouseEnterExitTracker);
                box.add(label);
                //box.add(Box.createHorizontalGlue());
                box.add(info);
                if (DEBUG.BOXES) box.setBorder(new LineBorder(Color.red));
                box.setPreferredSize(GUI.MaxSize);
                //box.setPreferredSize(new Dimension(200, 30));
                //box.setMaximumSize(new Dimension(200, 30)); // apparently no use
                
                c.weightx = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.insets.right = 0;
                add(box, c);
                c.insets.left = 0;
                //c.insets.right = 0;
                c.weightx = 0;
                c.fill = GridBagConstraints.NONE;
                
            } else {

                c.weightx = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                add(label, c);
                c.fill = GridBagConstraints.NONE;
                c.weightx = 0;
                add(info, c);

//                 add(Box.createHorizontalStrut(1));
//                 add(label);
            
//                 //add(Box.createHorizontalGlue());
            
//                 add(Box.createHorizontalStrut(1));
//                 //info.setBorder(new LineBorder(Color.red));
//                 //info.setPreferredSize(new Dimension(70,Short.MAX_VALUE));
//                 //info.setMinimumSize(new Dimension(60,0));
//                 add(info);
            }
            
            //add(Box.createHorizontalGlue(), c);

            if (layer.hasFlag(INTERNAL)) {            	
                add(locked, c);
                preview = null;
                return;
            }
            
            preview = new Preview(layer);
            //preview.setMinimumSize(new Dimension(128, 64));
            
            //preview.setPreferredSize(GUI.MaxSize);
            //preview.setSize(256,128);
            //preview.setPreferredSize(new Dimension(256, Short.MAX_VALUE));
            //preview.setMaximumSize(GUI.MaxSize);

            if (false && DEBUG.Enabled)
                layer.addLWCListener(new LWComponent.Listener() {
                        public void LWCChanged(LWCEvent e) {
                            // this is heavy duty!  Would be nice if UserActionCompleted
                            // came through the layer, and we could listen for that,
                            // but it comes through the map
                            preview.repaint();
                        }});
            
            

            //add(Box.createHorizontalGlue());
            
            if (preview != null) {
                c.weightx = 1;
                c.fill = GridBagConstraints.BOTH;
                //c.fill = GridBagConstraints.VERTICAL;
                //add(Box.createHorizontalStrut(7));
                add(preview, c);
                c.weightx = 0;
                c.fill = GridBagConstraints.NONE;
                
            }

            if (true) {
                JPanel fixed = new JPanel(new BorderLayout());
                fixed.setOpaque(true);                
                fixed.setBorder(GUI.makeSpace(3,1,3,1));
                fixed.setMinimumSize(new Dimension(50, 0));
                
                //fixed.add(exclusive, BorderLayout.WEST);
                grab.setMinimumSize(new Dimension(25, 0));
                locked.setMinimumSize(new Dimension(25, 0));                            
                fixed.add(grab, BorderLayout.CENTER);
                fixed.add(locked, BorderLayout.EAST);                
                c.fill = GridBagConstraints.BOTH;
                add(fixed, c);
            } else {            	
                // old-style before we added hiding these on mouse roll-off
                //add(activeIcon, c);
                add(grab, c);
                add(locked, c);
            }
            //setBorder(BorderFactory.createLineBorder(Color.red, 1));
            // set initial visibility states by simulating a mouse roll-off
            rollOff(); 

        }

        private void add(Component comp, GridBagConstraints c) {
            if (comp == null)
                return;
            //super.add(comp);
            super.add(comp, c);
            //comp.addMouseListener(RowMouseEnterExitTracker);
            //c.gridx++;
        }

        private void processEventUp(AWTEvent e) {
            super.processEvent(e);
        }

        private void setExclusive(final boolean excluding) {

            Row exclusiveRow = fetchExclusiveRow();

            //Log.debug("SET-EXCLUSIVE " + this + " = " + excluding + "; nowExclusive=" + exclusiveRow);

            if (excluding && exclusiveRow == this)
                return;

            final boolean wasExcluding = exclusiveRow != null;
            final Row lastExcluded = exclusiveRow;
            exclusiveRow = this;

            if (wasExcluding && excluding) {
                // disable old row
                lastExcluded.label.setEnabled(false);
                lastExcluded.layer.setHidden(LAYER_EXCLUDED);
                lastExcluded.exclusive.setVisible(false); // simulate rollOff
                lastExcluded.exclusive.setSelected(false);
                lastExcluded.exclusive.setBorderPainted(false); // clear sticky-state border
            }

            if (excluding) {
                if (!wasExcluding)
                    setExclusiveMode(true, this);
                storeExclusiveRow(this);
                if (layer instanceof Layer)
                    setActiveLayer((Layer) layer);
            } else if (exclusiveRow == this) {
                setExclusiveMode(false, null);
                return;
            }

            exclusive.setSelected(excluding);
            label.setEnabled(true);
                        
            layer.clearHidden(DEFAULT);
            layer.clearHidden(LAYER_EXCLUDED);
            layer.setLocked(false);

            indicateActiveLayers(null);
            //tufts.vue.ZoomTool.setZoomFit();
        }


        @Override
        protected void addImpl(Component comp, Object constraints, int index) {
            if (comp instanceof JComponent)
                ((JComponent)comp).setOpaque(false); // needed for Tiger (uneeded on Leopard)
            comp.setFocusable(false);
            //comp.setFocusable(comp instanceof JTextField);
            super.addImpl(comp, constraints, index);
        }
        
        @Override
        public void setBackground(Color bg) {
            //Log.debug(this + " setBackground " + bg);
            if (bg == null) {
                super.setBackground(defaultBackground);
//                 if (label != null)
//                     label.setBackground(defaultBackground);
            } else {
                super.setBackground(bg);
//                 if (label != null) {
//                     if (bg.getAlpha() != 255) {
//                         //label.setBackground(Color.red);
//                         label.setBackground(new Color(0,0,0,0));
//                         label.setOpaque(false);
//                     } else {
//                         label.setBackground(bg);
//                     }
//                 }
            }
        }
        

        private int dragStartX;
        private int dragStartY;
        private int dragStartMouseY;
        private int dragRowIndex;
        private int dragLastY;
        private boolean didReorder;
        private Color saveColor;


        public void mouseEntered(MouseEvent e) {
            RowMouseEnterExitTracker.recordMouseEntered(this, e);
        }
        
        public void mouseExited(MouseEvent e) {
//             //Util.printStackTrace("HERE");
//             RowMouseEnterExitTracker.setRow(this);
            if (DEBUG.FOCUS) Log.debug("SCHEDULING MOUSE-TRACKER on " + e);
            GUI.invokeAfterAWT(RowMouseEnterExitTracker);
            
        }

        void rollOn() { 
        	
            if (grab != null) {
                grab.setVisible(true);
                //grab.setFocusable(false); // NO HELP ON IGNORING MOUSE-MOTION
            }
            if (locked != null){
            	//locked.setBorder(BorderFactory.createEmptyBorder(0, 35, 0, 0));
                locked.setVisible(true);
            }
            if (exclusive != null)
                exclusive.setVisible(true);

        }
        
        void rollOff() { 
        	
            if (grab != null){            	
                grab.setVisible(false);                
            }
            if (locked != null && !locked.isSelected())
                locked.setVisible(false);
            if (exclusive != null && !exclusive.isSelected())
                exclusive.setVisible(false);
        }
        
        
        public void mouseClicked(MouseEvent e) {        	
            if (GUI.isDoubleClick(e)) {
            	if(e.isShiftDown()){  
            		if (VUE.getSelection() !=null)
            			VUE.getSelection().add(layer.getChildren());
            		else
            			VUE.getSelection().setTo(new LWSelection(layer.getChildren()));             		
            		          		
            		setActiveLayer((Layer) layer, !UPDATE);
            		
            	}else{
            		VUE.getSelection().setTo(layer.getAllDescendents());
            	}            	
            }
            if(((JButton)mToolbar.getComponent(3)).isBorderPainted()){
        		VUE.getSelection().setTo(layer.getAllDescendents());
        	}
        }

        public void mousePressed(MouseEvent e) {

            Log.debug(e);             
            if (layer instanceof Layer) {
// //                 if (e.isShiftDown())
// //                     mSelection.toggle(layer);
// //                 else
//                     mSelection.setTo(layer);
                
//                 if (inExclusiveMode()) // exlusive mode is no longer globally modal
//                     setExclusive(true);
//                 else            	
            		if (!AUTO_ADJUST_ACTIVE_LAYER || layer.isVisible()) 
                    setActiveLayer((Layer) layer, UPDATE);
            		
//                 if (VUE.getSelection().isEmpty() || VUE.getSelection().only() instanceof Layer)
//                     VUE.getSelection().setTo(layer);                   
            } else {
                // this case for debug/test only: we shouldn't normally
                // see regular objects a the top level of the map anymore
                VUE.getSelection().setTo(layer);                
            }
            //LayersUI.this.requestFocus();
            isDragUnderway = false;
            mDragRow = null;
            didReorder = false;
        }
        
        public void mouseReleased(MouseEvent e) {
            isDragUnderway = false;
            mDragRow = null;
            if (saveColor != null) {
                setBackground(saveColor);
                saveColor = null;
            }                
            layoutRows();
            if (didReorder) {
                didReorder = false;

                if (!DYNAMIC_UPDATE) {
                    Util.printStackTrace("unimplemented");
                    // a implement LWContainer.insertAt(index, LWComponent) (can just use mChildren.add(index, c)
                    return;
                }
                
                if (dragRowIndex == mRows.indexOf(this)) {
                    layer.getMap().getUndoManager().resetMark();
                } else {
                    String undoMsg;
                    if (dragRowIndex > mRows.indexOf(this))
                        undoMsg = "Raise Layer ";
                    else
                        undoMsg = "Lower Layer ";
                    layer.getMap().getUndoManager().mark(undoMsg + Util.quote(layer.getLabel()));
                }
                        
            }
        }
        
        public void mouseMoved(MouseEvent e){}

        private int getDropRegionSize() {
            if (mRows.size() > 7) {
                // by using less than total height, we leave a narrow visible region
                // where the original item was as a reminder to the user of where
                // they're dragging from
                return (int) (0.8f * getHeight());
            } else
                return getHeight();
        }

        private static final boolean DYNAMIC_UPDATE = true;

        public void mouseDragged(MouseEvent e) 
        {
            final Point mouse = SwingUtilities.convertPoint(this, e.getPoint(), getParent());
            
            if (!isDragUnderway) {
                //System.out.println("START-DRAG");
                dragStartX = getX();
                dragStartY = getY();
                dragStartMouseY = mouse.y;
                dragLastY = mouse.y;
                dragRowIndex = mRows.indexOf(this);
                getParent().setComponentZOrder(this, 0); // make sure always paints on top
                isDragUnderway = true;
                mDragRow = this;
                Color c = getBackground();
                setBackground(new Color(c.getRed(),c.getGreen(),c.getBlue(),128));
                saveColor = c;
                didReorder = false;
            }

            int newY = dragStartY + (mouse.y - dragStartMouseY);
            final int dy = newY - dragStartY;

            if (newY < 0)
                newY = 0;
            else if (newY > getParent().getHeight() - getHeight())
                newY = getParent().getHeight() - getHeight();

            //System.out.println("newY=" + newY + "; dy=" + dy);

            boolean moved = false;

            final int curIndex = mRows.indexOf(this);
            if (mouse.y < dragLastY && curIndex > 0) {

                final Row above = mRows.get(curIndex - 1);

                if (newY < above.getY() + above.getHeight() / 2) {
                    if (DYNAMIC_UPDATE) {
                        // will trigger reload/relayout of all rows                        
                        moved = layer.getParent().bringForward(layer);
                    } else {
                        if (DEBUG.Enabled) Log.debug("BUMP DOWN " + above);
                        above.setLocation(above.getX(), above.getY() + getDropRegionSize());
                        Collections.swap(mRows, curIndex, curIndex - 1);
                        moved = true;
                    }
                }
                
            } else if (mouse.y > dragLastY && curIndex < mRows.size() - 1) {

                final Row below = mRows.get(curIndex + 1);
                final int bottomEdge = newY + getHeight();
                
                if (bottomEdge > below.getY() + below.getHeight() / 2) {

                    if (DYNAMIC_UPDATE) {
                        // will trigger reload/relayout of all rows                        
                        moved = layer.getParent().sendBackward(layer);
                    } else {
                        if (DEBUG.Enabled) Log.debug("BUMP  UP  " + below);
                        below.setLocation(below.getX(), below.getY() - getDropRegionSize());
                        Collections.swap(mRows, curIndex, curIndex + 1);
                        moved = true;
                    }
                }
            }

            if (moved) {
                getParent().setComponentZOrder(this, 0); // make sure always paints on top
                didReorder = true;
            }
            
            setDragLocation(getX(), newY);

            dragLastY = mouse.y;
            
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            // This prevents us from being laid-out to the new location
            // during drags (jumping to the new location for one mouse-move)
            // Must be paired with setDragLocation to work.
            if (mDragRow == this)
                return;
            else
                super.setBounds(x, y, width, height);
        }

        private void setDragLocation(int x, int y) {
            super.setBounds(x, y, getWidth(), getHeight());
        }

        public void run() {
            setVisible(true);
        }
        

        @Override
        public String toString() {
            return "Row[" + mRows.indexOf(this) + "; " + layer + "]";
        }
    }
    public class SubtleSquareBorder implements Border
    {
        protected int m_w = 6;
        protected int m_h = 6;
        protected Color m_topColor = Color.gray;
        protected Color m_bottomColor = Color.gray;
        protected boolean roundc = false; // Do we want rounded corners on the border?
    
        public SubtleSquareBorder(boolean round_corners)
        {    
          roundc = round_corners;
        }
    
        public Insets getBorderInsets(Component c)
        {
            return new Insets(m_h, m_w, m_h, m_w);
        }
    
        public boolean isBorderOpaque()
        {
            return true;
        }
    
        public void paintBorder(Component c, Graphics g, int xx, int yy, int ww, int hh)
        {
            int x = xx + 5;
            int y = yy + 5;
            int w = ww - 12;
            int h = hh-12;
        //w = w - 3;
        //h = h - 3;
        x ++;
        y ++;
    
        // Rounded corners
        if(roundc)
        {
        g.setColor(m_topColor);
        g.drawLine(x, y + 3, x, y + h - 2);        
        g.drawLine(x + 2, y, x + w - 2, y);
        g.drawLine(x, y + 2, x + 2, y); // Top left diagonal
        g.drawLine(x, y + h - 2, x + 2, y + h); // Bottom left diagonal
        g.setColor(m_bottomColor);
        g.drawLine(x + w, y + 2, x + w, y + h - 2);
        g.drawLine(x + 2, y + h, x + w -2, y + h);
        g.drawLine(x + w - 2, y, x + w, y + 2); // Top right diagonal
        g.drawLine(x + w, y + h - 2, x + w -2, y + h); // Bottom right diagonal        
        }
    
        // Square corners
        else
        {
        g.setColor(m_topColor);
        g.drawLine(x, y, x, y + h);
        g.drawLine(x, y, x + w, y);
        g.setColor(m_bottomColor);
        g.drawLine(x + w, y, x + w, y + h);
        g.drawLine(x, y + h, x + w, y + h);
        }
    
        }

    }
}
        
    



    
//         addKeyListener(new KeyAdapter() {
//                 public void keyPressed(KeyEvent e) {
//                     // Why aren't the mark's working?
//                     //System.out.println("KP " + e);
//                     final tufts.vue.LWContainer layer = VUE.getActiveLayer();
//                     if (layer == null)
//                         return;
//                     if (e.getKeyCode() == KeyEvent.VK_UP) {
//                         if (layer.getParent().bringForward(layer)) {
//                             layer.getMap().getUndoManager().mark("Raise Layer " + Util.quote(layer.getLabel()));
//                         }
//                     } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
//                         if (layer.getParent().sendBackward(layer)) {
//                             layer.getMap().getUndoManager().mark("Lower Layer " + Util.quote(layer.getLabel()));
//                         }
//                     }
//                 }
//             });

//     private static final String LAYER_NEW = "New";
//     private static final String LAYER_DUPLICATE = "Duplicate";
//     private static final String LAYER_MERGE = "Merge";
//     private static final String LAYER_DELETE = "Delete";

//     public void actionPerformed(ActionEvent e) {

//         if (mMap == null)
//             return;

//         final String a = e.getActionCommand();
//         final Layer active = mMap.getActiveLayer();
        
//         if (a == LAYER_NEW) {

//             mMap.addChild(new LWMap.Layer());

//         } else if (a == LAYER_DUPLICATE) {

//             Layer dupe = (Layer) active.duplicate();
//             mMap.addChild(dupe);
            
//         } else if (a == LAYER_MERGE) {

//         } else if (a == LAYER_DELETE) {

//             mMap.deleteChildPermanently(active);

//         } else {
            
//             Log.warn("unhandled action: " + e);
//         }

//         mMap.getUndoManager().mark(a + " Layer");

//     }