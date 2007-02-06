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
    implements VUE.ActiveViewerListener
{
    private static int FORWARD = 1;
    private static int BACKWARD = -1;
    
    private JButton mStartButton;
    private LWComponent mCurrentPage;
    private LWComponent mNextPage; // is this really "startPage"?
    private LWComponent mLastPage;
    private LWLink mLastFollowed;
    private LWPathway mPathway;
    private int mPathwayIndex = 0;

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
        VUE.addActiveViewerListener(this);
    }
    
    public void activeViewerChanged(MapViewer viewer) {
        mCurrentPage = mNextPage = null;
        mBackList.clear();
    }
    
    public JPanel createToolPanel() {
        //JPanel p = super.createToolPanel();
        JPanel p = new JPanel();
        mStartButton = new JButton("Start Presenting");
        mToBlack.setSelected(false);
        mShowContext.setSelected(true);
        add(p, mStartButton);
        add(p, mShowContext);
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
        VUE.getActiveViewer().repaint();            
    }

    public boolean handleKeyPressed(java.awt.event.KeyEvent e) {
        //out(this + " handleKeyPressed " + e);
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
                mNextPage = mPathway.getChild(0);
                forwardPage();
            }
            repaint();
        } else
            //        } else if (k == 'z')             { mZoomToPage = !mZoomToPage;
            return false;
        return true;
    }

    public boolean handleKeyReleased(java.awt.event.KeyEvent e) {
        return false;
    }
    
    /*
    public LWComponent X_findComponentAt(LWMap map, float mapX, float mapY) {
        return map.findDeepestChildAt(mapX, mapY);
    }
    */

    private boolean isPresenting() {
        return !mShowContext.isSelected();
    }
    
    public boolean handleMousePressed(MapMouseEvent e) {

        /*
        final LWComponent hit;
        if (mCurrentPage == null)
            hit = e.getMap().findChildAt(e.getMapX(), e.getMapY());
        else
            hit = e.getMap().findDeepestChildAt(e.getMapX(), e.getMapY());
        */
        
        final LWComponent hit = e.getPicked();
        
        out(this + " handleMousePressed " + e + " hit on " + hit);
        if (hit != null && mCurrentPage != hit) {
            List linked = hit.getLinkedComponents();
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

    // need to handle this and have MapViewer allow interrupt, so clicks on
    // something selected don't pop a label edit!
    //public boolean handleMouseReleased(MapMouseEvent e)
    

    private void startPresentation()
    {
        out(this + " startPresentation");
        //mShowContext.setSelected(false);
        mBackList.clear();

        if (VUE.getActivePathway() != null) {
            mPathway = VUE.getActivePathway();
            mPathwayIndex = -1;
            forwardPage();
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

        //setPage((LWComponent) VUE.getActiveMap().getChildList().get(0));
        
        /*
        LWMap map = new LWMap(mCurrent) {
                // don't allow content to change (drops to add/remove, etc)
                // this doesn't totally cover it: the existing parent
                // (the good map) is asked to reparent it's children
                // to US, which it does happily...
                protected void addChildInternal(LWComponent c) {}
                protected void removeChildren(Iterator i) {}
                protected void removeChildInternal(LWComponent c) {}
                public void reparentTo(LWContainer newParent, Iterator possibleChildren) {}
            };
        MapViewer viewer = VUE.displayMap(map);
        //VUE.setActiveViewer(viewer);
        setPage(viewer, mCurrent);
        */
    }

    private void backPage() {

        if (mPathway != null && mCurrentPage.inPathway(mPathway)) {
            LWComponent prevPage = nextPathwayPage(BACKWARD);
            if (prevPage != null)
                setPage(prevPage);
        } else if (!mBackList.empty())
            setPage(VUE.getActiveViewer(), (LWComponent) mBackList.pop(), true);
    }
        

    private void forwardPage()
    {
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

        final LWComponent nextPage;

        if (mPathway == null || (mCurrentPage != null && !mCurrentPage.inPathway(mPathway)))
            nextPage = guessNextPage();
        else
            nextPage = nextPathwayPage(FORWARD);

        out("Next page: " + nextPage);
        
        if (nextPage != null)
            setPage(nextPage);
    }

    /** @param direction either 1 or -1 */
    private LWComponent nextPathwayPage(int direction)
    {
        if (direction == BACKWARD && mPathwayIndex == 0)
            return null;
        
        mPathwayIndex += direction;
        final LWComponent nextPathwayNode = mPathway.getChild(mPathwayIndex);
        out("Next pathway index: #" + mPathwayIndex + " " + nextPathwayNode);


        final LWComponent nextPage;

        if (nextPathwayNode != null)
            nextPage = nextPathwayNode.getFocalForPathway(mPathway);
        else
            nextPage = null;
        //LWComponent nextPage = nextPathwayNode.getSlideForPathway(mPathway);
        out("Next pathway slide/focal: " + nextPage);
        
        //if (nextPage == null) nextPage = nextPathwayNode;
        
        if (nextPage == null)
            mPathwayIndex -= direction;

        return nextPage;
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

    private void setPage(LWComponent page) {
        setPage(VUE.getActiveViewer(), page, false);
    }
    private void setPage(final MapViewer viewer, final LWComponent page, boolean backup) {
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

    private void zoomToPage(LWComponent page, boolean animate) {
        if (mZoomToPage == false)
            return;

        int margin = 0;
        if (page instanceof LWImage)
            margin = 0;
        else margin = 8; // turn this off soon
        //else margin = 32; // turn this off soon
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

        dc.setInteractive(false);
        dc.isFocused = true;
        
        if (focal instanceof LWSlide)
            drawSlide(dc, viewer, (LWSlide) focal);
        else
            drawFocal(dc, viewer, focal);
            

        if (VUE.inFullScreen() && mShowNavigator)
            drawNavigatorMap(dc);
        
    }


    protected void drawSlide(final DrawContext dc, MapViewer viewer, final LWSlide slide)
    {
        out("drawing slide " + slide);
        
        final LWSlide master = VUE.getActiveMap().getActivePathway().getMasterSlide();

        final Shape curClip = dc.g.getClip();

        dc.setRawDrawing();

        out("drawing master " + master);
        
        // When just filling the background with the master, only draw
        // what's in the containment box
        master.setLocation(0,0);// TODO: hack till we can lock these properties
        //dc.g.setClip(master.getBounds());
        master.draw(dc);
        //dc.g.setClip(curClip);

        //for (LWComponent c : mFocal.getChildList()) out("child to draw: " + c);

        // this hack-up just not getting us there:
        zoomToPage(slide, false);
        
        // Now draw the actual slide
        slide.draw(dc);
    }
    
    /** either draws the entire map (previously zoomed to to something to focus on),
     * or a fully focused part of of it, where we only draw that item.
     */

    protected void drawFocal(final DrawContext dc, MapViewer viewer, final LWComponent focal)
    {
        out("drawing focal " + focal);
        /*

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
        */
    }
    
    

    public void hacked_pres_mode_handleDraw(DrawContext dc, LWComponent focal) {
        LWMap map = focal.getMap();
        
        Color oldColor = null;
        if (mToBlack.isSelected()) {
            oldColor = map.getFillColor();
            map.takeFillColor(Color.black);
            dc.setBlackWhiteReversed(true);
        }

        dc.setInteractive(false);
        dc.setPresenting(isPresenting());
        dc.g.setColor(map.getFillColor());
        dc.g.fill(dc.g.getClipBounds());

        boolean clipped = false;
        Shape savedClip = null;
        Color savedColor = null;
        if (mCurrentPage instanceof LWNode && isPresenting()) {
            LWNode node = (LWNode) mCurrentPage;
            
            // if had an in-group link from one region of image to another, would still
            // want to present as a clipped: really need to check if it has any
            // out-bound links, or REALLY, if we were just *sent* here via link, as
            // opposed to clicking on us.  Tho what if somebody DOES click on us?  We
            // still want to be clipped, not follow the link: so any transparency wants
            // to clip?  But what about "check out the mouth" -- okay, that has a label:
            // so if any transparency and NO label, then we're covered, right?  NO!  The
            // left ring in the ladies, we just wanted to be a click region.  So,
            // there's no way to deterministically figure this out: user will have to
            // specify somehow if we want to offer both options.  COULD use a heursitc
            // where we follow first if it's an out-of-group link (and has label), tho
            // that's getting really hairy.

            // or, could get object-fancy and have LWRegion object, who's
            // label isn't displayed, but that's getting whack complex.

            // BTW: really only want to check if there are no *outbound* links,
            // to non-null locations.
            if (node.isTranslucent() && !node.hasLabel() && node.getLinks().size() == 0) {
                savedClip = dc.g.getClip();
                savedColor = mCurrentPage.getFillColor();
                mCurrentPage.takeFillColor(null);
                dc.g.clip(node.getShape());
                clipped = true;
                // FYI: drawing the whole map when clipped is noticably slower
                // than just drawing the current page (esp. non-rectangular shapes)
            }
        }
        
        // draw the map or our current component, or it's parent
        if (mCurrentPage == null || !isPresenting()) {
            map.draw(dc);
        } else if (clipped /*|| (mCurrentPage instanceof LWNode && mCurrentPage.isTranslucent() && !mCurrentPage.hasLabel())*/ ) {
            mCurrentPage.getParent().draw(dc);
        } else {
            if (false&&mCurrentPage instanceof LWGroup) {
                LWGroup group = (LWGroup) mCurrentPage;
                group.drawChildren(dc);
            } else {
                mCurrentPage.draw(dc);
            }
        }

        if (oldColor != null)
            map.takeFillColor(oldColor);

        //-------------------------------------------------------
        // draw connected goto's (maybe do only if presenting?)
        //-------------------------------------------------------
        
        if (clipped) {
            mCurrentPage.takeFillColor(savedColor);
            dc.g.setClip(savedClip);
        }

        if (VUE.inFullScreen() && mShowNavigator)
            drawNavigatorMap(dc);
        
        //if (DEBUG.Enabled&&mCurrentPage != null)
        //drawNavNodes(dc);
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
            out("zoomToPage " + getCurrentPage());
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
    public boolean supportsDraggedSelector(java.awt.event.MouseEvent e) { return false; }
    public boolean hasDecorations() { return true; }
    public boolean usesRightClick() { return true; }

    
}