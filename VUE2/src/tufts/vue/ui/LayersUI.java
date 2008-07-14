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
import tufts.vue.LWMap;
import tufts.vue.LWCEvent;
import tufts.vue.LWKey;
import tufts.vue.ActiveInstance;
import tufts.vue.ActiveEvent;
import tufts.vue.VueConstants;
import tufts.vue.DrawContext;
import tufts.vue.VueResources;
import tufts.vue.gui.*;

import tufts.Util;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;


/**
 * @version $Revision: 1.2 $ / $Date: 2008-07-14 18:35:47 $ / $Author: sfraize $
 * @author Scott Fraize
 */
public class LayersUI extends tufts.vue.gui.Widget implements LWComponent.Listener
{
    private final java.util.List<Row> mRows = new java.util.ArrayList();
    private LWMap mMap;
    private boolean isDragUnderway;
    private Row mExclusiveRow;
    private Row mDragRow;
    
    public LayersUI() {
        super("layers");
        setName("layersUI");
        //setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        //setLayout(new GridLayout(0,1));
        setLayout(new GridBagLayout());
        VUE.addActiveListener(LWMap.class, this);
        VUE.addActiveListener(LWMap.Layer.class, this);
        VUE.addActiveListener(LWComponent.class, this);

        //setFocusable(false);

//         addKeyListener(new KeyAdapter() {
//                 public void keyPressed(KeyEvent e) {
//                     // TODO: CLEANUP
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
    }

    public void activeChanged(ActiveEvent e, LWMap map) {
        loadMap(map);
    }

    public void activeChanged(ActiveEvent e, LWMap.Layer layer) {
        indicateActiveLayer(layer);
    }

    public void activeChanged(ActiveEvent e, LWComponent c) {
        enableForSingleSelection(c);

        // for debug / child-list mode:
        if (mMap != null && !mMap.isLayered())
            indicateActiveLayer(null);
    }

    
    private void loadMap(final LWMap map)
    {
        if (mMap == map)
            return;

        if (mMap != null)
            mMap.removeLWCListener(this);
        
        mMap = map;

        loadLayers(map);

        // TODO: we should be able to just listen for LWKey.HierarchyChanged, tho
        // this currently is only generated on UNDO's, and hardly anything is
        // currently listenting for it (OutlineViewTree, and some references to "hier.*)
        map.addLWCListener(this);
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
            enableForSingleSelection(VUE.getActiveComponent());
            indicateActiveLayer(map.getActiveLayer());
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

        repaint();
    }

    private void indicateActiveLayer(LWComponent nowActive) {

        Color select = VueConstants.COLOR_SELECTION;

        if (nowActive == null) {
            // this for debug/child-list mode
            nowActive = VUE.getActiveComponent();
            select = select.brighter();
        }
            

        for (Row row : mRows) {
            if (row.layer == nowActive)
                row.setBackground(select);
            else
                row.setBackground(null);
        }
        
    }
        
    private void enableForSingleSelection(LWComponent c) {
        if (c == null || c instanceof LWMap.Layer)
            setGrabsEnabled(c, false);
        else
            setGrabsEnabled(c, c.atTopLevel());
    }
    

    private void setGrabsEnabled(LWComponent c, boolean enabled) {
        if (true) return;// leave all on for now
        for (Row r : mRows) {
            if (enabled && c.getParent() != r.layer && c != r.layer)
                r.grab.setEnabled(enabled);
            else
                r.grab.setEnabled(false);
        }
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
    
    private class Row extends JPanel implements javax.swing.event.MouseInputListener, Runnable {

        final AbstractButton exclusive = new JRadioButton();
        final AbstractButton visible = new JCheckBox();
        final AbstractButton locked = new JRadioButton();
        final JLabel label = new JLabel();
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
            
//             addMouseListener(new tufts.vue.MouseAdapter() {
//                     public void mousePressed(MouseEvent e) {
//                         if (layer instanceof LWMap.Layer) {
//                             ActiveInstance.set(LWMap.Layer.class, Row.this, layer);
//                         } else {
//                             // this case for debug/test only: we shouldn't normally
//                             // see regular objects a the top level of the map anymore
//                             VUE.getSelection().setTo(layer);
//                         }
//                         LayersUI.this.requestFocus();
//                     }
//                 });

            addMouseListener(this);
            addMouseMotionListener(this);
            
            if (layer instanceof LWMap.Layer)
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
                        //grab.setEnabled(!layer.isLocked()); // TODO: need to disable selection changes from re-enabling
                    }});

            visible.setSelected(layer.isVisible());
            visible.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        layer.setVisible(visible.isSelected());
                        locked.setEnabled(layer.isVisible());
                        label.setEnabled(layer.isVisible());
                            
                    }});

            label.setEnabled(layer.isVisible());
            
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

            if (excluding && LayersUI.this.mExclusiveRow == this)
                return;

            if (excluding) {
                LayersUI.this.mExclusiveRow = this;
                ActiveInstance.set(LWMap.Layer.class, this, layer);
            } else if (LayersUI.this.mExclusiveRow == this)
                LayersUI.this.mExclusiveRow = null;

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
                System.out.println("EXCLUSIVE: " + this);
                if (layer.isHidden(HideCause.DEFAULT)) {
                    wasHiddenWhenMadeExclusive = true;
                    layer.clearHidden(HideCause.DEFAULT);
                }
                if (layer.isLocked()) {
                    wasLockedWhenMadeExclusive = true;
                    layer.setLocked(false);
                }
            } else {
                System.out.println("RELEASING: " + this);
                if (wasHiddenWhenMadeExclusive)
                    layer.setHidden(HideCause.DEFAULT);
                if (wasLockedWhenMadeExclusive)
                    layer.setLocked(true);
                
            }
        

            for (Row row : mRows) {
                if (row != mExclusiveRow) {
                    if (excluding) row.exclusive.setSelected(false);
                    
                    //row.setEnabled(!excluding);
                    
                    row.visible.setEnabled(!excluding);
                    row.locked.setEnabled(!excluding);
                    //row.visible.setVisible(!excluding);
                    //row.locked.setVisible(!excluding);
                
                    row.label.setEnabled(!excluding);
                    row.layer.setHidden(HideCause.LAYER_EXCLUSIVE, excluding);
                }
            }
        }
        

        @Override
        protected void addImpl(Component comp, Object constraints, int index) {
            comp.setFocusable(false);
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
            if (layer instanceof LWMap.Layer) {
                if (mExclusiveRow != null)
                    setExclusive(true);
                else if (layer.isVisible()) 
                    ActiveInstance.set(LWMap.Layer.class, Row.this, layer);
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
                return (int) 0.8f * getHeight();
            } else
                return getHeight();
        }

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
                    //System.out.println("BUMP DOWN " + above);
                    //above.setLocation(above.getX(), above.getY() + getDropRegionSize());
                    //Collections.swap(mRows, curIndex, curIndex - 1);
                    moved = layer.getParent().bringForward(layer); // will trigger reload/relayout of all rows
                }
                
            } else if (mouse.y > dragLastY && curIndex < mRows.size() - 1) {

                final Row below = mRows.get(curIndex + 1);
                final int bottomEdge = newY + getHeight();
                
                if (bottomEdge > below.getY() + below.getHeight() / 2) {
                    //System.out.println("BUMP UP " + below);
                    //below.setLocation(below.getX(), below.getY() - getDropRegionSize());
                    //Collections.swap(mRows, curIndex, curIndex + 1);
                    moved = layer.getParent().sendBackward(layer); // will trigger reload/relayout of all rows
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
            return getName();
        }
        
    }
    


}
