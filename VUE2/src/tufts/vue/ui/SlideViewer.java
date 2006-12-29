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

    private class Toolbar extends JPanel implements ActionListener {
        Toolbar() {
            //btnLocked.setFont(VueConstants.FONT_SMALL);
            add(btnLocked);
            add(Box.createHorizontalGlue());
            add(btnZoom);
            add(btnFocus);
            add(btnSlide);
            add(btnMaster);

            ButtonGroup exclusive = new ButtonGroup();
            exclusive.add(btnZoom);
            exclusive.add(btnFocus);
            exclusive.add(btnSlide);
            exclusive.add(btnMaster);
        }

        private void add(AbstractButton b) {
            b.addActionListener(this);
            super.add(b);
        }

        public void actionPerformed(ActionEvent e) {
            //out(e);
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
        super.LWCChanged(e);
        if (true||e.getComponent() == mFocal) {
            zoomToContents();
        }
    }

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
        pc.pickDepth = 1;

        return pc;
    }

    public void activePathwayChanged(LWPathway p) {
        if (inPathwaySlide) {
            reload();
        }
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
                if (c.getSlideForPathway(c.getMap().getActivePathway()) != null)
                    load(c);
                else
                    ; // do nothing for now: allows us to select non-slideworthy on map to drag into slide
            } else
                load(c);
        }
    }

    protected void load(LWComponent c)
    {
        mLastLoad = c;
        //btnSlide.setEnabled(true);

        LWComponent focal;

        // If no slide available, disable slide button, even if don't want it!

        if (c == null) {
            mZoomBorder = false;
            mZoomContent = null;
            inFocal = false;
            focal = null;
        } else {
            focal = getFocalAndConfigure(c);
        }
        
        super.loadFocal(focal);
        reshapeImpl(0,0,0,0);
        out("\nSlideViewer: focused is now " + mFocal + " from map " + mMap);
    }

    private LWSlide getActiveMasterSlide() {
        if (VUE.getActiveMap() != null)
            return VUE.getActiveMap().getActivePathway().getMasterSlide();
        else
            return null;
    }
    

    private LWComponent getFocalAndConfigure(LWComponent c)
    {
        final LWComponent focal;
        
        inPathwaySlide = false;
            
        if (btnZoom.isSelected()) {
            inFocal = false;
            mZoomContent = c;
            mZoomBorder = true;
            focal = c.getMap();
        } else if (btnFocus.isSelected()) {
            inFocal = true;
            mZoomBorder = false;
            mZoomContent = c;
            focal = c;
        } else if (btnSlide.isSelected()) {

            focal = c.getSlideForPathway(c.getMap().getActivePathway());
            // todo: if only on ONE pathway, and thus could only have
            // one slide, could still allow the slide selection
            // even if not current active pathway
            
            mZoomBorder = false;
            
            if (focal != null) {
                inPathwaySlide = true;
                btnSlide.setEnabled(true);
                mZoomContent = focal;
            } else {
                mZoomContent = null;
            }
            
        } else if (btnMaster.isSelected()) {
            inFocal = true;
            inPathwaySlide = true;
            mZoomBorder = false;
            mZoomContent = VUE.getActiveMap().getActivePathway().getMasterSlide();
            focal = mZoomContent;
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
            master.draw(dc);
            return;
        }
        
        final Shape curClip = dc.g.getClip();

        // When just filling the background with the master, only draw
        // what's in the containment box
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

        if (btnFocus.isSelected()) {
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
