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

import java.lang.*;
import java.util.*;
import javax.swing.*;
import java.awt.geom.Point2D;
import tufts.vue.beans.VueBeanState;

/**
 * VueTool for creating links.  Provides methods for creating default new links
 * based on the current state of the tools, as well as the handling drag-create
 * of new links.
 */

public class LinkTool extends VueTool
    implements VueConstants, LWPropertyProducer
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

    private static final Object LOCK = new Object();
    static LinkToolPanel getLinkToolPanel() {
        synchronized (LOCK) {
            if (sLinkContextualPanel == null)
                sLinkContextualPanel = new LinkToolPanel();
        }
        return sLinkContextualPanel;
    }

    /** LWPropertyProducer impl:
        @return LWKey.LinkCurves */
    final public Object getPropertyKey() { return LWKey.LinkCurves; }
    /** LWPropertyProducer impl:
        @return currently selected link tool curve count */
    public Object getPropertyValue() {
        return new Integer(getActiveSubTool().getCurveCount());
    }
    /** LWPropertyProducer impl: load the currently selected link tool to the one with given curve count */
    public void setPropertyValue(Object curveValue) {
        // Find the sub-tool with the matching curve-count, then load it's button icon images
        // into the displayed selection icon
        if (curveValue == null)
            return;
        Enumeration e = getSubToolIDs().elements();
        int curveCount = ((Integer)curveValue).intValue();
        while (e.hasMoreElements()) {
            String id = (String) e.nextElement();
            SubTool subtool = (SubTool) getSubTool(id);
            if (subtool.getCurveCount() == curveCount) {
                ((PaletteButton)mLinkedButton).setPropertiesFromItem(subtool.mLinkedButton);
                // call super.setSelectedSubTool to avoid firing the setters
                // as we're only LOADING the value here.
                super.setSelectedSubTool(subtool);
                break;
            }
        }
    }

    public void setSelectedSubTool(VueTool tool) {
        super.setSelectedSubTool(tool);
        if (VUE.getSelection().size() > 0) {
            SubTool subTool = (SubTool) tool;
            subTool.getSetterAction().fire(this);
        }
    }

    public SubTool getActiveSubTool() {
        return (SubTool) getSelectedSubTool();
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
            VueBeanState state = getLinkToolPanel().getCurrentState();
            if (state != null) {
            	state.applyState(creationLink);
            }
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
        if (DEBUG.CONTAINMENT) System.out.println("LINK-TARGET: " + over);
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
        //if (DEBUG.CONTAINMENT) System.out.println("findLWLinkTargetAt: directHit=" + directHit);
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
                pLinkDest = NodeTool.createNewNode();
                pLinkDest.setCenterAt(e.getMapPoint());
                commonParent.addChild(pLinkDest);
                createdNode = true;
            }

            LWLink link = new LWLink(pLinkSource, pLinkDest);

            // init link based on user defined state
            VueBeanState state = getLinkToolPanel().getCurrentState();
            if (state != null) {
                // override the curve count from the contextual tool state with
                // the state from the main link tool state.
                SubTool subTool = (SubTool) getSelectedSubTool();
                state.setPropertyValue(LWKey.LinkCurves, new Integer(subTool.getCurveCount()));
                state.applyState(link);

                // new ctrl points are on-center of curve: set ctrl pt off center a bit so can see curve
                //if (subTool.getCurveCount() > 0)
                //link.setCtrlPoint0(new Point2D.Float(link.getCenterX()-20, link.getCenterY()-10));
                
            }
            
            commonParent.addChild(link);
            // We ensure a paint sequence here because a link to a link
            // is currently drawn to it's center, which might paint over
            // a label.
            if (pLinkSource instanceof LWLink)
                commonParent.ensurePaintSequence(link, pLinkSource);
            if (pLinkDest instanceof LWLink)
                commonParent.ensurePaintSequence(link, pLinkDest);
            VUE.getSelection().setTo(link);
            e.getViewer().activateLabelEdit(createdNode ? pLinkDest : link);
        }
    }

    /**
     * VueTool class for each of the specifc link styles (straight, curved, etc).  Knows how to generate
     * an action for setting the shape.
     */
    public static class SubTool extends VueSimpleTool
    {
        private int curveCount = -1;
        private VueAction setterAction = null;
            
        public SubTool() {}

	public void setID(String pID) {
            super.setID(pID);
            try {
                curveCount = Integer.parseInt(getAttribute("curves"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public int getCurveCount() {
            return curveCount;
        }
        
        /** @return an action that will set the style of selected
         * LWLinks to the current link style for this SubTool */
        public VueAction getSetterAction() {
            if (setterAction == null) {
                setterAction = new Actions.LWCAction(getToolName(), getIcon()) {
                        void act(LWLink c) { c.setControlCount(curveCount); }
                    };
                setterAction.putValue("property.value", new Integer(curveCount)); // this may be handy
                // key is from: MenuButton.ValueKey
            }
            return setterAction;
        }
    }
    

}
