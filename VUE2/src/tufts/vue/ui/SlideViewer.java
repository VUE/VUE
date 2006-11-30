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

import java.awt.Color;
import java.awt.event.*;

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

public class SlideViewer extends tufts.vue.MapViewer
{
    private static SlideViewer singleton;

    private final VueTool PresentationTool = VueToolbarController.getController().getTool("viewTool");

    private boolean mBlackout = false;
    private LWComponent mZoomTo;
    private boolean inFocal = false;
    private boolean inSlideLayer;
        
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
    
    public SlideViewer(LWMap map) {
        super(map == null ? new LWMap("empty") : map, "SlideViewer");
        singleton = this;
        DEBUG_TIMER_ROLLOVER = false;
    }

    public static SlideViewer getInstance() {
        return singleton;
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
    
    public void XselectionChanged(LWSelection s) {
        out("selectionChanged ignored");
    }

    /*
    public void LWCChanged(LWCEvent e) {
        out("SLIDEVIEWER LWCChanged " + e);
        super.LWCChanged(e);
        //if (e.getComponent() == mFocused)
            zoomToContents();
    }
    */

    protected int getMaxLayer() {
        return inSlideLayer ? 1 : 0;
    }

    protected PickContext getPickContext() {
        PickContext pc = super.getPickContext();

        // This makes sure we can't select the background,
        // and we can initiate the drag selector box on
        // children.
        
        if (inFocal)
            pc.excluded = mFocal;

        // So we automatically pick inside groups
        pc.pickDepth = 1;

        return pc;
    }
    
    public void loadFocal(LWComponent c) {

        inSlideLayer = false;
        
        LWComponent slide;
        if (c instanceof LWSlide)
            slide = (LWSlide) c;
        else {
            slide = c.getSlide();
            if (slide != null)
                inSlideLayer = true;
        }
        
        if (slide != null) {
            super.loadFocal(slide);
            inFocal = true;
            mZoomTo = null;
        } else if (c instanceof tufts.vue.LWImage) {
            super.loadFocal(c);
            inFocal = true;
            mZoomTo = null;
        } else {
            super.loadFocal(c.getMap());
            inFocal = false;
            mZoomTo = c;
        }

        reshapeImpl(0,0,0,0);
        out("\nSlideViewer: focused is now " + mFocal + " from map " + mMap);
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
    
        
    protected void reshapeImpl(int x, int y, int w, int h) {
        zoomToContents();
    }
    
    protected void zoomToContents() {
        if (mZoomTo != null) {
            tufts.vue.ZoomTool.setZoomFitRegion(this,
                                                mZoomTo.getBounds(),
                                                DEBUG.MARGINS ? 0 : 20,
                                                false);
        } else if (mFocal != mMap) {
            tufts.vue.ZoomTool.setZoomFitRegion(this,
                                                mFocal.getBounds(),
                                                0,
                                                false);
        }
        /*
        LWComponent focus = (mFocused == null ? mMap : mFocused);
            //if (mFocused != null)
        tufts.vue.ZoomTool.setZoomFitRegion(this,
                                            focus.getBounds(),
                                            DEBUG.MARGINS ? 0 : 20,
                                            false);
        */

        //tufts.vue.ZoomTool.setZoomFit(this);
    }
    
    /*
    public void setFocused(LWComponent c) {

        out("setFocused " + c);
        
        LWComponent slide = c.getSlide();
        if (slide != null) {
            mSelected = slide;
        } else {
            mSelected = c;
        }
        
        if (mSelected == c) // we've already loaded this up
            return;


//         LWComponent toFocus = c.getSlide();
//         LWMap owningMap;
//         // TODO: NO SINGLETON MAP: ANOTHER MAP LAYEr
//         if (toFocus == null) {
//             toFocus = mSelected;
//             owningMap = mSelected.getMap();
//         } else {
//             owningMap = new SingletonMap(mSelected.getMap(), toFocus);
//             toFocus = null; // nothing within the map to focus on: just use the singletonMap
//         }
//         mFocused = toFocus;


        LWComponent toFocus = mSelected;
        LWMap owningMap = mSelected.getMap();

        mFocused = toFocus;
        loadFocal(owningMap);

        //singleton.loadMap(c.getMap()); // CAN ONLY DO THIS IF DOING ZOOM-TO on current map, not for a slide, which needs a virtual map
        //singleton.mFocused = c.getSlide();
        
        reshapeImpl(0,0,0,0);
        out("\nSlideViewer: focused is now " + toFocus + " from map " + c.getMap());
        //if (DEBUG.Enabled) tufts.Util.printStackTrace("SLIDE-VIEWER-SET-FOCUSED");
    }
*/

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

    protected void drawSelection(DrawContext dc, LWSelection s) {
        // Don't draw selection if its the focused component
        if (s.size() == 1 && s.first() == mFocal)
            return;
        super.drawSelection(dc, s);
    }

    /*
    protected PickContext getPickContext(float x, float y) {
        return new PickContext(mFocal, x, y, 1);
    }

    */
    

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

    
}
