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

import java.util.List;
import java.util.Iterator;
import java.awt.Color;
import java.awt.event.*;
import javax.swing.*;

public class PresentationTool extends VueTool
{
    private JButton mStartButton;
    private LWComponent mCurrentPage;
    private LWComponent mNextPage;
    private LWComponent mLastPage;
    private LWLink mLastFollowed;

    private JCheckBox mNavigate = new JCheckBox("Show All");
    private JCheckBox mToBlack = new JCheckBox("Black");
    private JCheckBox mZoomLock = new JCheckBox("Lock 100%");

    private boolean mFadeEffect = true;
        
    public JPanel createToolPanel() {
        //JPanel p = super.createToolPanel();
        JPanel p = new JPanel();
        mStartButton = new JButton("Start");
        mToBlack.setSelected(true);
        add(p, mStartButton);
        add(p, mNavigate);
        add(p, mToBlack);
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
            VUE.getActiveViewer().repaint();
        } else
            super.actionPerformed(ae);
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
        } else if (k == 'f') {
            mFadeEffect = !mFadeEffect;
        } else if (k == 's' || k == 'n') {
            mNavigate.doClick();
        } else if (k == 'b') {
            mToBlack.doClick();
        } else
            return false;
        return true;
    }
    
    public LWComponent X_findComponentAt(LWMap map, float mapX, float mapY) {
        return map.findDeepestChildAt(mapX, mapY);
    }

    private boolean isPresenting() {
        return !mNavigate.isSelected();
    }
    
    public boolean handleMousePressed(MapMouseEvent e) {
        LWComponent hit = e.getMap().findDeepestChildAt(e.getMapX(), e.getMapY());
        out(this + " handleMousePressed " + e + " hit on " + hit);
        if (hit != null && mCurrentPage != hit) {
            List linked = hit.getLinkedComponents();
            if (mCurrentPage != null && mCurrentPage.getParent() == hit) {
                // hack for clicking back up to parent group when had click-zoomed child image
                setPage(hit);
            } else if (linked.size() == 1/* && isPresenting()*/) {
                // not good during nav: don't follow links during nav? could skip if link is mCurrentPage
                // problem is case of nav'd to a link, then click on edge of node to right of link,
                // where you'd want to go to that node, but instead if that's it's only link, it follows
                // the link back to where you came from.  This is not a likely usage case tho.
                setPage((LWComponent) linked.get(0));
            } else
                setPage(hit);
        }
        return true;
    }

    // need to handle this and have MapViewer allow interrupt, so clicks on
    // something selected don't pop a label edit!
    //public boolean handleMouseReleased(MapMouseEvent e)
    

    private void startPresentation()
    {
        out(this + " start");
        mNavigate.setSelected(false);
        if (VUE.getSelection().size() > 0)
            setPage(VUE.getSelection().first());
        else
            setPage((LWComponent) VUE.getActiveMap().getChildList().get(0));
        
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
        if (mLastPage != null)
            setPage(mLastPage);
    }
        

    private void forwardPage()
    {
        if (mNextPage != null) {
            if (mNextPage != mCurrentPage) {
                setPage(mNextPage);
                mNextPage = null;
                return;
            }
        }

        // todo: only bother with links that have component endpoints!
        List links = mCurrentPage.getLinks();
        LWLink toFollow = null;
        if (links.size() == 1) {
            toFollow = (LWLink) mCurrentPage.getLinks().get(0);
        } else {
            Iterator i = mCurrentPage.getLinks().iterator();
            
            while (i.hasNext()) {
                LWLink link = (LWLink) i.next();
                if (link.getFarTarget(mCurrentPage) == null)
                    continue; // if a link to nothing, ignore
                if (link != mLastFollowed) {
                    toFollow = link;
                    break;
                }
            }
        }
        if (toFollow != null) {
            mLastFollowed = toFollow;
            LWComponent endpoint = toFollow.getFarTarget(mCurrentPage);
            setPage(endpoint);
        }
    }

    public LWComponent getCurrentPage() {
        return mCurrentPage;
    }
            
    private void setPage(LWComponent page) {
        setPage(VUE.getActiveViewer(), page);
    }
    
    private boolean invisible = false;

    private void makeInvisible() {
        if (VueUtil.isMacPlatform() && VUE.inNativeFullScreen()) {
            out("makeInvisible");
            try {
                tufts.macosx.Screen.makeMainInvisible();
                invisible = true;
            } catch (Exception e) {
                System.err.println(e);
            }
        }
    }
        
    private void makeVisible() {
        if (VueUtil.isMacPlatform() && VUE.inNativeFullScreen()) {
            out("makeVisible");
            try {
                if (tufts.macosx.Screen.isMainInvisible())
                    tufts.macosx.Screen.fadeUpMainWindow();
                invisible = false;
            } catch (Exception e) {
                System.err.println(e);
            }
        }
    }

    private void makeVisibleLater() {
        if (VueUtil.isMacPlatform() && VUE.inNativeFullScreen()) {
            VUE.invokeAfterAWT(new Runnable() {
                    public void run() {
                        out("makeVisibleLater");
                        //if (invisible)
                        makeVisible();
                    }
                });
        }
    }

    private void setPage(final MapViewer viewer, final LWComponent page) {
        mLastPage = mCurrentPage;
        mCurrentPage = page;
        viewer.clearTip();
        // It case there was a tip visible, we need to make sure
        // we wait for it to finish clearing before we move on,
        // so we need to put the rest of this in the queue.
        // (if we don't do this, the screen fades out & comes
        // back before the map has panned)
        VUE.invokeAfterAWT(new Runnable() {
                public void run() {
                    if (mFadeEffect)
                        makeInvisible();
                    zoomToPage(page);
                    if (invisible)
                        makeVisibleLater();
                }
            });
    }

    private void zoomToPage(LWComponent page) {
        int margin = 32;
        if (page instanceof LWImage)
            margin = 0;
        if (page != null)
            ZoomTool.setZoomFitRegion(page.getBounds(), margin);
        
        //VUE.ZoomFitButton.doClick(1000); // works
        // below doesn't work because viewer.getVisibleSize() returning 0,0 before it's displayed!
        //ZoomTool.setZoomFitRegion(viewer, viewer.getMap().getBounds(), margin);
        //ZoomTool.setZoomFit();
    }

    public void handleDraw(DrawContext dc, LWMap map) {
        Color oldColor = null;
        if (mToBlack.isSelected()) {
            oldColor = map.getFillColor();
            map.takeFillColor(Color.black);
            dc.setBlackWhiteReversed(true);
        }

        dc.g.setColor(map.getFillColor());
        dc.g.fill(dc.g.getClipBounds());

        boolean clipped = false;
        if (mCurrentPage instanceof LWNode && isPresenting()) {
            LWNode node = (LWNode) mCurrentPage;
            if (node.isTransparent() && !node.hasLabel()) {
                dc.g.clip(node.getShape());
                clipped = true;
                // FYI: drawing the whole map when clipped is noticably slower
                // than just drawing the current page (esp. non-rectangular shapes)
            }
        }
        
        // draw the map or our current component
        if (clipped || mCurrentPage == null || !isPresenting() ||
            (mCurrentPage instanceof LWNode && mCurrentPage.isTransparent() && !mCurrentPage.hasLabel()))
            // for transparency: really, can just draw last page we clicked in from as well?
            map.draw(dc);
        else
            mCurrentPage.draw(dc);

        // draw connected goto's
        
        if (oldColor != null)
            map.takeFillColor(oldColor);
    }

    public void handleFullScreen(boolean fullScreen) {
        // when entering or exiting full-screen, keep us zoomed
        // to current page.
        makeInvisible();
        if (getCurrentPage() != null) {
            out("zoomToPage " + getCurrentPage());
            zoomToPage(getCurrentPage());
        }
        makeVisibleLater();
    }
    
    public void handleToolSelection() {
        out(this + " SELECTED");
        mCurrentPage = null;
        handleSelectionChange(VUE.getSelection());
        //mStartButton.requestFocus();
    }
    
    public void handleSelectionChange(LWSelection s) {
        if (s.size() == 1)
            mNextPage = s.first();
        else
            mNextPage = null;
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