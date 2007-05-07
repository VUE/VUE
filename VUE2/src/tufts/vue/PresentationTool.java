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
import tufts.vue.gui.GUI;
import tufts.vue.gui.TextRow;

import java.util.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Dimension;
import java.awt.AlphaComposite;
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
    implements ActiveListener<LWPathway.Entry>
{
    enum Direction { FORWARD, BACKWARD };
    //private static int FORWARD = 1;
    //private static int BACKWARD = -1;

    private static final boolean RECORD_BACKUP = true;
    private static final boolean SKIP_BACKUP_RECORD = false;
    
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
    
    private boolean mShowNavNodes = false;

    private static class BackStack extends Stack<LWComponent> {
        public LWComponent peek() {
            return empty() ? null : super.peek();
        }
        public LWComponent push(LWComponent c) {
            // never allow null or the same item repeated on the top of the stack
            if (peek() != c && c != null)
                super.push(c);
            return c;
        }
        public LWComponent peekNode() {
            LWComponent c = peek();
            if (c instanceof LWSlide)
                return ((LWSlide)c).getSourceNode();
            else
                return c;
        }
    }

    
    private static PresentationTool singleton;
    
    private BackStack mBackList = new BackStack();

    private List<NavNode> mNavNodes = new java.util.ArrayList();

    private static final Font NavFont = new Font("SansSerif", Font.PLAIN, 16);
    private static final Color NavFillColor = new Color(154,154,154);
    //private static final Color NavFillColor = new Color(64,64,64,96);
    private static final Color NavStrokeColor = new Color(96,93,93);
    private static final Color NavTextColor = Color.darkGray;
    
    private class NavNode extends LWNode {
        
        final LWComponent destination;
        final LWPathway pathway;
        
        NavNode(LWComponent dest, LWPathway pathway)
        {
            super(null);

            if (dest == null)
                throw new IllegalArgumentException("destination can't be null");
            
            this.destination = dest;
            this.pathway = pathway;
            
            String label;

            if (pathway != null)
                label = pathway.getLabel();
            else
                label = destination.getDisplayLabel();
            
            if (label.length() > 20)
                label = label.substring(0,18) + "...";
            
            if (pathway == null && mBackList.peekNode() == destination)
                label = "BACK:" + label;
            //label = "    " + label + " ";
            setLabel(label);
            setFont(NavFont);
            setTextColor(NavTextColor);
            setFillColor(NavFillColor);
        
            if (pathway != null)
                setStrokeColor(pathway.getStrokeColor());
            else
                setStrokeColor(NavStrokeColor);
        
            setStrokeWidth(pathway == null ? 2 : 3);
            //setSyncSource(src); // TODO: these will never get GC'd, and will be updating for ever based on their source...
            //setShape(new RoundRectangle2D.Float(0,0, 10,10, 20,20)); // is default
            setAutoSized(false); 
            setSize(200, getHeight() + 6);
        }
        
        // force label at left (non-centered)                
        protected float relativeLabelX() { return -NavNodeX + 10; }

    }

    

    public PresentationTool() {
        super();
        if (singleton != null) 
            new Throwable("Warning: mulitple instances of " + this).printStackTrace();
        singleton = this;
        VUE.addActiveListener(LWPathway.Entry.class, this);
    }
    
    public void activeChanged(ActiveEvent<LWPathway.Entry> e) {
        if (isActive()) {
            // only do this if this is the active tool,
            // as we use the globally active viewer
            // when we change the page!
            if (!e.active.isPathway())
                setEntry(e.active, SKIP_BACKUP_RECORD);
        }
     }
    
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

    /** return the singleton instance of this class */
    public static PresentationTool getTool()
    {
        if (singleton == null) {
            new Throwable("Warning: PresentationTool.getTool: class not initialized by VUE").printStackTrace();
            //throw new IllegalStateException("NodeTool.getTool: class not initialized by VUE");
            new NodeTool();
        }
        return singleton;
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
        final int key = e.getKeyCode();
        final char k = e.getKeyChar();
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
        } else if (mShowNavigator && k == '+') {
            if (OverviewMapSizeIndex < OverviewMapScales.length-1)
                OverviewMapSizeIndex++;
            repaint();
        } else if (mShowNavigator && (k == '-' || k == '_')) { // allow "shift-minus" also
            if (OverviewMapSizeIndex > 0)
                OverviewMapSizeIndex--;
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
    
    private static float[] OverviewMapScales = {8, 6, 4, 3, 2.5f, 2};
    private static int OverviewMapSizeIndex = 2;
    private float mNavMapX, mNavMapY; // location of the overview navigator map
    private DrawContext mNavMapDC;

    
    /** Draw a ghosted panner */
    private void drawOverviewMap(DrawContext sourceDC)
    {
        //sourceDC.setRawDrawing();
        DrawContext dc = sourceDC.create();
        //dc.setRawDrawing();
        //final DrawContext dc = sourceDC;
        dc.setFrameDrawing();

        float overviewMapFraction = OverviewMapScales[OverviewMapSizeIndex];
        final Rectangle panner = new Rectangle(0,0,
                                               (int) (dc.frame.width / overviewMapFraction),
                                               (int) (dc.frame.height / overviewMapFraction));

        mNavMapX = dc.frame.width - panner.width;
        mNavMapY = dc.frame.height - panner.height;

        dc.g.translate(mNavMapX, mNavMapY);
        dc.setAlpha(0.3);
        // todo: black or white depending on brightess of the fill
        dc.g.setColor(Color.white);
        dc.g.fill(panner);
        //dc.setAlpha(1);

        dc.setAlpha(0.75);
        dc.g.clipRect(0,0, panner.width, panner.height);
        mNavMapDC = MapPanner.paintViewerIntoRectangle(dc.g, VUE.getActiveViewer(), panner);
    }

    public boolean handleMouseMoved(MapMouseEvent e)
    {
        boolean handled = false;
        if (DEBUG.PICK && mShowNavigator)
            handled = debugTrackNavMapMouseOver(e);

        boolean oldShowNav = mShowNavNodes;

        //int maxHeight = e.getViewer().getVisibleBounds().height;
        //if (e.getY() > maxHeight - 40) {
        if (e.getX() < 40) {
            //if (DEBUG.PRESENT) out("nav nodes on " + e.getY() + " max=" + maxHeight);
            mShowNavNodes = true;
        } else {
            if (mShowNavNodes) {
                if (e.getX() > 200)
                    mShowNavNodes = false;
            }
            //if (DEBUG.PRESENT) out("nav nodes off " + e.getY() + " max=" + maxHeight);
        }

        if (oldShowNav != mShowNavNodes)
            e.getViewer().repaint();

        return handled;
    }

    private boolean debugTrackNavMapMouseOver(MapMouseEvent e)
    {
        //out(mNavMapDC.g.getClipBounds().toString());
        //if (mNavMapDC.g.getClipBounds().contains(e.getPoint())) {
        if (e.getX() > mNavMapX && e.getY() > mNavMapY) {
            //out("over map at " + e.getPoint());
            Point2D.Float mapPoint = new Point2D.Float(e.getX() - mNavMapX,
                                                       e.getY() - mNavMapY);

            out(mNavMapDC.toString());
            out("over nav map rect at " +  mapPoint);

            mapPoint.x -= mNavMapDC.offsetX;
            mapPoint.y -= mNavMapDC.offsetY;
            mapPoint.x /= mNavMapDC.zoom;
            mapPoint.y /= mNavMapDC.zoom;
                
            out("over nav map at " +  mapPoint);
            PickContext pc = new PickContext(mapPoint.x, mapPoint.y);
            pc.root = e.getViewer().getMap();
            LWComponent hit = LWTraversal.PointPick.pick(pc);
            //out("hit=" + hit);
            if (hit != null)
                e.getViewer().setIndicated(hit);
            else
                e.getViewer().clearIndicated();
            return true;
        } else
            return false;
    }
    
    public boolean handleMousePressed(MapMouseEvent e)
    {
        for (NavNode nav : mNavNodes) {
            System.out.println("pickCheck " + nav + " point=" + e.getPoint() + " mapPoint=" + e.getMapPoint());
            if (nav.containsParentCoord(e.getX(), e.getY())) {
                System.out.println("HIT " + nav);
                if (mBackList.peek() == nav.destination || mBackList.peekNode() == nav.destination) {
                    backUp();
                } else if (nav.pathway != null) {
                     // jump to another pathway
                    setEntry(nav.pathway.getEntry(nav.pathway.firstIndexOf(nav.destination)));
                } else {
                    setPage(nav.destination);
                }
                return true;
            }
        }

        final LWComponent hit = e.getPicked();
        
        out("handleMousePressed " + e.paramString() + " hit on " + hit);
        if (hit != null && mCurrentPage != hit) {
            Collection linked = hit.getLinkEndPoints();
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
// followLink(hit, hit.getLinkTo((LWComponent) linked.get(0))); // NO LONGER A LIST...
                /*
                LWComponent linkingTo = 
                mLastFollowed = mCurrentPage.getLinkTo(linkingTo);
                setPage(linkingTo);
                */
            } else {
                setPage(hit);
            }
        } else if (mCurrentPage == hit && mEntry != null && mEntry.getFocal() != mCurrentPage) {
            backUp();
        }
        return true;
    }

    private void followLink(LWComponent src, LWLink link) {
        LWComponent linkingTo = link.getFarPoint(src);
        mLastFollowed = link;
        setPage(linkingTo);
    }

    private LWComponent currentNode() {
        if (mCurrentPage instanceof LWSlide)
            return mEntry == null ? null : mEntry.node;
        else
            return mCurrentPage;
    }

    public void startPresentation()
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

    private void backUp() {
       if (!mBackList.empty())
           setPage(VUE.getActiveViewer(), mBackList.pop(), SKIP_BACKUP_RECORD);
    }
    
    private void backPage() {
        if (mPathway != null && inCurrentPathway(mCurrentPage)) {
            setEntry(nextPathwayEntry(Direction.BACKWARD));
            //LWComponent prevPage = nextPathwayPage(Direction.BACKWARD);
            //LWComponent prevPage = null;
            //if (prevPage != null)
            //    setPage(prevPage);
        } else
            backUp();
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
        return null;
    }
        
    private LWComponent OLD_guessNextPage()
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
                if (link.getFarNavPoint(currentNode()) == null)
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
            nextPage = toFollow.getFarNavPoint(currentNode());
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
            
    private void setEntry(LWPathway.Entry e) {
        setEntry(e, RECORD_BACKUP);
    }
    
    private void setEntry(LWPathway.Entry e, boolean recordBackup)
    {
        out("setEntry " + e);
        if (e == null)
            return;
        mEntry = e;
        mPathway = e.pathway;
        setPage(VUE.getActiveViewer(), mEntry.getFocal(), recordBackup);
        VUE.setActive(LWPathway.Entry.class, this, mEntry);
    }

    private void setPage(LWComponent page) {
        setPage(VUE.getActiveViewer(), page, RECORD_BACKUP);
    }
    
    private void setPage(final MapViewer viewer, final LWComponent page, boolean recordBackup)
    {
        out("setPage " + page);

        if (page == null) // for now
            return;
        
        if (recordBackup)
            mBackList.push(mCurrentPage);

        mLastPage = mCurrentPage;
        mCurrentPage = page;
        mNextPage = null;
        mNavNodes.clear();
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
        VUE.getActiveViewer().loadFocal(page);
    }
    

    // todo: viewer & focal are already in the DrawContext!
    public boolean handleDraw(DrawContext dc, MapViewer viewer, LWComponent focal) {
        if (mShowNavigator)
            drawOverviewMap(dc);

        if (viewer instanceof tufts.vue.ui.SlideViewer) {
            // TODO: Will need a set of nav nodes per-viewer if this is to work in both
            // the main viewer and the slide viewer... for now, don't do them
            // if we're in the slide viewer.
            ;
        } else {
            if (mShowNavNodes) {
                dc.g.setComposite(AlphaComposite.Src);
                drawNavNodes(dc);
            }
        }

        
        if (DEBUG.PRESENT && DEBUG.CONTAINMENT) {
            dc.setFrameDrawing();
            //dc.g.translate(dc.frame.x, dc.frame.y);
            //dc.g.setFont(VueConstants.FixedFont);
            dc.g.setFont(new Font("Lucida Sans Typewriter", Font.BOLD, 12));
            //dc.g.setColor(new Color(128,128,128,192));
            dc.g.setColor(Color.gray);
            int y = 10;
            dc.g.drawString("Frame: " + tufts.Util.out(dc.frame), 10, y+=15);
            if (mEntry != null)         dc.g.drawString(mEntry.pathway.getDiagnosticLabel(), 10, y+=15);
            if (mCurrentPage != null)   dc.g.drawString("Page: " + mCurrentPage.getDiagnosticLabel(), 10, y+=15);
            if (mEntry != null)         dc.g.drawString(mEntry.toString(), 10, y+=15);
            dc.g.drawString("  Backup: " + mBackList.peek(), 10, y+=15);
            dc.g.drawString("BackNode: " + mBackList.peekNode(), 10, y+=15);
        }

        
        return false;
    }

    public DrawContext tweakDrawContext(DrawContext dc) {
        dc.setPresenting(true);
        if (false &&mShowNavNodes)
            dc.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f));
        return dc;
    }
        
    /*
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
    */
    
    /** either draws the entire map (previously zoomed to to something to focus on),
     * or a fully focused part of of it, where we only draw that item.
     */

    /*
    protected void drawFocal(final DrawContext dc, MapViewer viewer, final LWComponent focal)
    {
        final LWMap underlyingMap = focal.getMap();
        
        out("drawing focal " + focal + " in map " + underlyingMap);

        //dc.g.setColor(mFocal.getMap().getFillColor());
        //dc.g.fill(dc.g.getClipBounds());
        
        if (false && focal.isTranslucent() && focal != underlyingMap && focal instanceof LWGroup == false) { // groups not meant to operate transparently
            out("drawing clipped focal " + focal.getLocalShape());

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
    */

    private void drawNavNodes(DrawContext dc)
    {
        LWComponent node = currentNode();

        mNavNodes.clear();
        
        if (node == null || (node.getLinks().size() == 0 && node.getPathways().size() < 2))
            return;
        
        makeNavNodes(node, dc.getFrame());
        
        //dc = dc.create(); // just in case
        for (LWComponent c : mNavNodes) {
            dc.setFrameDrawing(); // reset the GC each time, as draw translate it to local coords each time
            c.draw(dc);
        }

    }

    private static final int NavNodeX = -10; // clip the left edge of the round-rect
    
    private void makeNavNodes(LWComponent node, Rectangle frame)
    {

        // always add the current pathway at the top
        if (node.inPathway(mPathway))
            mNavNodes.add(createNavNode(node, mPathway));
        
        for (LWPathway path : node.getPathways()) {
            if (path != mPathway)
                mNavNodes.add(createNavNode(node, path));
        }


        float x = NavNodeX, y = frame.y;
        for (NavNode nav : mNavNodes) {
            y += nav.getHeight() + 5;
            nav.setLocation(x, y);
        }
        
        if (!mNavNodes.isEmpty())
            y += 30;
        
        NavNode nav;
        for (LWLink link : node.getLinks()) {
            LWComponent farpoint = link.getFarNavPoint(node);
            if (farpoint != null) {
                if (false && link.hasLabel())
                    nav = createNavNode(link, null); // just need to set syncSource to the farpoint
                else
                    nav = createNavNode(farpoint, null);
                mNavNodes.add(nav);

                y += nav.getHeight() + 5;
                nav.setLocation(x, y);
            }
        }

        /*
        float spacePerNode = frame.width;
        if (mShowNavigator)
            spacePerNode -= (frame.width / OverviewMapFraction);
        spacePerNode /= mNavNodes.size();
        int cnt = 0;
        float x, y;
        //out("frame " + frame);
        for (LWComponent c : mNavNodes) {
            x = cnt * spacePerNode + spacePerNode / 2;
            x -= c.getWidth() / 2;
            y = frame.height - c.getHeight();
            c.setLocation(x, y);
            //out("location set to " + x + "," + y);
            cnt++;
        }
        */
    }

    private NavNode createNavNode(LWComponent src, LWPathway pathway) {
        return new NavNode(src, pathway);
    }
    
    /*
    private static final Font NavFont = new Font("SansSerif", Font.PLAIN, 16);
    private static final Color NavFillColor = new Color(154,154,154);
    //private static final Color NavFillColor = new Color(64,64,64,96);
    private static final Color NavStrokeColor = new Color(96,93,93);
    private static final Color NavTextColor = Color.darkGray;

    private NavNode createNavNode(LWComponent src, LWPathway pathway)
    {av
//         if (false) {
//             LWComponent c = src.duplicate();
//             c.setSyncSource(src);
//             return c;
//         }

        String label = src.getDisplayLabel();

        if (label.length() > 20)
            label = label.substring(0,18) + "...";

        if (pathway == null && mBackList.peekNode() == src)
            label = "BACK:" + label;
        //label = "    " + label + " ";

        final NavNode c = new NavNode(c, src, pathway);
        
        final LWNode c = new LWNode(label) {
                // force label at left (non-centered)                
                protected float relativeLabelX() { return -NavNodeX + 10; }
            };

        //LWComponent c = NodeTool.createTextNode(src.getDisplayLabel());
        c.setFont(NavFont);
        c.setTextColor(NavTextColor);
        c.setFillColor(NavFillColor);
        
        if (pathway != null)
            c.setStrokeColor(pathway.getStrokeColor());
        else
            c.setStrokeColor(NavStrokeColor);
        
        c.setStrokeWidth(2);
        c.setSyncSource(src); // TODO: these will never get GC'd, and will be updating for ever based on their source...
        c.setShape(new RoundRectangle2D.Float(0,0, 10,10, 20,20));
        c.setAutoSized(false); 
        c.setSize(200, c.getHeight() + 6);
        return c;
    }
    */
        
    
    public void XhandleFullScreen(boolean fullScreen) {
        // when entering or exiting full-screen, keep us zoomed
        // to current page.
        makeInvisible();
        if (getCurrentPage() != null) {
            zoomToPage(getCurrentPage(), false);
        }
        makeVisibleLater();
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