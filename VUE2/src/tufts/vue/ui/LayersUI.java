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

import tufts.vue.DEBUG;
import tufts.vue.VUE;
import tufts.vue.LWComponent;
import tufts.vue.LWComponent.HideCause;
import tufts.vue.LWContainer;
import tufts.vue.LWMap;
import tufts.vue.LWMap.Layer;
import tufts.vue.LWCEvent;
import tufts.vue.LWKey;
import tufts.vue.ActiveInstance;
import tufts.vue.ActiveEvent;
import tufts.vue.VueConstants;
import tufts.vue.DrawContext;
import tufts.vue.VueResources;
import tufts.vue.LWSelection;
import tufts.vue.gui.*;

import tufts.Util;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;


/**
 * @version $Revision: 1.8 $ / $Date: 2008-07-16 20:54:16 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public class LayersUI extends tufts.vue.gui.Widget implements LWComponent.Listener, LWSelection.Listener
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LayersUI.class);

    private final java.util.List<Row> mRows = new java.util.ArrayList();
    private LWMap mMap;
    private boolean isDragUnderway;
    private Row mDragRow;

    public LayersUI() {
        super("layers");
        setName("layersUI");
        //setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        //setLayout(new GridLayout(0,1));
        setLayout(new GridBagLayout());
        VUE.addActiveListener(LWMap.class, this);
        VUE.getSelection().addListener(this);
        //VUE.addActiveListener(Layer.class, this);
        //VUE.addActiveListener(LWComponent.class, this);
        //setFocusable(false);

    }

    public void activeChanged(ActiveEvent e, LWMap map) {
        loadMap(map);
    }

//     public void activeChanged(ActiveEvent e, Layer layer) {
//         indicateActiveLayer(layer, null);
//     }

//     public void activeChanged(ActiveEvent e, LWComponent c) {
//         enableForSingleSelection(c);
//         // for debug / child-list mode:
//         if (mMap != null && !mMap.isLayered())
//             indicateActiveLayer(null);
//     }

    private static final boolean UPDATE = true;
    
    private void setActiveLayer(Layer c) {
        setActiveLayer(c, !UPDATE);
    }

    private void setActiveLayer(Layer c, boolean update) {
        //if (DEBUG.Enabled) Log.debug("SET-ACTIVE: " + c);
        if (c != null)
            mMap.setClientProperty(Layer.class, "last", mMap.getActiveLayer());
        mMap.setActiveLayer(c);
        if (update)
            indicateActiveLayers(null);
    }

    private boolean canBeActive(LWComponent layer) {
        return canBeActive(layer, true);
    }
    private boolean canBeActive(LWComponent layer, boolean checkLocking) {
        if (layer == null || layer.isHidden() || (checkLocking && layer.isLocked()))
            return false;
        else
            return layer instanceof Layer;
    }

    private static final boolean AUTO_ADJUST_ACTIVE_LAYER = false;
    
    private void attemptAlternativeActiveLayer() {

        if (!AUTO_ADJUST_ACTIVE_LAYER) return;

        final Layer curActive = getActiveLayer();
        final Layer lastActive = mMap.getClientProperty(Layer.class, "last");

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

        if (visibleButLocked != null && curActive.isHidden() && curActive.isLocked()) {
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

        //Log.debug("selectionChanged: " + s + "; size=" + s.size() + "; " + Arrays.asList(s.toArray()) + "; parents=" + s.getParents());

        enableForSelection(s);

//         if (!s.getParents().contains(mMap.getActiveLayer()))
//             for (LWComponent c : s.getParents())
//                 if (c instanceof Layer)
//                     mMap.setActiveLayer(c);

        if (s.size() == 1 && s.first().getLayer() != null) {
            //if (DEBUG.Enabled) Log.debug("selectionChanged: single selection; activate layer of: " + s.first());
            setActiveLayer(s.first().getLayer());
        } else if (s.getParents().size() == 1 && s.first().getParent() instanceof Layer) {
            //if (DEBUG.Enabled) Log.debug("selectionChanged: one parent in selection; active parent " + s.first().getParent());
            setActiveLayer((Layer) s.first().getParent());
        }
        
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

    public void LWCChanged(LWCEvent e) {

        // ignore events from children: just want hierarchy events directly from the map
        // (as we're only interested in changes to map layers)
        
        if (e.key == LWKey.UserActionCompleted) {
            repaint(); // repaint the previews
        }
        else if (e.getSource() == mMap) {
            if (e.getName().startsWith("hier."))
                loadLayers(mMap);
        }
    }
    
    private final Color ActiveBG = VueConstants.COLOR_SELECTION;
    private final Color SelectedBG = VueConstants.COLOR_SELECTION.brighter();
    
    private void indicateActiveLayers(Collection<? extends LWComponent> layers) {

        //Log.debug("INDICATE ACTIVES: mapActive=" + mMap.getActiveLayer() + "; multiSelect=" + layers);

        final Layer activeLayer = getActiveLayer();

        // TODO: Why are selection bits being left on?  (why getting SelectedBG on multiple items!)
        // can probably just hack it and check the selection here manually...  tho this
        // shouldn't be happening...

        for (Row row : mRows) {

            // Log.debug("UPDATING: " + row.layer);
            
            if (row.layer == activeLayer) {
                //Log.debug("**ACTIVE: " + row.layer);
                row.activeIcon.setEnabled(true);
                row.setBackground(row.layer.isSelected() ? SelectedBG : ActiveBG);
                continue;
            }

            row.activeIcon.setEnabled(false);
            
            if (layers != null) 
                row.setBackground(layers.contains(row.layer) ? ActiveBG : null);
            else if (row.layer.isSelected())
                row.setBackground(SelectedBG);
            else
                row.setBackground(null);
        }
        
    }
        
    private void enableForSelection(LWSelection s) {

        final boolean enableAny = s.size() > 0 && !(s.only() instanceof Layer);
        final Collection parents = s.getParents();
        
        for (Row r : mRows) {
            if (enableAny && (parents.size() > 1 || !s.getParents().contains(r.layer)))
                r.grab.setEnabled(true);
            else
                r.grab.setEnabled(false);
        }
        
        
    }
    
    private void loadLayers(final LWMap map) {

        mRows.clear();
        
        if (map != null) {
            
            // handle in reverse order (top layer on top)
            for (int i = map.numChildren() - 1; i >= 0; i--) {
                LWComponent layer = map.getChild(i);
                Row row = produceRow(layer);
                mRows.add(row);
            }
        }

        if (!isDragUnderway) {
            enableForSelection(VUE.getSelection());
            indicateActiveLayers(null);
        }
        
        layoutRows(mRows);
    }

    private void layoutRows(final java.util.List<? extends JComponent> rows)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.weighty = 1; // 1 has all expanding to fill vertical, 0 leaves all at min height
        c.weightx = 1;
        c.gridheight = 1;
        c.gridwidth = 1;
        //c.gridwidth = GridBagConstraints.REMAINDER;
        //c.fill = GridBagConstraints.HORIZONTAL;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        
        // Each Row has a top and a bottom border line, so that
        // one is always visible no matter what, but we normally only
        // want to see a single line, so this will let them
        // overlap during standard display (e.g., but not when drag-reordering)
        c.insets = new Insets(0,0,-1,0);

        removeAll();

        if (!rows.isEmpty()) {
        
            for (JComponent row : rows) {
                add(row, c);
                c.gridy++;
            }
        
            if (c.weighty == 0) {
                // now add a default vertical expander so the rest of items stay at the top
                c.weighty = 1;
                c.gridy = rows.size() + 1;
                add(new JPanel(), c);
            }
        }

        revalidate(); // needed for Tiger (uneeded on Leopard)
        repaint();
    }

    private Row produceRow(final LWComponent layer)
    {
        Row row = layer.getClientProperty(Row.class);
        if (row != null) {
            return row;
        } else {
            row = new Row(layer);
            layer.setClientProperty(Row.class, row);
            return row;
        }
    }

    private boolean inExclusiveMode() {
        return getExclusiveRow() != null;
    }

    private Row getExclusiveRow() {
        return mMap.getClientProperty(Row.class, "exclusive");
    }
    private void setExclusiveRow(Row row) {
        mMap.setClientProperty(Row.class, "exclusive", row);
    }

    private Layer getPreExclusiveLayer() {
        return mMap.getClientProperty(Layer.class, "pre-exclusive");
    }
    private void setPreExclusiveLayer(Layer layer) {
        mMap.setClientProperty(Layer.class, "pre-exclusive", layer);
    }

    private class Row extends JPanel implements javax.swing.event.MouseInputListener, Runnable {

        final AbstractButton exclusive = new JRadioButton();
        final AbstractButton visible = new JCheckBox();
        final AbstractButton locked = new JRadioButton();
        final JLabel activeIcon = new JLabel();
        final JLabel label = new JLabel();
        //final JTextField label = new JTextField();
        final JPanel preview;
        final AbstractButton grab = new JButton("Grab");
        //final AbstractButton visible = new VueButton.Toggle("layerUI.button.visible");
        
        final LWComponent layer;

        private boolean wasHiddenWhenMadeExclusive;
        private boolean wasLockedWhenMadeExclusive;

        final Color defaultBackground;

        
        Row(final LWComponent layer)
        {
            this.layer = layer;
            setName("row:" + layer);
            //super(BoxLayout.X_AXIS);
            //setOpaque(true);
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setBorder(new CompoundBorder(new MatteBorder(1,0,1,0, Color.lightGray),
                                         GUI.makeSpace(3,7,3,7)));
            //setBorder(GUI.makeSpace(9,7,9,7));
            //setPreferredSize(new Dimension(Short.MAX_VALUE, 48));
            setMinimumSize(new Dimension(128, 48));

            //setBorder(new LineBorder(Color.black));
            
            addMouseListener(this);
            addMouseMotionListener(this);
            
            if (layer instanceof Layer)
                defaultBackground = null;
            else
                defaultBackground = Color.gray; // debug/test case
            setBackground(defaultBackground);
            
            if (true) {
                // looks a bit messy w/current icons, but more informative
                visible.setIcon(VueResources.getImageIcon("pathwayOff"));
                visible.setSelectedIcon(VueResources.getImageIcon("pathwayOn"));
                // need a bigger and/or colored icon -- to tough to see
                //locked.setIcon(VueResources.getImageIcon("lockOpen"));
                //locked.setSelectedIcon(VueResources.getImageIcon("lock"));
            }
            
            locked.setSelected(layer.isLocked());
            locked.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        layer.setLocked(locked.isSelected());
                        if (layer == getActiveLayer() && !canBeActive(layer))
                            attemptAlternativeActiveLayer();
                    }});

            visible.setSelected(layer.isVisible());
            visible.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        layer.setVisible(visible.isSelected());
                        locked.setEnabled(layer.isVisible());
                        label.setEnabled(layer.isVisible());
                        if (layer == getActiveLayer() && !canBeActive(layer))
                            attemptAlternativeActiveLayer();
                            
                    }});

            label.setEnabled(layer.isVisible());

            if (false){
            label.setMaximumSize(new Dimension(Short.MAX_VALUE, 24));
            //final Border b = new MatteBorder(label.getBorder().getBorderInsets(label), Color.red);
            final Border activeBorder = label.getBorder();
            final Border inactiveBorder = GUI.makeSpace(activeBorder.getBorderInsets(label));
            label.setBorder(inactiveBorder);
            label.setBackground(null);
            //label.setOpaque(false);
            label.addFocusListener(new FocusAdapter() {
                    public void focusGained(FocusEvent e) {
                        label.setBackground(Color.white);
//                         label.setOpaque(true);
                        label.setBorder(activeBorder);
                    }
                    public void focusLost(FocusEvent e) {
                        label.setBackground(null);
//                         label.setOpaque(false);
                        label.setBorder(inactiveBorder);
                    }
                });
            }
            
            exclusive.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Row.this.setExclusive(exclusive.isSelected());
                    }});
            
            
            if (layer.supportsChildren()) {
                grab.setFont(VueConstants.SmallFont);
                grab.putClientProperty("JButton.buttonType", "textured");
                //grab.putClientProperty("JButton.sizeVariant", "tiny");
                grab.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            if (VUE.getSelection().size() > 0) {
                                layer.addChildren(VUE.getSelection());
                                VUE.getUndoManager().mark("Move To Layer " + Util.quote(layer.getLabel()));
                            }
                        }});
            }

            preview = null;
//             preview = new JPanel() {
//                     @Override
//                     public void paintComponent(Graphics g) {
//                         System.out.println("bounds: " + Util.fmt(getBounds()));
//                         System.out.println("  clip: " + Util.fmt(g.getClipRect()));
//                         g.setColor(Color.lightGray);
//                         ((Graphics2D)g).fill(g.getClipRect());
//                         //LWComponent focal = layer;
//                         LWComponent focal = layer.getMap();
//                         DrawContext dc = new DrawContext(g, focal);
//                         //focal.drawFit(dc, getBounds(), 0);
//                         //focal.drawFit(dc, new java.awt.geom.Rectangle2D.Float(0,0,getWidth()-2,getHeight()-2), 0);
//                         focal.drawFit(dc, g.getClipRect(), 0);
//                         //layer.drawFit(dc, g.getClipRect(), 0);
//                         //layer.drawFit(dc, getSize(), 0);
//                     }
//                 };

//             preview.setOpaque(true);
//             //preview.setSize(32,32);
//             //preview.setBorder(new LineBorder(Color.darkGray));

            if (false && DEBUG.Enabled)
                layer.addLWCListener(new LWComponent.Listener() {
                        public void LWCChanged(LWCEvent e) {
                            // this is heavy duty!  Would be nice if UserActionCompleted
                            // came through the layer, and we could listen for that,
                            // but it comes through the map
                            preview.repaint();
                        }});
            
            

            LWComponent.Listener l;
            layer.addLWCListener(l = new LWComponent.Listener() {
                    public void LWCChanged(LWCEvent e) {
                        label.setText(layer.getDisplayLabel());
                    }},
                LWKey.Label);
            l.LWCChanged(null); // do the initial set


            final JLabel info = new JLabel();
            if (layer.supportsChildren()) {
                layer.addLWCListener(l = new LWComponent.Listener() {
                        public void LWCChanged(LWCEvent e) {
                            String counts = "";
                            final int nChild = layer.numChildren();
                            final int allChildren = layer.getDescendentCount();
                                
                            if (nChild > 0)
                                counts += nChild;
                            if (allChildren != nChild)
                                counts += "/" + allChildren;
                            info.setText(counts);                        
                        }},
                    LWKey.ChildrenAdded, LWKey.ChildrenRemoved);
            }
            l.LWCChanged(null); // do the initial set

            activeIcon.setIcon(VueResources.getIcon(VUE.class, "images/hand_open.png"));
            activeIcon.setDisabledIcon(new GUI.EmptyIcon(activeIcon.getIcon()));
            activeIcon.setBorder(GUI.makeSpace(4,0,0,0));

            
            //final JComponent label = new VueTextField(layer.getLabel());
            // VueTextField impl not useful to us (also not used anywhere)
            // -- we need an impl that works just like VueTextPane, except
            // as a single line of text.
            
            //final JLabel info = new JLabel("(" + layer.numChildren() + " items)");
            
            
            add(exclusive);

            add(Box.createHorizontalStrut(5));
            add(visible);

            add(Box.createHorizontalStrut(4));
            add(label);
            
            add(Box.createHorizontalGlue());
            
            add(Box.createHorizontalStrut(2));
            add(info);
            
            add(Box.createHorizontalStrut(5));
            add(activeIcon);
            
            if (preview != null) {
                add(Box.createHorizontalStrut(7));
                add(preview);
            }
            
            add(Box.createHorizontalStrut(5));
            add(locked);

            add(Box.createHorizontalStrut(5));
            if (layer.supportsChildren())
                add(grab);
            
            
        }


        private void setExclusive(final boolean excluding) {

            Row exclusiveRow = getExclusiveRow();

            //Log.debug("SET-EXCLUSIVE " + this + " = " + excluding + "; nowExclusive=" + exclusiveRow);

            if (excluding && exclusiveRow == this)
                return;

            exclusiveRow = this;

            if (excluding) {
                setPreExclusiveLayer(getActiveLayer());
                setExclusiveRow(this);
                if (layer instanceof Layer)
                    setActiveLayer((Layer) layer);
            } else if (exclusiveRow == this) {
                setExclusiveRow(null);
                final Layer activePreExclusive = getPreExclusiveLayer();
                if (activePreExclusive != layer)
                    setActiveLayer(activePreExclusive);
                setPreExclusiveLayer(null);
            }

            exclusive.setSelected(excluding);
            if (true) {
                visible.setEnabled(!excluding);
                locked.setEnabled(!excluding);
            } else {
                visible.setVisible(!excluding);
                locked.setVisible(!excluding);
            }
            label.setEnabled(true);
                        
            layer.clearHidden(HideCause.LAYER_EXCLUSIVE);

            if (excluding) {
                //if (DEBUG.Enabled) Log.debug("EXCLUSIVE: " + this);
                if (layer.isHidden(HideCause.DEFAULT)) {
                    wasHiddenWhenMadeExclusive = true;
                    layer.clearHidden(HideCause.DEFAULT);
                }
                if (layer.isLocked()) {
                    wasLockedWhenMadeExclusive = true;
                    layer.setLocked(false);
                }
            } else {
                //if (DEBUG.Enabled) Log.debug("RELEASING: " + this);
                if (wasHiddenWhenMadeExclusive)
                    layer.setHidden(HideCause.DEFAULT);
                if (wasLockedWhenMadeExclusive)
                    layer.setLocked(true);
                
            }
        

            for (Row row : mRows) {
                if (row != exclusiveRow) {
                    if (excluding) row.exclusive.setSelected(false);
                    
                    //row.setEnabled(!excluding);
                    
                    row.visible.setEnabled(!excluding);
                    row.locked.setEnabled(!excluding);
                    //row.visible.setVisible(!excluding);
                    //row.locked.setVisible(!excluding);
                
                    row.label.setEnabled(excluding ? false : row.visible.isSelected());
                    row.layer.setHidden(HideCause.LAYER_EXCLUSIVE, excluding);
                }
            }

            indicateActiveLayers(null);
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
            if (bg == null)
                super.setBackground(defaultBackground);
            else
                super.setBackground(bg);
        }
        

        private int dragStartX;
        private int dragStartY;
        private int dragStartMouseY;
        private int dragRowIndex;
        private int dragLastY;
        private boolean didReorder;
        private Color saveColor;


        public void mouseEntered(MouseEvent e) {}
        public void mouseExited(MouseEvent e) {}
        
        public void mousePressed(MouseEvent e) {

//             if (GUI.isDoubleClick(e)) {
//                 if (label.contains(e.getPoint()))
//                     Log.debug("HIT WITH " + e.getPoint());
//                 label.setFocusable(true);
//                 label.requestFocus();
//                 return;
//             }
            
            if (layer instanceof Layer) {
                if (inExclusiveMode())
                    setExclusive(true);
                else if (!AUTO_ADJUST_ACTIVE_LAYER || layer.isVisible()) 
                    setActiveLayer((Layer) layer, UPDATE);
                if (VUE.getSelection().isEmpty() || VUE.getSelection().only() instanceof Layer)
                    VUE.getSelection().setTo(layer);
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
            layoutRows(mRows);
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
        
        public void mouseClicked(MouseEvent e) {}
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
