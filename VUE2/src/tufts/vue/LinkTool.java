package tufts.vue;

import java.lang.*;
import java.util.*;
import javax.swing.*;
import java.awt.geom.Point2D;
import tufts.vue.beans.VueBeanState;

public class LinkTool extends VueTool
    implements VueConstants
{
    /** link tool contextual tool panel **/
    private static LinkToolPanel sLinkContextualPanel;
    
    LWComponent linkSource; // for starting a link

    private final LWComponent invisibleLinkEndpoint = new LWComponent();
    private final LWLink creationLink = new LWLink(invisibleLinkEndpoint);

    public LinkTool()
    {
        super();
        invisibleLinkEndpoint.addLinkRef(creationLink);
        invisibleLinkEndpoint.setSize(0,0);
        //creationLink.setStrokeColor(java.awt.Color.blue);
    }
    
    public JPanel getContextualPanel() {
        return getLinkToolPanel();
    }

    static LinkToolPanel getLinkToolPanel() {
        if (sLinkContextualPanel == null)
            sLinkContextualPanel = new LinkToolPanel();
        return sLinkContextualPanel;
    }

    public boolean supportsSelection() { return true; }

    public boolean handleMousePressed(MapMouseEvent e)
    {
        //System.out.println(this + " handleMousePressed " + e);
        LWComponent hit = e.getHitComponent();
        if (hit instanceof LWGroup)
            hit = ((LWGroup)hit).findDeepestChildAt(e.getMapPoint());

        if (hit != null) {
            linkSource = hit;
            // todo: pick up current default stroke color & stroke width
            // and apply to creationLink
            creationLink.setTemporaryEndPoint1(linkSource);
            invisibleLinkEndpoint.setLocation(e.getMapPoint());
            e.setDragRequest(invisibleLinkEndpoint);
            // using a LINK as the dragComponent is a mess because geting the
            // "location" of a link isn't well defined if any end is tied
            // down, and so computing the relative movement of the link
            // doesn't work -- thus we just use this invisible endpoint
            // to move the link around.
            return true;
        }
        
        return false;
    }

    public boolean handleMouseDragged(MapMouseEvent e)
    {
        if (linkSource == null)
            return false;
        setMapIndicationIfOverValidTarget(linkSource, null, e);

        //-------------------------------------------------------
        // we're dragging a new link looking for an
        // allowable endpoint
        //-------------------------------------------------------
        return true;
    }

    static void setMapIndicationIfOverValidTarget(LWComponent linkSource, LWLink link, MapMouseEvent e)
    {
        LWComponent indication = e.getViewer().getIndication();
        LWComponent over = findLWLinkTargetAt(linkSource, link, e);
        if (DEBUG_CONTAINMENT) System.out.println("LINK-TARGET: " + over);
        if (indication != null && indication != over) {
            //repaintRegion.add(indication.getBounds());
            e.getViewer().clearIndicated();
        }
        if (over != null && isValidLinkTarget(linkSource, over)) {
            e.getViewer().setIndicated(over);
            //repaintRegion.add(over.getBounds());
        }
    }

    public boolean handleMouseReleased(MapMouseEvent e)
    {
        //System.out.println(this + " " + e + " linkSource=" + linkSource);
        if (linkSource == null)
            return false;

        //System.out.println("dx,dy=" + e.getDeltaPressX() + "," + e.getDeltaPressY());
        if (Math.abs(e.getDeltaPressX()) > 10 ||
            Math.abs(e.getDeltaPressY()) > 10) { // todo: config
            //repaintMapRegionAdjusted(creationLink.getBounds());
            LWComponent linkDest = e.getViewer().getIndication();
            if (linkDest != linkSource)
                makeLink(e, linkSource, linkDest);
        }
        this.linkSource = null;
        return true;
    }

    public void handleDragAbort()
    {
        this.linkSource = null;
    }

    public void handlePaint(DrawContext dc)
    {
        if (linkSource != null)
            creationLink.draw(dc);
    }
    
    private static LWComponent findLWLinkTargetAt(LWComponent linkSource, LWLink link, MapMouseEvent e)
    {
        float mapX = e.getMapX();
        float mapY = e.getMapY();
        LWComponent directHit = e.getMap().findDeepestChildAt(mapX, mapY, link);
        //if (DEBUG_CONTAINMENT) System.out.println("findLWLinkTargetAt: directHit=" + directHit);
        if (directHit != null && isValidLinkTarget(linkSource, directHit))
            return directHit;
        
        java.util.List targets = new java.util.ArrayList();
        java.util.Iterator i = e.getMap().getChildIterator();
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (c.targetContains(mapX, mapY) && isValidLinkTarget(linkSource, c))
                targets.add(c);
        }
        return e.getViewer().findClosestEdge(targets, mapX, mapY);
    }
    
    /**
     * Make sure we don't create any links back on themselves.
     *
     * @param linkSource -- LWComponent at far (anchor) end of
     * the link we're trying to find another endpoint for -- can
     * be null, meaning unattached.
     * @param linkTarget -- LWComponent at dragged end
     * the link we're looking for an endpoint with.
     * @return true if linkTarget is a valid link endpoint given our other end anchored at linkSource
     */
    static boolean isValidLinkTarget(LWComponent linkSource, LWComponent linkTarget)
    {
        if (linkTarget == linkSource && linkSource != null)
            return false;
        
        // don't allow links between parents & children
        if (linkSource != null) {
            if (linkTarget.getParent() == linkSource ||
                linkSource.getParent() == linkTarget)
                return false;
        }
        
        boolean ok = true;
        if (linkTarget instanceof LWLink) {
            LWLink lwl = (LWLink) linkTarget;
            ok &= (lwl.getComponent1() != linkSource &&
                   lwl.getComponent2() != linkSource);
        }
        if (linkSource instanceof LWLink) {
            LWLink lwl = (LWLink) linkSource;
            ok &= (lwl.getComponent1() != linkTarget &&
                   lwl.getComponent2() != linkTarget);
        }
        return ok;
    }
    
    
    
    public void drawSelector(java.awt.Graphics2D g, java.awt.Rectangle r)
    {
        //g.setXORMode(java.awt.Color.blue);
        g.setColor(java.awt.Color.blue);
        super.drawSelector(g, r);
    }

    //public static void makeLink(LWMap pMap, Point2D pDropLocation, LWComponent pLinkSource, LWComponent pLinkDest)
    private void makeLink(MapMouseEvent e, LWComponent pLinkSource, LWComponent pLinkDest)
    {
        LWLink existingLink = null;
        if (pLinkDest != null)
            existingLink = pLinkDest.getLinkTo(pLinkSource);
        if (existingLink != null) {
            // There's already a link tween these two -- increment the weight
            existingLink.incrementWeight();
        } else {
            LWContainer commonParent = e.getMap();
            if (pLinkDest == null)
                commonParent = pLinkSource.getParent();
            else if (pLinkSource.getParent() == pLinkDest.getParent() &&
                     pLinkSource.getParent() != commonParent) {
                // todo: if parents different, add to the upper most parent
                commonParent = pLinkSource.getParent();
            }
            boolean createdNode = false;
            if (pLinkDest == null) {
                // // some compiler bug is requiring that we fully qualify NodeTool here!
                pLinkDest = NodeTool.createNode("new node");
                pLinkDest.setCenterAt(e.getMapPoint());
                commonParent.addChild(pLinkDest);
                createdNode = true;
            }
            LWLink link = new LWLink(pLinkSource, pLinkDest);

            if (getSelectedSubTool().getID().equals("linkTool.curve")) {
                link.setControlCount(1);
                // new ctrl points are on-center of curve: set ctrl pt off center a bit so can see curve
                link.setCtrlPoint0(new Point2D.Float(link.getCenterX()-20, link.getCenterY()-10));
            }
            
            // init link based on user defined state
            VueBeanState state = getLinkToolPanel().getValue();
            if (state != null)
            	state.applyState(link);
            
            commonParent.addChild(link);
            // We ensure a paint sequence here because a link to a link
            // is currently drawn to it's center, which might paint over
            // a label.
            if (pLinkSource instanceof LWLink)
                commonParent.ensurePaintSequence(link, pLinkSource);
            if (pLinkDest instanceof LWLink)
                commonParent.ensurePaintSequence(link, pLinkDest);
            e.getViewer().activateLabelEdit(createdNode ? pLinkDest : link);
        }
    }

}
