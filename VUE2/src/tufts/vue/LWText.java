package tufts.vue;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.Point2D.Float;

import tufts.Util;


public class LWText extends LWComponent {
	public static final Object TYPE_RICHTEXT = "richTextNode";
	protected transient RichTextBox labelBox = null;
    //protected RectangularShape mShape;
	private boolean WrapText = false;

	protected boolean isAutoSized = true; // compute size from label &
											// children
	public LWText() {
		super();
		
                // VUE-747
		Float p = null; 
                 
                if(VUE.getActiveViewer() != null)
                {
                       p =  VUE.getActiveViewer().getLastMapMousePoint();
                }
                
                if(p!=null)
                {    
		   setLocation(p.x, p.y);
                }
                // end VUE-747 mods
               
                super.label = label; // make sure label initially set for debugging
                initText();
	}

	public String getRichText()
	{
		return labelBox.getRichText();
	}
	public void setRichText(String text)
	{
		super.label = text;
		return;
	}
	public LWText(String label) {
		super();
		super.label = label; // make sure label initially set for debugging
		// super(label, 0, 0);
		// setAsTextNode(true);
                initText();

	}

	public LWText(String label, RectangularShape shape) {
		// super(label, 0, 0, shape);
		super.label = label; // make sure label initially set for debugging
		// setAsTextNode(true);
                initText();
	}

    private void initText() {
        //enableProperty(KEY_Alignment);
        //disablePropertyTypes(KeyType.STYLE);
        disableProperty(LWKey.Font);
        disableProperty(LWKey.FontName);
        disableProperty(LWKey.FontSize);
        disableProperty(LWKey.FontStyle);
        disableProperty(LWKey.TextColor);
        //mShape = new java.awt.geom.Rectangle2D.Float();
    }

	
	public Object getTypeToken() {
		return TYPE_RICHTEXT;
	}

	protected RichTextBox getRichLabelBox() {
		if (this.labelBox == null) {
			synchronized (this) {
				if (this.labelBox == null)
					this.labelBox = new RichTextBox(this, this.label);
			}
		}

		return this.labelBox;
	}

	public boolean isTextNode() {

		return true;
	}

	public void initRichTextBoxLocation(RichTextBox activeRichTextEdit) {
		activeRichTextEdit.setBoxCenter(getWidth() / 2, getHeight() / 2);

	}

	protected void drawNode(DrawContext dc) {
		// -------------------------------------------------------
		// Fill the shape (if it's not transparent)
		// -------------------------------------------------------

            getZeroShape(); // will load super.mZeroBounds

		if (isSelected() && dc.isInteractive() && dc.focal != this) {
			LWPathway p = VUE.getActivePathway();
			if (p != null && p.isVisible() && p.getCurrentNode() == this) {
				// SPECIAL CASE:
				// as the current element on the current pathway draws a huge
				// semi-transparent stroke around it, skip drawing our fat
				// transparent selection stroke on this node. So we just
				// do nothing here.
			} else {
				dc.g.setColor(COLOR_HIGHLIGHT);
                                if (isTransparent()) {
                                    dc.g.fill(grow((Rectangle2D.Float) mZeroBounds.clone(), SelectionStrokeWidth/2f));
                                } else {
                                    dc.g.setStroke(new BasicStroke(getStrokeWidth() + SelectionStrokeWidth));
                                    dc.g.draw(getZeroShape());
                                }
			}
		}

		// if (imageIcon != null) { // experimental
		// //imageIcon.paintIcon(null, g, (int)getX(), (int)getY());
		// imageIcon.paintIcon(null, dc.g, 0, 0);
		// } else

		if (false && (dc.isPresenting() || isPresentationContext())) { // old-style
			// "turn
			// off
			// the
			// wrappers"
			; // do nothing: no fill
		} else {
                        final Color fillColor = getFillColor();
			if (fillColor != null && fillColor.getAlpha() != 0) { // transparent
				// if null
                                dc.g.setColor(fillColor);
				// if (isZoomedFocus()) dc.g.setComposite(ZoomTransparency);
				dc.g.fill(mZeroBounds);
				// if (isZoomedFocus()) dc.g.setComposite(AlphaComposite.Src);
			}
		}

		/*
		 * if (!isAutoSized()) { // debug g.setColor(Color.green);
		 * g.setStroke(STROKE_ONE); g.draw(zeroShape); } else if
		 * (false&&isRollover()) { // debug // temporary debug //g.setColor(new
		 * Color(0,0,128)); g.setColor(Color.blue); g.draw(zeroShape); } else
		 */

		if (getStrokeWidth() > 0 /*
									 * && !isPresentationContext() &&
									 * !dc.isPresenting()
									 */) { // old
			// style
			// "turn
			// off
			// the
			// wrappers"
			// if (LWSelection.DEBUG_SELECTION && isSelected())
			// if (isSelected())
			// g.setColor(COLOR_SELECTION);
			// else
			dc.g.setColor(getStrokeColor());
			dc.g.setStroke(this.stroke);
			dc.g.draw(mZeroBounds);
		}

		// -------------------------------------------------------
		// Draw the generated icon
		// -------------------------------------------------------

		// drawNodeDecorations(dc);

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

		if (hasLabel() && this.labelBox != null
				&& this.labelBox.getParent() == null) {

			// if parent is not null, this box is an active edit on the map
			// and we don't want to paint it here as AWT/Swing is handling
			// that at the moment (and at a possibly slightly different offset)

			drawLabel(dc);
		}

	}

	@Override
	protected void drawImpl(DrawContext dc) {
		if (!isFiltered()) {
			// Desired functionality is that if this node is filtered, we don't
			// draw it, of course.
			// But also, even if this node is filtered, we still draw any
			// children who are
			// NOT filtered -- we just drop out the parent background.
			drawNode(dc);
		}

	}

	protected void drawLabel(DrawContext dc) {
		//float lx = 0;//relativeLabelX();
		//float ly = 0;//relativeLabelY();
		//dc.g.translate(lx, ly);
		// if (DEBUG.CONTAINMENT) System.out.println("*** " + this + " drawing
		// label at " + lx + "," + ly);
		this.labelBox.draw(dc);
		//dc.g.translate(-lx, -ly);

		// todo: this (and in LWLink) is a hack -- can't we
		// do this relative to the node?
		// this.labelBox.setMapLocation(getX() + lx, getY() + ly);
	}

	protected float relativeLabelX() {
		// Doing this risks slighly moving the damn TextBox just as you edit it.
		final float offset = (this.width - getTextSize().width) / 2;
		return offset + 1;

	}

	protected float relativeLabelY() {
		// Doing this risks slighly moving the damn TextBox just as you edit it.
		// Tho querying the underlying TextBox for it's size every time
		// we repaint this object is pretty gross also (e.g., every drag)
		return (this.height - getTextSize().height) / 2;

	}

	/**
	 * @return the current size of the label object, providing a margin of error
	 *         on the width given sometime java bugs in computing the accurate
	 *         length of a a string in a variable width font.
	 */
	private static final float TextWidthFudgeFactor = 1; // off for debugging

	// (Almost uneeded
	// in new Mac JVM's)

	protected Size getTextSize() {

		if (WrapText) {
			Size s = new Size(getRichLabelBox().getSize());
			// s.width += 3;
			return s;
		} else {

			// TODO: Check if this hack still needed in current JVM's

			// getSize somtimes a bit bigger thatn preferred size & more
			// accurate
			// This is gross, but gives us best case data: we want the largest
			// in width,
			// and smallest in height, as reported by BOTH getSize and
			// getPreferredSize.

			Size s = new Size(getRichLabelBox().getPreferredSize());
			Size ps = new Size(getRichLabelBox().getSize());
			// if (ps.width > s.width)
			// s.width = s.width; // what the hell
			if (ps.height < s.height)
				s.height = ps.height;
			s.width *= TextWidthFudgeFactor;
			s.width += 3;
			return s;
		}
	}

    @Override
    public float getWidth() {
        if (labelBox == null)
            return super.getWidth();
        else
            return labelBox.getWidth();
    }
    
    @Override
    public float getHeight() {
    	//The line height is always off by a 1 line..
        if (labelBox == null)
            return super.getHeight();
        else
            return labelBox.getHeight();
    }

    @Override
    public float getLocalWidth()       { return (float) (getWidth() * getScale()); }
    @Override
    public float getLocalHeight()      { return (float) (getHeight() * getScale()); }
    
    @Override
    public float getMapWidth()          { return (float) (getWidth() * getMapScale()); }
    @Override
    public float getMapHeight()         { return (float) (getHeight() * getMapScale()); }

    @Override
    public float getLocalBorderWidth() { return (float) ((getWidth() + mStrokeWidth.get()) * getScale()); }
    @Override
    public float getLocalBorderHeight() { return (float) ((getHeight() + mStrokeWidth.get()) * getScale()); }


	private boolean inLayout = false;

	/**
	 * Duplicate this node.
	 * 
	 * @return the new node -- will have the same style (visible properties) of
	 *         the old node
	 */
	@Override
	public LWComponent duplicate(CopyContext cc) {
		// LWText newNode = (LWNode) super.duplicate(cc);
		boolean isPatcherOwner = false;

		if (cc.patcher == null && cc.dupeChildren && hasChildren()) {

			// Normally VUE Actions (e.g. Duplicate, Copy, Paste)
			// provide a patcher for duplicating a selection of
			// objects, but anyone else may not have provided one.
			// This will take care of arbitrary single instances of
			// duplication, including duplicating an entire Map.

			cc.patcher = new LinkPatcher();
			isPatcherOwner = true;
		}

		final LWText containerCopy = (LWText) super.duplicate(cc);
		containerCopy.getRichLabelBox().setText(this.getRichLabelBox().getRichText());

		if (isPatcherOwner)
			cc.patcher.reconnectLinks();

		containerCopy.setSize(getWidth(), getHeight());
		return containerCopy;
	}

	public void setStyle(LWComponent parentStyle)
	{
		if (labelBox != null)
		 labelBox.overrideTextColor(parentStyle.getTextColor());
	}
	
	@Override
	protected void layoutImpl(Object triggerKey) {
            if (triggerKey == LWKey.Alignment) {
                // LWText doesn't use the aligment property on a whole-component bases: ignore this
                // layout request if we ever get it.
                return;
            }
            layout(triggerKey, new Size(getWidth(), getHeight()), null);
	}

	@Override
	public void setSize(float w, float h) {
		if (DEBUG.LAYOUT)
			out("*** setSize         " + w + "x" + h);
		if (isAutoSized() && (w > this.width || h > this.height)) // does this
																	// handle
																	// scaling?
			setAutomaticAutoSized(false);
		layout(LWKey.Size, new Size(getWidth(), getHeight()), new Size(w, h));
	}

	/**
	 * For triggering automatic shifts in the auto-size bit based on a call to
	 * setSize or as a result of a layout
	 */
	private void setAutomaticAutoSized(boolean tv) {
		if (isOrphan()) // if this is during a restore, don't do any automatic
						// auto-size computations
			return;
		if (isAutoSized == tv)
			return;
		if (DEBUG.LAYOUT)
			out("*** setAutomaticAutoSized " + tv);
		isAutoSized = tv;
	}

	/**
	 * @param triggerKey -
	 *            the property change that triggered this layout
	 * @param curSize -
	 *            the current size of the node
	 * @param request -
	 *            the requested new size of the node
	 */
	protected void layout(Object triggerKey, Size curSize, Size request) {
		if (inLayout) {
			if (DEBUG.Enabled)
				new Throwable("ALREADY IN LAYOUT " + this).printStackTrace();
			return;
		}
		inLayout = true;
		if (DEBUG.LAYOUT) {
			String msg = "*** LAYOUT, trigger=" + triggerKey + " cur="
					+ curSize + " request=" + request + " isAutoSized="
					+ isAutoSized();
			if (DEBUG.META)
				Util.printClassTrace("tufts.vue.LW", msg + " " + this);
			else
				out(msg);
		}

		if (DEBUG.LAYOUT
				&& getLabelBox().getHeight() != getLabelBox()
						.getPreferredSize().height) {
			// NOTE: prefHeight often a couple of pixels less than getHeight
			System.err.println("prefHeight != height in " + this);
			System.err.println("\tpref="
					+ getLabelBox().getPreferredSize().height);
			System.err.println("\treal=" + getLabelBox().getHeight());
		}

		// The current width & height is at this moment still a
		// "request" size -- e.g., the user may have attempted to drag
		// us to a size smaller than our minimum size. During that
		// operation, the size of the node is momentarily set to
		// whatever the user requests, but then is immediately laid
		// out here, during which we will revert the node size to the
		// it's minimum size if bigger than the requested size.

		// -------------------------------------------------------
		// If we're a rectangle (rect or round rect) we use
		// layoutBoxed, if anything else, we use layoutCeneter
		// -------------------------------------------------------

		final Size min;

		min = layoutBoxed(request, curSize, triggerKey);
		if (request == null)
			request = curSize;



		if (DEBUG.LAYOUT)
			out("*** layout computed minimum=" + min);

		// If the size gets set to less than or equal to
		// minimize size, lock back into auto-sizing.
		// if (request.height <= min.height && request.width <= min.width)
		// setAutomaticAutoSized(true);

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

		// layout label last in case size is bigger than min and label is
		// centered
		layoutBoxed_label();

		if (labelBox != null)
			labelBox.setBoxLocation(0,0);
			//labelBox.setBoxLocation(relativeLabelX(), relativeLabelY());

		if (this.parent != null && this.parent instanceof LWMap == false) {
			// todo: should only need to do if size changed
			this.parent.layout();
		}

		inLayout = false;
	}

	@Override
	public boolean isImageNode() {
		return false;
	}

	private void setSizeNoLayout(float w, float h) {
		if (DEBUG.LAYOUT)
			out("*** setSizeNoLayout " + w + "x" + h);
		setSize(w, h);
		//mShape.setFrame(0, 0, getWidth(), getHeight());
	}

	private transient Point2D.Float mLabelPos = new Point2D.Float(); // for

	// use
	// with
	// irregular
	// node
	// shapes

	/** set mLabelPos */
	private void layoutBoxed_label() {
		Size text = getTextSize();

		// only need this in case of small font sizes and an icon
		// is showing -- if so, center label vertically in row with the first
		// icon
		// Actually, no: center in whole node -- gak, we really want both,
		// but only to a certian threshold -- what a hack!
		// float textHeight = getLabelBox().getPreferredSize().height;
		// mLabelPos.y = (this.height - textHeight) / 2;
		mLabelPos.y = (this.height - text.height) / 2;

		// -------------------------------------------------------
		// horizontally center if no icons
		// -------------------------------------------------------
		// if (WrapText)
		// mLabelPos.x = (this.width - text.width) / 2 + 1;
		// else
		mLabelPos.x = 200; // marked bad because unused in this case
		// }

	}

	private Size layoutBoxed(Size request, Size oldSize, Object triggerKey) {
		final Size min;

		// if (WrapText)
		// min = layoutBoxed_floating_text(request, oldSize, triggerKey);
		// else
		min = layoutBoxed_vanilla(request);

		return min;

	}

	private static final int EdgePadY = 4; // Was 3 in VUE 1.5
	private static final int LabelPadLeft = 8; // Was 6 in VUE 1.5; fixed

	// distance to right of
	// iconMargin dividerLine

	/** @return new minimum size of node */

	private Size layoutBoxed_vanilla(final Size request) {
		final Size min = new Size();
		final Size text = getTextSize();

		min.width = text.width;
		min.height = EdgePadY + text.height + EdgePadY;

		// *** set icon Y position in all cases to a centered vertical
		// position, but never such that baseline is below bottom of
		// first icon -- this is tricky tho, as first icon can move
		// down a bit to be centered with the label!

		min.width += LabelPadLeft;

		return min;
	}

}
