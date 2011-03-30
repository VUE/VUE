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

import sun.tools.tree.SuperExpression;
import tufts.Util;
import static tufts.Util.fmt;
import tufts.vue.LWComponent.Alignment;
import tufts.vue.LWComponent.CopyContext;
import tufts.vue.LWComponent.Flag;
import tufts.vue.LWIBISNode.Column;
import tufts.vue.ibisimage.*;
import tufts.vue.shape.RectangularPoly2D;
                       
import edu.tufts.vue.preferences.PreferencesManager;
import edu.tufts.vue.preferences.VuePrefEvent;
import edu.tufts.vue.preferences.VuePrefListener;
import edu.tufts.vue.preferences.implementations.ShowIconsPreference;
import edu.tufts.vue.preferences.interfaces.VuePreference;
    
import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import java.awt.*;
import java.awt.geom.*;
import java.io.File;

import javax.swing.ImageIcon;

/**
 * @author 
 */

// todo: node layout code could use cleanup, as well as additional layout
// features (multiple columns).
// todo: "text" nodes are currently a total hack

//public class LWIBISNode extends LWContainer
public class LWIBISNode extends LWNode
{
    protected static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LWIBISNode.class);

    public static final Object TYPE_TEXT = "textNode";
    
    final static boolean WrapText = false; // under development
    
    public static final Font  DEFAULT_NODE_FONT = VueResources.getFont("node.font");
    public static final Color DEFAULT_NODE_FILL = VueResources.getColor("node.fillColor");
    public static final int   DEFAULT_NODE_STROKE_WIDTH = VueResources.getInt("node.strokeWidth");
    public static final Color DEFAULT_NODE_STROKE_COLOR = VueResources.getColor("node.strokeColor");
    public static final Font  DEFAULT_TEXT_FONT = VueResources.getFont("text.font");
    // HO 12/12/2010 BEGIN ************
    public static final float DEFAULT_IMAGE_SIZE = 64f;
    public static final float DEFAULT_IMAGE_HEIGHT = 64f;
    public static final float DEFAULT_IMAGE_WIDTH = 64f;
    // HO 12/12/2010 BEGIN ************
    
    /** how much smaller children are than their immediately enclosing parent (is cumulative) */
    static final float ChildScale = VueResources.getInt("node.child.scale", 75) / 100f;

    //------------------------------------------------------------------
    // Instance info
    //------------------------------------------------------------------
    
    
    /** 0 based with current local width/height */
    protected RectangularShape mShape;
    protected boolean isAutoSized = true; // compute size from label & children
    
    // HO 03/11/2010 BEGIN ******************
    protected LWImage ibisImage;
    // HO 03/11/2010 END ******************

    //-----------------------------------------------------------------------------
    // consider moving all the below stuff into a layout object

    private transient float mBoxedLayoutChildY;

    private transient boolean isRectShape = true;
    
    // HO 03/11/2010 BEGIN *************
    private transient boolean isIBISNode = true;
    // HO 03/11/2010 END *************

    private transient Line2D.Float mIconDivider = new Line2D.Float(); // vertical line between icon block & node label / children
    private transient Point2D.Float mLabelPos = new Point2D.Float(); // for use with irregular node shapes
    private transient Point2D.Float mChildPos = new Point2D.Float(); // for use with irregular node shapes

    private transient Size mMinSize;

    private transient boolean inLayout = false;
    private transient boolean isCenterLayout = false;// todo: get rid of this and use mChildPos, etc for boxed layout also

    private java.awt.Dimension textSize = null; // only for use with wrapped text
    
    private Class<? extends LWImage> nodeImageClass = null;
    private String mIBISType = null;
    
    // HO 16/12/2010 BEGIN *************
    public LWImage getIbisImage() {
    	return ibisImage;
    }
    
    public void setIbisImage(LWImage theImage) {
    	ibisImage = theImage;
    }
    
    // HO 16/12/2010 END *****************


    private final LWIcon.Block mIconBlock =
        new LWIcon.Block(this,
                         IconWidth, IconHeight,
                         null,
                         LWIcon.Block.VERTICAL);

    

    private void initNode() {
        enableProperty(KEY_Alignment);
    }
    
    @Override protected void drawImpl(DrawContext dc)
    {
        if (!isFiltered()) {
            // Desired functionality is that if this node is filtered, we don't draw it, of course.
            // But also, even if this node is filtered, we still draw any children who are
            // NOT filtered -- we just drop out the parent background.
            drawNode(dc);
        }
            
        //-------------------------------------------------------
        // Draw any children
        //-------------------------------------------------------
            
        if (hasChildren()) {
            //if (isZoomedFocus()) dc.g.setComposite(ZoomTransparency);
            drawChildren(dc);
        }

        // even if filtered, we indicate if it's selected, as it *shouldn't* be selected
        // if it's filtered, but we'll want to know if that is happening.

        if (isSelected() && dc.isInteractive() && dc.focal != this)
            drawSelection(dc);
        
    }
    
    LWIBISNode(String label, float x, float y, RectangularShape shape)
    {
        // HO 06/12/2010 BEGIN *********
        initNode();
        // HO 07/12/2010 BEGIN ***************
        //super.label = label; // make sure label initially set for debugging
        super.setLabel(label, true);
        // HO 07/12/2010 END ***************
        setFillColor(DEFAULT_NODE_FILL);
        if (shape == null)
            setShape(tufts.vue.shape.RoundRect2D.class);
          //setShape(new RoundRectangle2D.Float(0,0, 10,10, 20,20));
        else if (shape != null)
            setShapeInstance(shape);
        setStrokeWidth(DEFAULT_NODE_STROKE_WIDTH);
        setStrokeColor(DEFAULT_NODE_STROKE_COLOR);
        setLocation(x, y);
        this.width = NEEDS_DEFAULT;
        this.height = NEEDS_DEFAULT;
        setFont(DEFAULT_NODE_FONT);
        setLabel(label); 
    	// HO 06/12/2010 END ************
    }
    
    public LWIBISNode(String label) {
    	// HO 12/12/2010 BEGIN ************
    	this (label, new tufts.vue.ibisimage.IBISIssueImage());
    	// HO 12/12/2010 END ************
    	
    }
    
    /**
     * if asText is true, make this a text node, and isTextNode should return true.
     * If asText is false, do the minimum to this node such that isTextNode will
     * no longer return true.
     */
    public void setAsTextNode(boolean asText)
    {
        if (asText) {
            setShape(java.awt.geom.Rectangle2D.Float.class); // now enforced
            disableProperty(LWKey.Shape);
            setFillColor(COLOR_TRANSPARENT);
            setFont(DEFAULT_TEXT_FONT);
        } else {
            enableProperty(LWKey.Shape);
            setFillColor(DEFAULT_NODE_FILL);
        }
        if (asText)
            setAutoSized(true);
    }
    
    @Override
    public boolean handleDoubleClick(MapMouseEvent e)
    {

        // HO 08/12/2010 BEGIN ********
    	//if (this instanceof LWPortal) // hack: clean this up -- maybe move all below to LWComponent...
            //return super.handleDoubleClick(e);
    	// HO 08/12/2010 END ********
        
        final Point2D.Float localPoint = e.getLocalPoint(this);
        final float cx = localPoint.x;
        final float cy = localPoint.y;

        if (textBoxHit(cx, cy)) {
            // TODO: refactor w/MapViewer mouse handing & VueTool handling code
            // e.g.: this does NOT want to happen with the presentation tool.
            e.getViewer().activateLabelEdit(this);
        } else {
            if (!mIconBlock.handleDoubleClick(e)) {
                // by default, a double-click anywhere else in
                // node opens the resource
                if (hasResource()) {
                	// 30/03/2011 BEGIN *******
                	// we just want to skip this for IBIS images
                	//getResource().displayContent();
                	// 30/03/2011 END *******
                    
                }
            }
        }
        return true;
    }
    
    @Override
    public boolean supportsUserLabel() { return true; }
    
    @Override
    public boolean supportsUserResize() {
        if (isTextNode())
            return !isAutoSized(); // could be confusing, as once is shrunk down, can't resize again w/out undo
        else
            return true;
    }
    
    @Override
    protected List<LWComponent> sortForIncomingZOrder(Collection<? extends LWComponent> toAdd)
    {
        // Use the YSorter -- as we stack out children, this will then
        // display them in the same vertical order they had wherever
        // they came from.  Only guaranteed to make sense when all the
        // incoming nodes are on the same parent/canvas, (the same
        // coordinate space).
        
        return java.util.Arrays.asList(sort(toAdd, YSorter));
    }
    
    /** @return false if this is a text node */
    @Override
    public boolean supportsChildren() {
        if (hasFlag(Flag.SLIDE_STYLE) && hasResource() && getResource().isImage()) {
            // so a text item that links to an image is allowed to have an
            // image dropped into it (ideally, it would only allow the image with the same resource)
            return true;
        } else
            return !isTextNode();
    }
    
    @Override
    public boolean isExternalResourceLinkForPresentations() {
        return hasResource() && !hasChildren() && !iconShowing() && getStyle() != null;
        // may even need to check if this has the LINK slide style in particular
    }
    
    @Override
    protected void removeChildImpl(LWComponent c)
    {
        c.setScale(1.0); // just in case, get everything
        // HO 07/12/2010 BEGIN ***************
        //super.removeChildImpl(c);
        super.removeChildImpl(c, true);
        // HO 07/12/2010 BEGIN ***************
    }
    
    @Override
    public void initTextBoxLocation(TextBox textBox) {
        textBox.setBoxLocation(relativeLabelX(), relativeLabelY());
    }

    public LWIBISNode(String label, IBISImage image) {
    	this(label, 0, 0);
    	if(image == null) {
    		// if there's no image, assign the default
    		setImage(IBISIssueImage.class);
    	} else if (image != null) {
    		setImageInstance(image);
    	}
    	// make sure the fill color is white
    	this.setFillColor(java.awt.Color.white);
    	this.setStrokeWidth(0);
    }

    LWIBISNode(String label, RectangularShape shape) {
    	this(label, 0, 0, shape);
    }
    
    LWIBISNode(String label, float x, float y) {
    	// HO 06/12/2010 BEGIN *************
    	// super(label, x, y);
    	this(label, x, y, null);
    	// HO 06/12/2010 END *************
    }
    
    LWIBISNode(String label, Resource resource)
    {
        this(label, 0, 0);
        setResource(resource);
    }
    
    @Override
    public int getFocalMargin() {
        return -10;
    }
    
    @Override
    public void setCollapsed(boolean collapsed) {

        if (COLLAPSE_IS_GLOBAL)
            throw new Error("collapse is set to global impl");
        
        if (hasFlag(Flag.COLLAPSED) != collapsed) {
            setFlag(Flag.COLLAPSED, collapsed);
            layout(KEY_Collapsed);
            notify(KEY_Collapsed, !collapsed);
        }

        // if we run into problems with children being visible / pickable anywhere, we
        // could always make all descendents additionally hidden via a new
        // HideCause.COLLAPSED, but they're currently being successfully truncated by an
        // appropriatly false return in from LWComponent.hasPicks(), or a excluded from
        // the list returned by LWComponent.getPickList().  The drawback to apply
        // an additional HideCause to all descendents would be the generation of
        // lots of events on collapse, tho we would no longer need isAncestorCollapsed().
    }
    
    @Override
    public void addChildren(java.util.Collection<? extends LWComponent> children, Object context)
    {
        if (!mXMLRestoreUnderway && !hasResource() && !hasChildren() && children.size() == 1) {
            final LWComponent first = Util.getFirst(children);
            if (first instanceof LWImage) {
                // we do this BEFORE calling super.addChildren, so the soon to be
                // added LWImage will know to auto-update itself to node icon status
                // in it's setParent (or we could call first.updateNodeIconStatus
                // directly if we made it public)
                
                // don't call setResource, or our special LWIBISNode impl will auto
                // install the image as a node icon, and then addChildren will add
                // it a second time.
                
                // TODO: however, this not undoable...  so we'll want to do this
                // after...
                
                // TODO: Also, dragging OUT a non-attached image to the map, but
                // canceling the drag, triggers this code in the re-add, and
                // then the image gets 'stuck' as a node icon.
                
                takeResource(first.getResource());
            }
            
        }
        
        // HO 22/09/2010 BEGIN ****************
        //super.addChildren(children, context);
        super.addChildren(children, context, true);
        // HO 22/09/2010 BEGIN ****************
        
        // HO 22/09/2010 BEGIN ****************
        reparentWormholeNode(children);

        // HO 22/09/2010 END ****************
        
        //Log.info("ADDED CHILDREN: " + Util.tags(iterable));

    }
    
    @Override
    public void XML_completed(Object context) {
    	// HO 08/12/2010 BEGIN ****************
    	//super.XML_completed(context);
        super.XML_completed(context, true);
        // HO 08/12/2010 END ****************
        if (hasChildren()) {
            if (DEBUG.WORK||DEBUG.XML||DEBUG.LAYOUT) Log.debug("XML_completed: scaling down LWIBISNode children in: " + this);
            for (LWComponent c : getChildren()) {
                if (isScaledChildType(c))
                    c.setScale(ChildScale);
            }

            if (hasResource() && getChild(0) instanceof LWImage) {
                final LWImage image = (LWImage) getChild(0);
                final Resource IR = image.getResource();
                final Resource r = getResource();
                
                if (r != null && IR != null && r != IR && r.equals(IR)) {

                    // node & image start with same instance of a Resource object when
                    // intially created, but two instances are created during
                    // persistance.  This restore the single instance condition upon
                    // restore.  The Resource owned by the image takes priority, as it's
                    // going to have the most complete & up to date meta-data.

                    // This should work fine (it's the same state things are in when
                    // image nodes are initially created), tho we should watch for
                    // side-effects with filtering & meta-data, or even possible
                    // threading issues, in case this brings to light other bugs we
                    // haven't caught yet. SMF 2008-04-01
                    
                    takeResource(IR);
                    
                }

            }
            
        }

    }
    
    @Override
    public boolean isManagingChildLocations() {
        return true;
    }
    
    /** @return true -- a node is always considered to have content */
    @Override
    public boolean hasContent() {
        return true;
    }
    
    public LWImage getImage() {
        if (isImageNode(this))
            return (LWImage) getChild(0);
        else
            return null;
    }
    
    public RectangularShape getXMLshape() {
        return mShape;
    }
    
    @Override
    protected void addChildImpl(LWComponent c, Object context)
    {
        // must set the scale before calling the super
        // handler, as scale must be in place before
        // notifyHierarchyChanging/Changed calls.
        if (isScaledChildType(c))
            c.setScale(LWIBISNode.ChildScale);
        // HO 08/12/2010 BEGIN **************
        //super.addChildImpl(c, context);
        super.addChildImpl(c, context, true);
        // HO 08/12/2010 BEGIN **************
    }
  
    public static final Key KEY_Shape =
        new Key<LWIBISNode,Class<? extends RectangularShape>>("node.shape", "shape") {
        @Override
        public boolean setValueFromCSS(LWIBISNode c, String cssKey, String cssValue) {
            RectangularShape shape = NodeTool.getTool().getNamedShape(cssValue);
            if (shape == null) {
                return false;
            } else {
                setValue(c, shape.getClass());
                System.err.println("applied shape: " + this + "=" + getValue(c));
                return true;
            }
        }
        @Override
        public void setValue(LWIBISNode c, Class<? extends RectangularShape> shapeClass) {
            c.setShape(shapeClass);
        }
        @Override
        public Class<? extends RectangularShape> getValue(LWIBISNode c) {
            try {
                return c.mShape.getClass();
            } catch (NullPointerException e) {
                return null;
            }
        }

        /**
         * This is overridden to allow for equivalence tests against an instance value
         * RectangularShape, as opposed to just types of Class<? extends
         * RectangularShape>.
         *
         * @param other
         * If this is an instance of RectangularShape, we compare
         * our getValue() against it's Class object, not it's instance.
         */
        @Override
        boolean valueEquals(LWIBISNode c, Object other)
        {
            final Class<? extends RectangularShape> value = getValue(c);
            final Class<? extends RectangularShape> otherValue;

            if (other instanceof RectangularShape) {
                
                otherValue = ((RectangularShape)other).getClass();
                
            } else if (other instanceof Class) {

                otherValue = (Class) other;
                
            } else if (other != null) {
                
                if (DEBUG.Enabled) Log.warn(this + "; valueEquals against unexpected type: " + Util.tags(other));
                return false;
                
            } else
                otherValue = null;

            return value == otherValue || (otherValue != null && otherValue.equals(value));
        }
        
    };
    
    // HO 27/10/2010 BEGIN ********************
    public static final Key KEY_IBISSymbol =
        new Key<LWIBISNode,Class<? extends LWImage>>("node.IBISSymbol", "IBISSymbol") {
        @Override
        public boolean setValueFromCSS(LWIBISNode c, String cssKey, String cssValue) {
            LWImage image = IBISNodeTool.getTool().getNamedImage(cssValue);

            if (image == null) {
                return false;
            } else {
                setValue(c, image.getClass());
                System.err.println("applied image: " + this + "=" + getValue(c));
                return true;
            }
        }
        @Override
        public void setValue(LWIBISNode c, Class<? extends LWImage> imageClass) {
        	c.setImage(imageClass);
        }
        
        @Override
        public Class<? extends LWImage> getValue(LWIBISNode c) {
            try {
            	if (c.ibisImage != null)
            		return c.ibisImage.getClass();
            	else
            		return null;
            } catch (NullPointerException e) {
                return null;
            }
        }

        /**
         * This is overridden to allow for equivalence tests against an instance value
         * LWImage, as opposed to just types of Class<? extends
         * LWImage>.
         *
         * @param other
         * If this is an instance of LWImage, we compare
         * our getValue() against its Class object, not its instance.
         */
        @Override
        boolean valueEquals(LWIBISNode c, Object other)
        {
            final Class<? extends LWImage> value = getValue(c);
            final Class<? extends LWImage> otherValue;

            if (other instanceof LWImage) {
                
                otherValue = ((LWImage)other).getClass();
                
            } else if (other instanceof Class) {

                otherValue = (Class) other;
                
            } else if (other != null) {
                
                if (DEBUG.Enabled) Log.warn(this + "; valueEquals against unexpected type: " + Util.tags(other));
                return false;
                
            } else
                otherValue = null;

            return value == otherValue || (otherValue != null && otherValue.equals(value));
        }
        
    };   
    
    // HO 27/10/2010 END **********************
    
    // HO 12/12/2010 BEGIN ************
    private void setImageToDefaultSize() {
    	if (ibisImage != null) {
    		// ibisImage.setMaxDimension(64);
            ibisImage.setMaxDimension(DEFAULT_IMAGE_SIZE);
            //if (ibisImage.isNodeIcon()) {
                ibisImage.clearHidden(HideCause.IMAGE_ICON_OFF);
                layout("imageIconShow");
                VUE.getActiveViewer().repaintSelection();
            //}
    	}
    }
    // HO 12/12/2010 BEGIN ************
    
    // HO 03/11/2010 BEGIN *****************************
    /**
     * @param imageClass -- a class object this is a subclass of LWImage
     */
    public void setImage(Class<? extends LWImage> imageClass) {

        if (ibisImage != null && IsSameImage(ibisImage.getClass(), imageClass))
            return;

        try {
            setImageInstance(imageClass.newInstance());
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t);
        }
    }
    
    @Override
    public boolean isTextNode() {

        // todo: "text" node should display no note icon, but display
        // the note if any when any part of it is rolled over.  Just
        // what a text node is is a bit confusing right now, but it's
        // useful guess for now.
        
        return isLikelyTextNode() && mShape instanceof Rectangle2D;
    }
    
    @Override
    public void setToNaturalSize() {
    	Size m = this.getMinimumSize();
        setSize(m.width, m.height);
    }
    
    @Override
    public Object getTypeToken() {
    	// HO 08/12/2010 BEGIN ***********
    	//return isTextNode() ? TYPE_TEXT : super.getTypeToken();
        return isTextNode() ? TYPE_TEXT : super.getTypeToken(true);
        // HO 08/12/2010 END ***********
    }
    
    @Override
    public boolean isLikelyTextNode() {

        // SMF 2008-04-25: This is a hack for VUE-822 until the underlying bug can be
        // found ("text" nodes being created with non-rectangular shapes).  The
        // PresentationTool is going to call this directly. These are all the conditions
        // from the original isTextNode, less the check for the shape.  We're not just
        // changing isTextNode as getTypeToken relies on it, and allowing shapes into
        // text nodes would changed EditorManager behaivor for how style properties are
        // used to load tool states and copy/apply style values.
        
        return getClass() == LWIBISNode.class // sub-classes don't count
            && isTranslucent()
            && !hasChildren()
            && !inPathway(); // heuristic to exclude LWIBISNode portals (not likely to just put a piece of text alone on a pathway)
    }
    
    @Override
    protected Point2D getZeroSouthEastCorner() {
        if (isRectShape)
        	// HO 08/12/2010 BEGIN *****************
            //return super.getZeroSouthEastCorner();
        	return super.getZeroSouthEastCorner(true);
        	// HO 08/12/2010 BEGIN *****************

        // find out where a line drawn from our local center to our
        // lower right bounding box intersects the lower right edge of
        // our local shape
        
        final float[] corner =
            VueUtil.computeIntersection(getWidth() / 2, getHeight() / 2,
                                        getWidth(), getHeight(),
                                        mShape,
                                        null);

        return new Point2D.Float(corner[0], corner[1]);
    }
    
    protected boolean intersectsImpl(final Rectangle2D mapRect)
    {
        if (isRectShape) {
            // if we're a rect-ish shape, the standard bounding-box impl will do
            // (it will over-include the corners on round-rects, but that's okay)
        	// HO 08/12/2010 BEGIN ******************
            //return super.intersectsImpl(mapRect);
        	return super.intersectsImpl(mapRect, true);
            // HO 08/12/2010 END ******************
        } else {
            // TODO: only use the fast reject if this is for paint-clip testing?  already overkill?
        	// HO 08/12/2010 BEGIN ******************
            //if (super.intersectsImpl(mapRect) == false) {
        	if (super.intersectsImpl(mapRect, true) == false) {
            	// HO 08/12/2010 END ******************
                return false; // fast-reject
            } else {
                return getZeroShape().intersects(transformMapToZeroRect(mapRect));
            }
        }
        
    }

    protected boolean containsImpl(float x, float y, PickContext pc) {
        if (isRectShape) {
            // won't be perfect for round-rect at big scales, but good
            // enough, and takes into account stroke width
        	// HO 08/12/2010 BEGIN ***********
            //return super.containsImpl(x, y, pc);
        	return super.containsImpl(x, y, pc, true);
        //} else if (super.containsImpl(x, y, pc)) {
        } else if (super.containsImpl(x, y, pc, true)) {
        	// HO 08/12/2010 END ***********
            // above was a fast-reject check on the bounding box, now check the actual shape:
            
            // TODO: need to figure out a way to compenstate for stroke width on
            // arbitrary shapes.  (This is only noticable when zoomed up to massive
            // scales with large stroke widths). We could compute a connector and check
            // the distance^2 against the (strokeWidth/2)^2, and in that case we could
            // override pickDistance if we want near picking of nodes, tho I don't think
            // we need that.
            
            return mShape.contains(x, y);
        } else
            return false;
    }
    
    protected void layoutImpl(Object triggerKey) {
        layoutNode(triggerKey, new Size(getWidth(), getHeight()), null);
    }
    
    /**
     * @param shape a new instance of a shape for us to use: should be a clone and not an original
     */
    protected void setShapeInstance(RectangularShape shape)
    {
        if (DEBUG.CASTOR) System.out.println("SETSHAPE " + shape.getClass() + " in " + this + " " + shape);

        if (IsSameShape(mShape, shape))
            return;

        final Object old = mShape;
        isRectShape = (shape instanceof Rectangle2D || shape instanceof RoundRectangle2D);
        mShape = shape;
        mShape.setFrame(0, 0, getWidth(), getHeight());
        layout(LWKey.Shape);
        updateConnectedLinks(null);
        notify(LWKey.Shape, new Undoable(old) { void undo() { setShapeInstance((RectangularShape)old); }} );
        
    }
    
    public void setXMLshape(RectangularShape shape) {
        setShapeInstance(shape);
    }
    
    @Override
    public void setLocation(float x, float y)
    {
        // HO 08/12/2010 BEGIN **********
    	//super.setLocation(x, y);
    	super.setLocation(x, y, true);
    	// HO 08/12/2010 END **********

        // Must lay-out children seperately from layout() -- if we
        // just call layout here we'll recurse when setting the
        // location of our children as they they try and notify us
        // back that we need to layout.
        
        layoutChildren();
    }
    
    /**
     * Need to be able to do this seperately from layout -- this
     * get's called everytime a node's location is changed so
     * that's it's children will follow along with it.
     *
     * Children are laid out relative to the parent, but given
     * absolute map coordinates.  Note that because if this, anytime
     * we're computing a location for a child, we have to factor in
     * the current scale factor of the parent.
     */
    
    void layoutChildren() {
        layoutChildren(null, 0f, false);
    }
    
    /**
     * @param image a new instance of an image for us to use: should be a clone and not an original
     */
    protected void setImageInstance(LWImage image)
    {
        if (DEBUG.CASTOR) System.out.println("IMAGE " + image.getClass() + " in " + this + " " + image);

        if (IsSameImage(ibisImage, image))
            return;

        final IBISImage old = (IBISImage)ibisImage;
        isIBISNode = (image instanceof IBISImage);
        
        // HO 22/12/2010 BEGIN **************
        //if (isIBISNode)
        	//image.setNodeIcon(true);
        // HO 22/12/2010 END **************

        if (ibisImage == null) {
        	ibisImage = image;
       	
        	this.setResource(ibisImage.getResource());
        }
        
        
        // HO 01/12/2010 BEGIN out with the old
        if (old != null) {
        	Resource resource = image.getResource();
        	this.setResource(resource);
        	ibisImage = image;
        }
        	else {
        // HO 01/12/2010 END

        	}
      
        notify(LWKey.IBISSymbol, new Undoable(old) { void undo() { setImageInstance((LWImage)old); }} );
        
    } 
    
    // HO 03/11/2010 END *****************************
    
    // HO 15/11/2010 BEGIN *****************************
    // these don't actually seem to get called anywhere
    public void setXMLIBISImage(LWImage image) {
        setImageInstance(image);
    }
                                                     
    public LWImage getXMLIBISImage() {
        return ibisImage;
    }
    // HO 15/11/2010 END *****************************

    /**
     * This fixed value depends on the arc width/height specifications in our RoundRect2D, which
     * are currently 20,20.
     * If that ever changes, this will need to be recomputed (see commented out code in
     * in getZeroNorthWestCorner) and updated.
     * @see tufts.vue.shape.RoundRect2D
     */
    private static final Point2D RoundRectCorner = new Point2D.Double(2.928932, 2.928932);

    // was text box hit?  coordinates are component local
    private boolean textBoxHit(float cx, float cy)
    {
        // todo cleanup: this is a fudgey computation: IconPad / PadTop not always used!
        final float lx = relativeLabelX() - IconPadRight;
        final float ly = relativeLabelY() - PadTop;
        final Size size = getTextSize();
        final float h = size.height + PadTop;
        final float w = size.width + IconPadRight;
        //float height = getLabelBox().getHeight() + PadTop;
        //float width = (IconPadRight + getLabelBox().getWidth()) * TextWidthFudgeFactor;

        return
            cx >= lx &&
            cy >= ly &&
            cx <= lx + w &&
            cy <= ly + h;
    }
    
    /**
     * For triggering automatic shifts in the auto-size bit based on a call
     * to setSize or as a result of a layout
     */
    private void setAutomaticAutoSized(boolean tv)
    {
        if (isOrphan()) // if this is during a restore, don't do any automatic auto-size computations
            return;
        if (isAutoSized == tv)
            return;
        if (DEBUG.LAYOUT) out("*** setAutomaticAutoSized " + tv);
        isAutoSized = tv;
    }

    private static boolean IsSameShape(
                                       Class<? extends RectangularShape> c1,
                                       Class<? extends RectangularShape> c2) {
        if (c1 == null || c2 == null)
            return false;
        if (c1 == c2) {
            if (java.awt.geom.RoundRectangle2D.class.isAssignableFrom(c1))
                return false; // just in case arc's are different
            else
                return true;
        } else
            return false;
    }

    private static boolean IsSameShape(Shape s1, Shape s2) {
        if (s1 == null || s2 == null)
            return false;
        if (s1.getClass() == s2.getClass()) {
            if (s1 instanceof java.awt.geom.RoundRectangle2D) {
                RoundRectangle2D rr1 = (RoundRectangle2D) s1;
                RoundRectangle2D rr2 = (RoundRectangle2D) s2;
                return
                    rr1.getArcWidth() == rr2.getArcWidth() &&
                    rr1.getArcHeight() == rr2.getArcHeight();
            } else
                return true;
        } else
            return false;
    }
    
    // HO 03/11/2010 BEGIN *****************
    private static boolean IsSameImage(
            Class<? extends LWImage> lw1,
            Class<? extends LWImage> lw2) {
    			if (lw1 == null || lw2 == null)
    				return false;
    			if (lw1 == lw2) {
    				if (tufts.vue.ibisimage.IBISImage.class.isAssignableFrom(lw1))
    					return false; 
    				else
    					return true;
    			} else
    				return false;
    }
    
    private static boolean IsSameImage(LWImage lw1, LWImage lw2) {
        if (lw1 == null || lw2 == null)
            return false;
        if (lw1.getClass() == lw2.getClass()) {
            if (lw1 instanceof tufts.vue.LWImage) {
                LWImage ll1 = (LWImage) lw1;
                LWImage ll2 = (LWImage) lw2;
                return 
                	ll1.getResource().getActiveDataFile().equals(ll2.getResource().getActiveDataFile());

            } else
                return true;
        } else
            return false;
    }    
    
    
    // HO 03/11/2010 END ******************
    
    // HO 22/09/2010 BEGIN ******************
    /* private void reparentWormholeNode(java.util.Collection<? extends LWComponent> children) {
        if (!mXMLRestoreUnderway) {
        	final LWComponent first = Util.getFirst(children);
        	if (first instanceof LWWormholeNode) {
        		System.out.println("Woot! It's a wormhole node!");
        		LWWormholeNode wn = (LWWormholeNode)first;
        		WormholeResource wr = (WormholeResource)wn.getResource();
        		String strURI = "";
        		try	{
        			strURI = wr.getOriginatingComponentURIString();
        		} catch(NullPointerException e) {
        			return;
        		}
        		
        		// if we have an originating component URI,
        		// and it doesn't match this component's URI,
        		// we need to recreate the wormhole
        		if (strURI != null) {
        			if(!strURI.equals(this.getURIString())) {
        				LWWormhole worm = new LWWormhole(wn, wr, strURI, this);
        				takeResource(wn.getResource());
        			}
        		}
        	}
        }

    } */
    
    // HO 22/09/2010 BEGIN ******************
    private void reparentWormholeNode(java.util.Collection<? extends LWComponent> children) {
        if (!mXMLRestoreUnderway) {
        	final LWComponent first = Util.getFirst(children);
        	if (first instanceof LWWormholeNode) {
        		LWWormholeNode wn = (LWWormholeNode)first;
        		WormholeResource wr = (WormholeResource)wn.getResource();
        		String strURI = "";
        		try	{
        			strURI = wr.getOriginatingComponentURIString();
        		} catch(NullPointerException e) {
        			return;
        		}
        		
        		// if we have an originating component URI,
        		// and it doesn't match this component's URI,
        		// we need to recreate the wormhole
        		if (strURI != null) {
        			if(!strURI.equals(this.getURIString())) {
        				// flag that we're creating a wormhole on this map
        				LWMap parentMap = this.getParentOfType(LWMap.class);
        				parentMap.bConstructingWormholes = true;
        				// create the wormhole
        				LWWormhole worm = new LWWormhole(wn, wr, strURI, this);
        				// flag that we're done creating the wormhole
        				parentMap.bConstructingWormholes = false;
        				takeResource(wn.getResource());
        			}
        		}
        	}
        }

    }
    // HO 22/09/2010 END ******************
    
    public void setResource(final Resource r)
    {
        // HO 08/12/2010 BEGIN ***************
    	//super.setResource(r);
    	super.setResource(r, true);
    	// HO 08/12/2010 END ***************
        if (r == null || mXMLRestoreUnderway)
            return;

        //=============================================================================
        // LWImage ise dramatically simplified by just creating a new one when the
        // resource changes.  We don't have to to deal with async undo stuff(?)  That
        // could be one case where we preserve the aspect for the new content.  We'd
        // still want to do a duplicate in case of any styling/title/notes info.
        // =============================================================================

        LWImage newImageIcon = null;

        boolean rebuildImageIcon = true;
        
        if (getChild(0) instanceof LWImage) {
            final LWImage image0 = (LWImage) getChild(0);
            if (DEBUG.IMAGE) out("checking for resource sync to image @child(0): " + image0);
            if (r.isImage()) {
            	// HO 22/12/2010 BEGIN **********
                //if (image0.isNodeIcon() && !r.equals(image0.getResource())) { // we already know r can't be null
            	if (!r.equals(image0.getResource())) { // we already know r can't be null
                	// HO 22/12/2010 END **********
                    deleteChildPermanently(image0);
                    newImageIcon = LWImage.createNodeIcon(image0, r);
                }
            } else {
                deleteChildPermanently(image0);
            }
        } else if (r.isImage()) {
            newImageIcon = LWImage.createNodeIcon(r); 
        }

        if (newImageIcon != null) {
            addChild(newImageIcon);
            sendToBack(newImageIcon);
        }
        
    }
    // HO 22/09/2010 END ******************
    
    /**
     * @param shapeClass -- a class object this is a subclass of RectangularShape
     */
    public void setShape(Class<? extends RectangularShape> shapeClass) {

        if (mShape != null && IsSameShape(mShape.getClass(), shapeClass))
            return;

        // todo: could skip instancing unless we actually go to draw ourselves (lazy
        // create the instance) -- it's completely useless for LWIBISNodes serving as style
        // holders to create the instance, tho then we would need to keep a ref
        // to the class object...

        try {
            setShapeInstance(shapeClass.newInstance());
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t);
        }
    }

    private void loadAssetToVueMetadata(Osid2AssetResource r)
        throws org.osid.repository.RepositoryException
    {
        // adding metadtaa for Osid2AssetResource. 
        //TODO: This should be refactored into Osit2AssetResource or some other place.  Similar stuff is done with properties
        
        org.osid.repository.Asset asset = r.getAsset();
        if (asset == null) {
            Log.warn(this + "; can't load asset meta-data: Resource has no asset: " + r);
            //Log.warn(r, new IllegalArgumentException("can't load asset meta-data: Resource has no asset: " + r));
            return;
        }
        org.osid.repository.RecordIterator recordIterator = asset.getRecords();
        while (recordIterator.hasNextRecord()) {
            org.osid.repository.Record record = recordIterator.nextRecord();
            //        System.out.println("-Processing Record: "+record.getDisplayName());
            org.osid.repository.PartIterator partIterator = record.getParts();
            String recordDesc = null;
            while (partIterator.hasNextPart()) {
                org.osid.repository.Part part = partIterator.nextPart();
                //           System.out.println("--Processing Part: "+part.getDisplayName());
                org.osid.repository.PartStructure partStructure = part.getPartStructure();
                if ( (part != null) && (partStructure != null) ) {
                    org.osid.shared.Type partStructureType = partStructure.getType();
                    final String description = partStructure.getDescription();
                    java.io.Serializable value = part.getValue();
                    String key;
                    if (description != null && description.trim().length() > 0) {
                        key = description;
                    } else {
                        key = partStructureType.getKeyword();
                    }
                    if(!key.startsWith(VueResources.getString("metadata.dublincore.url"))) continue;
                    if (key == null) {
                        Log.warn(this + " Asset Part [" + part + "] has null key.");
                        continue;
                    }
                    if (value == null) {
                        Log.warn(this + " Asset Part [" + key + "] has null value.");
                        continue;
                    }
                    if (value instanceof String) {
                        String s = ((String)value).trim(); 
                        // Don't add field if it's empty
                        if (s.length() <= 0)
                            continue;
                                    
                        if (s.startsWith("<p>") && s.endsWith("</p>")) {
                            // Ignore empty HTML paragraphs
                            String body = s.substring(3, s.length()-4);
                            if (body.trim().length() == 0) {
                                if (DEBUG.DR)
                                    value = "[empty <p></p> ignored]";
                                else
                                    continue;
                            }
                        }
                        //                                  addProperty(key, value);
                        edu.tufts.vue.metadata.VueMetadataElement vueMDE = new edu.tufts.vue.metadata.VueMetadataElement();
                        vueMDE.setKey(key);
                        vueMDE.setValue(value.toString());
                        vueMDE.setType(edu.tufts.vue.metadata.VueMetadataElement.CATEGORY);
                        getMetadataList().addElement(vueMDE);
                    }
                }
            }
        }
    }

    static boolean isScaledChildType(LWComponent c) {
        return c instanceof LWIBISNode || c instanceof LWSlide; // slide testing
    }

    private void setSizeNoLayout(float w, float h)
    {
        if (DEBUG.LAYOUT) out("*** setSizeNoLayout " + w + "x" + h);
        // HO 06/12/2010 BEGIN *******************
        //super.setSizeImpl(w, h, false);
        super.setSizeImpl(w, h, false, true);
        // HO 06/12/2010 END *******************
        mShape.setFrame(0, 0, getWidth(), getHeight());
    }
    
    public Size getMinimumSize() {
        return mMinSize;
    }
    
    protected final void setSizeImpl(float w, float h, boolean internal)
    {
        if (DEBUG.LAYOUT) out("*** setSize         " + w + "x" + h);
        if (isAutoSized() && (w > this.width || h > this.height)) // does this handle scaling?
            setAutomaticAutoSized(false);
        layoutNode(LWKey.Size,
                   new Size(getWidth(), getHeight()),
                   new Size(w, h));
    }

    /**
     * @param triggerKey - the property change that triggered this layout
     * @param curSize - the current size of the node
     * @param request - the requested new size of the node
     */
    private void layoutNode(Object triggerKey, Size curSize, Size request)
    {
        if (inLayout) {
            if (DEBUG.Enabled) {
                if (DEBUG.LAYOUT)
                    new Throwable("ALREADY IN LAYOUT " + this).printStackTrace();
                else
                    Log.warn("already in layout: " + this);
            }
            return;
        }
        inLayout = true;
        if (DEBUG.LAYOUT) {
            String msg = "*** layoutNode, trigger="+triggerKey
                + " cur=" + curSize
                + " request=" + request
                + " isAutoSized=" + isAutoSized();
            if (DEBUG.META)
                Util.printClassTrace("tufts.vue.LW", msg + " " + this);
            else
                out(msg);
        }


        mIconBlock.layout(); // in order to compute the size & determine if anything showing

        if (DEBUG.LAYOUT && labelBox != null) {
            // do NOT call getLabelBox -- has caching side effect
            final int prefHeight = labelBox.getPreferredSize().height;
            final int realHeight = labelBox.getHeight();
            // NOTE: prefHeight often a couple of pixels less than getHeight
            if (prefHeight != realHeight) {
                Log.debug("prefHeight != height in " + this
                          + "\n\tprefHeight=" + prefHeight
                          + "\n\trealHeight=" + realHeight);
            }
        }

        if (triggerKey == Flag.COLLAPSED) {
            final boolean collapsed = isCollapsed();
            for (LWComponent c : getChildren()) {
                c.setHidden(HideCause.COLLAPSED, collapsed);
            }
        }

        // The current width & height is at this moment still a
        // "request" size -- e.g., the user may have attempted to drag
        // us to a size smaller than our minimum size.  During that
        // operation, the size of the node is momentarily set to
        // whatever the user requests, but then is immediately laid
        // out here, during which we will revert the node size to the
        // it's minimum size if bigger than the requested size.
        
        //-------------------------------------------------------
        // If we're a rectangle (rect or round rect) we use
        // layoutBoxed, if anything else, we use layoutCeneter
        //-------------------------------------------------------

        final Size min;

        isCenterLayout = !isRectShape;

        if (isCenterLayout) {
            if (request == null)
                request = curSize;
            min = layoutCentered(request);
        } else {
            min = layoutBoxed(request, curSize, triggerKey);
            if (request == null)
                request = curSize;
        }
        

        mMinSize = new Size(min);

        if (DEBUG.LAYOUT) out("*** layout computed minimum=" + min);

        // If the size gets set to less than or equal to
        // minimize size, lock back into auto-sizing.
        if (request.height <= min.height && request.width <= min.width)
            setAutomaticAutoSized(true);
        
        final float newWidth;
        final float newHeight;

        if (isAutoSized()) {
            newWidth = min.width;
            newHeight = min.height;
        } else {
            // we always compute the minimum size, and
            // never let us get smaller than that -- so
            // only use given size if bigger than min size.
            if (request.width > min.width)
                newWidth = request.width;
            else
                newWidth = min.width;
            if (request.height > min.height)
                newHeight = request.height;
            else
                newHeight = min.height;
        }

        setSizeNoLayout(newWidth, newHeight);

        if (isCenterLayout == false) {
            // layout label last in case size is bigger than min and label is centered
            layoutBoxed_label();

            // ??? todo: cleaner move this to layoutBoxed, and have layout methods handle
            // the auto-size check (min gets set to request if request is bigger), as
            // layout_centered has to compute that now anyway.
            // HO 06/12/2010 BEGIN *********************
            //mIconDivider.setLine(IconMargin, MarginLinePadY, IconMargin, newHeight-MarginLinePadY);
            mIconDivider.setLine(newWidth - IconMargin, MarginLinePadY, newWidth - IconMargin, newHeight-MarginLinePadY);
            // HO 08/12/2010 BEGIN *********************
            mIconBlock.setLocation(newWidth - IconWidth, MarginLinePadY);
            // HO 06/12/2010 END *********************
            // HO 06/12/2010 END *********************
            // mIconDivider set by layoutCentered in the other case
        }

        // HO 09/12/2010 maybe this is where to reposition the label box
        // by putting it to the left of the icon block?
        if (labelBox != null) {
        	// HO 09/12/2010 BEGIN ****************
        	/* float xPos = relativeLabelX();
        	float curWidth = this.width;
        	float needWidth = xPos + getTextSize().width + LabelPadRight;
        	if (iconShowing()) {
        		needWidth += IconMargin;
        	}
        	if (this.width < needWidth)
        		this.l */
        	// HO 09/12/2010 END *******************
            labelBox.setBoxLocation(relativeLabelX(), relativeLabelY());
        }
        
        
        //if (this.parent != null && this.parent instanceof LWMap == false) {
        if (isLaidOut()) {
            // todo: should only need to do if size changed
            this.parent.layout();
        }
        
        inLayout = false;
    }
    
    /** @return the current size of the label object, providing a margin of error
     * on the width given sometime java bugs in computing the accurate length of a
     * a string in a variable width font. */
    
    protected Size getTextSize() {

    	// HO 22/12/2010 BEGIN ********************
    	/* if (WrapText) {
            Size s = new Size(getLabelBox().getSize());
            //s.width += 3;
            return s;
        } else { */
        	// HO 22/12/2010 END ********************
            // TODO: Check if this hack still needed in current JVM's
        
            // getSize somtimes a bit bigger thatn preferred size & more accurate
            // This is gross, but gives us best case data: we want the largest in width,
            // and smallest in height, as reported by BOTH getSize and getPreferredSize.

            Size s = new Size(getLabelBox().getPreferredSize());
            Size ps = new Size(getLabelBox().getSize());
            //if (ps.width > s.width) 
            //    s.width = s.width; // what the hell
            if (ps.height < s.height)
                s.height = ps.height;
            s.width *= TextWidthFudgeFactor;
            s.width += 3;
            return s;
         // HO 22/12/2010 BEGIN ********************
            //}
         // HO 22/12/2010 END ********************
    } 
    
    /** If true, compute node size from label & children */
    @Override
    public boolean isAutoSized() {
    	// HO 22/12/2010 BEGIN ********************
    	//if (WrapText)
            //return false; // LAYOUT-NEW
       // else
        	// HO 22/12/2010 end ********************
            return isAutoSized;
    }
    
    @Override
    public void setAutoSized(boolean makeAutoSized)
    {
    	// HO 22/12/2010 BEGIN ********************
    	//if (WrapText) return; // LAYOUT-NEW
    	// HO 22/12/2010 END ********************
        
        if (isAutoSized == makeAutoSized)
            return;
        if (DEBUG.LAYOUT) out("*** setAutoSized " + makeAutoSized);

        // We only need an undo event if going from not-autosized to
        // autosized: i.e.: it wasn't an automatic shift triggered via
        // set size. Because size events aren't delieverd if autosized
        // is on (which would normally notice the size change), we need
        // to remember the old size manually here if turning autosized
        // back on)

        Object old = null;
        if (makeAutoSized)
            old = new Point2D.Float(this.width, this.height);
        isAutoSized = makeAutoSized;
        if (isAutoSized && !inLayout)
            layout();
        if (makeAutoSized)
            notify("node.autosized", new Undoable(old) {
                    void undo() {
                        Point2D.Float p = (Point2D.Float) old;
                        setSize(p.x, p.y);
                    }});
    }

    private int getTextWidth() {
    	// HO 22/12/2010 BEGIN ********************
        //if (WrapText)
            //return labelBox.getWidth();
        //else
        	// HO 22/12/2010 END ********************
            return Math.round(getTextSize().width);
    }

    
    /**
     * Layout the contents of the node centered, and return the min size of the node.
     * @return the minimum rectangular size of node shape required to to contain all
     * the visual node contents
     */
    private Size layoutCentered(Size request)
    {
        NodeContent content = getLaidOutNodeContent();
        Size minSize = new Size(content);
        Size node = new Size(content);

        // Current node size is largest of current size, or
        // minimum content size.
        if (!isAutoSized()) {
            node.fit(request);
        }

        RectangularShape nodeShape = (RectangularShape) mShape.clone();
        nodeShape.setFrame(0,0, content.width, content.height);
        
        // todo perf: allow for skipping of searching for minimum size
        // if current size already big enough for content

        // todo: we shouldn't even by trying do a layout if have no children or label...
        if ((hasLabel() || hasChildren()) && growShapeUntilContainsContent(nodeShape, content)) {
            // content x/y is now at the center location of our MINUMUM size,
            // even tho our current size may be bigger and require moving it..
            minSize.fit(nodeShape);
            node.fit(minSize);
        }

        nodeShape.setFrame(0,0, node.width, node.height);
        layoutContentInShape(nodeShape, content);
        if (DEBUG.LAYOUT) out("*** content placed at " + content + " in " + nodeShape);

        content.layoutTargets();
        
        return minSize;
    }

    /**
     * Brute force increase the size of the given arbitrary shape until it's borders fully
     * contain the given rectangle when it is centered in the shape.  Algorithm starts
     * with size of content for shape (which would work it it was rectangular) then increases
     * width & height incrememntally by %10 until content is contained, then backs off 1 pixel
     * at a time to tighten the fit.
     *
     * @param shape - the shape to grow: expected be zero based (x=y=0)
     * @param content - the rectangle to ensure we can contain (x/y is ignored: it's x/y value at end will be centered)
     * @return true if the shape was grown
     */
    private boolean growShapeUntilContainsContent(RectangularShape shape, NodeContent content)
    {
        final int MaxTries = 1000; // in case of loops (e.g., a broke shape class whose contains() never succeeds)
        final float increment;
        if (content.width > content.height)
            increment = content.width * 0.1f;
        else
            increment = content.height * 0.1f;
        final float xinc = increment;
        final float yinc = increment;
        int tries = 0;
        while (!shape.contains(content) && tries < MaxTries) {
            shape.setFrame(0, 0, shape.getWidth() + xinc, shape.getHeight() + yinc);
            layoutContentInShape(shape, content);
            tries++;
        }
        if (tries > 0) {
            final float shrink = 1f;
            if (DEBUG.LAYOUT) System.out.println("Contents of " + shape + "  rought  fit  to " + content + " in " + tries + " tries");
            do {
                shape.setFrame(0, 0, shape.getWidth() - shrink, shape.getHeight() - shrink);
                layoutContentInShape(shape, content);
                tries++;
            } while (content.fitsInside(shape) && tries < MaxTries);
            shape.setFrame(0, 0, shape.getWidth() + shrink, shape.getHeight() + shrink);
        }
        
        if (tries >= MaxTries) {
            Log.error("Contents of " + shape + " failed to contain " + content + " after " + tries + " tries.");
        } else if (tries > 0) {
            if (DEBUG.LAYOUT) System.out.println("Contents of " + shape + " grown to contain " + content + " in " + tries + " tries");
        } else
            if (DEBUG.LAYOUT) System.out.println("Contents of " + shape + " already contains " + content);
        if (DEBUG.LAYOUT) out("*** content minput at " + content + " in " + shape);
        return tries > 0;
    }
    
    /**
     * Layout the given content rectangle in the given shape.  The default is to center
     * the content rectangle in the shape, however, if the shape in an instance
     * of tufts.vue.shape.RectangularPoly2D, it will call getContentGravity() to
     * determine layout gravity (CENTER, NORTH, EAST, etc).
     *
     * @param shape - the shape to layout the content in
     * @param content - the region to layout in the shape: x/y values will be set
     *
     * @see tufts.vue.shape.RectangularPoly2D, 
     */
    private void layoutContentInShape(RectangularShape shape, NodeContent content)
    {
        final float width = (float) shape.getWidth();
        final float height = (float) shape.getHeight();
        final float margin = 0.5f; // safety so 100% sure will be in-bounds
        boolean content_laid_out = false;

        if (shape instanceof RectangularPoly2D) {
            int gravity = ((RectangularPoly2D)shape).getContentGravity();
            content_laid_out = true;
            if (gravity == RectangularPoly2D.CENTER) {
                content.x = (width - content.width) / 2;
                content.y = (height - content.height) / 2;
            } else if (gravity == RectangularPoly2D.EAST) {
                content.x = margin;
                content.y = (float) (height - content.height) / 2;
            } else if (gravity == RectangularPoly2D.WEST) {
                content.x = (width - content.width) + margin;
                content.y = (float) Math.floor((height - content.height) / 2);
            } else if (gravity == RectangularPoly2D.NORTH) {
                content.x = (width - content.width) / 2;
                content.y = margin;
            } else if (gravity == RectangularPoly2D.SOUTH) {
                content.x = (width - content.width) / 2;
                content.y = (height - content.height) - margin;
            } else {
                Log.error(new Error("Unsupported content gravity " + gravity + " on shape " + shape + "; defaulting to CENTER"));
                content_laid_out = false;
            }
        }
        if (!content_laid_out) {
            // default is center layout
            content.x = (width - content.width) / 2;
            content.y = (height - content.height) / 2;
        }
    }

    /**
     * Provide a center-layout frame work for all the node content.
     * Constructing this object creates the layout.  It get's sizes
     * for all the potential regions in the node (icon block, label,
     * children) and lays out those regions relative to each other,
     * contained in a single rectangle.  Then that containging
     * rectangle can be used to quickly compute the the size of a
     * non-rectangular node shape required to enclose it completely.
     * Layout of the actual underlying targets doesn't happen until
     * layoutTargets() is called.
     */
    private class NodeContent extends Rectangle2D.Float {

        // regions for icons, label & children
        private Rectangle2D.Float rIcons;
        private Rectangle2D.Float rLabel = new Rectangle2D.Float();
        private Rectangle2D.Float rChildren;

        /**
         * Initial position is 0,0.  Regions are all normalized to offsets from 0,0.
         * Construct node content layout object: layout the regions for
         * icons, label & children.  Does NOT do the final layout (setting
         * LWIBISNode member variables, laying out the child nodes, etc, until
         * layoutTargts() is called).
         */
        NodeContent()
        {
        	// HO 09/12/2010 BEGIN ******
        	if (hasLabel()) {
                Size text = getTextSize();
                rLabel.width = text.width;
                rLabel.height = text.height;
            }
        	if (hasChildren() && !isCollapsed()) {
                Size children = layoutChildren(new Size(), 0f, true);
                float childx = ChildPadX;
                float childy = ChildPadY;
                rChildren = new Rectangle2D.Float(childx,childy, children.width, children.height);

                // can set absolute height based on label height & children height
                this.height = Math.max(rLabel.height, children.height) + ChildPadY;
                
                // make sure we're wide enough for the children in case children wider than label
                // fitWidth(rLabel.x + children.width); // as we're 0 based, rLabel.x == width of gap at left of children
                // make sure label is in the right place 
                rLabel.x = childx + children.width + ChildPadX;
                // HO 22/12/2010 BEGIN **********
                //rLabel.y = childy;
                rLabel.y = (this.height - rLabel.height) / 2;
                // HO 22/12/2010 END ************
                // HO 10/12/2010 BEGIN ******
                //fitWidth(rLabel.x + getTextSize().width + ChildPadX + children.width);
                fitWidth(rLabel.x + getTotalTextWidth());
                // HO 10/12/2010 END ******
                // HO 09/12/2010 END ******
            }
        	// HO 09/12/2010 END ******
        	if (hasLabel()) {
                if (!hasChildren()) {
                	rLabel.x = ChildPadX;
                	this.width = rLabel.x + getTextSize().width;
                	this.height = getTextSize().height;
                }
            } 
            if (iconShowing()) {
                rIcons = new Rectangle2D.Float(0, 0, mIconBlock.width, mIconBlock.height);
                this.width += mIconBlock.width;
                this.width += ChildPadX; // add space at right of label to match space at left
                // move label to right to make room for icon block at left
                // HO 09/12/2010 BEGIN ******
                //rLabel.x += mIconBlock.width;
                // or vice versa for icon blocks on the right
                // or not
                // rLabel.x -= mIconBlock.width;
                // HO 09/12/2010 END ******
            }
            /*if (hasChildren() && !isCollapsed()) {
                Size children = layoutChildren(new Size(), 0f, true);
                // HO 09/12/2010 BEGIN ******
                //float childx = rLabel.x;
                float childx = ChildPadX;
                //float childy = rLabel.height + ChildPadY;
                float childy = ChildPadY;
                rChildren = new Rectangle2D.Float(childx,childy, children.width, children.height);

                // can set absolute height based on label height & children height
                //this.height = rLabel.height + ChildPadY + children.height;
                this.height = Math.max(rLabel.height, children.height) + ChildPadY;
                
                // make sure we're wide enough for the children in case children wider than label
                // fitWidth(rLabel.x + children.width); // as we're 0 based, rLabel.x == width of gap at left of children
                // make sure label is in the right place 
                rLabel.x = childx + children.width + ChildPadX;
                rLabel.y = childy;
                fitWidth(rLabel.x + getTextSize().width + ChildPadX + children.width);
                // HO 09/12/2010 END ******
            }*/
            
            if (rIcons != null) {
                fitHeight(mIconBlock.height);

                if (mIconBlock.height < height) {
                    // vertically center icon block if less than total height
                    rIcons.y = (height - rIcons.height) / 2;
                } else if (height > rLabel.height && !hasChildren()) {
                    // vertically center the label if no children & icon block is taller than label
                    rLabel.y = (height - rLabel.height) / 2;
                }
            }
        }

        /** do the center-layout for the actual targets (LWIBISNode state) of our regions */
        void layoutTargets() {
            if (DEBUG.LAYOUT) out("*** laying out targets");
            // HO 09/12/2010 BEGIN **********
            /* float labelPosX = ChildPadX;
            float labelPosY = ChildPadY;
            if (rChildren != null)  {
            	labelPosX = x + rChildren.x + rChildren.width;
            	labelPosY = y + rChildren.y;
            }
            mLabelPos.setLocation(x + labelPosX, y + labelPosY); */
            // HO 09/12/2010 END **********
            mLabelPos.setLocation(x + rLabel.x, y + rLabel.y);
            
            if (rIcons != null) {
            	// HO 08/12/2010 BEGIN **************
                //mIconBlock.setLocation(x + rIcons.x, y + rIcons.y);
            	rIcons.x = width - rIcons.width;
            	//float theX = width - (x + rIcons.x);
            	//mIconBlock.setLocation(theX, y + rIcons.y);
            	mIconBlock.setLocation(x + rIcons.x, y + rIcons.y);
                // HO 08/12/2010 END **************
                // Set divider line to height of the content, at right of icon block
            	// HO 08/12/2010 BEGIN **************
                //mIconDivider.setLine(mIconBlock.x + mIconBlock.width, this.y,
                                     //mIconBlock.x + mIconBlock.width, this.y + this.height);
            	//float newX = theX - IconPadLeft;
            	float newX = (x + rIcons.x) - ChildPadX;
            	//mIconDivider.setLine((width-(theX + IconPadLeft)), this.y,
                        //(width-(mIconBlock.x + IconPadLeft)), this.y + this.height);
            	mIconDivider.setLine(newX, this.y,
                        newX, this.y + this.height);
                // HO 08/12/2010 END **************
            }
            if (rChildren != null) {
                mChildPos.setLocation(x + rChildren.x, y + rChildren.y);
                layoutChildren();
            }
        }
        
        /** @return true if all of the individual content items, as currently positioned, fit
            inside the given shape.  Note that this may return true even while outer dimensions
            of the NodeContent do NOT fit inside the shape: it's okay to clip corners of
            the NodeContent box as long as the individual components still fit: the NodeContent
            box is used for <i>centering</i> the content in the bounding box of the shape,
            and for the initial rough estimate of an enclosing shape.
        */
        private Rectangle2D.Float checker = new Rectangle2D.Float();
        boolean fitsInside(RectangularShape shape) {
            //return shape.contains(this);
            boolean fit = true;
            copyTranslate(rLabel, checker, x, y);
            fit &= shape.contains(checker);
            if (rIcons != null) {
                copyTranslate(rIcons, checker, x, y);
                fit &= shape.contains(checker);
            }
            if (rChildren != null) {
                copyTranslate(rChildren, checker, x, y);
                fit &= shape.contains(checker);
            }
            return fit;
        }

        private void copyTranslate(Rectangle2D.Float src, Rectangle2D.Float dest, float xoff, float yoff) {
            dest.width = src.width;
            dest.height = src.height;
            dest.x = src.x + xoff;
            dest.y = src.y + yoff;
        }

        private void fitWidth(float w) {
            if (width < w)
                width = w;
        }
        private void fitHeight(float h) {
            if (height < h)
                height = h;
        }
        
        public String toString() {
            return "NodeContent[" + VueUtil.out(this) + "]";
        }
    }

    /** 
     * @return internal node content already laid out
     */
    
    private NodeContent _lastNodeContent;
    /** get a center-layout framework */
    private NodeContent getLaidOutNodeContent()
    {
        return _lastNodeContent = new NodeContent();
    }
    
    // good for single column layout only.  layout code is in BAD NEED of complete re-architecting.
    protected float getMaxChildSpan()
    {
        java.util.Iterator i = getChildIterator();
        float maxWidth = 0;
        
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            float w = c.getLocalBorderWidth();
            if (w > maxWidth)
                maxWidth = w;
        }
        return childOffsetX() + maxWidth + ChildPadX;
    }
    
    // HO 09/12/2010 BEGIN *************
    // good for single column layout only.  layout code is in BAD NEED of complete re-architecting.
    protected float getMaxChildHeight()
    {
        java.util.Iterator i = getChildIterator();
        float maxHeight = 0;
        
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            float w = c.getLocalBorderHeight();
            if (w > maxHeight)
                maxHeight = w;
        }
        // HO 10/12/2010 BEGIN *********
        return childOffsetY() + maxHeight + ChildPadY;
        //return childOffsetY() + maxHeight + ChildPadY + ChildrenPadBottom;
        // HO 10/12/2010 END *********
    }    
    // HO 09/12/2010 END ***************
    
    private Size layoutBoxed(Size request, Size oldSize, Object triggerKey) {
        final Size min;
        
        // HO 22/12/2010 BEGIN ********************
        //if (WrapText)
            //min = layoutBoxed_floating_text(request, oldSize, triggerKey);
        //else
        	// HO 22/12/2010 END ********************
            min = layoutBoxed_vanilla(request);

        return min;

    }
    
    // HO 09/12/2010 BEGIN **********
    private float calculateTotalChildWidth() {
    	// this only works for column-aligned nodes
    	float childWidth = 0f;
    	// HO 10/12/2010 BEGIN *************
    	// if (hasChildren()) {
    	if (hasChildren() && !isCollapsed()) {
    		// HO 10/12/2010 END *********
    		childWidth += getMaxChildSpan();
    	}
    	
    	return childWidth;
    }
    
    private float calculateTotalChildHeight() {
    	// this only works for column-aligned nodes
    	float childHeight = 0f;
    	// this only works for column-aligned nodes
    	// HO 10/12/2010 BEGIN *************
    	// if (hasChildren()) {
    	if (hasChildren() && !isCollapsed()) {
    		// HO 10/12/2010 END *********
    		childHeight += getMaxChildHeight();
    	}
    	
    	return childHeight;
    }
    
    private Size calculateTotalChildSize() {
    	Size theSize = new Size(calculateTotalChildWidth(), calculateTotalChildHeight());
    	return theSize;
    }
    
    private float getTotalIconBlockWidth() {
    	float theWidth = 0f;
    	if (iconShowing()) {
    		theWidth = IconMargin;
    	}
    	return theWidth;
    }
    
    private float getTotalTextHeight() {
    	Size textSize = getTextSize();
    	float textHeight = EdgePadY + textSize.height + EdgePadY;
    	return textHeight;
    }
    
    private float getTotalTextWidth() {
    	Size textSize = getTextSize();
    	float textWidth = LabelPadLeft + textSize.width + LabelPadRight;
    	return textWidth;
    }
    // HO 09/12/2010 END **********

    
    /** @return new minimum size of node */
    private Size layoutBoxed_vanilla(final Size request)
    {
        // HO 09/12/2010 this might be the place to put
    	// the right-aligned label?
    	final Size min = new Size();
        // HO 09/12/2010 BEGIN **********
        final Size child = calculateTotalChildSize();
        // HO 09/12/2010 END **********
        final Size text = getTextSize();
        // HO 10/12/2010 BEGIN **********
        final float iconWidth = getTotalIconBlockWidth();
        // HO 10/12/2010 END **********

        // make sure the node is at least wide enough to contain the text
        // so far we may only have the label        
        // HO 09/12/2010 BEGIN **********
        // min.width = text.width;
        // HO 10/12/2010 BEGIN **********
        //min.width = child.width + text.width;
        min.width = child.width + text.width + iconWidth;
        // HO 10/12/2010 BEGIN **********

        //float textHeight = EdgePadY + text.height + EdgePadY;
        float textHeight = getTotalTextHeight();
        //float childHeight = EdgePadY + child.height + EdgePadY;
        
        //min.height = EdgePadY + text.height + EdgePadY;
        // HO 10/12/2010 BEGIN **********
        //min.height = textHeight;
        float childHeight = calculateTotalChildHeight();
        min.height = Math.max(textHeight, childHeight);
        // HO 10/12/2010 BEGIN **********
        // HO 09/12/2010 END **********

        // *** set icon Y position in all cases to a centered vertical
        // position, but never such that baseline is below bottom of
        // first icon -- this is tricky tho, as first icon can move
        // down a bit to be centered with the label!

        if (!iconShowing()) {
            min.width += LabelPadLeft;
        } else {
        	// HO 09/12/2010 BEGIN **********
            //float dividerY = EdgePadY + text.height;
        	float dividerY = EdgePadY + min.height; 
            //double stubX = LabelPositionXWhenIconShowing + text.width;
        	// HO 10/12/2010 BEGIN **********
        	//double stubX = EdgePadX + child.width + LabelPadLeft + text.width;
        	//double stubX = child.width + LabelPadLeft + text.width;
        	//double stubX = child.width + LabelPadLeft + text.width + LabelPadRight;
        	double stubX = child.width + getTotalTextWidth() + iconWidth;
        	//double stubX = EdgePadX + child.width + EdgePadX + text.width;
        	// HO 10/12/2010 BEGIN **********
            // HO 09/12/2010 END **********
            double stubHeight = DividerStubAscent;

            min.width = (float)stubX + IconPadLeft; // be symmetrical with left padding
        }

        if (hasChildren() && !isCollapsed()) {
            if (DEBUG.LAYOUT) out("*** textSize b4 layoutBoxed_children: " + text);
            layoutBoxed_children(min, text);
        }
        
        if (iconShowing())
            layoutBoxed_icon(request, min, text);
        
        return min;
    }

    /** set mLabelPos */
    private void layoutBoxed_label()
    {
        Size text = getTextSize();
        
     // HO 22/12/2010 BEGIN ********************
        /* if (hasChildren()) {
            mLabelPos.y = EdgePadY;
        } else { */
            // only need this in case of small font sizes and an icon
            // is showing -- if so, center label vertically in row with the first icon
            // Actually, no: center in whole node -- gak, we really want both,
            // but only to a certian threshold -- what a hack!
            //float textHeight = getLabelBox().getPreferredSize().height;
            //mLabelPos.y = (this.height - textHeight) / 2;
        	// HO 22/12/2010 END ********************
            mLabelPos.y = (this.height - text.height) / 2;
         // HO 22/12/2010 BEGIN ********************    
    	//}
 // HO 22/12/2010 END ********************
     // HO 22/12/2010 BEGIN ********************
        /* if (iconShowing()) {
            //layoutBoxed_icon(request, min, newTextSize);
            // TODO:
            // need to center label between the icon block and the RHS
            // we currently need more space at the RHS.
            // does relativeLabelX even use this in this case?
            // really: do something that isn't a total freakin hack like all our current layout code.
            //mLabelPos.x = LabelPositionXWhenIconShowing;
            mLabelPos.x = -100;  // marked bad because should never see this this: is IGNORED if icon is showing
        } else {
            //-------------------------------------------------------
            // horizontally center if no icons
            //-------------------------------------------------------
            if (WrapText)
                mLabelPos.x = (this.width - text.width) / 2 + 1;
            else
                mLabelPos.x = 200; // marked bad because unused in this case
        } */
     // HO 22/12/2010 END ********************
        
    }
    
    //-----------------------------------------------------------------------------
    // I think these are done dynamically instead of always using
    // mLabelPos.x and mLabelPos.y because we haven't always done a
    // layout when we need this?  Is that true?  Does this have
    // anything to do with activating an edit box on a newly created
    // node?
    //-----------------------------------------------------------------------------
    
    protected float relativeLabelX()
    {
        if (isCenterLayout) { // non-rectangular shapes
            return mLabelPos.x;
        } else if (iconShowing()) {
        	// HO 09/12/2010 BEGIN ************
        	// no change, but the value is now label left-padding
            //return LabelPositionXWhenIconShowing;
        	float xOffset = 0f;
        	// now if there's a child, it should be on the left
        	if (hasChildren())  {
        		// HO 10/12/2010 BEGIN **********
        		//theOffset += EdgePadX + calculateChildWidth();
        		xOffset += calculateTotalChildWidth();
        		// HO 10/12/2010 END *************
        	}
        	// now pad the label on the left
        	xOffset += LabelPadLeft;
        	return xOffset;
        	// HO 09/12/2010 END ************
        } else {
            // horizontally center if no icons

        	// HO 22/12/2010 BEGIN ********************
        	/* if (WrapText) {
                return mLabelPos.x;
            } else { */
            	// HO 22/12/2010 END ********************
                // todo problem: pre-existing default alignment w/out icons
                // is center label, left children: when we move to generally
                // suporting left/center/right alignment, that configuration won't
                // be supported: we may need a special "old-style" alignment style
        	// HO 22/12/2010 BEGIN ********************
                /* if (mAlignment.get() == Alignment.LEFT && hasFlag(Flag.SLIDE_STYLE)) {
                    return ChildPadX;
                } else if (mAlignment.get() == Alignment.RIGHT) {
                    return (this.width - getTextSize().width) - 1;
                } else { */
                	// HO 22/12/2010 END ********************
                    // CENTER:
                    // Doing this risks slighly moving the damn TextBox just as you edit it.
                	// HO 09/12/2010 BEGIN *****************
                	// we want it left-aligned, but positioned to the right of any children
                	// the below will just center it
                	// however we need to test at this point to see if there are children
                	float xOffset = 0f;
                	if (hasChildren())  {
                		// HO 10/12/2010 BEGIN *****************
                		//theOffset += EdgePadX + getMaxChildSpan() + LabelPadLeft;
                		xOffset += calculateTotalChildWidth() + LabelPadLeft;
                		// HO 09/12/2010 END *****************
                	} else {
                		xOffset += (this.width - getTextSize().width) / 2;
                	}
                	//theOffset += ((this.width - getTextSize().width) / 2);
                	//final float offset = (this.width - getTextSize().width) / 2;
                	final float offset = xOffset;
                	 // float childSpace = getMaxChildSpan();
                	// final float offset = childSpace;
                	// not sure that all the required space is included in here...
                	// HO 09/12/2010 END *****************
                	
                    
                    return offset + 1;
                 // HO 22/12/2010 BEGIN ********************
                //}
             // HO 22/12/2010 END ********************
             // HO 22/12/2010 BEGIN ********************
            //}
        	// HO 22/12/2010 END ********************
        }
    }
    
    /** Duplicate this node.
     * @return the new node -- will have the same style (visible properties) of the old node */
    @Override
    public LWIBISNode duplicate(CopyContext cc)
    {
        // HO 08/12/2010 BEGIN ***************
    	//LWIBISNode newNode = (LWIBISNode) super.duplicate(cc);
    	LWIBISNode newNode = (LWIBISNode) super.duplicate(cc, true);
    	// HO 08/12/2010 END ***************
        // make sure shape get's set with old size:
        if (DEBUG.STYLE) out("re-adjusting size during duplicate to set shape size");
        newNode.setSize(super.getWidth(), super.getHeight()); 
        return newNode;
    }
    
    public static boolean isTextNode(LWComponent c) {
        if (c instanceof LWIBISNode)
            return ((LWIBISNode)c).isTextNode();
        else
            return false;
    }

    /**
     * This is consulted during LAYOUT, which can effect the size of the node.
     * So if anything happens that changes what this returns, the node has
     * to be laid out again.  (E.g., if we turn them all of with a pref,
     * all nodes need to be re-laid out / resized
     */
    protected boolean iconShowing()
    {    	
//         if (hasFlag(Flag.SLIDE_STYLE) || isTextNode()) // VUE-1220 - never hide resource icon, even on slides
//             return false;
//          else
        return !hasFlag(Flag.INTERNAL) && mIconBlock.isShowing(); // remember not current till after a layout
    }

    //----------------------------------------------------------------------------------------
    // Crap.  We need the max child width first to know the min width for wrapped text,
    // but then need the text height to compute the child X location.
    //----------------------------------------------------------------------------------------

    /** will CHANGE min.width and min.height */ 
    private void layoutBoxed_children(Size min, Size labelText) {
        if (DEBUG.LAYOUT) out("*** layoutBoxed_children; min=" + min + " text=" + labelText);

        // HO 10/12/2010 BEGIN ************
        //mBoxedLayoutChildY = EdgePadY + labelText.height; // must set before layoutChildren, as may be used in childOffsetY()
        mBoxedLayoutChildY = EdgePadY;
        // HO 10/12/2010 BEGIN ************
        
        float minWidth;
        if (false && isPresentationContext()) {
            minWidth = Math.max(labelText.width, getWidth()-20);
            // Prob will have to just let it compute max child width, then center
            // the whole child box in the node (this isn't letting shrink node via drag-resize properly,
            // even with a 20px margin of error...)
        } else
            minWidth = 0;
        
        final Size children = layoutChildren(new Size(), minWidth, false);
        final float childSpan = childOffsetX() + children.width + ChildPadX;

        // HO 09/12/2010 BEGIN ******************
        // HO 10/12/2010 BEGIN ******************
        float textWidth = getTotalTextWidth();
        //float textWidth = getTextSize().width;
        //float textWidth = getTextSize().width + LabelPadRight;
        // HO 09/12/2010 END ******************
        float iconWidth = 0f;
        if (iconShowing())
        	iconWidth += IconMargin;
        float neededWidth = childSpan + textWidth + iconWidth;
        
        //if (min.width < childSpan)
            //min.width = childSpan;
        if (min.width < neededWidth)
            min.width = neededWidth;
        // HO 09/12/2010 END ******************
        
        // HO 10/12/2010 BEGIN ******************
        // the major height bug
        float neededHeight = Math.max(getTextSize().height, children.height);
        //float neededHeight = Math.max(getTextSize().height, calculateTotalChildHeight());
        //float neededHeight = Math.max(getTextSize().height, children.height, calculateTotalChildHeight());
        //min.height += children.height;
        min.height = neededHeight;
        // HO 10/12/2010 END ******************
        
        
        //min.height += ChildOffsetY + ChildrenPadBottom;
        min.height += ChildOffsetY + ChildrenPadBottom;
        min.height += childOffsetY() + ChildPadY; // additional space below last child before bottom of node
    }

    /** will CHANGE min */
    private void layoutBoxed_icon(Size request, Size min, Size text) {

        if (DEBUG.LAYOUT) out("*** layoutBoxed_icon");
        
        float iconWidth = IconWidth;
        float iconHeight = IconHeight;
        // HO 06/12/2010 BEGIN ***********
        //float iconX = IconPadLeft;
        // HO 06/12/2010 END ***********

        // this will be the X position of the Icon pillar, funnily enough
        float iconPillarX = 0;
        float iconPillarY = IconPillarPadY;

        float totalIconHeight = (float) mIconBlock.getHeight();
        float iconPillarHeight = totalIconHeight + IconPillarPadY * 2;

        // if the minimal height is less than needed to accommodate
        // the icon pillar, we need to make sure that the minimal
        // height becomes at least as high as the icon pillar
        if (min.height < iconPillarHeight) {
            min.height += iconPillarHeight - min.height;
        } else if (isRectShape) {
            // special case prettification -- if vertically centering
            // the icon stack would only drop it down by up to a few
            // pixels, go ahead and do so because it's so much nicer
            // to look at.
            float centerY = (min.height - totalIconHeight) / 2;
            if (centerY > IconPillarPadY+IconPillarFudgeY)
                centerY = IconPillarPadY+IconPillarFudgeY;
            iconPillarY = centerY;
        }
            
        if (!isRectShape) {
            float height;
            if (isAutoSized())
                height = min.height;
            else
                height = Math.max(min.height, request.height);
            iconPillarY = height / 2 - totalIconHeight / 2;
        }

        float width = Math.max(min.width, this.width);

        // I really don't think we can reliably set this at this point
        // because it's one thing if it's on the left: it's always going
        // to be a fixed distance from the left. But if it's on the right,
        // if the width isn't certain we can't be certain where it will go.
        iconPillarX = width - IconWidth;
      
        //mIconBlock.setLocation(iconPillarX, iconPillarY);

    }
    
    // HO 09/12/2010 BEGIN *********
    private float calculateTotalWidth() {
    	float totalWidth = 0f;
    	
    	return totalWidth;
    }
    // HO 09/12/2010 END *********

    /**
     * @param curSize - if non-null, re-layout giving priority to currently requested size (getWidth/getHeight)
     * if null, give priority to keeping the existing TexBox as unchanged as possible.
     *
     * @param request - requested size -- can be null, which means adjust size because something changed
     * @param curSize - the current/old size of the node, in case it's already been resized
     *
     *
     * @return new size of node, resizing the text box as needed -- because we're laying out
     * text, this is NOT the minimum size of the node: it includes request size
     */

    // TODO: need to have a special curSize that is the uninitialized size,
    // either that or a requestSize that is a special "natural" size, and in
    // this special case, put all on one line if width isn't "too big", (e.g.,
    // at least "Node Node" for sure), or if is really big (e.g., drop of a big
    // text clipping) set to some default preferred aspect, such as 3/4, or perhaps
    // the current screen aspect).

    // TODO: PROBLEM: if children wider than label, label is NOT STABLE!  TextBox can be dragged
    // full width of node, but then snaps back to min-size on re-layout!

    // todo: may not need all three args
    private Size layoutBoxed_floating_text(Size request, Size curSize, Object triggerKey)
    {
        if (DEBUG.LAYOUT) out("*** layoutBoxed_floating_text, req="+request + " cur=" + curSize + " trigger=" + triggerKey);

        final Size min = new Size(); // the minimum size of the Node

        getLabelBox(); // make sure labelBox is set

        //------------------------------------------------------------------
        // start building up minimum width & height
        //------------------------------------------------------------------
        
        if (iconShowing())
            min.width = LabelPositionXWhenIconShowing;
        else
            min.width = LabelPadLeft;
        min.width += LabelPadRight;
        min.height = EdgePadY + EdgePadY;

        final float textPadWidth = min.width;
        final float textPadHeight = min.height;

        //------------------------------------------------------------------
        // adjust minimum width & height for text size and requested size
        //------------------------------------------------------------------
        
        final Size newTextSize;
        final boolean resizeRequest;

        // resizeRequest is true if we're requesting a new size for
        // this node, otherwise, resizeRequest is false and some
        // property is changing that may effect the size of the node

        if (request == null) {
            resizeRequest = false;
            request = curSize;
        } else
            resizeRequest = true;

        if (hasChildren())
            request.fitWidth(getMaxChildSpan());

        //if (request.width <= MIN_SIZE && request.height <= MIN_SIZE) {
        if (curSize.width == NEEDS_DEFAULT) { // NEEDS_DEFAULT meaningless now: will never be true (oh, only on restore?)
            if (DEBUG.WORK) out("SETTING DEFAULT - UNITIALIZED WIDTH");
            // usually this happens with a new node
            newTextSize = new Size(labelBox.getPreferredSize());
        } else if (textSize == null) {
            if (DEBUG.WORK) out("SETTING DEFAULT - NO TEXT SIZE");
            newTextSize = new Size(labelBox.getPreferredSize());
        } else {
            //newTextSize = new Size();
            newTextSize = new Size(textSize);

            //if (triggerKey == LWKey.Size) {
            if (resizeRequest) {
                // ADJUST TEXT TO FIT NODE

                // fit the text to the new size as best we can.
                // (we're most likely drag-resizing the node)
                
                newTextSize.width = request.width - textPadWidth;
                newTextSize.height = request.height - textPadHeight;
                newTextSize.fitWidth(labelBox.getMaxWordWidth());


            } else {
                // ADJUST NODE TO FIT TEXT

                // adjust node size around text size
                // e.g., we changed font: trust that the labelBox is already sized as it needs to
                // be and size the node around it.
                
                //if (triggerKey == LWKey.Font && isAutoSized()) {
                //if (false && triggerKey == LWKey.Font) {
                if (true) {
                    // this should work even if our current width is > maxWordWidth
                    // and not matter if we're auto-sized or not: we just want
                    // to force an increase in the width only
                    
                    // So what's the new width?

                    // When NEWLINES are in text, preferred width is width of longest LINE.
                    // So actually, preferred with is always width of longest line.

                    // So how to handle the one-line that's been wrapped case?
                    // (a single UNWRAPPED line would in fact just use preferred size for new width in, eg., a bold font)

                    // Okay, either we're going to have to eat that case,
                    // (or delve into TextUI, etc: forget that!), or we
                    // could seek out the right width by slowly increasing it
                    // until preferred height comes to match the old preferred height...

                    // Or maybe, in fact, we don't want to do anything?  Could go either
                    // way: which is more important: the current size of the node
                    // or the current breaks in the text?  Can we do this only
                    // if autoSized?  Is autoSized even possible when text is wrapped?
                    // (and if not, we're not handling that right).  AND, autoSized
                    // may effect the hard-line-breaks case we think we have handled above...

                    // note that restoring wrapped text isn't working right now either...

                    // basically, we're trying to have a new kind of autoSized, which remembers
                    // the current user size, but on ADJUSTMENT does different things.

                    // AT LEAST: if our old txt width is equal to old max word width,
                    // then keep that same relationship here.

                    boolean keepPreferredWidth = false;
                    boolean keepMaxWordWidth = false;
                    final int curWidth = labelBox.getWidth();

                    // damn! if font set, labelBox preferred and max word width is already adjusted!
                    
                    if (curWidth == labelBox.getPreferredSize().width)
                        keepPreferredWidth = true;
                    else if (curWidth == labelBox.getMaxWordWidth())
                        keepMaxWordWidth = true;
                    
                    newTextSize.width = labelBox.getMaxWordWidth();
                } else {
                    newTextSize.width = labelBox.getWidth();
                    newTextSize.fitWidth(labelBox.getMaxWordWidth());
                }
                newTextSize.height = labelBox.getHeight();

            }
        }

        
        labelBox.setSizeFlexHeight(newTextSize);
        newTextSize.height = labelBox.getHeight();
        this.textSize = newTextSize.dim();
        
        min.height += newTextSize.height;
        min.width += newTextSize.width;

        //-------------------------------------------------------
        // Now that we have our minimum width and height, layout
        // the label and any icons.
        //-------------------------------------------------------

        if (hasChildren()) {
            layoutBoxed_children(min, newTextSize);
        }

        if (iconShowing())
            layoutBoxed_icon(request, min, newTextSize);
            
        return min;
    }
    
    // for computing size only
    private Size layoutChildren(Size result) {
        return layoutChildren(0f, 0f, 0f, result);
    }

    //private Rectangle2D child_box = new Rectangle2D.Float(); // for debug
    private Size layoutChildren(Size result, float minWidth, boolean sizeOnly)
    {
        if (DEBUG.LAYOUT) out("*** layoutChildren; sizeOnly=" + sizeOnly);
        
        if (!hasChildren())
            return Size.None;

        float baseX = 0;
        float baseY = 0;

        if (!sizeOnly) {
            baseX = childOffsetX();
            baseY = childOffsetY();
        }

        return layoutChildren(baseX, baseY, minWidth, result);
    }
        
    private void layoutChildren(float baseX, float baseY) {
        layoutChildren(baseX, baseY, 0f, null);
    }
    
    private Size layoutChildren(float baseX, float baseY, float minWidth, Size result)
    {
        if (DEBUG.LAYOUT) out("*** layoutChildren at " + baseX + "," + baseY);
        if (DEBUG.LAYOUT && DEBUG.META) Util.printClassTrace("tufts.vue.LW", "*** layoutChildren");
        //if (baseX > 0) new Throwable("LAYOUT-CHILDREN").printStackTrace();
//         if (isPresentationContext())
//             layoutChildrenGrid(baseX, baseY, result, 1, minWidth);
//         else
        if (hasFlag(Flag.SLIDE_STYLE) && mAlignment.get() != Alignment.LEFT && isImageNode(this))
            layoutChildrenColumnAligned(baseX, baseY, result);
        else
            layoutChildrenSingleColumn(baseX, baseY, result);

//         if (result != null) {
//             //if (DEBUG.BOXES)
//             //child_box.setRect(baseX, baseY, result.width, result.height);
//         }
        return result;
    }
    
    public static boolean isImageNode(LWComponent c) {
        if (c instanceof LWIBISNode) {
            final LWIBISNode node = (LWIBISNode) c;
            final LWComponent childZero = node.getChild(0);
            return childZero instanceof LWImage && childZero.hasResource() && childZero.getResource().equals(node.getResource());
        } else
            return false;
    }
    
    // If nColumn == 1, it does center layout.  minWidth only meant for single column
    protected void layoutChildrenGrid(float baseX, float baseY, Size result, int nColumn, float minWidth)
    {
        float y = baseY;
        float totalWidth = 0;
        float maxHeight = 0;
        
        Column[] cols = new Column[nColumn];
        java.util.Iterator i = getChildIterator();
        int curCol = 0;
        while (i.hasNext()) {
            LWComponent c = (LWComponent) i.next();
            if (cols[curCol] == null)
                cols[curCol] = new Column(minWidth);
            cols[curCol].addChild(c);
            if (++curCol >= nColumn)
                curCol = 0;
        }

        float colX = baseX;
        float colY = baseY;
        for (int x = 0; x < cols.length; x++) {
            Column col = cols[x];
            if (col == null)
                break;
            col.layout(colX, colY, nColumn == 1);
            colX += col.width + ChildHorizontalGap;
            totalWidth += col.width + ChildHorizontalGap;
            if (col.height > maxHeight)
                maxHeight = col.height;
        }
        // peel back the last gap as no more columns to right
        totalWidth -= ChildHorizontalGap;

        if (result != null) {
            result.width = totalWidth;
            result.height = maxHeight;
        }
    }
    
    class Column extends java.util.ArrayList<LWComponent>
    {
        float width;
        float height;

        Column(float minWidth) {
            width = minWidth;
        }

        void layout(float baseX, float baseY, boolean center)
        {
            float y = baseY;
            Iterator i = iterator();
            while (i.hasNext()) {
                LWComponent c = (LWComponent) i.next();
                if (center)
                    c.setLocation(baseX + (width - c.getLocalBorderWidth())/2, y);
                else
                    c.setLocation(baseX, y);
                y += c.getHeight();
                y += ChildVerticalGap * getScale();
            }
            height = y - baseY;
        }

        void addChild(LWComponent c)
        {
            super.add(c);
            float w = c.getLocalBorderWidth();
            if (w > width)
                width = w;
        }
    }

    
    protected void layoutChildrenSingleColumn(float baseX, float baseY, Size result)
    {
        float y = baseY;
        float maxWidth = 0;
        boolean first = true;

        for (LWComponent c : getChildren()) {
            if (c instanceof LWLink) // todo: don't allow adding of links into a manged layout node!
                continue;
            if (c.isHidden())
                continue;
            if (first)
                first = false;
            else
                y += ChildVerticalGap * getScale();
            c.setLocation(baseX, y);
            y += c.getLocalHeight();

            if (result != null) {
                // track max width
                float w = c.getLocalBorderWidth();
                if (w > maxWidth)
                    maxWidth = w;
            }
        }

        if (result != null) {
            result.width = maxWidth;
            result.height = (y - baseY);
        }
    }
    
    protected void layoutChildrenColumnAligned(float baseX, float baseY, Size result)
    {
        float maxWidth = 0;

        for (LWComponent c : getChildren()) {
            if (c instanceof LWLink) // todo: don't allow adding of links into a manged layout node!
                continue;
            float w = c.getLocalBorderWidth();
            if (w > maxWidth)
                maxWidth = w;
        }

        // TODO: need to re-arch to handle center/right alignment: e.g., removing widest
        // child doesn't do a re-layout, and on parent drag-resize, layout is falling behind
        
        float maxLayoutWidth = Math.max(maxWidth, getWidth() - baseX*2);
        
        float y = baseY;
        boolean first = true;
        for (LWComponent c : getChildren()) {
            if (c instanceof LWLink) // todo: don't allow adding of links into a manged layout node!
                continue;
            if (first)
                first = false;
            else
                y += ChildVerticalGap * getScale();

            if (mAlignment.get() == Alignment.RIGHT)
                c.setLocation(baseX + maxLayoutWidth - c.getLocalWidth(), y);
            else if (mAlignment.get() == Alignment.CENTER)
                c.setLocation(baseX + (maxLayoutWidth - c.getLocalWidth()) / 2, y);
            else
                c.setLocation(baseX, y);
            y += c.getLocalHeight();
        }

        if (result != null) {
            result.width = maxWidth;
            result.height = (y - baseY);
        }
    }

    /** Draw without rendering any textual glyphs, possibly without children, possibly as a rectanlge only */
    private void drawNodeWithReducedLOD(final DrawContext dc, final float renderScale)
    {
        //=============================================================================
        // DRAW FAST (with little or no detail)
        //=============================================================================

        // Level-Of-Detail rendering -- increases speed when lots of nodes rendered
        // all we do is fill the shape
                
        boolean hasVisibleFill = true;
        
        if (isSelected()) {
            dc.g.setColor(COLOR_SELECTION);
        } else {

            final Color renderFill = getRenderFillColor(dc);

            if (isTransparent() || renderFill.equals(getParent().getRenderFillColor(dc)))
                hasVisibleFill = false;
            else
                dc.g.setColor(renderFill);
        }

        if (this.height * renderScale > 5) {

            // MEDIUM LEVEL OF DETAIL: retain shape & draw children

            if (hasVisibleFill)
                dc.g.fill(getZeroShape());
            else
                drawLODTextLine(dc);

            if (hasChildren())
                drawChildren(dc);
                
        } else {

            // LOWEST LEVEL OF DETAIL -- shape is always a rectangle, don't draw children
            
            if (hasVisibleFill) {
                if (mShape.getClass() == Rectangle2D.Float.class)
                    dc.g.fill(mShape);
                else
                    dc.g.fillRect(0, 0, (int)getWidth(), (int)getHeight());
            } else
                drawLODTextLine(dc);
        }
                
    }
    
    @Override
    public RectangularShape getZeroShape() {
        return mShape;
    }
    
    @Override
    protected void drawChildren(DrawContext dc) {
        if (isCollapsed()) {
            if (COLLAPSE_IS_GLOBAL == false) {
                // draw an indicator on this individual node showing that it's collapsed
                dc.g.setStroke(STROKE_ONE);
                dc.g.setColor(getRenderFillColor(dc));
                final int bottom = (int) (getHeight() + getStrokeWidth() / 2f + 2.5f);
                dc.g.drawLine(1, bottom, (int) (getWidth() - 0.5f), bottom);
            }
            return;
        } else {
        	// HO 08/12/2010 BEGIN ***********
            //super.drawChildren(dc);
        	super.drawChildren(dc, true);
            // HO 08/12/2010 END ***********
        }
    }
    
    @Override
    public boolean isCollapsed() {
        if (COLLAPSE_IS_GLOBAL) {
            return isGlobalCollapsed;
        }
        else {
        	// HO 08/12/2010 BEGIN *********
            //return super.isCollapsed();
        	return super.isCollapsed(true);
            // HO 08/12/2010 BEGIN *********
        }
    }

    private void drawLODTextLine(final DrawContext dc) {
        final int hh = (int) ((getHeight() / 2f) + 0.5f);
        //dc.setAntiAlias(false); // too crappy
        dc.g.setStroke(STROKE_SEVEN);
        dc.g.setColor(mTextColor.get());
        dc.g.drawLine(0, hh, getLabelBox().getWidth(), hh);
    }

    private void drawSelection(DrawContext dc) {
        //             final LWPathway p = VUE.getActivePathway();
        //             if (p != null && p.isVisible() && p.getCurrentNode() == this) {
        //                 // SPECIAL CASE:
        //                 // as the current element on the current pathway draws a huge
        //                 // semi-transparent stroke around it, skip drawing our fat 
        //                 // transparent selection stroke on this node.  So we just
        //                 // do nothing here.
        //             } else {
        dc.g.setColor(COLOR_HIGHLIGHT);
        if (dc.zoom < 1)
            dc.setAbsoluteStroke(SelectionStrokeWidth);
        else
            dc.g.setStroke(new BasicStroke(getStrokeWidth() + SelectionStrokeWidth));
        dc.g.draw(mShape);
    }

    private void drawNode(DrawContext dc) {
        
        if (dc.isLODEnabled()) {

            // if net on-screen point size is less than 5 for all text, we allow drawing
            // with reduced LOD (level-of-detail)
            
            final float renderScale = (float) dc.getAbsoluteScale();            
            final float renderFont = mFontSize.get() * renderScale;
            final boolean canSkipLabel = renderFont < 5; 
            final boolean canSkipIcon;
            
            if (iconShowing())
                canSkipIcon = LWIcon.FONT_ICON.getSize() * renderScale < 5;
            else
                canSkipIcon = true;

            if (canSkipLabel && canSkipIcon) {
                drawNodeWithReducedLOD(dc, renderScale);
                return; // WE'RE DONE
            } // else: fall thru and draw full node
        }

        drawFullNode(dc);
    }
    

    /**  DRAW COMPLETE (with full detail) */
    private void drawFullNode(DrawContext dc)
    {
        //-------------------------------------------------------
        // Fill the shape (if it's not transparent)
        //-------------------------------------------------------
        
        if (false && (dc.isPresenting() || isPresentationContext())) { // old-style "turn off the wrappers"
            ; // do nothing: no fill
        } else {
            Color fillColor = getRenderFillColor(dc);
            if (fillColor != null && fillColor.getAlpha() != 0) { // transparent if null
                dc.g.setColor(fillColor);
                dc.g.fill(mShape);
            }
        }
        
        if (getStrokeWidth() > 0 /*&& !isPresentationContext() && !dc.isPresenting()*/) { // old style "turn off the wrappers"
                dc.g.setColor(getStrokeColor());
            dc.g.setStroke(this.stroke);
            dc.g.draw(mShape);
        }


        if (DEBUG.BOXES) {
            dc.setAbsoluteStroke(0.5);
            if (false && _lastNodeContent != null && !isRectShape) {
                dc.g.setColor(Color.darkGray);
                dc.g.draw(_lastNodeContent);
            } else {
                dc.g.setColor(Color.blue);
                dc.g.draw(mShape);
            }
        }
            
        //-------------------------------------------------------
        // Draw the generated icon
        //-------------------------------------------------------

        try {
            drawNodeDecorations(dc);
        } catch (Throwable t) {
            Log.error("decoration failed: " + this + " in + " + dc + "; " + t);
            Util.printStackTrace(t);
        }

        // todo: create drawLabel, drawBorder & drawBody
        // LWComponent methods so can automatically turn
        // this off in MapViewer, adjust stroke color for
        // selection, etc.
        
        // TODO BUG: label sometimes getting "set" w/out sending layout event --
        // has to do with case where we pre-fill a textbox with "label", and
        // if they type nothing we don't set a label, but that's not working
        // entirely -- it manages to not trigger an update event, but somehow
        // this.label is still getting set -- maybe we have to null it out
        // manually (and maybe labelBox also)
        
        if (hasLabel() && this.labelBox != null && this.labelBox.getParent() == null) {
            
            // if parent is not null, this box is an active edit on the map
            // and we don't want to paint it here as AWT/Swing is handling
            // that at the moment (and at a possibly slightly different offset)

            drawLabel(dc);
        }

    }
    
    protected void drawLabel(DrawContext dc)
    {
        float lx = relativeLabelX();
        float ly = relativeLabelY();
        dc.g.translate(lx, ly);
        //if (DEBUG.CONTAINMENT) System.out.println("*** " + this + " drawing label at " + lx + "," + ly);
        this.labelBox.draw(dc);
        dc.g.translate(-lx, -ly);
        
        // todo: this (and in LWLink) is a hack -- can't we
        // do this relative to the node?
        //this.labelBox.setMapLocation(getX() + lx, getY() + ly);
    }

    private void drawNodeDecorations(DrawContext dc)
    {
        final Graphics2D g = dc.g;
            
        //-------------------------------------------------------
        // paint the node icons
        //-------------------------------------------------------

        if (/*!dc.isPresenting() &&*/ iconShowing()) {
            mIconBlock.draw(dc);
            // draw divider if there's a label
            if (hasLabel()) {
                g.setColor(getContrastStrokeColor(dc));
                g.setStroke(STROKE_ONE);
                g.draw(mIconDivider);
            }
        }
    }
    
    public boolean handleSingleClick(MapMouseEvent e)
    {
        final Point2D.Float localPoint = e.getLocalPoint(this);
        final float cx = localPoint.x;
        final float cy = localPoint.y;
        
    	 if (!textBoxHit(cx, cy)) 
    	 {
             return mIconBlock.handleSingleClick(e);
    	 }
         return false;
    }
    
    @Override
    public void mouseOver(MapMouseEvent e)
    {
        if (iconShowing())
            mIconBlock.checkAndHandleMouseOver(e);
    }

    private float childOffsetX() {
        if (isCenterLayout) {
            //System.out.println("\tchildPos.x=" + mChildPos.x);
            return mChildPos.x;
        }
        // HO 10/12/2010 BEGIN *************
        //return iconShowing() ? ChildOffsetX : ChildPadX;
        return ChildPadX;
        // HO 10/12/2010 END ************
    }
    private float childOffsetY() {
        if (isCenterLayout) {
            return mChildPos.y;
        }
        float baseY = 0f;
        if (iconShowing()) {
            baseY = mBoxedLayoutChildY;
            if (DEBUG.LAYOUT) out("*** childOffsetY starting with precomputed " + baseY + " to produce " + (baseY + ChildOffsetY));
        } else {
        	// HO 10/12/2010 BEGIN ******************
            /* final TextBox labelBox = getLabelBox();
            int labelHeight = labelBox == null ? 12 : labelBox.getHeight();
            baseY = relativeLabelY() + labelHeight; */
        	//baseY = relativeLabelY();
            // HO 10/12/2010 END ******************
        }
        baseY += ChildOffsetY;
        return baseY;
    }

    //------------------------------------------------------------------
    // Constants for layout of the visible objects in a node.
    // This is some scary stuff.
    // (label, icons & children, etc)
    //------------------------------------------------------------------

    // HO 09/12/2010 BEGIN ***************
    private static final int EdgePadX = 2;
    // HO 09/12/2010 END ***************
    private static final int EdgePadY = 4; // Was 3 in VUE 1.5
    private static final int PadTop = EdgePadY;

    private static final int IconGutterWidth = 26;

    private static final int IconPadLeft = 2;
    private static final int IconPadRight = 0;
    
    private static final int IconWidth = IconGutterWidth - IconPadLeft; // 22 is min width that will fit "www" in our icon font
    private static final int IconHeight = VueResources.getInt("node.icon.height", 14);

    private static final int IconMargin = IconPadLeft + IconWidth + IconPadRight;
    /** this is the descent of the closed icon down below the divider line */
    private static final float IconDescent = IconHeight / 3f;
    /** this is the rise of the closed icon above the divider line */
    private static final float IconAscent = IconHeight - IconDescent;
    private static final int IconPadBottom = (int) IconAscent;
    private static final int IconMinY = IconPadLeft;

    // HO 22/12/2010 BEGIN *****************
    //private static final int LabelPadLeft = 8; // Was 6 in VUE 1.5; fixed distance to right of iconMargin dividerLine
    //private static final int LabelPadRight = 8; // Was 6 in VUE 1.5; minimum gap to right of text before right edge of node
    private static final int LabelPadLeft = 2; // Was 6 in VUE 1.5; fixed distance to right of iconMargin dividerLine
    private static final int LabelPadRight = 2; // Was 6 in VUE 1.5; minimum gap to right of text before right edge of node
    // HO 22/12/2010 END *****************
    private static final int LabelPadX = LabelPadLeft;
    private static final int LabelPadY = EdgePadY;
    // HO 09/12/2010 BEGIN ************
    //private static final int LabelPositionXWhenIconShowing = IconMargin + LabelPadLeft;
    //private static final int LabelPositionXWhenIconShowing = IconMargin + LabelPadRight;
    private static final int LabelPositionXWhenIconShowing = LabelPadLeft;
    // HO 09/12/2010 END ************

    // TODO: need to multiply all these by ChildScale (huh?)
    
    // HO 09/12/2010 BEGIN ******************
    //private static final int ChildOffsetX = IconMargin + LabelPadLeft; // X offset of children when icon showing
    //private static final int ChildOffsetX = LabelPadLeft + IconMargin + LabelPadRight; // X offset of children when icon showing
    //private static final int ChildOffsetX = LabelPadLeft; // X offset of children when icon showing    
    // HO 22/12/2010 BEGIN *****************
    private static final int ChildPadX = 5; // min space at left/right of children
    //private static final int ChildPadX = 2; // min space at left/right of children
    // HO 22/12/2010 END *****************
    private static final int ChildOffsetX = ChildPadX; // X offset of children when icon showing
    // HO 09/12/2010 END ******************
    private static final int ChildOffsetY = 4; // how far children down from bottom of label divider line
    private static final int ChildPadY = ChildOffsetY;
    private static final int ChildVerticalGap = 3; // vertical space between children
    private static final int ChildHorizontalGap = 3; // horizontal space between children
    private static final int ChildrenPadBottom = ChildPadX - ChildVerticalGap; // make same as space at right
    
    
    private static final float DividerStubAscent = IconDescent;
    
    // at some zooms (some of the more "irregular" ones), we get huge
    // understatement errors from java in computing the width of some
    // font strings, so this pad needs to be big enough to compensate
    // for the error in the worst case, which we're guessing at here
    // based on a small set of random test cases.
    private static final float TextWidthFudgeFactor = 1; // off for debugging (Almost uneeded in new Mac JVM's)
    // put back to constant??  Also TODO: Text nodes left-aligned, not centered, and for real disallow BG color.

    private static final int MarginLinePadY = 5;
    private static final int IconPillarPadY = MarginLinePadY;
    private static final int IconPillarFudgeY = 4; // attempt to get top icon to align with top of 1st caps char in label text box

    /** for castor restore, internal default's and duplicate use only
     * Note special case: this creates a node with autoSized set to false -- this is probably for backward compat with old save files */
    public LWIBISNode()
    {
        initNode();
        isRectShape = true;
        isAutoSized = false;
        // I think we may only need this default shape setting for backward compat with old save files.
        mShape = new java.awt.geom.Rectangle2D.Float();

        // Force the creation of the TextBox (this.labelBox).
        // We need this for now to make sure wrapped text nodes don't unwrap
        // to one line on restore. I think the TextBox needs to pick up our size
        // before setLabel for it to work.
        //getLabelBox(); LAYOUT-NEW
    }

    /**
     * construct an absolutely minimal node, completely uninitialized (including label, font, size, etc) except for having a rectangular shape
     * Useful for constructing a node that's immediatley going to be styled.
     */
    public static LWIBISNode createRaw()
    {
        LWIBISNode n = new LWIBISNode();
        n.isAutoSized = true;
        return n;
    }
    
    protected float relativeLabelY()
    {
    	if (isCenterLayout) {
            return mLabelPos.y;
        } // HO 22/12/2010 BEGIN ********************
    	/*
    	else if (hasChildren()) {
            return EdgePadY;
        } else {
            
            if (false && WrapText)
                return mLabelPos.y;
                */
            	// HO 22/12/2010 END ********************
            else { 
                // Doing this risks slighly moving the damn TextBox just as you edit it.
                // Tho querying the underlying TextBox for it's size every time
                // we repaint this object is pretty gross also (e.g., every drag)
                return (this.height - getTextSize().height) / 2;
             }
    	// HO 22/12/2010 BEGIN ********************
        /*    
        }*/
    	// HO 22/12/2010 END ********************
    }
    
    public Class<? extends LWImage> getNodeImageClass() {
    	return nodeImageClass;
    }
    
    public void setNodeImageClass(Class<? extends LWImage> theClass) {
    	nodeImageClass = theClass;
    }
    
    public String getIBISType() {
    	return mIBISType;
    }
    
    public void setIBISType(String theType) {
    	mIBISType = theType;
    }
    
    // HO 16/12/2010 BEGIN ***********
    public void determineNodeImageAndType () {
    	File thisImage = this.getResource().getActiveDataFile();
    	if (thisImage.equals(new IBISIssueImage().getImageFile())) {
    		setIBISType(VueResources.getString("IBISNodeTool.issue.type"));
    		setNodeImageClass(IBISIssueImage.class);
    	} else if (thisImage.equals(new IBISIssue_ResolvedImage().getImageFile())) {
    		setIBISType(VueResources.getString("IBISNodeTool.issue_resolved.type"));
    		setNodeImageClass(IBISIssue_ResolvedImage.class);
    	} else if (thisImage.equals(new IBISIssue_InsolubleImage().getImageFile())) {
    		setIBISType(VueResources.getString("IBISNodeTool.issue_insoluble.type"));
    		setNodeImageClass(IBISIssue_InsolubleImage.class);
    	} else if (thisImage.equals(new IBISIssue_RejectedImage().getImageFile())) {
    		setIBISType(VueResources.getString("IBISNodeTool.issue_rejected.type"));
    		setNodeImageClass(IBISIssue_RejectedImage.class);
    	}	else if (thisImage.equals(new IBISAnswerImage().getImageFile())) {
    		setIBISType(VueResources.getString("IBISNodeTool.answer.type"));
    		setNodeImageClass(IBISAnswerImage.class);
    	}	else if (thisImage.equals(new IBISAnswer_AcceptedImage().getImageFile())) {
    		setIBISType(VueResources.getString("IBISNodeTool.answer_accepted.type"));
    		setNodeImageClass(IBISAnswer_AcceptedImage.class);
    	} else if (thisImage.equals(new IBISAnswer_LikelyImage().getImageFile())) {
    		setIBISType(VueResources.getString("IBISNodeTool.answer_likely.type"));
    		setNodeImageClass(IBISAnswer_LikelyImage.class);
    	} else if (thisImage.equals(new IBISAnswer_UnlikelyImage().getImageFile())) {
    		setIBISType(VueResources.getString("IBISNodeTool.answer_unlikely.type"));
    		setNodeImageClass(IBISAnswer_UnlikelyImage.class);
    	} else if (thisImage.equals(new IBISAnswer_RejectedImage().getImageFile())) {
    		setIBISType(VueResources.getString("IBISNodeTool.answer_rejected.type"));
    		setNodeImageClass(IBISAnswer_RejectedImage.class);
    	} else if (thisImage.equals(new IBISProArgumentImage().getImageFile())) {
    		setIBISType(VueResources.getString("IBISNodeTool.pro_argument.type"));
    		setNodeImageClass(IBISProArgumentImage.class);
    	} else if (thisImage.equals(new IBISProArgument_DominantImage().getImageFile())) {
    		setIBISType(VueResources.getString("IBISNodeTool.pro_argument_dominant.type"));
    		setNodeImageClass(IBISProArgument_DominantImage.class);
    	} else if (thisImage.equals(new IBISProArgument_FailingImage().getImageFile())) {
    		setIBISType(VueResources.getString("IBISNodeTool.pro_argument_failing.type"));
    		setNodeImageClass(IBISProArgument_FailingImage.class);
    	} else if (thisImage.equals(new IBISConArgumentImage().getImageFile())) {
    		setIBISType(VueResources.getString("IBISNodeTool.con_argument.type"));
    		setNodeImageClass(IBISConArgumentImage.class);
    	} else if (thisImage.equals(new IBISConArgument_DominantImage().getImageFile())) {
    		setIBISType(VueResources.getString("IBISNodeTool.con_argument_dominant.type"));
    		setNodeImageClass(IBISConArgument_DominantImage.class);
    	} else if (thisImage.equals(new IBISConArgument_FailingImage().getImageFile())) {
    		setIBISType(VueResources.getString("IBISNodeTool.con_argument_failing.type"));
    		setNodeImageClass(IBISConArgument_FailingImage.class);
    	} else {
    		setIBISType(null);
    		setNodeImageClass(null);
    	}
    }
    // HO 16/12/2010 END ***********
    
    
    
}