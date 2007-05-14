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
 * Handles presentation mode key commands and screen decorations.
 *
 * @version $Revision: 1.418 $ / $Date: 2007/05/06 20:14:17 $ / $Author: sfraize $
 * @author Scott Fraize
 *
 */
public class PresentationTool extends VueTool
    implements ActiveListener<LWPathway.Entry>
{
    private static final String FORWARD = "FORWARD";
    private static final String BACKWARD = "BACKWARD";
    private static final boolean RECORD_BACKUP = true;
    private static final boolean BACKING_UP = false;
    
    private JButton mStartButton;
    private final JCheckBox mShowContext = new JCheckBox("Show Context");
    private final JCheckBox mToBlack = new JCheckBox("Black");
    private final JCheckBox mZoomLock = new JCheckBox("Lock 100%");
    
    private final Page NO_PAGE = new Page((LWComponent)null);
    
    private Page mCurrentPage = NO_PAGE;
    private LWPathway mPathway;
    private Page mLastPathwayPage;
    
    private LWComponent mNextPage; // is this really "startPage"?
    //private LWComponent mLastPage;
    //private LWLink mLastFollowed;
    //private int mPathwayIndex = 0;
    //private LWPathway.Entry mEntry;

    private boolean mFadeEffect = false;
    private boolean mShowNavigator = DEBUG.NAV;
    private boolean mShowNavNodes = false;
    private boolean mForceShowNavNodes = false;
    private boolean mScreenBlanked = false;

    /** a presentation moment (data for producing a single presentation screen) */
    private static class Page {
        final LWPathway.Entry entry;
        final LWComponent node;
        
        Page(LWPathway.Entry e) {
            entry = e;
            node = null;
        }
        Page(LWComponent c) {
            entry = null;
            node = c;
        }

        /** @return the object we're actually going to draw on screen for this presentation moment */
        public LWComponent getPresentationFocal() {
            return entry == null ? node : entry.getFocal();
        }


        /** @return the map node that is the source of this presentation item.
         * May return null if the this is a pathway entry that was a combo slide.
         */
        public LWComponent getOriginalMapNode() {
            if (node != null)
                return node;
            else if (entry != null)
                return entry.node;
            return null;
        }

        public boolean isMapView() {
            return entry == null ? true : entry.isMapView();
        }

        public boolean onPathway() {
            return entry != null;
        }

        public LWPathway pathway() {
            return entry == null ? null : entry.pathway;
        }

        public boolean inPathway(LWPathway p) {
            if (node != null)
                return node.inPathway(p);
            else if (entry != null)
                return entry.pathway == p;
            else
                return false;
        }

        public String getDisplayLabel() {
            if (node != null)
                return node.getDisplayLabel();
            else
                return entry.getLabel();
        }

        public String toString() {
            //String s = "PAGE: ";
            String s = "";
            if (node != null)
                s += node;
            else 
                s += entry;
            return s;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o instanceof Page) {
                final Page other = (Page) o;
                return entry == other.entry && node == other.node;
            } else if (o instanceof LWComponent) {
                final LWComponent c= (LWComponent) o;
                return node == c || (entry != null && entry.node == c);
            } else
                return false;
        }
    }

    /**

     * Implements a stack/queue that works like standard web browser forward/back page
     * visit lists: You can always go back in a straight line, and you can keep going
     * forward in a straight line until you push something else on the stack (browse off
     * the current already visited list), in which case everything forward of where you
     * are in the list is throw away.
     
     */
    private static class PageQueue<T> extends ArrayList<T>
    {
        int index;

        public boolean isEmpty() {
            // somehow, ArrayList is getting to a size == -1 condition,
            // and the default isEmpty checks size == 0, so it doesn't
            // think it's empty in that case!
            return size() <= 0;
        }
         	
        public void rollBack() {
            if (index > 0)
                index--;
            else
                throw new IndexOutOfBoundsException("index=" + index);
        }
        public void rollForward() {
            if (index < lastIndex())
                index++;
            else
                throw new IndexOutOfBoundsException("index=" + index + " lastIndex=" + lastIndex());
        }
        
        private final int lastIndex() {
            return size() - 1;
        }
        
        public T first() {
            return size() <= 0 ? null : get(0);
        }
        public T last() {
            return size() <= 0 ? null : get(lastIndex());
        }

        public T jumpFirst() {
            index = 0;
            return first();
        }
        public T jumpLast() {
            index = lastIndex();
            return last();
        }
        
        
        public void push(T o) {
            // never allow null in the stack, or the same item repeated on the top of the stack
            if (o == null)
                return;

            if (o.equals(next())) {
                rollForward();
            } else if (o.equals(current())) {
                ; // do nothing: never repeat items in the queue
            } else {
                if (!isEmpty() && index < lastIndex()) {
                    // toss out forward stack information whenever we push anything:
                    removeRange(index+1, size());
                }
                add(o);
                index = size() - 1;
            } 
        }
        
        public T current() {
            return isEmpty() ? null : get(index);
        }
        
        public boolean hasPrev() {
            return !isEmpty() && index > 0;
        }

        public boolean hasNext() {
            return index < lastIndex();
        }
        
        /**
         * @return the next item back in the queue without changing the queue position.
         * Will return null if hasPrev() returns false.
         */
        public T prev() {
            return hasPrev() ? get(index - 1) : null;
        }

        public T next() {
            return hasNext() ? get(index + 1) : null;
        }

        
        /** move the queue position back one, and return what's there */
        public T popPrev() {
            if (!hasPrev())
                throw new NullPointerException(this + " empty; can't pop");
            return get(--index);
        }

        public T popNext() {
            if (!hasNext())
                throw new NullPointerException(this + "index="+index + "; size="+size() + "; already at end");
            return get(++index);
        }
    }

    
    private static PresentationTool singleton;
    
    private final PageQueue<Page> mVisited = new PageQueue();
    
    private final List<NavNode> mNavNodes = new java.util.ArrayList();

    private static final Font NavFont = new Font("SansSerif", Font.PLAIN, 16);
    private static final Color NavFillColor = new Color(154,154,154);
    //private static final Color NavFillColor = new Color(64,64,64,96);
    private static final Color NavStrokeColor = new Color(96,93,93);
    private static final Color NavTextColor = Color.darkGray;
    
    private class NavNode extends LWNode {
        
        //final LWComponent destination;
        final Page page; // destination page
        //final LWPathway pathway;
        
        //NavNode(LWComponent destination, LWPathway pathway)
        NavNode(Page destinationPage)
        {
            super(null);

//             if (destination == null)
//                 throw new IllegalArgumentException("destination can't be null");
            
            //this.page = new Page(destination);
            this.page = destinationPage;
            //this.pathway = pathway;

            final LWPathway pathway;

            if (destinationPage.entry != null)
                pathway = destinationPage.entry.pathway;
            else
                pathway = null;
            
            String label;

            if (pathway != null)
                label = pathway.getLabel();
            else
                label = page.getDisplayLabel();

            if (pathway == null && destinationPage.equals(mVisited.prev()))
                label = "<- " + label;
            
            if (label.length() > 20)
                label = label.substring(0,18) + "...";
            
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
                setEntry(e.active, BACKING_UP);
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
        //out("repaint");
        VUE.getActiveViewer().repaint();
    }

    
    private boolean inCurrentPathway() {
        final Page p = mCurrentPage;
        if (p == null || p == NO_PAGE || mPathway == null)
            return false;

        return p.inPathway(mPathway);

        // This appears to be checking if we've descended into the contents of a pathway page...
        //|| (p.node != null && page.node.getParent() == mPathway); // page.node != entry.node !!
    }

    private void revisitPrior() { revisitPrior(false); }
    private void revisitPrior(boolean allTheWay) {
        if (allTheWay)
            setPage(mVisited.jumpFirst());
        else if (mVisited.hasPrev())
           setPage(mVisited.popPrev(), BACKING_UP);
    }
    
    private void revisitNext() { revisitNext(false); }
    private void revisitNext(boolean allTheWay) {
        if (allTheWay)
            setPage(mVisited.jumpLast());
        else if (mVisited.hasNext())
            setPage(mVisited.popNext(), BACKING_UP);
    }
    
    private void goBackward(boolean allTheWay)
    {
        if (inCurrentPathway()) {
            if (allTheWay) {
                // todo: skip to start of queue if been there instead
                // adding more to the end!
                setEntry(mPathway.getFirst());
            } else {
                setEntry(nextPathwayEntry(BACKWARD), BACKING_UP);
            }
        } else {
            revisitPrior(allTheWay);
        }
    }

    private static final boolean SINGLE_STEP = false;
    private static final boolean GUESSING = true;

    private void goForward(boolean allTheWay) { goForward(allTheWay, false); }
    private void goForward(boolean allTheWay, boolean guessing)
    {
        if (mCurrentPage == NO_PAGE && mNextPage == null) {
            
            startPresentation();
            
        } else if (inCurrentPathway()) {
            if (false && allTheWay) {
                // todo: skip to end of queue if been here instead
                // of blowing it away!
                // skipping far forward complicates the hell
                // out of maintaining a sane seeming backlist...
                setEntry(mPathway.getLast());
            } else {
                setEntry(nextPathwayEntry(FORWARD));
            }
        } else {
            if (guessing) {
                // if we hit space and there's nowhere to go, show them their options:
                mForceShowNavNodes = mShowNavNodes = true;
                repaint();
            } else
                revisitNext(allTheWay); 
        }

    }
    
    

    public boolean handleKeyPressed(java.awt.event.KeyEvent e) {
        //out("handleKeyPressed " + e);
        final int keyCode = e.getKeyCode();
        final char keyChar = e.getKeyChar();

        boolean handled = true;

        final boolean amplified = e.isShiftDown();

        switch (keyCode) {
        case KeyEvent.VK_SPACE:
            goForward(SINGLE_STEP, GUESSING);
            break;

        case KeyEvent.VK_BACK_QUOTE:
            // toggle showing the non-linear nav options:
            mForceShowNavNodes = mShowNavNodes = !mForceShowNavNodes;
            repaint();
            break;
            
        case KeyEvent.VK_DOWN:
            goForward(amplified);
            break;
        case KeyEvent.VK_UP:
            goBackward(amplified);
            break;
        case KeyEvent.VK_LEFT:
            revisitPrior(amplified);
            break;
        case KeyEvent.VK_RIGHT:
            if (mVisited.hasNext())
                revisitNext(amplified);
            else
                goForward(amplified);
            break;
            
        default:
            handled = false;
        }

        if (handled)
            return true;

        handled = true;

        switch (keyChar) {
            
//         case '1':
//             if (mPathway != null) {
//                 mPathwayIndex = 0;
//                 mNextPage = mPathway.getNodeEntry(0);
//                 forwardPage();
//             }
//             repaint();
//             break;
            
        case 'F':
            mFadeEffect = !mFadeEffect;
            break;
//         case 'C':
//         case 'N':
//             mShowContext.doClick();
//             break;
//         case 'B':
//             mToBlack.doClick();
//             break;
        case 'm':
            mShowNavigator = !mShowNavigator;
            repaint();
            break;
            
        case '+':
        case '=': // allow "non-shift-plus"
            if (mShowNavigator && OverviewMapSizeIndex < OverviewMapScales.length-1) {
                OverviewMapSizeIndex++;
                repaint();
            } else
                handled = false;
            break;
        case '-':
        case '_': // allow "shift-minus" also
            if (mShowNavigator && OverviewMapSizeIndex > 0) {
                OverviewMapSizeIndex--;
                repaint();
            } else
                handled = false;
            break;
        default:
            handled = false;
        }

        return handled;
    }

    public boolean handleKeyReleased(java.awt.event.KeyEvent e) {
        return false;
    }
    
    //private boolean isPresenting() { return !mShowContext.isSelected(); }
    
    private static float[] OverviewMapScales = {8, 6, 4, 3, 2.5f, 2, 1.5f, 1};
    //private static int OverviewMapSizeIndex = 2;
private static int OverviewMapSizeIndex = 5;
    private float mNavMapX, mNavMapY; // location of the overview navigator map
    private DrawContext mNavMapDC;

    
    /** Draw a ghosted panner */
    private void drawOverviewMap(DrawContext dc)
    {
        //sourceDC.setRawDrawing();
        //DrawContext dc = sourceDC.create();
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

        // todo: black or white depending on brightess of the fill
        dc.g.setColor(Color.gray);
        dc.g.fill(panner);

        // clip in case borders that might extend outside the panner if the fit is tight to the raw map shapes        
        dc.g.clipRect(0,0, panner.width, panner.height);
        
        //final LWComponent focused = mCurrentPage.isMapView() ? null : mCurrentPage.getOriginalMapNode();
        final LWComponent focused = mCurrentPage.getOriginalMapNode();


        final MapViewer viewer = VUE.getActiveViewer();
        
        dc.skipDraw = focused;
        mNavMapDC = MapPanner.paintViewerIntoRectangle(null,
                                                       dc.g.create(),
                                                       viewer,
                                                       panner,
                                                       false); //mCurrentPage.isMapView());

        if (focused != null) {
            dc.g.setColor(Color.black);
            dc.setAlpha(0.5);
            dc.g.fill(panner);
            
            mNavMapDC.setAntiAlias(true);
            
            if (false && mShowNavNodes) {
                // Also highlight connectd nav nodes if non-linear nav overlay is showing:
                for (NavNode nav : mNavNodes)
                    if (nav.page.node != null && nav.page.node != focused)
                        nav.page.node.draw(mNavMapDC.create());
            }

            if (mCurrentPage.isMapView()) {
                // redraw the reticle at full brightness:
                mNavMapDC.g.setColor(mCurrentPage.getPresentationFocal().getMap().getFillColor());
                mNavMapDC.setAlpha(0.333);
                mNavMapDC.g.fill(viewer.getVisibleMapBounds());
                mNavMapDC.setAlpha(1);
                mNavMapDC.g.setColor(Color.red);
                mNavMapDC.g.setStroke(VueConstants.STROKE_THREE);
                mNavMapDC.g.draw(viewer.getVisibleMapBounds());
            }
            
            focused.draw(mNavMapDC.create());
            
        }
            
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
            mForceShowNavNodes = false;
        } else {
            if (mShowNavNodes && !mForceShowNavNodes) {
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
            if (DEBUG.PRESENT) System.out.println("pickCheck " + nav + " point=" + e.getPoint() + " mapPoint=" + e.getMapPoint());
            if (nav.containsParentCoord(e.getX(), e.getY())) {
                if (DEBUG.PRESENT) System.out.println("HIT " + nav);
                //if (mVisited.peek() == nav.destination || mVisited.peekNode() == nav.destination) {
                if (nav.page.equals(mVisited.prev())) {
                    revisitPrior();
//                 } else if (nav.pathway != null) {
//                      // jump to another pathway
//                     setEntry(nav.pathway.getEntry(nav.pathway.firstIndexOf(nav.destination)));
                } else {
                    setPage(nav.page);
                }
                return true;
            }
        }

        final LWComponent hit = e.getPicked();
        
        if (DEBUG.PRESENT) out("handleMousePressed " + e.paramString() + " hit on " + hit);
        if (hit != null && !mCurrentPage.equals(hit)) {
            Collection linked = hit.getLinkEndPoints();
            if (mCurrentPage == NO_PAGE) {
                // We have no current page, so just zoom to what's been clicked on and start there.
                mVisited.clear();
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
        }
//         else if (mCurrentPage.equals(hit) && mEntry != null && mEntry.getFocal() != mCurrentPage) {
//             // what the hell we doing here???
//             revisitPrior();
//         }
        return true;
    }

//     private LWComponent currentNode() {
//         if (mCurrentPage instanceof LWSlide)
//             return mEntry == null ? null : mEntry.node;
//         else
//             return mCurrentPage;
//     }

    public void startPresentation()
    {
        out(this + " startPresentation");
        //mShowContext.setSelected(false);
        mVisited.clear();

        if (VUE.getActivePathway() != null && VUE.getActivePathway().length() > 0) {
            
            setEntry(VUE.getActivePathway().getEntry(0));
            
        } else {
            mPathway = null;
            //mPathwayIndex = 0;
            if (VUE.getSelection().size() > 0)
                setPage(VUE.getSelection().first());
//             else if (mCurrentPage != NO_PAGE) // won't have any effect!
//                 setPage(mCurrentPage);
//             else
//                 setPage(mNextPage);
        }
    }

    
    /** @param direction either 1 or -1 */
    private LWPathway.Entry nextPathwayEntry(String direction)
    {
        if (mLastPathwayPage == null)
            return null;

        int index = mLastPathwayPage.entry.index();

        if (direction == FORWARD)
            index++;
        else
            index--;

        return mLastPathwayPage.entry.pathway.getEntry(index);
        
        
        /*
        //out("nextPathwayEntry " + direction);
        if (direction == BACKWARD && mPathwayIndex == 0)
            return null;
        
        if (direction == FORWARD)
            mPathwayIndex++;
        else
            mPathwayIndex--;

        final LWPathway.Entry nextEntry = mPathway.getEntry(mPathwayIndex);
        //out("Next pathway index: #" + mPathwayIndex + " = " + nextEntry);

        if (nextEntry == null) {
            //nextEntry = mPathway.getFirstEntry(nextPathwayNode);
            mPathwayIndex -= (direction == FORWARD ? 1 : -1);
        }
        //out("Next pathway slide/focal: " + nextEntry);
        return nextEntry;
        */
        
    }
    
    //private LWComponent guessNextPage() { return null; }
        
    private void setEntry(LWPathway.Entry e) {
        setEntry(e, RECORD_BACKUP);
    }
    
    /** this will jump us to the pathway for the entry, and set us viewing the given entry */
    private void setEntry(LWPathway.Entry entry, boolean recordBackup)
    {
        //out("setEntry " + entry);
        if (entry == null)
            return;
        //mEntry = e;
        //mPathway = entry.pathway;
        //mPathwayIndex = entry.index();
        setPage(new Page(entry), recordBackup);
        //setPage(new Page(mEntry), recordBackup);
        //setPage(mEntry.getFocal(), recordBackup);
        //VUE.setActive(LWPathway.Entry.class, this, entry);
    }

    private void setPage(LWComponent destination) {
        if (destination != null)
            setPage(new Page(destination), RECORD_BACKUP);
    }
    
    private void setPage(Page page) {
        setPage(page, RECORD_BACKUP);
    }

    private void setPage(final Page page, boolean recordBackup)
    {
        final MapViewer viewer = VUE.getActiveViewer();
        
        if (DEBUG.PRESENT) out("setPage " + page);

        if (page == null) // for now
            return;
        
        if (recordBackup) {
            mVisited.push(page);
        } else {
            // we're backing up:
            //System.out.println("\nCOMPARING:\n\t" + page + "\n\t" + mVisited.prev());
            if (page.equals(mVisited.prev()))
                mVisited.rollBack();
        }

        //mLastPage = mCurrentPage;
        mCurrentPage = page;

        if (page.onPathway()) {
            if (page.pathway() != mPathway)
                mPathway = page.pathway();
            VUE.setActive(LWPathway.Entry.class, this, page.entry);
            mLastPathwayPage = page;
            //mPathwayIndex = page.entry.index();
        }
        
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
                        zoomToFocal(page.getPresentationFocal(), !mFadeEffect);
                        if (mScreenBlanked)
                            makeVisibleLater();
                    }
                });
            
        } else {
            zoomToFocal(page.getPresentationFocal(), true);
        }
    }
    
    private void zoomToFocal(LWComponent focal, boolean animate) {
        VUE.getActiveViewer().loadFocal(focal);
    }
    

    @Override public void handlePreDraw(DrawContext dc, MapViewer viewer) {
        if (mPathway != null) {
            if (mCurrentPage != null) {
                final LWSlide master = mPathway.getMasterSlide();
                boolean drawMaster = false;
                if (mCurrentPage.entry != null) {
                    dc.fill(mCurrentPage.entry.getFullScreenFillColor());
                    if (mCurrentPage.entry.isMapView())
                        drawMaster = true;
                } else {
                    dc.fill(master.getFillColor());
                    // current page has no entry: we've navigated off pathway
                    // onto an arbitrary map node:
                    drawMaster = false;
                }
                if (drawMaster) {
                    master.drawIntoFrame(dc);
                    /*
                    dc = dc.create();
                    dc.setRawDrawing();
                    final Point2D.Float offset = new Point2D.Float();
                    final double zoom = ZoomTool.computeZoomFit(dc.frame.getSize(), 0, master.getBounds(), offset);
                    dc.g.translate(-offset.x, -offset.y);
                    dc.g.scale(zoom,zoom);
                    if (DEBUG.BOXES || DEBUG.PRESENT || DEBUG.CONTAINMENT) {
                        dc.g.setColor(Color.green);
                        dc.g.setStroke(VueConstants.STROKE_TWO);
                        dc.g.draw(master.getLocalShape());
                    }
                    master.draw(dc);
                    */
                }
            }
        }
    }

    /*
    public static void drawRawMasterSlideInto(DrawContext dc, LWSlide master)
    {
        dc = dc.create();
        dc.setFrameDrawing();
        final Point2D.Float offset = new Point2D.Float();
        final double zoom = ZoomTool.computeZoomFit(dc.frame.getSize(), 0, master.getBounds(), offset);
        dc.g.translate(-offset.x, -offset.y);
        dc.g.scale(zoom, zoom);
        if (DEBUG.BOXES || DEBUG.PRESENT || DEBUG.CONTAINMENT) {
            dc.g.setColor(Color.green);
            dc.g.setStroke(VueConstants.STROKE_TWO);
            dc.g.draw(master.getLocalShape());
        }
        master.draw(dc);
    }
    */

    
    
    @Override
    public void handlePostDraw(DrawContext dc, MapViewer viewer)
    {
        if (mShowNavNodes) {
            
            // TODO: Will need a set of nav nodes per-viewer if this is to work in both
            // the main viewer and the slide viewer... for now, the slide viewer ignores
            // all drawing effects of the active tool, so we don't have to worry about
            // it.
        
            //dc.g.setComposite(AlphaComposite.Src);
            drawNavNodes(dc.create());
        }


        // Be sure to draw the navigator after the nav nodes, as navigator
        // display can depend on the current nav nodes, which are created
        // at draw time.
        if (mShowNavigator)
            drawOverviewMap(dc.create());

        
        if (DEBUG.NAV) {
            dc.setFrameDrawing();
            //dc.g.translate(dc.frame.x, dc.frame.y);
            //dc.g.setFont(VueConstants.FixedFont);
            dc.g.setFont(new Font("Lucida Sans Typewriter", Font.BOLD, 10));
            //dc.g.setColor(new Color(128,128,128,192));
            dc.g.setColor(Color.gray);
            int y = 10;
            dc.g.drawString("  Frame: " + tufts.Util.out(dc.frame), 10, y+=15);
            dc.g.drawString("Pathway: " + mPathway, 10, y+=15);
            dc.g.drawString("   Page: " + mCurrentPage, 10, y+=15);
            dc.g.drawString("  Focal: " + mCurrentPage.getPresentationFocal(), 10, y+=15);
            dc.g.drawString(" OnPath: " + inCurrentPathway(), 10, y+=15);
            y+=5;

            dc.g.setFont(new Font("Lucida Sans Typewriter", Font.BOLD, 10));
            int i = 0;
            for (Page p : mVisited) {
                if (p == mVisited.current())
                    dc.g.setColor(Color.green);
                else
                    dc.g.setColor(Color.gray);
                dc.g.drawString(String.format("%2d %s", i, p), 10, y+=15);
                i++;
            }
            
            //dc.g.drawString("  Backup: " + mVisited.peek(), 10, y+=15);
            //dc.g.drawString("BackNode: " + mVisited.peekNode(), 10, y+=15);
        }
    }


    private void drawNavNodes(DrawContext dc)
    {
        LWComponent node = mCurrentPage.getOriginalMapNode();

        mNavNodes.clear();
        
        if (node == null || (node.getLinks().size() == 0 && node.getPathways().size() < 2))
            return;
        
        makeNavNodes(mCurrentPage, dc.getFrame());

        for (LWComponent c : mNavNodes) {
            dc.setFrameDrawing(); // reset the GC each time, as draw translate it to local coords each time
            c.draw(dc);
        }
    }

    private static final int NavNodeX = -10; // clip the left edge of the round-rect
    
    private void makeNavNodes(Page page, Rectangle frame)
    {
        // always add the current pathway at the top
        //if (node.inPathway(mPathway))
        if (page.inPathway(mPathway))
            mNavNodes.add(createNavNode(page));

        final LWComponent mapNode = page.getOriginalMapNode();
        
        for (LWPathway otherPath : mapNode.getPathways()) {
            if (otherPath != mPathway)
                mNavNodes.add(createNavNode(new Page(otherPath.getFirstEntry(mapNode))));
        }


        float x = NavNodeX, y = frame.y;

        if (DEBUG.NAV) {
            // make room for diagnostics:
            y += 200;
        }

        
        for (NavNode nav : mNavNodes) {
            y += nav.getHeight() + 5;
            nav.setLocation(x, y);
        }
        
        if (!mNavNodes.isEmpty())
            y += 30;

        
        
        
        NavNode nav;
        for (LWLink link : mapNode.getLinks()) {
            LWComponent farpoint = link.getFarNavPoint(mapNode);
            if (farpoint != null) {
                if (false && link.hasLabel())
                    nav = createNavNode(new Page(link));
                //nav = createNavNode(link, null); // just need to set syncSource to the farpoint
                else
                    nav = createNavNode(new Page(farpoint));
                //nav = createNavNode(farpoint, null);
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

    private NavNode createNavNode(Page page) {
        return new NavNode(page);
    }
//     private NavNode createNavNode(LWComponent src, LWPathway pathway) {
//         return new NavNode(src, pathway);
//     }
    
    

    public DrawContext tweakDrawContext(DrawContext dc) {
        dc.setPresenting(true);
        if (false &&mShowNavNodes)
            dc.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.5f));
        return dc;
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
        mCurrentPage = NO_PAGE;
        if (VUE.getSelection().size() == 1)
            mNextPage = VUE.getSelection().first();
        else
            mNextPage = null;

        //handleSelectionChange(VUE.getSelection());
        //mStartButton.requestFocus();
    }

    /*
    public void handleSelectionChange(LWSelection s) {
        out("SELECTION CHANGE");
        // TODO: if active map changes, need to be sure to clear current page!
        if (s.size() == 1)
            mCurrentPage = s.first();
        
//         if (s.size() == 1)
//             mNextPage = s.first();
//         else
//             mNextPage = null;

    }
    */

//     private void followLink(LWComponent src, LWLink link) {
//         LWComponent linkingTo = link.getFarPoint(src);
//         mLastFollowed = link;
//         setPage(linkingTo);
//     }

    
    /*
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
        if (nextPage == null && !mVisited.isEmpty() && mVisited.peek() != mCurrentPage)
            nextPage = mVisited.peek();

        return nextPage;
    }
    */

    
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