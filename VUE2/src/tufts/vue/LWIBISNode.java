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
import javax.swing.ImageIcon;

/**
 * @author 
 */

// todo: node layout code could use cleanup, as well as additional layout
// features (multiple columns).
// todo: "text" nodes are currently a total hack

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
    
    /** how much smaller children are than their immediately enclosing parent (is cumulative) */
    static final float ChildScale = VueResources.getInt("node.child.scale", 75) / 100f;

    //------------------------------------------------------------------
    // Instance info
    //------------------------------------------------------------------
    
    
    // HO 23/11/2010 BEGIN ******************
    /** 0 based with current local width/height */
    // protected RectangularShape mShape;
    // protected boolean isAutoSized = true; // compute size from label & children
    // HO 23/11/2010 END ******************
    
    // HO 03/11/2010 BEGIN ******************
    protected LWImage mIBISImage;
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


    private final LWIcon.Block mIconBlock =
        new LWIcon.Block(this,
                         IconWidth, IconHeight,
                         null,
                         LWIcon.Block.VERTICAL);

    

    private void initNode() {
        enableProperty(KEY_Alignment);
    }
    
    LWIBISNode(String label, float x, float y, RectangularShape shape)
    {
        super(label, x, y, shape);        
    }
    
    public LWIBISNode(String label) {
    	super(label);
    }

    public LWIBISNode(String label, IBISImage image) {
    	this(label, 0, 0);
    	if(image == null) {
    		// if there's no image, assign the default
    		setImage(IBISAcceptedIssueImage.class);
    	} else if (image != null) {
    		setImageInstance(image);
    	}
    	// make sure the fill color is white
    	this.setFillColor(java.awt.Color.white);
    }

    LWIBISNode(String label, RectangularShape shape) {
    	super(label, shape);
    }
    
    LWIBISNode(String label, float x, float y) {
    	super(label, x, y);
    }
    
    LWIBISNode(String label, Resource resource)
    {
    	super(label, resource);
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
            	if (c.mIBISImage != null)
            		return c.mIBISImage.getClass();
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
    
    // HO 03/11/2010 BEGIN *****************************
    /**
     * @param imageClass -- a class object this is a subclass of LWImage
     */
    public void setImage(Class<? extends LWImage> imageClass) {

        if (mIBISImage != null && IsSameImage(mIBISImage.getClass(), imageClass))
            return;

        try {
            setImageInstance(imageClass.newInstance());
        } catch (Throwable t) {
            tufts.Util.printStackTrace(t);
        }
    }
    
    /**
     * @param image a new instance of an image for us to use: should be a clone and not an original
     */
    protected void setImageInstance(LWImage image)
    {
        if (DEBUG.CASTOR) System.out.println("IMAGE " + image.getClass() + " in " + this + " " + image);

        if (IsSameImage(mIBISImage, image))
            return;

        final IBISImage old = (IBISImage)mIBISImage;
        isIBISNode = (image instanceof IBISImage);

        if (mIBISImage == null) {
        	mIBISImage = image;
        	//mIBISImage.setFrame(0, 0, getWidth(), getHeight());
            
            //this.addChild(mIBISImage);
        	this.setResource(mIBISImage.getResource());
            //layout(LWKey.IBISSymbol);
            // HO 03/11/2010 shouldn't need to update links?
            //updateConnectedLinks(null);
        }
        
        
        // HO 01/12/2010 BEGIN out with the old
        if (old != null) {
        	Resource resource = image.getResource();
        	this.setResource(resource);
        	mIBISImage = image;
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
        return mIBISImage;
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
    private void reparentWormholeNode(java.util.Collection<? extends LWComponent> children) {
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

    }
    // HO 22/09/2010 END ******************

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
        super.setSizeImpl(w, h, false);
        mShape.setFrame(0, 0, getWidth(), getHeight());
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
            mIconDivider.setLine(IconMargin, MarginLinePadY, IconMargin, newHeight-MarginLinePadY);
            // mIconDivider set by layoutCentered in the other case
        }

        if (labelBox != null)
            labelBox.setBoxLocation(relativeLabelX(), relativeLabelY());
        
        //if (this.parent != null && this.parent instanceof LWMap == false) {
        if (isLaidOut()) {
            // todo: should only need to do if size changed
            this.parent.layout();
        }
        
        inLayout = false;
    }

    private int getTextWidth() {
        if (WrapText)
            return labelBox.getWidth();
        else
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
         * LWNode member variables, laying out the child nodes, etc, until
         * layoutTargts() is called).
         */
        NodeContent()
        {
            if (hasLabel()) {
                Size text = getTextSize();
                rLabel.width = text.width;
                rLabel.height = text.height;
                rLabel.x = ChildPadX;
                this.width = ChildPadX + text.width;
                this.height = text.height;
            } 
            if (iconShowing()) {
                rIcons = new Rectangle2D.Float(0, 0, mIconBlock.width, mIconBlock.height);
                this.width += mIconBlock.width;
                this.width += ChildPadX; // add space at right of label to match space at left
                // move label to right to make room for icon block at left
                rLabel.x += mIconBlock.width;
            }
            if (hasChildren() && !isCollapsed()) {
                Size children = layoutChildren(new Size(), 0f, true);
                float childx = rLabel.x;
                float childy = rLabel.height + ChildPadY;
                rChildren = new Rectangle2D.Float(childx,childy, children.width, children.height);

                // can set absolute height based on label height & children height
                this.height = rLabel.height + ChildPadY + children.height;

                // make sure we're wide enough for the children in case children wider than label
                fitWidth(rLabel.x + children.width); // as we're 0 based, rLabel.x == width of gap at left of children
            }
            
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

        /** do the center-layout for the actual targets (LWNode state) of our regions */
        void layoutTargets() {
            if (DEBUG.LAYOUT) out("*** laying out targets");
            mLabelPos.setLocation(x + rLabel.x, y + rLabel.y);
            if (rIcons != null) {
                mIconBlock.setLocation(x + rIcons.x, y + rIcons.y);
                // Set divider line to height of the content, at right of icon block
                mIconDivider.setLine(mIconBlock.x + mIconBlock.width, this.y,
                                     mIconBlock.x + mIconBlock.width, this.y + this.height);
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
    
    private Size layoutBoxed(Size request, Size oldSize, Object triggerKey) {
        final Size min;
        
        if (WrapText)
            min = layoutBoxed_floating_text(request, oldSize, triggerKey);
        else
            min = layoutBoxed_vanilla(request);

        return min;

    }

    
    /** @return new minimum size of node */
    private Size layoutBoxed_vanilla(final Size request)
    {
        final Size min = new Size();
        final Size text = getTextSize();

        min.width = text.width;
        min.height = EdgePadY + text.height + EdgePadY;

        // *** set icon Y position in all cases to a centered vertical
        // position, but never such that baseline is below bottom of
        // first icon -- this is tricky tho, as first icon can move
        // down a bit to be centered with the label!

        if (!iconShowing()) {
            min.width += LabelPadLeft;
        } else {
            float dividerY = EdgePadY + text.height;
            double stubX = LabelPositionXWhenIconShowing + text.width;
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
        
        if (hasChildren()) {
            mLabelPos.y = EdgePadY;
        } else {
            // only need this in case of small font sizes and an icon
            // is showing -- if so, center label vertically in row with the first icon
            // Actually, no: center in whole node -- gak, we really want both,
            // but only to a certian threshold -- what a hack!
            //float textHeight = getLabelBox().getPreferredSize().height;
            //mLabelPos.y = (this.height - textHeight) / 2;
            mLabelPos.y = (this.height - text.height) / 2;
        }

        if (iconShowing()) {
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
        }
        
    }

    //----------------------------------------------------------------------------------------
    // Crap.  We need the max child width first to know the min width for wrapped text,
    // but then need the text height to compute the child X location.
    //----------------------------------------------------------------------------------------

    /** will CHANGE min.width and min.height */ 
    private void layoutBoxed_children(Size min, Size labelText) {
        if (DEBUG.LAYOUT) out("*** layoutBoxed_children; min=" + min + " text=" + labelText);

        mBoxedLayoutChildY = EdgePadY + labelText.height; // must set before layoutChildren, as may be used in childOffsetY()

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

        if (min.width < childSpan)
            min.width = childSpan;
        
        min.height += children.height;
        min.height += ChildOffsetY + ChildrenPadBottom; // additional space below last child before bottom of node
    }

    /** will CHANGE min */
    private void layoutBoxed_icon(Size request, Size min, Size text) {

        if (DEBUG.LAYOUT) out("*** layoutBoxed_icon");
        
        float iconWidth = IconWidth;
        float iconHeight = IconHeight;
        float iconX = IconPadLeft;

        float iconPillarX = iconX;
        float iconPillarY = IconPillarPadY;

        float totalIconHeight = (float) mIconBlock.getHeight();
        float iconPillarHeight = totalIconHeight + IconPillarPadY * 2;


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
            
        mIconBlock.setLocation(iconPillarX, iconPillarY);

    }

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

    private float childOffsetX() {
        if (isCenterLayout) {
            //System.out.println("\tchildPos.x=" + mChildPos.x);
            return mChildPos.x;
        }
        return iconShowing() ? ChildOffsetX : ChildPadX;
    }
    private float childOffsetY() {
        if (isCenterLayout) {
            return mChildPos.y;
        }
        float baseY;
        if (iconShowing()) {
            baseY = mBoxedLayoutChildY;
            if (DEBUG.LAYOUT) out("*** childOffsetY starting with precomputed " + baseY + " to produce " + (baseY + ChildOffsetY));
        } else {
            final TextBox labelBox = getLabelBox();
            int labelHeight = labelBox == null ? 12 : labelBox.getHeight();
            baseY = relativeLabelY() + labelHeight;
        }
        baseY += ChildOffsetY;
        return baseY;
    }

    //------------------------------------------------------------------
    // Constants for layout of the visible objects in a node.
    // This is some scary stuff.
    // (label, icons & children, etc)
    //------------------------------------------------------------------

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

    private static final int LabelPadLeft = 8; // Was 6 in VUE 1.5; fixed distance to right of iconMargin dividerLine
    private static final int LabelPadRight = 8; // Was 6 in VUE 1.5; minimum gap to right of text before right edge of node
    private static final int LabelPadX = LabelPadLeft;
    private static final int LabelPadY = EdgePadY;
    private static final int LabelPositionXWhenIconShowing = IconMargin + LabelPadLeft;

    // TODO: need to multiply all these by ChildScale (huh?)
    
    private static final int ChildOffsetX = IconMargin + LabelPadLeft; // X offset of children when icon showing
    private static final int ChildOffsetY = 4; // how far children down from bottom of label divider line
    private static final int ChildPadY = ChildOffsetY;
    private static final int ChildPadX = 5; // min space at left/right of children
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
        super();
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
    
    
    
}