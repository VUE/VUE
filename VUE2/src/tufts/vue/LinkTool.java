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

package tufts.vue;

import java.util.*;
import java.awt.event.KeyEvent;
import javax.swing.*;

import edu.tufts.vue.ontology.ui.OntologySelectionEvent;
import tufts.vue.NodeTool.NodeModeTool;
import java.awt.geom.Point2D;
//import tufts.vue.beans.VueBeanState;

/**
 * VueTool for creating links.  Provides methods for creating default new links
 * based on the current state of the tools, as well as the handling drag-create
 * of new links.
 */

public class LinkTool extends VueTool
    implements VueConstants//, LWEditor
{
    /** link tool contextual tool panel **/
//    private static LinkToolPanel sLinkContextualPanel;
    private static LinkTool singleton = null;
    

    public LinkTool()
    {
        super();
        //creationLink.setStrokeColor(java.awt.Color.blue);
        if (singleton != null) 
            new Throwable("Warning: mulitple instances of " + this).printStackTrace();
        singleton = this;
        VueToolUtils.setToolProperties(this,"linkTool");
    }
    
    /** return the singleton instance of this class */
    public static LinkTool getTool()
    {
        if (singleton == null) {
         //   new Throwable("Warning: LinkTool.getTool: class not initialized by VUE").printStackTrace();
            new LinkTool();
        }
        return singleton;
    }
    
    /** @return an array of actions, with icon set, that will set the shape of selected
     * LinkTools */
    public Action[] getSetterActions() {
        Action[] actions = new Action[getSubToolIDs().size()];
        Enumeration e = getSubToolIDs().elements();
        int i = 0;
        while (e.hasMoreElements()) {
            String id = (String) e.nextElement();
            LinkTool.SubTool nt = (LinkTool.SubTool) getSubTool(id);
            actions[i++] = nt.getSetterAction();
        }
        return actions;
    }
    
    public JPanel getContextualPanel() {
        return null;//getLinkToolPanel();
    }

    /** @return LWLink.class */
    @Override
    public Class getSelectionType() { return LWLink.class; }

    private static final Object LOCK = new Object();
  /*  static LinkToolPanel getLinkToolPanel() {
        synchronized (LOCK) {
            if (sLinkContextualPanel == null)
                sLinkContextualPanel = new LinkToolPanel();
        }
        return sLinkContextualPanel;
    }
*/
    /*

    final public Object getPropertyKey() { return LWKey.LinkShape; }
    public Object produceValue() {
        return new Integer(getActiveSubTool().getCurveCount());
    }
    /** LWPropertyProducer impl: load the currently selected link tool to the one with given curve count 
    public void displayValue(Object curveValue) {
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
    */

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

   

    static void setMapIndicationIfOverValidTarget(LWComponent linkSource, LWLink link, MapMouseEvent e)
    {
        final MapViewer viewer = e.getViewer();
        final LWComponent over = pickLinkTarget(link, e);
        
        if (DEBUG.CONTAINMENT || DEBUG.PICK || DEBUG.LINK)
            System.out.println("LINK-TARGET-POTENTIAL: " + over + "; src=" + linkSource + "; link=" + link);
        
        if (over != null && isValidLinkTarget(link, linkSource, over)) {
            viewer.setIndicated(over);
        } else {
            final LWComponent currentIndication = viewer.getIndication();
            if (currentIndication != null && currentIndication != over)
                viewer.clearIndicated();
        }
        

    }

    private static LWComponent pickLinkTarget(LWLink link, MapMouseEvent e)
    {
        PickContext pc = e.getViewer().getPickContext(e.getMapX(), e.getMapY());
        pc.excluded = link; // this will override default focal exclusion: check manually below
        final LWComponent hit = LWTraversal.PointPick.pick(pc);
        if (hit == e.getViewer().getFocal())
            return null;
        else
            return hit;
    }
    

    private static void reject(LWComponent target, String reason) {
        System.out.println("LinkTool; rejected: " + reason + "; " + target);
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
    static boolean isValidLinkTarget(LWLink link, LWComponent linkSource, LWComponent linkTarget)
    {
        if (linkTarget == linkSource && linkSource != null) {
            if (DEBUG.LINK) reject(linkTarget, "source == target");
            return false;
        }
        // TODO: allow loop-back link if it's a CURVED link...

        // don't allow links between parents & children
        if (linkSource != null) {
            if (linkTarget.getParent() == linkSource ||
                linkSource.getParent() == linkTarget) {
                if (DEBUG.LINK) reject(linkTarget, "parent-child link");
                return false;
            }
            if (linkTarget != null) {
                if (!linkSource.canLinkTo(linkTarget)) {
                    if (DEBUG.LINK) reject(linkTarget, "source denies target; src=" + linkSource);
                    return false;
                }
            }
        }

        if (link != null && linkTarget == link.getParent()) {
            // if a link is inside something, don't link to it (?)
            if (DEBUG.LINK) reject(linkTarget, "target is parent of the new link");
            return false;
        }

        if (link != null && linkTarget instanceof LWLink && linkTarget.isConnectedTo(link)) {
            // Don't permit a link to link back to a link that's connected to it.
            if (DEBUG.LINK) reject(linkTarget, "this link already connected to target");
            return false;
        }


// New code, tho we don't actually need these constraints:
//         if (linkTarget instanceof LWLink) {
//             if (DEBUG.LINK) outln("target is link: " + linkTarget);
//             if (linkTarget.isConnectedTo(linkSource)) {
//                 if (DEBUG.LINK) reject(linkTarget, "target link-to-link loop");
//                 return false;
//             } else if (link != null && linkTarget.isConnectedTo(link)) {
//                 if (DEBUG.LINK) reject(linkTarget, "this link already connected to target");
//                 return false;
//             }
//         }
    
//         if (linkSource instanceof LWLink) {
//             if (linkSource.isConnectedTo(linkTarget)) {
//                 if (DEBUG.LINK) reject(linkTarget, "source link-to-link loop; src=" + linkSource);
//                 return false;
//             }// else if (link != null
//         }


        return true;


// The old code:         
//         boolean ok = true;
//         if (linkTarget instanceof LWLink) {
//             LWLink target = (LWLink) linkTarget;
//             ok &= (target.getHead() != linkSource &&
//                    target.getTail() != linkSource);
//         }
//         if (linkSource instanceof LWLink) {
//             LWLink source = (LWLink) linkSource;
//             ok &= (source.getHead() != linkTarget &&
//                    source.getTail() != linkTarget);
//         }
//         return ok;
    }
    
    
    
    public void drawSelector(DrawContext dc, java.awt.Rectangle r)
    {
        //g.setXORMode(java.awt.Color.blue);
        dc.g.setColor(java.awt.Color.blue);
        super.drawSelector(dc, r);
    }


    /*
    private void makeLink(MapMouseEvent e,
                          LWComponent pLinkSource,
                          LWComponent pLinkDest,
                          boolean pMakeConnection)
    {
        LWLink existingLink = null;
        if (pLinkDest != null)
            existingLink = pLinkDest.getLinkTo(pLinkSource);
        if (false && existingLink != null) {
            // There's already a link tween these two -- increment the weight
            // [ WE NOW ALLOW MULTIPLE LINKS BETWEEN NODES ]
            existingLink.incrementWeight();
        } else {

            // TODO: don't create new node at end of new link inside
            // parent of source node (e.g., content view/traditional node)
            // unless mouse is over that node!  (E.g., should be able to
            // drag link out from a node that is a child)
            
            LWContainer commonParent = e.getMap();
            if (pLinkDest == null)
                commonParent = pLinkSource.getParent();
            else if (pLinkSource.getParent() == pLinkDest.getParent() &&
                     pLinkSource.getParent() != commonParent) {
                // todo: if parents different, add to the upper most parent
                commonParent = pLinkSource.getParent();
            }
            boolean createdNode = false;
            if (pLinkDest == null && pMakeConnection) {
                pLinkDest = NodeModeTool.createNewNode();
                pLinkDest.setCenterAt(e.getMapPoint());
                commonParent.addChild(pLinkDest);
                createdNode = true;
            }

            LWLink link;
            if (pMakeConnection) {
                link = new LWLink(pLinkSource, pLinkDest);
            } else {
                link = new LWLink(pLinkSource, null);
                link.setTailPoint(e.getMapPoint()); // set to drop location
            }
            
            commonParent.addChild(link);
            // We ensure a paint sequence here because a link to a link
            // is currently drawn to it's center, which might paint over
            // a label.
            if (pLinkSource instanceof LWLink)
                commonParent.ensurePaintSequence(link, pLinkSource);
            if (pLinkDest instanceof LWLink)
                commonParent.ensurePaintSequence(link, pLinkDest);
            //VUE.getSelection().setTo(link);
            if (pMakeConnection)
                e.getViewer().activateLabelEdit(createdNode ? pLinkDest : link);
        }
    }
    */

    static class ComboModeTool extends LinkModeTool
    {
    	public ComboModeTool()
    	{
    		super();
    		setComboMode(true);
//    		 Mac overrides CONTROL-MOUSE to look like right-click (context menu popup) so we can't
            // use CTRL wih mouse drag on a mac.
            setActiveWhileDownKeyCode(KeyEvent.VK_ALT);
    	}
    }
    
    public static class OntologyLinkModeTool extends LinkModeTool implements edu.tufts.vue.ontology.ui.OntologySelectionListener
    {
    //	private final LWComponent invisibleLinkEndpoint = new LWComponent();
     //   private LWLink creationLink = new LWLink(invisibleLinkEndpoint);
    	
        public OntologyLinkModeTool()
    	{
        	edu.tufts.vue.ontology.ui.OntologyBrowser.getBrowser().addOntologySelectionListener(this);
        	//creationLink.setID("<creationLink>"); // can't use label or it will draw one         
        	//invisibleLinkEndpoint.addLinkRef(creationLink);
        	creationLink=null;
            invisibleLinkEndpoint.setSize(0,0);
            
    	}

        public boolean handleMousePressed(MapMouseEvent e)
        {
        	if (creationLink == null)
        	{
        		VueUtil.alert(VueResources.getString("ontologyLinkError.message"), VueResources.getString("ontologyLinkError.title"));
        		return true;
        	}
        	else
        		return false;
        }
        
        @Override
        public boolean handleMouseReleased(MapMouseEvent e)
        {
            //System.out.println(this + " " + e + " linkSource=" + linkSource);
            if (linkSource == null)
                return false;

            //System.out.println("dx,dy=" + e.getDeltaPressX() + "," + e.getDeltaPressY());
            if (Math.abs(e.getDeltaPressX()) > 10 ||
                Math.abs(e.getDeltaPressY()) > 10)  // todo: config min dragout distance
    	        {
    	            //repaintMapRegionAdjusted(creationLink.getBounds());
    	            LWComponent linkDest = e.getViewer().getIndication();
    	            if (linkDest != linkSource)
    	                makeLink(e, linkSource, linkDest, !e.isShiftDown(),false);
    	        }
            this.linkSource = null;
            return true;
        }
    	   
        
        private void makeLink(MapMouseEvent e,
                LWComponent pLinkSource,
                LWComponent pLinkDest,
                boolean pMakeConnection,
                boolean comboMode)
        {
        	int existingLinks = 0;
        	int existingCurvedLinks = 0;
        	if (pLinkDest != null) {
        		existingLinks = pLinkDest.countLinksTo(pLinkSource);
        		existingCurvedLinks = pLinkDest.countCurvedLinksTo(pLinkSource);
        	}
        	final int existingStraightLinks = existingLinks - existingCurvedLinks;

        	// TODO: don't create new node at end of new link inside
        	// parent of source node (e.g., content view/traditional node)
        	// unless mouse is over that node!  (E.g., should be able to
        	// drag link out from a node that is a child)
      
        	LWContainer commonParent = e.getMap();
        	if (pLinkDest == null)
        		commonParent = pLinkSource.getParent();
        	else if (pLinkSource.getParent() == pLinkDest.getParent() &&
        			pLinkSource.getParent() != commonParent) {
        	// todo: if parents different, add to the upper most parent
        		commonParent = pLinkSource.getParent();
        	}
        	boolean createdNode = false;
        	if (pLinkDest == null && (pMakeConnection && comboMode)) {
        		pLinkDest = NodeModeTool.createNewNode();
        		pLinkDest.setCenterAt(e.getMapPoint());
        		commonParent.addChild(pLinkDest);
        		createdNode = true;
        	}

        	LWLink link;
        	if (pMakeConnection && (comboMode || pLinkDest!=null)) {
        		//link = new LWLink(pLinkSource, pLinkDest);
        		link = (LWLink)creationLink.duplicate();
        		link.setHead(pLinkSource);
        		link.setTail(pLinkDest);
        		if (existingStraightLinks > 0)
        			link.setControlCount(1);
        	} else {
        		link = (LWLink)creationLink.duplicate();//new LWLink(pLinkSource, null);
        		link.setTailPoint(e.getMapPoint()); // set to drop location
        	}
   //     	EditorManager.targetAndApplyCurrentProperties(link);
      
        	commonParent.addChild(link);
        	// 	We ensure a paint sequence here because a link to a link
        	// is currently drawn to it's center, which might paint over
        	// a label.
        	if (pLinkSource instanceof LWLink)
        		commonParent.ensurePaintSequence(link, pLinkSource);
        	if (pLinkDest instanceof LWLink)
        		commonParent.ensurePaintSequence(link, pLinkDest);
        	VUE.getSelection().setTo(link);
        	if (pMakeConnection && comboMode)
        		e.getViewer().activateLabelEdit(createdNode ? pLinkDest : link);

        }
        @Override
        public boolean handleComponentPressed(MapMouseEvent e)
        {
            //System.out.println(this + " handleMousePressed " + e);
            LWComponent hit = e.getPicked();
            // TODO: handle LWGroup picking
            //if (hit instanceof LWGroup)
            //hit = ((LWGroup)hit).findDeepestChildAt(e.getMapPoint());

            if (hit != null && hit.canLinkTo(null)) {
                linkSource = hit;
                // todo: pick up current default stroke color & stroke width
                // and apply to creationLink
                creationLink.setTemporaryEndPoint1(linkSource);
          //      EditorManager.applyCurrentProperties(creationLink);
                // never let drawn creator link get less than 1 pixel wide on-screen
                float minStrokeWidth = (float) (1 / e.getViewer().getZoomFactor());
                if (creationLink.getStrokeWidth() < minStrokeWidth)
                    creationLink.setStrokeWidth(minStrokeWidth);
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
        edu.tufts.vue.ontology.ui.TypeList list = null;
        
        public void ontologySelected(OntologySelectionEvent e) {
			edu.tufts.vue.ontology.ui.TypeList l = e.getSelection();
			
			if (list != l)
				creationLink=null;
			
			list = l;
			
			LWComponent c = l.getSelectedComponent();
			if (c != null)
			{
				if (c instanceof LWLink)
				{
					creationLink = (LWLink)c;
					
				//	creationLink.setID("<creationLink>"); // can't use label or it will draw one
					invisibleLinkEndpoint.addLinkRef(creationLink);
		            invisibleLinkEndpoint.setSize(0,0);
		            creationLink.setTail(invisibleLinkEndpoint);//		          
		        	
				}
			}						
		}
    }
    
    static class LinkModeTool extends VueTool
    {
    	private boolean comboMode = false;
        protected LWComponent linkSource; // for starting a link
        
        protected final LWComponent invisibleLinkEndpoint = new LWComponent() {
                @Override
                public void setMapLocation(double x, double y) {
                    super.takeLocation((float)x, (float)y);
                    creationLink.notifyEndpointMoved(null, this);
                }
            };
        protected LWLink creationLink = new LWLink(invisibleLinkEndpoint);

        public LinkModeTool()
        {
            super();
            invisibleLinkEndpoint.takeSize(0,0);
            invisibleLinkEndpoint.setID("<invisibleLinkEndpoint>"); // can't use label or it will have a size > 0, offsetting the creationLink endpoint
            creationLink.setArrowState(LWLink.ARROW_TAIL);
            creationLink.setID("<creationLink>"); // can't use label or it will draw one as it's being dragged around

        //    VueToolUtils.setToolProperties(this,"linkModeTool");
            
            // Mac overrides CONTROL-MOUSE to look like right-click (context menu popup) so we can't
            // use CTRL wih mouse drag on a mac.
            setActiveWhileDownKeyCode(0);
            //setActiveWhileDownKeyCode(VueUtil.isMacPlatform() ? KeyEvent.VK_ALT : KeyEvent.VK_CONTROL);
        }    	     

        /** @return LWLink.class */
        @Override
        public Class getSelectionType() { return LWLink.class; }

        @Override
        public void handleDragAbort()
        {
            this.linkSource = null;
        }

        @Override
        public void handlePostDraw(DrawContext dc, MapViewer viewer) {
            if (linkSource != null)
                creationLink.draw(dc);
        }

        public void setComboMode(boolean comboMode)
        {
            this.comboMode = comboMode;
        }

        @Override
        public boolean handleComponentPressed(MapMouseEvent e)
        {
            //System.out.println(this + " handleMousePressed " + e);
            LWComponent hit = e.getPicked();
            // TODO: handle LWGroup picking
            //if (hit instanceof LWGroup)
            //hit = ((LWGroup)hit).findDeepestChildAt(e.getMapPoint());

            if (hit != null && hit.canLinkTo(null)) {
                linkSource = hit;
                // todo: pick up current default stroke color & stroke width
                // and apply to creationLink

                creationLink.setParent(linkSource.getParent()); // needed for new relative-to-parent link code
                //invisibleLinkEndpoint.setParent(linkSource.getParent()); // needed for new relative-to-parent link code
                
                creationLink.setTemporaryEndPoint1(linkSource);
                EditorManager.applyCurrentProperties(creationLink); // don't target until / unless link actually created
                // never let drawn creator link get less than 1 pixel wide on-screen
                float minStrokeWidth = (float) (1 / e.getViewer().getZoomFactor());
                if (creationLink.getStrokeWidth() < minStrokeWidth)
                    creationLink.setStrokeWidth(minStrokeWidth);
                invisibleLinkEndpoint.setLocation(e.getMapPoint());
                creationLink.notifyEndpointMoved(null, invisibleLinkEndpoint);
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

        @Override
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
        
        @Override
        public boolean handleMouseReleased(MapMouseEvent e)
        {
            //System.out.println(this + " " + e + " linkSource=" + linkSource);
            if (linkSource == null)
                return false;

            //System.out.println("dx,dy=" + e.getDeltaPressX() + "," + e.getDeltaPressY());
            if (Math.abs(e.getDeltaPressX()) > 10 ||
                Math.abs(e.getDeltaPressY()) > 10)  // todo: config min dragout distance
    	        {
    	            //repaintMapRegionAdjusted(creationLink.getBounds());
    	            LWComponent linkDest = e.getViewer().getIndication();
    	            if (linkDest != linkSource)
    	                makeLink(e, linkSource, linkDest, !e.isShiftDown(),comboMode);
    	        }
            this.linkSource = null;
            return true;
        }
    	   
        @Override
        public void drawSelector(DrawContext dc, java.awt.Rectangle r)
        {
            //g.setXORMode(java.awt.Color.blue);
            dc.g.setColor(java.awt.Color.blue);
            super.drawSelector(dc, r);
        }

        private void makeLink(MapMouseEvent e,
                              LWComponent pLinkSource,
                              LWComponent pLinkDest,
                              boolean pMakeConnection,
                              boolean comboMode)
        {
            int existingLinks = 0;
            int existingCurvedLinks = 0;
            if (pLinkDest != null) {
                existingLinks = pLinkDest.countLinksTo(pLinkSource);
                existingCurvedLinks = pLinkDest.countCurvedLinksTo(pLinkSource);
            }
            final int existingStraightLinks = existingLinks - existingCurvedLinks;
            
            // TODO: don't create new node at end of new link inside
            // parent of source node (e.g., content view/traditional node)
            // unless mouse is over that node!  (E.g., should be able to
            // drag link out from a node that is a child)
    	            
            LWContainer commonParent = e.getMap();
            if (pLinkDest == null)
                commonParent = pLinkSource.getParent();
            else if (pLinkSource.getParent() == pLinkDest.getParent() &&
                     pLinkSource.getParent() != commonParent) {
                // todo: if parents different, add to the upper most parent
                commonParent = pLinkSource.getParent();
            }
            boolean createdNode = false;
            if (pLinkDest == null && (pMakeConnection && comboMode)) {
                pLinkDest = NodeModeTool.createNewNode();
                pLinkDest.setCenterAt(e.getMapPoint());
                commonParent.addChild(pLinkDest);
                createdNode = true;
            }

            final LWLink link;
            if (pMakeConnection && (comboMode || pLinkDest!=null)) {
                link = new LWLink(pLinkSource, pLinkDest);
                if (existingStraightLinks > 0)
                    link.setControlCount(1);
            } else {
                link = new LWLink(pLinkSource, null);
                link.setTailPoint(e.getMapPoint()); // set to drop location
            }
            EditorManager.targetAndApplyCurrentProperties(link);
    	            
            commonParent.addChild(link);
            // We ensure a paint sequence here because a link to a link
            // is currently drawn to it's center, which might paint over
            // a label.
            if (pLinkSource instanceof LWLink)
                commonParent.ensurePaintSequence(link, pLinkSource);
            if (pLinkDest instanceof LWLink)
                commonParent.ensurePaintSequence(link, pLinkDest);
            VUE.getSelection().setTo(link);
            if (pMakeConnection)
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
                setterAction.putValue(Action.SMALL_ICON, this.getRawIcon());
                // key is from: MenuButton.ValueKey
            }
            return setterAction;
        }
    }
    

}