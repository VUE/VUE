/*
* Copyright 2003-2010 Tufts University  Licensed under the
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import tufts.vue.DEBUG;
import tufts.vue.DrawContext;
import tufts.vue.LWCEvent;
import tufts.vue.LWComponent;
import tufts.vue.LWMap;
import tufts.vue.LWPathway;
import tufts.vue.LWSelection;
import tufts.vue.LWSlide;
import tufts.vue.VUE;
import tufts.vue.VueResources;
import tufts.vue.gui.Widget;

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
    //private final VueTool PresentationTool = VueToolbarController.getController().getTool("viewTool");

    private boolean isMapView = false;
    private boolean mBlackout = false;
    
    private boolean mZoomBorder;
    private boolean inFocal;
    //private LWComponent mZoomContent; // what we zoom-to
    private LWPathway.Entry mLastLoad;

    private final AbstractButton btnLocked;
    private final AbstractButton btnZoom;
    private final AbstractButton btnFocus;
    private final AbstractButton btnSlide;
    private final AbstractButton btnMaster;
    private final AbstractButton btnMapView;
    private final AbstractButton btnFill;
    private final AbstractButton btnRebuild;
    private final AbstractButton btnRevert;
    //private final AbstractButton btnPresent;

    private static final String lblRevertOnSlide = VueResources.getString("button.resetstyle.label");
    private static final String lblRevertOnMaster = VueResources.getString("button.resetallstyle.label");

    private boolean masterJustPressed;
    private boolean slideJustPressed;

    private class Toolbar extends JPanel implements ActionListener {
        Toolbar() {
            //btnLocked.setFont(VueConstants.FONT_SMALL);
            //add(btnLocked);
            add(Box.createHorizontalGlue());
            //add(btnZoom);
            //if (DEBUG.Enabled) add(btnFocus);
            add(btnSlide);
            add(btnMaster);
            //add(btnMapView);
            //add(btnFill);
            //add(btnPresent);
            add(btnRebuild);
            add(btnRevert);

            ButtonGroup exclusive = new ButtonGroup();
            exclusive.add(btnZoom);
            if (DEBUG.Enabled) exclusive.add(btnFocus);
            exclusive.add(btnSlide);
            exclusive.add(btnMaster);
            //exclusive.add(btnPresent);

            btnRebuild.setEnabled(false);
            btnRevert.setEnabled(false);
        }

      
        private void add(AbstractButton b) {
            b.addActionListener(this);
            super.add(b);
        }

        public void actionPerformed(ActionEvent e) {
            if (DEBUG.PRESENT) out(e);
            if (e.getSource() == btnMapView) {
                if (mLastLoad != null && !mLastLoad.isPathway())
                    mLastLoad.setMapView(btnMapView.isSelected());
                reload();
            } else if (e.getSource() == btnMaster) {
                masterJustPressed = true;
                reload();
            } else if (e.getSource() == btnSlide) {
                slideJustPressed = true;
                reload();
            } else if (e.getSource() == btnRebuild) {
                mLastLoad.rebuildSlide();
                mLastLoad.pathway.getMap().getUndoManager().mark(btnRebuild.getText());
                reload();
            } else if (e.getSource() == btnRevert) {
                if (btnMaster.isSelected()) {
                    //out("REVERTING ALL IN " + mLastLoad.pathway);
                    for (LWPathway.Entry entry : mLastLoad.pathway.getEntries()) {
                        //out("REVERTING ENTRY " + entry);
                        LWSlide slide = entry.getSlide();
                        if (slide != null) 
                            slide.revertToMasterStyle();
                    }
                } else {
                    //out("REVERTING CURRENT ENTRY " + mLastLoad);
                    mLastLoad.revertSlideToMasterStyle();
                }
                mLastLoad.pathway.getMap().getUndoManager().mark(btnRevert.getText());
            }

        }

    }
     
    
    public SlideViewer(LWMap map) {
        super(null, "SLIDE");
        //super(map == null ? new LWMap("empty") : map, "SlideViewer");

        setName("Viewer"); // for DockWindow title and backward compat with window saved location

        btnLocked = new JCheckBox(VueResources.getString("checkbox.lock.label"));
        //btnLocked = makeButton("Lock");
        btnZoom = makeButton(VueResources.getString("button.zoom.label"));
        btnFocus = makeButton(VueResources.getString("button.focus.label"));
        btnSlide = makeButton(VueResources.getString("button.slide..label"));
        btnMaster = makeButton(VueResources.getString("checkbox.masterslide.label"));
        btnMapView = new JCheckBox(VueResources.getString("checkbox.mapview.label"));
        btnFill = new JCheckBox(VueResources.getString("checkbox.fill.label"));
        btnRebuild = new JButton(VueResources.getString("button.revert.label"));
        btnRevert = new JButton(VueResources.getString("button.resetstyle.label"));
        //btnPresent = makeButton("Present");

        btnSlide.setSelected(true);

        //DEBUG_TIMER_ROLLOVER = false;

        //VUE.addActiveListener(LWPathway.Entry.class, this);

        /*
        VUE.addActiveListener(LWPathway.Entry.class, new VUE.ActiveListener() {
            public void activeChanged(VUE.ActiveEvent e) {
                load((LWPathway.Entry) e.active);
            }
        });
        */
        
        /*
        VUE.ActivePathwayEntryHandler.addListener(new VUE.ActiveListener<LWPathway.Entry>() {
                public void activeChanged(VUE.ActiveEvent<LWPathway.Entry> e) {
                    load(e.active);
                }
            });
        */
        
    }

    public void activeChanged(tufts.vue.ActiveEvent e, LWPathway.Entry entry) {
        // todo: if entry is null, leave the current contents, but NOT
        // if this is because the map has been closed (will want
        // to be tracking all active instances of LWMap to make this easy --
        // e.g., if this entry's map is in the set, we're good).
        load(entry);
    }
    
    public void showSlideViewer()
    {
        if (Widget.isHidden(this) || !Widget.isExpanded(this))
            Widget.setExpanded(this, true);
    }
    
    
    protected AbstractButton makeButton(String name) {
        AbstractButton b = new JToggleButton(name);
        b.setFocusable(false);
        //b.setBorderPainted(false);
        return b;
    }

//     protected String getEmptyMessage() {
//         if (mLastLoad != null && btnSlide.isSelected())
//             return "Not on Pathway";
//         else
//             return "Empty";
//     }
    
//     @Override
//     public void addNotify() {
//         super.addNotify();
//         getParent().add(new Toolbar(), BorderLayout.NORTH);
//     }

    @Override
    public void grabVueApplicationFocus(String from, ComponentEvent event) {
        final int id = event == null ? 0 : event.getID();

        if (id == MouseEvent.MOUSE_ENTERED)
            out("GVAF " + from + " ignored");
        else
            super.grabVueApplicationFocus(from, event);
    }
    

    @Override
    public void LWCChanged(LWCEvent e) {
        //Util.printStackTrace(e.toString());
        if (DEBUG.PRESENT) out("SLIDEVIEWER LWCChanged " + e);

        if (e.component instanceof LWPathway) {
            // If we're displaying a slide for a node, and
            // the pathway has changed, it may be that the
            // node was just added to the pathway and we
            // need to load it's new slide (or it was
            // removed, and we also need to display that)
            reload();
        } else {
            super.LWCChanged(e);
            if (true||e.getComponent() == mFocal) {
                fitToFocal();
            }
        }
    }

    public void showMasterSlideMode()
    {
        out("showMasterSlideMode");
    	masterJustPressed = true;
    	reload();
    }
    
    public void showSlideMode()
    {
        out("showSlideMode");
    	slideJustPressed = true;
    	reload();
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

    /*
    protected PickContext getPickContext(float x, float y) {
        PickContext pc = super.getPickContext(x, y);

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
    */
    
    /*
    private LWPathway mCurrentPath;
    public void activeChanged(VUE.ActivePathwayEntry.Event e) {
        load(e.entry);

//         if (mCurrentPath != null)p
//             mCurrentPath.removeLWCListener(this);
//         mCurrentPath = path;
//         mCurrentPath.addLWCListener(this);
//         reload();

    }
*/

    private void reload() {
        // TODO: load nothing if active pathway from a different map
        //if (mLastLoad != null)
        if (mLastLoad == null) {
            if (VUE.getActivePathway() != null)
                load(VUE.getActivePathway().asEntry());
        } else
            load(mLastLoad);
    }
        
    /*
    public void selectionChanged(LWSelection s) {
        super.selectionChanged(s);

if (true) return;

        if (btnLocked.isSelected())
            return;
            
        if (s.getSource() != this && s.size() == 1) {
            final LWComponent picked = s.first();
            if (btnMaster.isSelected())
                mLastLoad = picked;
            else if (btnSlide.isSelected()) {
                final LWPathway activePathway = picked.getMap().getActivePathway();
                //if (true || c.getSlideForPathway(c.getMap().getActivePathway()) != null)
                if (activePathway != null && activePathway.contains(picked))
                    load(picked);
                else
                    ; // do nothing for now: allows us to select non-slideworthy on map to drag into slide
            } else
                load(picked);
        }
    }
    */

    @Override
    protected boolean skipAllPainting() {
        return VUE.inNativeFullScreen();
    }
    

    protected void load(LWPathway.Entry entry)
    {
        if (VUE.inNativeFullScreen()) {
            // don't slow us down with our updates if we're in native full screen / presenting
            return;
        }
        
        if (DEBUG.PRESENT) out("SlideViewer: loading " + entry);
        mLastLoad = entry;

        LWComponent focal;

        // If no slide available, disable slide button, even if don't want it!

        if (entry == null) {
            mZoomBorder = false;
            //mZoomContent = null;
            inFocal = false;
            focal = null;
            btnMapView.setEnabled(false);
            btnRebuild.setEnabled(false);
            btnRevert.setEnabled(false);
            //} else if (btnMaster.isSelected() || entry.isPathway()) {
        } else if (masterJustPressed || (entry.isPathway() && !slideJustPressed)) {
            if (DEBUG.PRESENT) out("master auto-select");
            btnMaster.setSelected(true);
            isMapView = false;
            focal = entry.pathway.getMasterSlide();
            inFocal = true;
            btnRebuild.setEnabled(false);
            btnRevert.setEnabled(true);
            btnRevert.setText(lblRevertOnMaster);
        } else {
            if (DEBUG.PRESENT) out("slide auto-select");
            btnSlide.setSelected(true);
            
            if (entry.isPathway()) {
                entry = entry.pathway.getCurrentEntry();
                if (entry == null) {
                    // pathway is empty -- TODO: handle cleanly
                    return;
                }
            }

            isMapView = entry.isMapView();
            btnMapView.setSelected(isMapView);
            btnRebuild.setEnabled(!entry.isOffMapSlide());
            btnRevert.setEnabled(true);
            btnRevert.setText(lblRevertOnSlide);
            focal = entry.getFocal();
            inFocal = true;
        }

        //mZoomContent = focal;

        masterJustPressed = slideJustPressed = false;
        
        super.loadFocal(focal);
        //reshapeImpl(0,0,0,0);
        if (DEBUG.PRESENT) out("SlideViewer: focused is now " + mFocal + " from map " + mMap);
    }

    private LWSlide getActiveMasterSlide() {
        if (VUE.getActiveMap() != null)
            return VUE.getActiveMap().getActivePathway().getMasterSlide();
        else
            return null;
    }

    /** @return false -- a no-op -- don't allow focal popping in slide viewer */
    @Override
    protected boolean popFocal(boolean toTopLevel, boolean animate) { return false; }
    
//     @Override
//     public void loadFocal(LWComponent focal, boolean fitToFocal, boolean animate) {
//         if (focal instanceof LWMap) {
//             // never load the map in the slide viewer
//         } else {
//             super.loadFocal(focal, fitToFocal, animate);
//         }
//     }

    @Override
    protected Color getBackgroundFillColor(DrawContext dc) {
        //return Color.white;
        return Color.darkGray;
// this code produces "filled" slide viewer look,
// tho then we can't make out the edge of the slide:
//         final LWPathway.Entry entry = VUE.getActiveEntry();
//         if (entry != null)
//             return entry.getFullScreenFillColor();
//         else
//             return Color.gray;
    }

    @Override
    protected void drawFocal(DrawContext dc) {
        //if (mLastLoad != null && mLastLoad.isMapView()) {
        if (mLastLoad != null && !mLastLoad.canProvideSlide()) {
            // have to fill first, or super.drawFocal will fill over us...
//             if (DEBUG.Enabled)
//                 dc.fillBackground(Color.red);
//             else
                dc.fillBackground(getBackgroundFillColor(dc));
                mLastLoad.pathway.getMasterSlide().drawFit(dc.push(), 0);
                dc.pop();
            //mLastLoad.pathway.getMasterSlide().drawIntoFrame(dc);
        }
        super.drawFocalImpl(dc);
            
    }


    @Override
    protected void drawSelection(DrawContext dc, LWSelection s) {
        // Don't draw selection if its the focused component
        if (s.size() == 1 && s.first() == mFocal)
            return;
        super.drawSelection(dc, s);
    }


    @Override
    protected DrawContext getDrawContext(Graphics2D g) {
        DrawContext dc = super.getDrawContext(g);
        dc.setDrawPathways(false);        
        //dc.setEditMode(btnMaster.isSelected());
        /* dc.focal now handles this 
        if (isMapView) {
            dc.isFocused = true;// turns off pathway drawing
            //dc.setInteractive(false); // turns off selection drawing
        }
        */
        return dc;
    }

    @Override
    public void fireViewerEvent(int id, String cause) {
        if (DEBUG.PRESENT) out("fireViewerEvent <" + id + "> skipped");
    }

    /*
    protected void reshapeImpl(int x, int y, int w, int h) {
        if (DEBUG.PRESENT) out("reshapeImpl");
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
                                            btnFill.isSelected() ? 0 : 20,
                                            //mZoomBorder ? 20 : 0,
                                            false);
        out("zoomed to " + zoomBounds + " @ " + tufts.vue.ZoomTool.prettyZoomPercent(getZoomFactor()));

    }
    */
    
    /*
    protected void drawMap(DrawContext dc)
    {
        if (mFocal == null)
            return;
        if (isMapView) {
            drawFocal(dc);
        } else {
            //out("Drawing focal " + mFocal);
            dc.setEditMode(btnMaster.isSelected());
            mFocal.draw(dc);
            //drawSlide(dc);
        }
    }
    */
    
    /* either draws the entire map (previously zoomed to to something to focus on),
     * or a fully focused part of of it, where we only draw that item.
     */
    /*
    protected void drawFocal(DrawContext dc)
    {
        // TODO: need to get this working to wherever we've been "zoom fitted" to, and/or
        // fundamentally change all object drawing to be zero based.
        //drawMasterSlide(dc); 
        
        out("drawing focal " + mFocal);

        if (isMapView) {
            dc.isFocused = true;// turns off pathway drawing
            //dc.setInteractive(false); // turns off selection drawing
        }
        
//         if (btnFocus.isSelected())
//             dc.g.setColor(Color.black);
//         else
//             dc.g.setColor(mFocal.getMap().getFillColor());
//         dc.g.fill(dc.g.getClipBounds());
        
        final LWMap underlyingMap = mFocal.getMap();

        //zoomToContents();
        
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
    */
    

    /*
    protected void XsetDragger(LWComponent c) {
        // need to cleanup MapViewer such that overriding this actually works
        if (btnMaster.isSelected() && c instanceof LWSlide)
            return;
        else {
            out("setting dragger to: " + c);
            super.setDragger(c);
        }
    }
    */
    
    /*
    protected void drawSlide(DrawContext dc)
    {
        // just drawing the master slide here only happens to work because it's exactly
        // the same size as the slide itself (mFocal is a slide of we're in here), and
        // both exist in the "ether", (not on the map), so they both happen to have the
        // same location (0,0), so we don't have to worry about panning around the map
        // to get them both to draw (like we will have to with map-based focals) -- this
        // really highlights that it would be nice to better DrawContext controls for
        // moving back and forth between "immediate"(?)  mode v.s. map-mode, and perhaps
        // change the drawing model to always be zero based -- the translating could
        // happen during a draw traversal, which could be shared code with similar
        // translation we'd need to do via pick traversals.
        
        //drawMasterSlide(dc); 
        
        // Now draw the actual slide
        mFocal.draw(dc);
    }

    private void drawMasterSlide(DrawContext dc)
    {
        drawMasterSlide(dc, mLastLoad.pathway.getMasterSlide(), btnFill.isSelected(), btnMaster.isSelected());
    }
    
    public static void drawMasterSlide(DrawContext dc, LWSlide master, boolean fillMode, boolean editMode)
    {
        if (fillMode) {
            dc.g.setColor(master.getFillColor());
            dc.g.fill(dc.g.getClipBounds());
        }

        //out("drawMasterSlide: offsetX/Y: " + dc.offsetX + "," + dc.offsetY);
        //dc.setRawDrawing();
        master.setLocation(0,0);// TODO: hack till we can lock these properties
        //master.setLocation(-dc.offsetX,-dc.offsetY);

        if (editMode) {
            // When editing the master, allow us to see stuff outside of it
            // (no need to clip);
            dc.setEditMode(true);
            master.draw(dc);
        } else {

            final Shape curClip = dc.g.getClip();
            //out("curClip: " + curClip);
            
            // When just filling the background with the master, only draw
            // what's in the containment box
            dc.g.setClip(master.getBounds());
            master.draw(dc);
            dc.g.setClip(curClip);
        }

        //Dc.setMapDrawing();
    }
    */
    
    

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

    /*
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