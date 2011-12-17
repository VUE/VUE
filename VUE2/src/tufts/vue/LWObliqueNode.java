package tufts.vue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import tufts.Util;
import tufts.vue.LWComponent.CopyContext;
import tufts.vue.LWComponent.Key;
import tufts.vue.ibisimage.IBISIssueImage;

public class LWObliqueNode extends LWNode {

    
    protected static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LWObliqueNode.class);
    
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
    
    //private static final int IconPadRight = 4;
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
    //    private static final int ChildrenPadBottom = 3; // space at bottom after all children
    
    
    private static final float DividerStubAscent = IconDescent;
    
    // at some zooms (some of the more "irregular" ones), we get huge
    // understatement errors from java in computing the width of some
    // font strings, so this pad needs to be big enough to compensate
    // for the error in the worst case, which we're guessing at here
    // based on a small set of random test cases.
    //private static final float TextWidthFudgeFactor = 1 + 0.1f; // 10% fudge
    //private static final float TextWidthFudgeFactor = 1 + 0.05f; // 5% fudge
    private static final float TextWidthFudgeFactor = 1; // off for debugging (Almost uneeded in new Mac JVM's)
    // put back to constant??  Also TODO: Text nodes left-aligned, not centered, and for real disallow BG color.
    //private static final float TextWidthFudgeFactor = 1;
    //private static final int DividerStubPadX = TextWidthFudgeAmount;

    private static final int MarginLinePadY = 5;
    private static final int IconPillarPadY = MarginLinePadY;
    private static final int IconPillarFudgeY = 4; // attempt to get top icon to align with top of 1st caps char in label text box
		
    public LWObliqueNode(String label) {    	
    	this(label, 0, 0);
    	// make sure the fill color is white
    	this.setFillColor(java.awt.Color.white);
    	this.setStrokeWidth(1);
    }
    
    LWObliqueNode(String label, float x, float y) {
    	this(label, x, y, null);
    }
    
    LWObliqueNode(String label, float x, float y, RectangularShape shape)
    {
        initNode();
        super.setLabel(label, true);
        setFillColor(java.awt.Color.white);
        if (shape == null)
            setShape(tufts.vue.shape.RoundRect2D.class);
        else if (shape != null)
            setShapeInstance(shape);
        setStrokeWidth(1);
        setStrokeColor(java.awt.Color.black);
        setLocation(x, y);
        this.width = 120;
        this.height = 60;
        setFont(VueResources.getFont("node.font"));
        setLabel(label); 
    }
    
    LWObliqueNode(String label, RectangularShape shape) {
    	this(label, 0, 0, shape);
    }
	
    /** for castor restore, internal default's and duplicate use only
     * Note special case: this creates a node with autoSized set to false -- this is probably for backward compat with old save files */
    public LWObliqueNode()
    {
        initNode();
        isRectShape = true;
        isAutoSized = false;
        // I think we may only need this default shape setting for backward compat with old save files.
        mShape = new java.awt.geom.Rectangle2D.Float();
    }
    
    private void initNode() {
        enableProperty(KEY_Alignment);
    }
	
    public static final Key KEY_Shape =
        new Key<LWNode,Class<? extends RectangularShape>>("node.shape", "shape") {
        @Override
        public boolean setValueFromCSS(LWNode c, String cssKey, String cssValue) {
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
        public void setValue(LWNode c, Class<? extends RectangularShape> shapeClass) {
            c.setShape(shapeClass);
        }
        @Override
        public Class<? extends RectangularShape> getValue(LWNode c) {
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
        boolean valueEquals(LWNode c, Object other)
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

    /**
     * This fixed value depends on the arc width/height specifications in our RoundRect2D, which
     * are currently 20,20.
     * If that ever changes, this will need to be recomputed (see commented out code in
     * in getZeroNorthWestCorner) and updated.
     * @see tufts.vue.shape.RoundRect2D
     */
    private static final Point2D RoundRectCorner = new Point2D.Double(2.928932, 2.928932);
    
    public static boolean isImageNode(LWComponent c) {
        if (c instanceof LWObliqueNode) {
            final LWObliqueNode node = (LWObliqueNode) c;
            final LWComponent childZero = node.getChild(0);
            return childZero instanceof LWImage && childZero.hasResource() && childZero.getResource().equals(node.getResource());
        } else
            return false;
    }
    
    public static boolean isTextNode(LWComponent c) {
        if (c instanceof LWObliqueNode)
            return ((LWObliqueNode)c).isTextNode();
        else
            return false;
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
    
    static boolean isScaledChildType(LWComponent c) {
        return c instanceof LWNode || c instanceof LWSlide; // slide testing
    }
    
    /**
     * construct an absolutely minimal node, completely uninitialized (including label, font, size, etc) except for having a rectangular shape
     * Useful for constructing a node that's immediatley going to be styled.
     */
    public static LWObliqueNode createRaw()
    {
        LWObliqueNode n = new LWObliqueNode("Oblique Strategies");
        n.isAutoSized = true;
        return n;
    }
}
