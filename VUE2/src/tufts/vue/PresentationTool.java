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

package tufts.vue;

import tufts.macosx.MacOSX;
import tufts.vue.gui.TextRow;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.*;
import javax.swing.*;

/**
 * Tool for presentation mode.
 *
 * @version $Revision: 1. $ / $Date: 2006/01/20 17:17:29 $ / $Author: sfraize $
 * @author Scott Fraize
 *
 */
public class PresentationTool extends VueTool
//    implements VUE.ActiveViewerListener
{
    enum Direction { FORWARD, BACKWARD };
    //private static int FORWARD = 1;
    //private static int BACKWARD = -1;
    
    private JButton mStartButton;
    private LWComponent mCurrentPage;
    private LWComponent mNextPage; // is this really "startPage"?
    private LWComponent mLastPage;
    private LWLink mLastFollowed;
    private LWPathway mPathway;
    private int mPathwayIndex = 0;
    private LWPathway.Entry mEntry;
    //private LWSlide mMasterSlide;

    private boolean mScreenBlanked = false;

    

    private JCheckBox mShowContext = new JCheckBox("Show Context");
    private JCheckBox mToBlack = new JCheckBox("Black");
    private JCheckBox mZoomLock = new JCheckBox("Lock 100%");
    private boolean mShowNavigator = false;

    private boolean mFadeEffect = true;
    private boolean mZoomToPage = true;

    private Stack mBackList = new Stack();

    public PresentationTool() {
        super();
        //VUE.addActiveViewerListener(this);
    }
    
//     public void activeViewerChanged(MapViewer viewer) {
//         mCurrentPage = mNextPage = null;
//         mBackList.clear();
//     }
    
    public JPanel createToolPanel() {
        //JPanel p = super.createToolPanel();
        JPanel p = new JPanel();
        mStartButton = new JButton("Start Presenting");
        mToBlack.setSelected(false);
        mShowContext.setSelected(true);
        add(p, mStartButton);
        //add(p, mShowContext);
        //add(p, mToBlack);
        //p.add(mZoomLock, 0);
        return p;
    }

    private void add(JPanel p, AbstractButton c) {
        c.setFocusable(false);
        c.setFont(VueConstants.FONT_SMALL);
        if (c instanceof JCheckBox)
            c.setContentAreaFilled(false);
        p.add(c, 0);
        c.addActionListener(this);
    }
    
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == mStartButton) {
            startPresentation();
            VUE.toggleFullScreen(true);
        } else if (ae.getSource() instanceof JCheckBox) {
            repaint();
        } else
            super.actionPerformed(ae);
    }

    private void repaint() {
        out("repaint");
        VUE.getActiveViewer().repaint();
    }

    public boolean handleKeyPressed(java.awt.event.KeyEvent e) {
        out("handleKeyPressed " + e);
        int key = e.getKeyCode();
        char k = e.getKeyChar();
        if (key == KeyEvent.VK_SPACE || key == KeyEvent.VK_RIGHT) {
            if (mCurrentPage == null && mNextPage == null)
                startPresentation();
            else
                forwardPage();
        } else if (key == KeyEvent.VK_LEFT) {
            backPage();
        } else if (k == 'f')             { mFadeEffect = !mFadeEffect;
        } else if (k == 'c' || k == 'n') { mShowContext.doClick();
        } else if (k == 'b')             { mToBlack.doClick();
        } else if (k == 'm')             {
            mShowNavigator = !mShowNavigator;
            repaint();
        } else if (k == '1') {
            if (mPathway != null) {
                mPathwayIndex = 0;
                mNextPage = mPathway.getNodeEntry(0);
                forwardPage();
            }
            repaint();
        } else
            //        } else if (k == 'z')             { mZoomToPage = !mZoomToPage;
            return false;
        //repaint();
        return true;
    }

    public boolean handleKeyReleased(java.awt.event.KeyEvent e) {
        return false;
    }
    
    //private boolean isPresenting() { return !mShowContext.isSelected(); }
    
    public boolean handleMousePressed(MapMouseEvent e)
    {
        final LWComponent hit = e.getPicked();
        
        out("handleMousePressed " + e + " hit on " + hit);
        if (hit != null && mCurrentPage != hit) {
            List linked = hit.getLinkEndpoints();
            if (mCurrentPage == null) {
                // We have no current page, so just zoom to what's been clicked on and start there.
                mBackList.clear();
                setPage(hit);
            } 
            
            /*if (mCurrentPage != null && mCurrentPage.getParent() == hit) {
                // hack for clicking back up to parent group when had click-zoomed child image
                setPage(hit);
                } else*/
            else if (mCurrentPage != null && linked.size() == 1/* && isPresenting()*/) {
                // not good during nav: don't follow links during nav? could skip if link is mCurrentPage
                // problem is case of nav'd to a link, then click on edge of node to right of link,
                // where you'd want to go to that node, but instead if that's it's only link, it follows
                // the link back to where you came from.  This is not a likely usage case tho.
                followLink(hit, hit.getLinkTo((LWComponent) linked.get(0)));
                /*
                LWComponent linkingTo = 
                mLastFollowed = mCurrentPage.getLinkTo(linkingTo);
                setPage(linkingTo);
                */
            } else {
                setPage(hit);
            }
        }
        return true;
    }

    private void followLink(LWComponent src, LWLink link) {
        LWComponent linkingTo = link.getFarPoint(src);
        mLastFollowed = link;
        setPage(linkingTo);
    }

    private void startPresentation()
    {
        out(this + " startPresentation");
        //mShowContext.setSelected(false);
        mBackList.clear();

        if (VUE.getActivePathway() != null && VUE.getActivePathway().length() > 0) {
            mPathway = VUE.getActivePathway();
            mPathwayIndex = 0;
            mEntry = mPathway.getEntry(mPathwayIndex);
            setEntry(mEntry);
        } else {
            mPathway = null;
            mPathwayIndex = 0;
            if (VUE.getSelection().size() > 0)
                setPage(VUE.getSelection().first());
            else if (mCurrentPage != null)
                setPage(mCurrentPage);
            else
                setPage(mNextPage);
        }
    }

    private void backPage() {

        if (mPathway != null && inCurrentPathway(mCurrentPage)) {
            setEntry(nextPathwayEntry(Direction.BACKWARD));
            //LWComponent prevPage = nextPathwayPage(Direction.BACKWARD);
            //LWComponent prevPage = null;
            //if (prevPage != null)
            //    setPage(prevPage);
        } else if (!mBackList.empty())
            setPage(VUE.getActiveViewer(), (LWComponent) mBackList.pop(), true);
    }
        

    private void forwardPage()
    {
        out("forwardPage");
        
        if (mNextPage != null) {
            out("NextPage is already " + mNextPage);
            if (mNextPage != mCurrentPage) {
                setPage(mNextPage);
                mNextPage = null;
                return;
            }
        }

        //if (mPathway == null && mCurrentPage.inPathway(mCurrentPage.getMap().getPathwayList().getActivePathway())) {
        if (mPathway == null && mCurrentPage.inPathway(VUE.getActivePathway())) {
            //mPathway = mCurrentPage.getMap().getPathwayList().getActivePathway();
            mPathway = VUE.getActivePathway();
            mPathwayIndex = mPathway.indexOf(mCurrentPage);
            out("Joined pathway " + mPathway + " at index " + mPathwayIndex);
        }


        if (mPathway == null || !inCurrentPathway(mCurrentPage))
            setPage(guessNextPage());
        else
            setEntry(nextPathwayEntry(Direction.FORWARD));

        /*
        final LWComponent nextPage;

        if (mPathway == null || !inCurrentPathway(mCurrentPage))
            nextPage = guessNextPage();
        else
            nextPage = nextPathwayPage(Direction.FORWARD);

        out("Next page: " + nextPage);
        
        if (nextPage != null)
            setPage(nextPage);
        */
    }

    private boolean inCurrentPathway(LWComponent c) {
        if (c == null)
            return false;
        else
            return c.inPathway(mPathway) || c.getParent() == mPathway;
        // slides in a pathway have their parent set to the pathway
    }

    /** @param direction either 1 or -1 */
    private LWPathway.Entry nextPathwayEntry(Direction direction)
    {
        out("nextPathwayEntry " + direction);
        if (direction == Direction.BACKWARD && mPathwayIndex == 0)
            return null;
        
        if (direction == Direction.FORWARD)
            mPathwayIndex++;
        else
            mPathwayIndex--;

        final LWPathway.Entry nextEntry = mPathway.getEntry(mPathwayIndex);
        out("Next pathway index: #" + mPathwayIndex + " = " + nextEntry);

        if (nextEntry == null) {
            //nextEntry = mPathway.getFirstEntry(nextPathwayNode);
            mPathwayIndex -= (direction == Direction.FORWARD ? 1 : -1);
        }
        //out("Next pathway slide/focal: " + nextEntry);
        return nextEntry;
    }
    
    private LWComponent guessNextPage()
    {
        // todo: only bother with links that have component endpoints!
        List links = mCurrentPage.getLinks();
        LWLink toFollow = null;
        if (links.size() == 1) {
            toFollow = (LWLink) mCurrentPage.getLinks().get(0);
        } else {
            Iterator i = mCurrentPage.getLinks().iterator();
            
            while (i.hasNext()) {
                LWLink link = (LWLink) i.next();
                if (link.getFarNavPoint(mCurrentPage) == null)
                    continue; // if a link to nothing, ignore
                if (link != mLastFollowed) {
                    toFollow = link;
                    break;
                }
            }
        }
        LWComponent nextPage = null;
        if (toFollow != null) {
            mLastFollowed = toFollow;
            nextPage = toFollow.getFarNavPoint(mCurrentPage);
            if (nextPage.getParent() instanceof LWGroup)
                nextPage = nextPage.getParent();
        }
        // TODO: each page keeps a mPrevPage, which we use instead of
        // the peek here to find the "default" backup.  Maybe
        // a useful "uparrow" functionality can also make use of this?
        // uparrow should also go back up to the parent of the current
        // page if it's anything other than the map itself
        if (nextPage == null && !mBackList.empty() && mBackList.peek() != mCurrentPage)
            nextPage = (LWComponent) mBackList.peek();

        return nextPage;
    }

    public LWComponent getCurrentPage() {
        return mCurrentPage;
    }
            
    private void makeInvisible() {
        if (VueUtil.isMacPlatform() && VUE.inNativeFullScreen()) {
            //out("makeInvisible");
            try {
                MacOSX.makeMainInvisible();
                mScreenBlanked = true;
            } catch (Error e) {
                System.err.println(e);
            }
        }
    }
        
    private void makeVisible() {
        if (VueUtil.isMacPlatform() && VUE.inNativeFullScreen()) {
            //out("makeVisible");
            try {
                if (MacOSX.isMainInvisible())
                    MacOSX.fadeUpMainWindow();
                mScreenBlanked = false;
            } catch (Error e) {
                System.err.println(e);
            }
        }
    }

    private void makeVisibleLater() {
        if (VueUtil.isMacPlatform() && VUE.inNativeFullScreen()) {
            VUE.invokeAfterAWT(new Runnable() {
                    public void run() {
                        //out("makeVisibleLater");
                        //if (invisible)
                        makeVisible();
                    }
                });
        }
    }

    private void setEntry(LWPathway.Entry e)
    {
        out("setEntry " + e);
        if (e == null)
            return;
        mEntry = e;
        mPathway = e.pathway;
        setPage(VUE.getActiveViewer(), mEntry.getFocal(), false);
        VUE.setActivePathwayEntry(mEntry);
    }
    private void setPage(LWComponent page) {
        setPage(VUE.getActiveViewer(), page, false);
    }
    
    private void setPage(final MapViewer viewer, final LWComponent page, boolean backup)
    {
        out("setPage " + page);
        if (!backup && mCurrentPage != null) {
            if (mBackList.empty() || mBackList.peek() != page)
                mBackList.push(mCurrentPage);
        }
        mLastPage = mCurrentPage;
        mCurrentPage = page;
        mNextPage = null;
        viewer.clearTip();

        if (mFadeEffect) {
        
            // It case there was a tip visible, we need to make sure
            // we wait for it to finish clearing before we move on, so
            // we need to put the rest of this in the queue.  (if we
            // don't do this, the screen fades out & comes back before
            // the map has panned)
            
            VUE.invokeAfterAWT(new Runnable() {
                    public void run() {
                        if (mFadeEffect)
                            makeInvisible();
                        zoomToPage(page, !mFadeEffect);
                        if (mScreenBlanked)
                            makeVisibleLater();
                    }
                });
            
        } else {
            zoomToPage(page, true);
        }
            
    }

    private void zoomToPage(LWComponent page, boolean animate)
    {
        animate = false; // TODO: is forced off for now: currently meaningless to animate between slides

        if (mZoomToPage == false)
            return;

        //tufts.Util.printStackTrace("zoomToPage");
        
        int margin = 0;
//         if (page instanceof LWImage)
//             margin = 0;
//         else margin = 8; // turn this off soon
        //else margin = 32; // turn this off soon

        //if (page instanceof LWGroup && !(page instanceof LWSlide))
        if (page instanceof LWSlide == false)
            margin = (int) (tufts.vue.gui.GUI.GScreenHeight * 0.05);

        out("zoomToPage " + page + " margin=" + margin);
        
        if (page != null)
            ZoomTool.setZoomFitRegion(VUE.getActiveViewer(), page.getBounds(), margin, animate);
        
        //VUE.ZoomFitButton.doClick(1000); // works
        // below doesn't work because viewer.getVisibleSize() returning 0,0 before it's displayed!
        //ZoomTool.setZoomFitRegion(viewer, viewer.getMap().getBounds(), margin);
        //ZoomTool.setZoomFit();
    }

    public void handleDraw(DrawContext dc, MapViewer viewer, LWComponent focal) {
        //LWMap map = focal.getMap();

        // TODO TODO TODO: This is a disaster.  Either need to make a PresentationViewer
        // ala the SlideViewer, or move SlideViewer functionality up into MapViewer
        // (most powerful, as then we could do all that stuff in the regular viewer).
        // Tho after doing that, we may still want a PresentationViewer to subclass
        // the MapViewer to turn off all the stuff we're not going to want to be
        // active when in presentation mode.

        // TODO ACTION: okay, we can move the focal code up to the MapViewer for
        // the current slide (and thus also will get us reshapeImpl handling),
        // and we could JUST do the master slide handling here, tho it would
        // still be nice to see the master stuff for working full-screen edit mode.
        // What that really means is exposes the MapViewer to the LWPathway.Entry
        // concept -- oh well, I suppose we can live with that, tho would
        // be nice to find a way around it.

        dc.setInteractive(false);
        dc.setPresenting(true);
        dc.isFocused = true;

        if (mEntry != null && !mEntry.isPathway())
            drawPathwayEntry(dc, viewer, mEntry);
        else
            drawFocal(dc, viewer, focal);
        
        /*
        if (focal instanceof LWSlide)
            drawSlide(dc, viewer, (LWSlide) focal);
        else
            drawFocal(dc, viewer, focal);
        */

        if (VUE.inFullScreen() && mShowNavigator)
            drawNavigatorMap(dc);
        
    }


    protected void drawPathwayEntry(final DrawContext dc, MapViewer viewer, final LWPathway.Entry entry)
    {
        out("drawing entry " + entry);
        
        final LWSlide master = entry.pathway.getMasterSlide();
        final Shape curClip = dc.g.getClip();
        final LWComponent focal = entry.getFocal();

        out("drawing master " + master);

        //zoomToPage(master, false); // can't do here: causes loop: need to handle via reshapeImpl

        dc.g.setColor(master.getFillColor());
        dc.g.fill(dc.g.getClipBounds());
        //if (focal.getX() != 0 || focal.getY() != 0) {
        if (focal instanceof LWSlide) {
            // slides are always at 0,0, and the exact same size (thus zoom) as the master
            // slide, so we can just draw the master slide w/out further ado
            dc.g.setClip(master.getBounds());
            master.draw(dc);

            // Now just draw the slide
            dc.g.setClip(curClip);
            focal.draw(dc);

            
        } else {

            Point2D.Float offset = new Point2D.Float();
            double masterZoom = ZoomTool.computeZoomFit(viewer.getVisibleSize(),
                                                        0,
                                                        master.getShapeBounds(), // don't include border
                                                        offset,
                                                        true);
            
            out("master slide compensated to zoom " + masterZoom + " at " + offset);

            dc.setRawDrawing();

            dc.g.translate(-offset.x, -offset.y);
            dc.g.scale(masterZoom, masterZoom);
            master.draw(dc);
            dc.g.scale(1/masterZoom, 1/masterZoom);
            dc.g.translate(offset.x, offset.y);

            dc.setMapDrawing();

            dc.g.setClip(curClip);
            drawFocal(dc, viewer, focal);
        }
        

        //out("drawing focal " + focal);
        //focal.draw(dc); // will not work if is map-view
        out("-------------------------------------------------------");
    }
    
    /** either draws the entire map (previously zoomed to to something to focus on),
     * or a fully focused part of of it, where we only draw that item.
     */

    protected void drawFocal(final DrawContext dc, MapViewer viewer, final LWComponent focal)
    {
        final LWMap underlyingMap = focal.getMap();
        
        out("drawing focal " + focal + " in map " + underlyingMap);

        //dc.g.setColor(mFocal.getMap().getFillColor());
        //dc.g.fill(dc.g.getClipBounds());
        
        if (focal.isTranslucent() && focal != underlyingMap && focal instanceof LWGroup == false) { // groups not meant to operate transparently
            out("drawing clipped focal " + focal.getShape());

            //out("drawing underlying map " + underlyingMap);

            // If our fill is in any way translucent, the underlying
            // map can show thru, thus we have to draw the whole map
            // to see the real result -- we just set the clip to
            // the shape of the focal.
            
            final Shape curClip = dc.g.getClip();
            dc.g.setClip(focal.getShape());
            underlyingMap.draw(dc);
            dc.g.setClip(curClip);
            
        } else {
            out("drawing raw focal");
            focal.draw(dc);
        }
    }
    
    /** Draw a ghosted panner */
    private void drawNavigatorMap(DrawContext sourceDC) {

        final Rectangle panner = new Rectangle(0,0, 192,128);

        sourceDC.setRawDrawing();
        DrawContext dc = sourceDC.create();
        //dc.setAlpha(0.2);
        //dc.g.setColor(Color.white);
        dc.setAlpha(0.3);
        dc.g.setColor(Color.black);
        dc.g.fill(panner);
        dc.setAlpha(1);
        dc.g.clipRect(0,0, panner.width, panner.height);
        MapPanner.paintViewerIntoRectangle(dc.g, VUE.getActiveViewer(), panner);
    }

    private void drawNavNodes(DrawContext dc)
    {
        dc.setRawDrawing();
        Rectangle frame = dc.getFrame();
        dc.g.translate(frame.x, frame.y);

        if (DEBUG.Enabled) {
            dc.g.setFont(VueConstants.FixedFont);
            dc.g.setColor(Color.gray);
            dc.g.drawString(mCurrentPage.getDiagnosticLabel(), 10, 20);
        }

        if (mCurrentPage.getLinks().size() == 0)
            return;
        
        List labels = getNavLabels(mCurrentPage);
        //out("got nav labels: " + labels);
        
        int y = frame.height;
        Iterator i = labels.iterator();
        int cnt = 0;
        while (i.hasNext()) {
            int x = frame.width / labels.size() * cnt++;
            LWComponent nav = makeNavNode((String)i.next(), x, y);
            nav.translate(0, -nav.getHeight());
            nav.draw(dc);
        }
    }

    private static List getNavLabels(LWComponent page) {
        List navs = new ArrayList();
        List links = page.getLinks();
        Iterator i = links.iterator();
        while (i.hasNext()) {
            LWLink link = (LWLink) i.next();
            LWComponent farpoint = link.getFarNavPoint(page);
            if (farpoint != null) {
                if (link.hasLabel())
                    navs.add(link.getLabel());
                else
                    navs.add(farpoint.getDisplayLabel());
            }
        }
        return navs;
    }
        
    
    private static Font NavFont = new Font("SansSerif", Font.PLAIN, 18);
    //private static Color NavFill = new Color(255,255,255,32);
    //private static Color NavFillColor = new Color(0,0,0,64);
    private static Color NavFillColor = new Color(64,64,64,96);
    private static Color NavTextColor = Color.gray;

    private LWComponent makeNavNode(String label, int x, int y) {
        LWNode n = new LWNode("  " + label + " ", x, y);
        n.setFont(NavFont);
        n.setTextColor(NavTextColor);
        n.setFillColor(NavFillColor);
        n.setStrokeWidth(0);
        n.setShape(new RoundRectangle2D.Float(0,0, 10,10, 30,30));
        return n;
    }
        
    private void drawNavTest(DrawContext dc)
    {
        dc.setRawDrawing();
        dc.g.setFont(NavFont);
        Rectangle frame = dc.getFrame();
        dc.g.translate(frame.x, frame.y);
        
        String labels[] = { "One", "Three", "Jumping", "Twenty" };
        dc.g.setColor(Color.gray);
        dc.g.drawString("PRESENTATION", 20, 20);
        int y = frame.height;
        //y -= makeNavNode("Kj",0,0).getHeight(); // calibrate height
        for (int i = 0; i < labels.length; i++) {
            int x = frame.width / labels.length * i;
            LWComponent nav = makeNavNode(labels[i], x, y);
            nav.translate(0, -nav.getHeight());
            nav.draw(dc);
            
            //TextRow row = new TextRow(labels[i], dc.g);
            //row.draw(dc.g, x, y - row.height);
            //dc.g.drawString(labels[i], x, y);
        }
    }
    
    public void handleFullScreen(boolean fullScreen) {
        // when entering or exiting full-screen, keep us zoomed
        // to current page.
        makeInvisible();
        if (getCurrentPage() != null) {
            zoomToPage(getCurrentPage(), false);
        }
        makeVisibleLater();
    }
    
    public void handleToolSelection() {
        out(this + " SELECTED");
        mCurrentPage = null;
        if (VUE.getSelection().size() == 1)
            mNextPage = VUE.getSelection().first();
        else
            mNextPage = null;

        //handleSelectionChange(VUE.getSelection());
        //mStartButton.requestFocus();
    }
    
    public void handleSelectionChange(LWSelection s) {
        out("SELECTION CHANGE");
        // TODO: if active map changes, need to be sure to clear current page!
        if (s.size() == 1)
            mCurrentPage = s.first();
        
        /*
        if (s.size() == 1)
            mNextPage = s.first();
        else
            mNextPage = null;
        */
    }
    
    /*    
    public void handleSelectionChange(LWSelection s) {
        if (s.size() == 1 &&
            VUE.multipleMapsVisible() &&
            VUE.getRightTabbedPane().getSelectedViewer().getMap() == VUE.getActiveMap())
        {
            MapViewer viewer = VUE.getRightTabbedPane().getSelectedViewer();
            ZoomTool.setZoomFitRegion(viewer, s.first().getBounds(), 16);
        }
    }
    */
    
    public boolean supportsSelection() { return true; }
    public boolean supportsResizeControls() { return false; }
    public boolean supportsDraggedSelector(MapMouseEvent e) { return false; }
    public boolean hasDecorations() { return true; }
    public boolean usesRightClick() { return true; }

    
}