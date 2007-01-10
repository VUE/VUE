/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue.ui;

import tufts.Util;
import tufts.vue.VUE;
import tufts.vue.LWPathway;
import tufts.vue.LWComponent;
import tufts.vue.LWMap;
import tufts.vue.LWSelection;
import tufts.vue.LWSlide;
import tufts.vue.MapViewer;
import tufts.vue.DrawContext;
import tufts.vue.VueTool;
import tufts.vue.VueToolbarController;
import tufts.vue.VueConstants;
import tufts.vue.LWCEvent;
import tufts.vue.VueAction;
import tufts.vue.PickContext;
import tufts.vue.gui.GUI;
import tufts.vue.DEBUG;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// TWO CASES TO HANDLE:

// 1 - focal is slide: can we get MapViewer to handle displaying just an LWContainer?  Undo will still need to point
// to the original map...  Note that LWSlide's behind each node aren't really in the hierarchy (maybe put them there?)
// Reminder: the problem with trying to have the children in the slide be the *same object* somehow as the children
// on the main map is that all the info needs to be different EXCEPT the meta-data (x,y,style,etc).
// We could do that only if there was a "raw" node of just meta-data, including resource and it's meta-data,
// and then multiple styled renderings of that "raw node".  In practice, the only data usually in this raw node
// would be a single string, for the label, and a resource.  (Tho may have notes, user assigned meta-data, etc).
// For SYNCING, will want to sync all meta-data (maybe both nodes reference the same meta-data objcect?)

// 2 - focal is existing node in map: need MapViewer to both only DRAW this node, but prevent
// selection of nearby now-"invisible" objects (in the slideviewer).  Maybe disallow all clicks
// out side the node?

// TODO: switch off features one by one fore SlideViewer/NodeViewer, and once done that,
// can use the switch marks to manage the change to a simpler superclass / fancier
// full map viewing subclass if you like...

// SLIDES STILL NEED TO BE OWNED BY MAP (for getnextuniqeID, persistance, UNDO),
// but we can't just have them marked hidden, or they won't draw in their
// own context...  perhaps put them in a different layer?    If we
// had them in a different layer, if we wanted, that could be the "displayed" map:
// the map with the slides scattered about, tho don't know if that would really be useful...

// Okay, so they can be owned by the map, but could just have bit for now that says "invisible",
// if we can handle the direct viewing, drawing & selecting of a container with clever traversals.

public class SlideViewer extends tufts.vue.MapViewer implements VUE.ActivePathwayListener
{
    private final VueTool PresentationTool = VueToolbarController.getController().getTool("viewTool");

    private boolean isMapSlide = false;
    private boolean mBlackout = false;
    
    private boolean mZoomBorder;
    private boolean inFocal;
    private LWComponent mZoomContent; // what we zoom-to
    private boolean inPathwaySlide;
    private LWComponent mLastLoad;

    private final AbstractButton btnLocked;
    private final AbstractButton btnZoom;
    private final AbstractButton btnFocus;
    private final AbstractButton btnSlide;
    private final AbstractButton btnMaster;
    private final AbstractButton btnMapSlide;
    //private final AbstractButton btnPresent;

    private class Toolbar extends JPanel implements ActionListener {
        Toolbar() {
            //btnLocked.setFont(VueConstants.FONT_SMALL);
            add(btnLocked);
            add(Box.createHorizontalGlue());
            add(btnZoom);
            //add(btnFocus);
            add(btnSlide);
            add(btnMaster);
            add(btnMapSlide);
            //add(btnPresent);

            ButtonGroup exclusive = new ButtonGroup();
            exclusive.add(btnZoom);
            //exclusive.add(btnFocus);
            exclusive.add(btnSlide);
            exclusive.add(btnMaster);
            //exclusive.add(btnPresent);
        }

        private void add(AbstractButton b) {
            b.addActionListener(this);
            super.add(b);
        }

        public void actionPerformed(ActionEvent e) {
            if (DEBUG.PRESENT) out(e);
            if (e.getSource() == btnMapSlide) {
                mLastLoad.setSlideIsNodeForPathway(mLastLoad.getMap().getActivePathway(),
                                                   btnMapSlide.isSelected());
            }
            reload();
        }

    }
        
    public SlideViewer(LWMap map) {
        super(null, "Viewer");
        //super(map == null ? new LWMap("empty") : map, "SlideViewer");

        btnLocked = new JCheckBox("Lock");
        //btnLocked = makeButton("Lock");
        btnZoom = makeButton("Zoom");
        btnFocus = makeButton("Focus");
        btnSlide = makeButton("Slide");
        btnMaster = makeButton("Master Slide");
        btnMapSlide = new JCheckBox("Map Slide");
        //btnPresent = makeButton("Present");

        btnSlide.setSelected(true);

        DEBUG_TIMER_ROLLOVER = false;
        VUE.addActivePathwayListener(this);
    }

    protected AbstractButton makeButton(String name) {
        AbstractButton b = new JToggleButton(name);
        b.setFocusable(false);
        //b.setBorderPainted(false);
        return b;
    }

    protected String getEmptyMessage() {
        if (mLastLoad != null && btnSlide.isSelected())
            return "Not on Pathway";
        else
            return "Empty";
    }
    
    public void addNotify() {
        super.addNotify();
        getParent().add(new Toolbar(), BorderLayout.NORTH);
    }

    public void XfocusGained(FocusEvent e) {
        out("focusGained ignored");
    }
    public void XfocusLost(FocusEvent e) {
        out("focusLost ignored");
    }

    public void grabVueApplicationFocus(String from, ComponentEvent event) {
        final int id = event == null ? 0 : event.getID();

        if (id == MouseEvent.MOUSE_ENTERED)
            out("GVAF " + from + " ignored");
        else
            super.grabVueApplicationFocus(from, event);
    }
    

    public void LWCChanged(LWCEvent e) {
        if (DEBUG.Enabled) out("SLIDEVIEWER LWCChanged " + e);

        if (e.getComponent() instanceof LWPathway) {
            // If we're displaying a slide for a node, and
            // the pathway has changed, it may be that the
            // node was just added to the pathway and we
            // need to load it's new slide (or it was
            // removed, and we also need to display that)
            reload();
        } else {
            super.LWCChanged(e);
            if (true||e.getComponent() == mFocal) {
                zoomToContents();
            }
        }
    }

    /*
    public LWComponent pickDropTarget(float mapX, float mapY, Object dropping) {
        PickContext pc = getPickContext();
        if (dropping == null)
            pc.dropping = POSSIBLE_RESOURCE; // most lenient targeting if unknown
        else
            pc.dropping = dropping;
        return LWTraversal.PointPick.pick(pc, mapX, mapY);
    }
    */
    

    // no longer relevant: maxLayer hack currently not in use
    //protected int getMaxLayer() { return inPathwaySlide ? 1 : 0; }

    protected PickContext getPickContext() {
        PickContext pc = super.getPickContext();

        // This makes sure we can't select the background,
        // and we can initiate the drag selector box on
        // children.
        
        if (inFocal && !btnMaster.isSelected())
            pc.excluded = mFocal;

        // So we automatically pick inside groups
        // (same as auto-turning on the direct selection tool)
        pc.pickDepth = 1;

        return pc;
    }

    private LWPathway mCurrentPath;
    public void activePathwayChanged(LWPathway path) {
        if (mCurrentPath != null)
            mCurrentPath.removeLWCListener(this);
        mCurrentPath = path;
        mCurrentPath.addLWCListener(this);
        reload();
    }

    private void reload() {
        // TODO: load nothing if active pathway from a different map
        //if (mLastLoad != null)
        load(mLastLoad);
    }
        
    public void selectionChanged(LWSelection s) {
        super.selectionChanged(s);

        if (btnLocked.isSelected())
            return;
            
        if (s.getSource() != this && s.size() == 1) {
            final LWComponent c = s.first();
            if (btnMaster.isSelected())
                mLastLoad = c;
            else if (btnSlide.isSelected()) {
                if (true || c.getSlideForPathway(c.getMap().getActivePathway()) != null)
                    load(c);
                else
                    ; // do nothing for now: allows us to select non-slideworthy on map to drag into slide
            } else
                load(c);
        }
    }

    protected void XsetDragger(LWComponent c) {
        // need to cleanup MapViewer such that overriding this actually works
        if (btnMaster.isSelected() && c instanceof LWSlide)
            return;
        else {
            out("setting dragger to: " + c);
            super.setDragger(c);
        }
    }

    protected void load(LWComponent c)
    {
        if (DEBUG.Enabled) out("\nSlideViewer: loading " + c);
        mLastLoad = c;
        //btnSlide.setEnabled(true);

        LWComponent focal;


        // If no slide available, disable slide button, even if don't want it!

        if (c == null && !btnMaster.isSelected()) {
            mZoomBorder = false;
            mZoomContent = null;
            inFocal = false;
            focal = null;
            btnMapSlide.setEnabled(false);
        } else {
            if (btnMaster.isSelected()) {
                isMapSlide = false;
            } else {
                btnMapSlide.setSelected(c.getSlideIsNodeForPathway(c.getMap().getActivePathway()));
                isMapSlide = btnFocus.isSelected() || btnMapSlide.isSelected();
            }
            focal = getFocalAndConfigure(c);
        }
        
        super.loadFocal(focal);
        reshapeImpl(0,0,0,0);
        if (DEBUG.Enabled) out("SlideViewer: focused is now " + mFocal + " from map " + mMap);
    }

    private LWSlide getActiveMasterSlide() {
        if (VUE.getActiveMap() != null)
            return VUE.getActiveMap().getActivePathway().getMasterSlide();
        else
            return null;
    }


    /**
     * Given the selected on-map node, and the current viewer config, return the proper focal:
     * Either the node itself, or it's slide for the current pathway, or the master slide for
     * the current pathway (if the master slide button is selected).
     */
    private LWComponent getFocalAndConfigure(final LWComponent mapNode)
    {
        final LWComponent focal;

        inPathwaySlide = false;
            
        if (btnZoom.isSelected()) {
            inFocal = false;
            mZoomContent = mapNode;
            mZoomBorder = true;
            focal = mapNode.getMap();
            btnMapSlide.setEnabled(false);
        } else if (isMapSlide) {
            inFocal = true;
            mZoomBorder = false;
            mZoomContent = mapNode;
            focal = mapNode;
            btnMapSlide.setEnabled(true);
        } else if (btnSlide.isSelected()) {

            focal = mapNode.getFocalForPathway(mapNode.getMap().getActivePathway());
            //focal = mapNode.getSlideForPathway(mapNode.getMap().getActivePathway());

            final boolean focalIsPathwaySlide = (mapNode != focal);
            
            
            // todo: if only on ONE pathway, and thus could only have
            // one slide, could still allow the slide selection
            // even if not current active pathway
            
            mZoomBorder = false;
            
            if (focal != null) {
                if (focalIsPathwaySlide)
                    inPathwaySlide = true;
                else
                    inPathwaySlide = false;
                mZoomContent = focal;
                btnMapSlide.setEnabled(true);
            } else {
                mZoomContent = null;
                btnMapSlide.setEnabled(false);
            }

            
        } else if (btnMaster.isSelected()) {
            inFocal = true;
            inPathwaySlide = true;
            mZoomBorder = false;
            mZoomContent = VUE.getActiveMap().getActivePathway().getMasterSlide();
            focal = mZoomContent;
            btnMapSlide.setEnabled(false);
        } else {
            focal = null;
            throw new InternalError();
        }
        
        return focal;
    }

    public void fireViewerEvent(int id) {
        if (DEBUG.Enabled) out("fireViewerEvent <" + id + "> skipped");
    }

    protected void reshapeImpl(int x, int y, int w, int h) {
        out("reshapeImpl");
        zoomToContents();
    }
    
    protected void zoomToContents() {
        if (mZoomContent == null)
            return;

        final java.awt.geom.Rectangle2D zoomBounds;

        if (inFocal) {
            // don't include any bounds due to current
            // state decorations, such as being no a pathway
            zoomBounds = mZoomContent.getShapeBounds();
            //out("zoomToContents: shapeBounds=" + zoomBounds);
        } else {
            zoomBounds = mZoomContent.getBounds();
            //out("zoomToContents: bounds=" + zoomBounds);
        }


        tufts.vue.ZoomTool.setZoomFitRegion(this,
                                            zoomBounds,
                                            20,
                                            //mZoomBorder ? 20 : 0,
                                            false);
    }
    
    
    protected void drawSelection(DrawContext dc, LWSelection s) {
        // Don't draw selection if its the focused component
        if (s.size() == 1 && s.first() == mFocal)
            return;
        super.drawSelection(dc, s);
    }

    /*
    protected DrawContext getDrawContext(Graphics2D g) {
        DrawContext dc = super.getDrawContext(g);
        //if (inFocal)
        if (btnFocus.isSelected()) {
            dc.isFocused = true;
            dc.setInteractive(false);
        }
            dc.isFocused = true;
        return dc;
    }
    */

    protected void drawMap(DrawContext dc) {

        if (mFocal == null)
            return;

        if (inPathwaySlide) {

            drawSlide(dc);
            
        } else {

            drawFocal(dc);
        }

    }

    protected void drawSlide(DrawContext dc) {
        
        final LWSlide master = VUE.getActiveMap().getActivePathway().getMasterSlide();

        if (btnMaster.isSelected()) {
            // When editing the master, allow us to see stuff outside of it
            // (no need to clip);
            master.setLocation(0,0);// TODO: hack till we can lock these properties
            master.draw(dc);
            return;
        }
        
        final Shape curClip = dc.g.getClip();

        // When just filling the background with the master, only draw
        // what's in the containment box
        master.setLocation(0,0);// TODO: hack till we can lock these properties
        dc.g.setClip(master.getBounds());
        master.draw(dc);
        dc.g.setClip(curClip);

        //for (LWComponent c : mFocal.getChildList()) out("child to draw: " + c);
        
        // Now draw the actual slide
        mFocal.draw(dc);
    }
    
    /** either draws the entire map (previously zoomed to to something to focus on),
     * or a fully focused part of of it, where we only draw that item.
     */

    protected void drawFocal(DrawContext dc)
    {
        //out("drawing focal " + mFocal);

        if (isMapSlide) {
            dc.isFocused = true;
            dc.setInteractive(false);
        }
        
        dc.g.setColor(mFocal.getMap().getFillColor());
        dc.g.fill(dc.g.getClipBounds());
        
        final LWMap underlyingMap = mFocal.getMap();
        
        if (mFocal.isTranslucent() && mFocal != underlyingMap) {

            //out("drawing underlying map " + underlyingMap);

            // If our fill is in any way translucent, the underlying
            // map can show thru, thus we have to draw the whole map
            // to see the real result -- we just set the clip to
            // the shape of the focal.
            
            final Shape curClip = dc.g.getClip();
            dc.g.setClip(mFocal.getShape());
            underlyingMap.draw(dc);
            dc.g.setClip(curClip);
            
        } else {
            mFocal.draw(dc);
        }
    }
    
    
    

    /*
      // not relevant: we'lre not in a scroll pane
    protected void panScrollRegionImpl(int dx, int dy, boolean allowGrowth) {
        if (inFocal)
            return;
        else
            super.panScrollRegionImpl(dx, dy, allowGrowth);
    }
    */
    
    /*
    public void keyPressed(java.awt.event.KeyEvent e) {
        super.keyPressed(e);
        if (e.isConsumed())
            return;
        char c = e.getKeyChar();
        if (c == 'b') {
            mBlackout = !mBlackout;
            repaint();
        }
    }
    */

    protected void XsetMapOriginOffsetImpl(float panelX, float panelY, boolean update) {
        if (inFocal) {
            super.setMapOriginOffsetImpl(mFocal.getX(), mFocal.getY(), update);
        } else {
            super.setMapOriginOffsetImpl(panelX, panelY, update);
        }
    }

    protected float XscreenToMapX(float x) {
        float nx = super.screenToMapX(x);
        out("SCREEN-X " + x + " MAP-X " + nx);
        return nx;
    }

    public float XgetOriginX() {
        if (inFocal)
            return mFocal.getX();
        else
            return super.getOriginX();
    }
    public float XgetOriginY() {
        if (inFocal)
            return mFocal.getY();
        else
            return super.getOriginY();
    }
    
        
    /*
    protected void drawMap(DrawContext dc) {
        if (mBlackout) {
            dc.g.setColor(Color.black);
            dc.setBlackWhiteReversed(true);
            dc.setPresenting(true);
            dc.setInteractive(false);
        } else
            dc.g.setColor(mMap.getFillColor());
        dc.g.fill(dc.g.getClipBounds());
        if (mFocused == null)
            mMap.draw(dc);
        else
            mFocused.draw(dc);

        dc.setRawDrawing();
        //dc.g.setFont(VueConstants.FONT_MEDIUM);
        dc.g.setFont(new java.awt.Font("Apple Chancery", java.awt.Font.PLAIN, 14));
        //dc.g.setColor(Color.blue);
        dc.g.setColor(new Color(89,130,203));

        int x = -getX() + 2;
        int y = -getY() + 15;
        dc.g.drawString("Header", x, y);
        dc.g.drawString("Footer", x, -getY() + getHeight() - 3);
    }
    */


    //    public void reshape(int x, int y, int w, int h) {
    //        out("     reshape: "+ w + " x " + h+ " "+ x + "," + y);
    //    }
    
    /*
    private VueAction[] mMenuActions;
    public void addNotify() {
        super.addNotify();
        mMenuActions = new VueAction[] {
            tufts.vue.Actions.ZoomFit,
            tufts.vue.Actions.ZoomActual,
            new VueAction("1/8 Screen") {
                public void act() {
                    java.awt.GraphicsConfiguration gc = GUI.getDeviceConfigForWindow(SlideViewer.this);
                    Rectangle screen = gc.getBounds();
                    slideDock.setSize(screen.width / 4, screen.height / 4);
                    //GUI.refreshGraphicsInfo();
                    //slideDock.setSize(GUI.GScreenWidth / 4, GUI.GScreenHeight / 4);
                }
            },
            new VueAction("1/4 Screen") {
                public void act() {
                    java.awt.GraphicsConfiguration gc = GUI.getDeviceConfigForWindow(SlideViewer.this);
                    Rectangle screen = gc.getBounds();
                    slideDock.setSize(screen.width / 2, screen.height / 2);
                    //GUI.refreshGraphicsInfo();
                    //slideDock.setSize(GUI.GScreenWidth / 2, GUI.GScreenHeight / 2);
                }
            },
            new VueAction("Maximize") {
                public void act() {
                    slideDock.setBounds(GUI.getMaximumWindowBounds(slideDock));
                }
            }
        };
        Widget.setMenuActions(this, mMenuActions);
    }
    */

    /*
    private static class SingletonMap extends LWMap {
        private final LWMap srcMap;
        SingletonMap(LWMap srcMap, LWComponent singleChild) {
            super("SingletonMap");
            this.srcMap = srcMap;
            super.children = java.util.Collections.singletonList(singleChild);
        }

        public tufts.vue.LWPathwayList getPathwayList() {
            return srcMap == null ? null : srcMap.getPathwayList();
        }
    }
    */
    
    

    
}
