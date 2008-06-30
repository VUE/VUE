/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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

import tufts.vue.gui.ColorMenuButton;
import tufts.vue.gui.DockWindow;
import tufts.vue.gui.GUI;
import tufts.vue.gui.TextRow;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Date;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.TextUI;
import javax.swing.text.*;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.lightdev.app.shtm.SHTMLDocument;
import com.lightdev.app.shtm.SHTMLEditorKit;

/**
 * A multi-line editable text object that supports left/center/right
 * aligment for its lines of text.
 *
 * Used in two modes: (1) "normal" mode -- used to paint multi-line
 * text objects (labels, notes, etc) and (2) "edit".  In normal mode,
 * this JComponent has no parent -- it isn't added to any AWT
 * hierarchy -- it's only used to paint as part of the
 * LWMap/LWContainer paint tree (via the draw(Graphics2D)) method.  In
 * edit mode, it's temporarily added to the canvas so it can receive
 * user input.  Only one instance of these is ever added and active in
 * AWT at the same time.  We have to do some funky wrangling to deal
 * with zoom, because the JComponent can't paint and interact on a
 * zoomed (scaled) graphics context (unless we were to implement mouse
 * event retargeting, which is a future possibility).  So if there is
 * a scale active on the currently displayed map, we manually derive a
 * new font for the whole text object (the Document) and set it to
 * that temporarily while it's active in edit mode, and then re-set it
 * upon removal.  Note that users of this class (e.g., LWNode) should
 * not bother to paint it (call draw()) if it's in edit mode
 * (getParent() != null) as the AWT/Swing tree is dealing with that
 * while it's in its activated edit state.
 *
 * We use a JTextPane because it supports a StyledDocument, which is
 * what we need to be able to set left/center/right aligment for all
 * the paragraphs in the document.  This is a bit heavy weight for our
 * uses right now as we only make use of one font at a time for the whole
 * document (this is the heaviest weight text component in Swing).
 * JTextArea would have worked for us, except it only supports its
 * fixed default of left-aligned text.  However, eventually we're
 * probably going to want to suport intra-string formatting (fonts,
 * colors, etc) and so we'll be ready for that, with the exception of
 * the hack mentioned above to handle zooming (tho we could
 * theoretically iterate through the whole document, individually
 * deriving zoomed fonts for every font found in the Document.)
 *
 * Note that you can get center/variable alignment even with
 * a JLabel if it's text starts with &lt;HTML&gt;, and you
 * put in a center tag.  Unfortunately, JTextArea's don't
 * do HTML w/out setting up a StyledDocument manually.
 *
 *
 * @author Scott Fraize
 * @version $Revision: 1.29 $ / $Date: 2008-06-30 20:52:54 $ / $Author: mike $
 *
 */

public class RichTextBox extends com.lightdev.app.shtm.SHTMLEditorPane
    implements VueConstants
               , FocusListener
               , KeyListener
               , DocumentListener
{
   
    private static final Color SelectionColor =GUI.getTextHighlightColor();// VueResources.getColor("mapViewer.textBox.selection.color");
    
    private static boolean TestDebug = false;
    private static boolean TestHarness = false;
    
    private LWComponent lwc;
    /** bounds: generally used by the component as local coordinates (relative to the coordinate 0,0)
     * The width/height are set here in TextBox */
    private final Rectangle2D.Float mBounds = new Rectangle2D.Float();
    private boolean wasOpaque; /** were we opaque before we started an edit? */  
    
    RichTextBox(LWComponent lwc)
    {
        this(lwc, null);
    }

    RichTextBox(LWComponent lwc, String text)
    {
    
        if (DEBUG.TEXT && DEBUG.LAYOUT) tufts.Util.printClassTrace("tufts.vue.", "NEW RichTextBox, txt=" + text);
        if (TestDebug||DEBUG.TEXT) out("NEW [" + text + "] " + lwc);
    	SHTMLEditorKit kit = new SHTMLEditorKit(/* renderMode */);
		 //kit.resetStyleSheet();
		setEditorKit(kit);
        this.lwc = lwc;
        
        setDragEnabled(false);
        setBorder(null);
    	
        if (text != null)
            setText(text);
        
        setMargin(null);
        setOpaque(false); // don't bother to paint background
        setVisible(true);
     
        addKeyListener(this);
        addFocusListener(this);
        getDocument().addDocumentListener(this);
        //setSize(getPreferredSize());
        
        if (VueUtil.isWindowsPlatform() && SelectionColor != null)
            setSelectionColor(SelectionColor);
        if (VueUtil.isWindowsPlatform() && SelectionColor != null)
        	setSelectedTextColor(Color.black);
        mBounds.x = Float.NaN; // mark as uninitialized
        mBounds.y = Float.NaN; // mark as uninitialized
        
        if (TestDebug||DEBUG.TEXT) out("constructed " + getSize());
    }

    /*
    public String getText() {
        //java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        Document doc = getDocument();
        String text = null;
        try {
            // better to use doc.getText(0, doc.getLength())
            //getEditorKit().write(buf, doc, 0, doc.getLength());
            text = doc.getText(0, doc.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (text == null || text.length() == 0)
            return ":"+super.getText();
        return text;
        //return buf.toString();
    }
    */

  

    LWComponent getLWC() {
        return this.lwc;
    }


    /*
     * When activated for editing, draw an appropriate background color
     * for the node -- the we need to do because if it's a small on-screen
     * font at the moment (depending on zoom level), we make the
     * text box always appear at the 100% zoomed font (because we're
     * not managing scaled repaint of the added object or retargeting
     * scaled mouse events, etc).  Also, when it's transparent, the
     * whole map has to be repainted each cursor blink just in case
     * there is some LWComponent under the transparent text item.
     * (Tho with a very small clip region).
     */
    private Font preZoomFont = null;
    private String mUnchangedText;
    private Dimension mUnchangedSize;
    private boolean keyWasPressed = false;
    //private static final int MinEditSize = 11; // todo: prefs
    // todo bug: on PC, font edits at size < 11 are failing produce the
    // right selection or cursor coordinates, and what you see
    // is NOT what you get anymore.  ACTUALLY, this may be due
    // to special charactes in the string -- it was a piece of
    // pasted HTML text with "1/2" chars and \226 dashes..
    // Okay, no -- even vanilla text at 10 point does it
    // (SansSerif-plain-10) -- okay, this is crap -- a "regular"
    // node v.s. a text node is working fine down at nine-point,
    // tho it does have only 3 lines -- ugh, this is going
    // to require alot of fiddling and testing.

    public void setBounds(Rectangle r)
    {
    	super.setBounds(r);
    }
    
    
    /** called by MapViewer before we go active on the map */
    void saveCurrentState()
    {
        mUnchangedText = getRichText();
        mUnchangedSize = getPreferredSize();
    }
    
    /** deprecated */
    void saveCurrentText() {
        saveCurrentState();
    }
    
    @Override
    public void addNotify()
    {
        if (TestDebug||DEBUG.TEXT) out("*** ADDNOTIFY ***");
        if (getText().length() < 1)
            setText("<label>");
        keyWasPressed = false;
        //Dimension size = getPreferredSize();
        super.addNotify();                         
              
        
        wasOpaque = isOpaque();
        Color background = lwc.getRenderFillColor(null);
        //if (c == null && lwc.getParent() != null && lwc.getParent() instanceof LWNode)
        final LWContainer nodeParent = lwc.getParent();
        
        if (background == null && nodeParent != null)
        {
            background = nodeParent.getRenderFillColor(null); // todo: only handles 1 level transparent embed!         
        }
        // todo: could also consider using map background if the node itself
        // is transpatent (has no fill color)

        // TODO: this workaround until we can recursively find a real fill color
        // node that for SLIDES, we'd have to get awfully fancy and
        // usually pull the color of the master slide (unless the slide
        // happened to have it's own special color).  Only super clean
        // way to do this would be to have established some kind of
        // rendering pipeline record... (yeah, right)
        if (background == null) background = Color.gray;
        //out("BACKGROUND COLOR " + background);

        // TODO: the *selection* color always appears to be gray in for edits
        // in the slide viewer on the mac, even if we manually set the selection
        // color (which works in the main MapViewer) -- this is an oddity...
        
        if (background != null) {
            // note that if we set opaque to false, interaction speed is
            // noticably slowed down in edit mode because it has to consider
            // repainting the entire map each cursor blink as the object
            // is transparent, and thus it's background is the displayed
            // map.  So if we can guess at a reasonable fill color in edit mode,
            // we temporarily set us to opaque.
            setOpaque(true);
            setBackground(background);
        }
        setSize(getPreferredSize());
/*
        Dimension preferred = getPreferredSize();
        int w = getWidth() + 1;
        if (w < preferred.width)
            mKeepTextWidth = true;
        else
            mKeepTextWidth = false;
        if (TestDebug||DEBUG.TEXT) out("width+1=" + w + " < preferred.width=" + preferred.width + " fixedWidth=" + mKeepTextWidth);
        
        if (TestDebug||DEBUG.TEXT) out("addNotify end: insets="+getInsets());
        mFirstAfterAddNotify = true;
    */
    }
   
    /*
     * Return to the regular transparent state.
     */
    @Override
    public void removeNotify()
    {
        if (TestDebug||DEBUG.TEXT) out("*** REMOVENOTIFY ***");
        
        //------------------------------------------------------------------
        // We need to clear any text selection here as a workaround
        // for an obscure bug where sometimes if the focus change is
        // to a pop-up menu, the edit properly goes inactive, but the
        // selection within it is still drawn with it's highlighted
        // background.
        clearSelection();
        //------------------------------------------------------------------
        
        super.removeNotify();

     /*   if (mFirstAfterAddNotify == false) {
            // if cleared, it was used
            out("restoring expanded width");
            setSize(new Dimension(getWidth()-1, getHeight()));
            //out("SKPPING restoring expanded width");
        } else
            mFirstAfterAddNotify = false;
       */ 
        setBorder(null);
        /*if (preZoomFont != null) {
            setDocumentFont(preZoomFont);
            preZoomFont = null;
            setSize(getPreferredSize());                       
        }*/
        
        if (wasOpaque != isOpaque())
            setOpaque(wasOpaque);
        if (TestDebug||DEBUG.TEXT) out("*** REMOVENOTIFY end: insets="+getInsets());
    }

    public void clearSelection() {
        // this set's the "mark to the point" -- sets them to the same
        // location, thus clearing the selection.
        setCaretPosition(getCaretPosition());
    }

    @Override
    public void setText(String text)
    {
        super.setText(text);
     
        
        setSize(getPreferredSize());
       //
    	if (lwc.getParent() !=null)
    		lwc.getParent().layoutChildren();
        //setSize(getPreferredSize());
    	
    }
    
    public void setXMLText(String text)
    {
        super.setText(text);
     
        
    
    	
    }

   


    private void setDocumentFont(Font f)
    {
     //   if (DEBUG.TEXT) out("setDocumentFont " + f);
     //   SimpleAttributeSet a = new SimpleAttributeSet();
     //   setFontAttributes(a, f);
     //   SHTMLDocument doc = (SHTMLDocument)this.getDocument();
     //   doc.setParagraphAttributes(0, doc.getEndPosition().getOffset(), a, false);
     //   computeMinimumWidth(f, lwc.getLabel());
     //   mKeepHeight = true;
    }

    private void setDocumentColor(Color c)
    {
    //	System.out.println("Rich text set DOcument color to :" + c.toString());
    //    StyleConstants.setForeground(mAttributeSet, c);
    //    SHTMLDocument doc = (SHTMLDocument)this.getDocument();
    //    doc.setParagraphAttributes(0, doc.getEndPosition().getOffset(), mAttributeSet, true);
    }


    private static void setFontAttributes(MutableAttributeSet a, Font f)
    {
    //    StyleConstants.setFontFamily(a, f.getFamily());
    //    StyleConstants.setFontSize(a, f.getSize());
    //    StyleConstants.setItalic(a, f.isItalic());
    //    StyleConstants.setBold(a, f.isBold());
    }

    
    // this called every time setText is called to ensure we get
    // the font style encoded in our owning LWComponent
    void copyStyle(LWComponent c)
    {
      /*  if (DEBUG.TEXT) out("copyStyle " + c);
        SimpleAttributeSet a = new SimpleAttributeSet();
        if (TestHarness || c instanceof LWNode && ((LWNode)c).isTextNode())
            StyleConstants.setAlignment(a, StyleConstants.ALIGN_LEFT);
        else
            StyleConstants.setAlignment(a, StyleConstants.ALIGN_CENTER);
        StyleConstants.setForeground(a, c.getTextColor());
        final Font font = c.getFont();
        setFontAttributes(a, font);

        SHTMLDocument doc = (SHTMLDocument)this.getDocument();
        if (DEBUG.TEXT) getPreferredSize();
        doc.setParagraphAttributes(0, doc.getEndPosition().getOffset(), a, false);
        if (DEBUG.TEXT) getPreferredSize();
        computeMinimumWidth(font, lwc.getLabel());
        mKeepHeight = true;
        mAttributeSet = a;

        if (WrapText) {
            // adjust WIDTH ONLY (or: attempt to keep aspect)
            //adjustSize(false);
        } else {
            setSize(getPreferredSize());
            setSize(getPreferredSize());
        }
        */
    }

      /** override Container.doLayout: only called when we've been added to a map for interactive editing.
     * Is called during interactive edit's after each modification.
     */
    public void doLayout()
    {
        if (getParent() instanceof MapViewer) {
            // Automatic layout (e.g. FlowLayout)
            // produces two layout passes -- perhaps
            // this is why we need to call this TWICE
            // here so that the box size doesn't
            // temporarily flash bigger on every update.
            if (TestDebug || DEBUG.LAYOUT) out("doLayout w/adjustSizeDynamically");
         //   if (false)
         //       out("skipping size fix-up");
         //   else {
                //if (WrapText)
                
                    //adjustSizeDynamically();
                //else {
                    setSize(getPreferredSize());
                    setSize(getPreferredSize());
               // }
           // }
        } else {
            if (!TestHarness)
                new Throwable(this + " UNPARENTED doLayout").printStackTrace();
            
        }
        //super.layout();
    }

    private boolean mFirstAfterAddNotify = false;
    private boolean mKeepTextWidth = false;
   
    public void keyReleased(KeyEvent e) 
    { 
    	setSize(getPreferredSize());
    	lwc.notify(this, LWKey.Repaint);
    	e.consume();
    }
    public void keyTyped(KeyEvent e) {    	
        // todo: would be nice if centered labels stayed center as you typed them
        //setLocation((int)lwc.getLabelX(), (int)lwc.getLabelY());
        // needs something else, plus can't work at zoom because
        // width isn't updated till the end (because width at + zoom
        // is overstated based on temporarily scaled font)
        // Man, it would be REALLY nice if we could paint the
        // real component in a scaled GC w/out the font tweaking --
        // problems like this would go away.
    	//setSize(getPreferredSize());
    	//lwc.notify(this, LWKey.Repaint);
    	//e.consume();
    	//System.out.println("KEY TYPED");
    }
    private static boolean isFinishEditKeyPress(KeyEvent e) {
        // if we hit return key either on numpad ("enter" key), or
        // with any modifier down except a shift alone (in case of
        // caps lock) complete the edit.
        return e.getKeyCode() == KeyEvent.VK_ENTER &&
            (
             e.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD ||
             (e.getModifiersEx() != 0 && !e.isShiftDown())
             )
            == true;
        //== false; // reversed logic of below description
        
    }   
    private boolean revert = false;
    public void keyPressed(KeyEvent e)
    {
        if (DEBUG.KEYS) out(e.toString());
        //System.out.println("TextBox: " + e);

        //if (VueUtil.isAbortKey(e)) // check for ESCAPE for CTRL-Z or OPTION-Z if on mac
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            e.consume();
       
       //     System.out.println(mUnchangedText);
            //setText(mUnchangedText);
            revert = true;
            getParent().remove(this); // will trigger a save (via focusLost)
            return;
           // setSize(mUnchangedSize); // todo: won't be good enough if we ever resize the actual node as we type
        } else if (isFinishEditKeyPress(e)) {
            keyWasPressed = true;
            e.consume();
            getParent().remove(this); // will trigger a save (via focusLost)
            VUE.getFormattingPanel().getTextPropsPane().getFontEditorPanel().updateFormatControlsTB(this);
        } else if (e.getKeyCode() == KeyEvent.VK_U && e.isMetaDown()) {
            e.consume();
            String t = getText();
            if (e.isShiftDown())
                setText(t.toUpperCase()); // upper whole string
            else
                setText(Character.toTitleCase(t.charAt(0)) + t.substring(1)); // upper first char
        } else
            keyWasPressed = true;

        setSize(getPreferredSize());
        
        // action keys will be ignored if we consume this here!
        // (e.g., "enter" does nothing)
        //e.consume();
        
    }

    /**
     * This is what triggers the final save of the new text value to the LWComponent,
     * and notify's the UndoManager that a user action was completed.
     */
    public void focusLost(FocusEvent e)
    {
        final java.awt.Component opposite = e.getOppositeComponent();
        
        
    	if (opposite != null) {
    		if ((opposite.getName() != null && opposite.getName().equals(FontEditorPanel.SIZE_FIELD_NAME)) ||
    		opposite.getClass() == ColorMenuButton.class)
    			return;
            else if (opposite.getClass() == FontEditorPanel.class ||
                     DockWindow.isDockWindow(opposite) ||
                     // todo: something more generic than this getName check: set a property on the JComponent tagging it as a tool/editor?
                     opposite.getClass() == JComboBox.class ||
                     (opposite.getName() != null && opposite.getName().equals(tufts.vue.gui.ColorMenuButton.COLOR_POPUP_NAME)) ||
                     //quaqua makes this a bit awkard, this is for quaqua's color chooser.
                     (opposite.getName() != null && opposite.getName().equals("dialog0")))
            {
            	//Earlier i was just returning here, but this creates a problem
            	//because the component has already lost the focus...and so it doesn't 
            	//get another focusLost the next time....so re-request the focus if you've lost
            	//it so that we get the event again when we really want to get rid of the focus
            	//so we can properly remove the edit control.
            	            	
            	requestFocus();
            	return;
            }
    		
        }
    	else if (opposite == null)
    	{
    		if (DEBUG.FOCUS)
    			outc("Focus not lost because opposite component = null");
    		requestFocus();
        	return;
    	}
    	
    	//System.out.println(e.getComponent().toString());
    	//System.out.println(e.getOppositeComponent().toString());
        if (TestDebug||DEBUG.FOCUS)
        	outc("focusLost to " + e.getOppositeComponent() + "   " + opposite.getName());
        if (TestHarness == false && getParent() != null)
        {
            getParent().remove(this);
           // VUE.getFormattingPanel().getTextPropsPane().getFontEditorPanel().updateFormatControlsTB(this);
        }
        if (keyWasPressed || !keyWasPressed) { // TODO: as per VueTextField, need to handle drag & drop detect
            // only do this if they typed something (so we don't wind up with "label"
            // for the label on an accidental edit activation)
            if (TestDebug||DEBUG.FOCUS) out("key was pressed; setting label to: [" + getText() + "]");
            String text = getText();
            if (revert)
            {
            	text = mUnchangedText;
            	revert =false;
            //	setText(text);
            }
            lwc.setLabel0(text, false);
           
            VUE.getUndoManager().mark();
        }
        setSize(getPreferredSize());
        if (lwc.getParent() !=null && lwc.getParent() instanceof LWNode)
    		lwc.getParent().layout();
        lwc.notify(this, LWKey.Repaint);
    }

    public void focusGained(FocusEvent e)
    {
        if (TestDebug||DEBUG.FOCUS) outc("focusGained from " + e.getOppositeComponent());
    }

    /** do not use, or won't be able to get out actual text height */
    @Override
    public void setPreferredSize(Dimension preferredSize) {
        if (true||TestDebug) out("setPreferred " + preferredSize);
        super.setPreferredSize(preferredSize);
    }
    
    	
    //private Dimension prevDim = null;
    @Override
    public Dimension getPreferredSize() 
    {
      
   		Dimension s = super.getPreferredSize();
   	
   		
   		
   		   		
   			
   	
        Dimension minS = super.getMinimumSize();
              
        //System.out.println(javax.swing.SwingUtilities.getLocalBounds(this));
        //if (TestDebug||DEBUG.TEXT) out("getPrefer", s);
        
        // System.out.println("GetPrefSize : " +  mBounds.width + " "+ s.width);
        //    System.out.println("Required Lines : " + s.width/mBounds.width);
        Caret c = this.getCaret();
        Point position = c.getMagicCaretPosition();
        //    if (position != null)
        //    	System.out.println("magic caret : " + position.getX());
        
        //HTMLDocument builds a hierarchical Element structure where each Element
        //represents a structural block of HTML, and not just a line of text. so I'm not sure
        //how to figure out what line you're on.
       
        Dimension min = new Dimension();
        min.width = 150;//text.width;
    	
        int EdgePadY = 4; // Was 3 in VUE 1.5
        int LabelPadLeft = 8; // Was 6 in VUE 1.5; fixed
        
		min.height = EdgePadY + min.height + EdgePadY;
		//System.out.println("Text.height : " + text.height);
		// *** set icon Y position in all cases to a centered vertical
		// position, but never such that baseline is below bottom of
		// first icon -- this is tricky tho, as first icon can move
		// down a bit to be centered with the label!

		min.width += LabelPadLeft;
		
		min.width = Math.max(min.width,minS.width);
		/*
		System.out.println("Min.Width =" + min.width);
		System.out.println("MinS.Width =" + minS.width);
		System.out.println("S.Width =" + s.width);
		System.out.println("MBounds.Width = " + mBounds.width);
		System.out.println("GetSize.Width = " + getSize().width);
		System.out.println("Bounds : " + this.getBounds().width);
		System.out.println("LWC width : " + lwc.width);
		System.out.println("LWC bounds : " + lwc.getBounds().width);
		System.out.println("LWC layoutbounds : " + lwc.getLayoutBounds().width);
		System.out.println("LWC min size : " + lwc.getMinimumSize().width);
		System.out.println("Get super width : " + super.getWidth());
		System.out.println("Selection Eend : " + this.getSelectionEnd());
		System.out.println("Selection Start : " + 		this.getSelectionStart());
		System.out.println("Bounds Box Width : " +this.getBoxBounds().getBounds().width);		
		System.out.println("Visible Rect : " +this.getVisibleRect().width);*/
//**		Rectangle p2 = null;
		//if (this.getGraphics() != null)
		//{
		//	System.out.println("GRAPHICS : " + this.getGraphics().toString());
		//	if (this.getGraphics().getClipBounds() != null)
		//	System.out.println("GRAPHICS : " + this.getGraphics().getClipBounds().width);
		//	this.getPreferredScrollableViewportSize().width
		//	
		//	
		
	     View ui = getUI().getRootView(this);
	     ui = ui.createFragment(this.getSelectionStart(), this.getSelectionEnd());
	     //System.out.println("min span: " + ui.getMinimumSpan(View.X_AXIS));
	     
//**	     int start = getSelectionStart();
//**	     int end = getSelectionEnd();
//**	    float f = ui.getPreferredSpan(View.X_AXIS);
	    //System.out.println("Preferred span : " + f);
	    	    
//**	    f = ui.getPreferredSpan(View.Y_AXIS);
	    //System.out.println("Preferred span y : " + f);
//**	    f = ui.getPreferredSpan(View.X_AXIS);
	    //System.out.println("{referred span X : " + f);
	    //ui.breakView(View.X_AXIS, offset, pos, len)
	    //((HTMLEditorKit)this.getEditorKit()).getViewFactory().create(this.getDocument().getDefaultRootElement()).
	  //  System.out.println("BREAK WEIGHT : " + ui.getBreakWeight(View.X_AXIS, this.getCaretPosition(), this.getSelectionEnd()));
	  //  System.out.println("RESIZE WEIGHT : " + ui.getResizeWeight(View.X_AXIS));
	  //  System.out.println("RESIZE WEIGHTY : " + ui.getResizeWeight(View.Y_AXIS));
//**	    float align = ui.getAlignment(View.X_AXIS);
	  //  System.out.println("align span : " + align);
		
	    
//**		try {
//**			p2 = this.modelToView(this.getSelectionEnd());
//**			
//**		} catch (BadLocationException e) {			
//**			//Nothing we can do really.
//**		}
//**		//if (p2 != null)
//**			//System.out.println("Selection end rect : " + p2.x);
//**		
        if (mBounds.width > 0)
        {
//**        	if (p2 != null && p2.x > mBounds.width)
//**        		mBounds.width = p2.x;
    
        	//int height = 48;
        	/*if (getFont() != null &&this.getFontMetrics(getFont())!=null)
        	{
        	System.out.println("A:"+this.getFontMetrics(getFont()).getMaxAscent());
        	System.out.println("B:"+this.getFontMetrics(getFont()).getMaxDescent());
        	}*/
        	
                	
        int		height = s.height;//Math.max(s.height, 48);
        	
        	if (position !=null)
        	{
        		
        		float minSpan = ui.getMinimumSpan(View.X_AXIS);
        		float mins2 = Math.max(minSpan,(float)position.getX());
        		s = new Dimension((int)(mBounds.width > mins2 ? mBounds.width : mins2),(int)height);
        	}
        	else
        	{
        		float minSpan = ui.getMinimumSpan(View.X_AXIS);
        		s = new Dimension((int)(mBounds.width > minSpan ? mBounds.width : minSpan),(int)height);
        	}
        	//float widthRatio = ((float)s.width /(float)lwc.width);
       	//	float heightRatio =((float)s.height/(float)lwc.height);
       /* 	if ((widthRatio > 0) && (heightRatio > 0) && (widthRatio != heightRatio))
       		{
       			
       			s.height = (int)(lwc.height * widthRatio);
       		//	System.out.print("NEW SHEIGHT : " + s.height);
       		//	prevDim = s;
       			//   			
       		}
        	System.out.println("width ratio : " + widthRatio);
       		System.out.println("height ratio : " + heightRatio);
       	*/	
        /*	if (this.getGraphics() != null)
        	System.out.println("FONT HEIGHT : " +this.getGraphics().getFontMetrics().getMaxAscent());
        	System.out.println("===================================");
        	System.out.println("super pref height : " + s.height);
        	System.out.println("Min Vertical Span : " +ui.getMinimumSpan(View.Y_AXIS));
        	System.out.println("MBounds height : " + mBounds.height);
        	System.out.println("super pref height : " + s.height);
        	System.out.println("Min Vertical Span : " +ui.getMinimumSpan(View.Y_AXIS));
        	System.out.println("MBounds height : " + mBounds.height);
        	System.out.println("Mins height : " + min.height);
        	System.out.println("Bounds Y : " + this.getBounds().y);
        	System.out.println("Bounds height :" +this.getBounds().height);
        	System.out.println("Bounds2D Y : " +this.getBoxBounds().getY());
        	System.out.println("Bounds2D height : " +this.getBoxBounds().getHeight());
        	if (this.getCaret().getMagicCaretPosition() != null)
        	System.out.println("Cursor : " + this.getCaret().getMagicCaretPosition().toString());
        	//this.getCaret().getMagicCaretPosition().
        	System.out.println("===================================");*/
        	/*if (position != null)
        	{
        		System.out.println("Y Pos : " + position.getY());
        	 	 try {
					Rectangle r = modelToView(this.getCaretPosition());
					System.out.println("model pos : " + r.y);
					System.out.println("Model height : " + r.height);
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}*/
        	
        }
        
        return s;
    }

    public void setSize(Size s) {
        setSize(s.dim());
    }


    public void setSize(Dimension s) 
    {
    	
    		if (TestDebug||DEBUG.TEXT) out("setSize", s);
    		super.setSize(s);
    		mBounds.width = s.width;
    		mBounds.height = s.height;
    	

    }

    /**
     * Set the size to the given size, increasing or decreasing height as
     * needed to provide a fit around our text
     */
    public void setSizeFlexHeight(Size newSize) {

        //------------------------------------------------------------------
        // Tell the JTextPane to take on the size requested.  It will
        // then set the preferred height to the minimum height able to
        // contain the given text at that width.
        //------------------------------------------------------------------

        setSize(newSize);
        
        //------------------------------------------------------------------
        // Now adjust our height to the new preferred height, which should
        // just contain our text.
        //------------------------------------------------------------------
        
        final Dimension s = getPreferredSize();
        s.width = getWidth();
        
        if (TestDebug||DEBUG.TEXT) out("flexHeigt", s);
        super.setSize(s.width, s.height);
    }


    public void setSize(float w, float h) {
        setSize(new Dimension((int)w, (int)h));
    }
    public void setPreferredSize(float w, float h) {
        setPreferredSize(new Dimension((int)w, (int)h));
    }
    
    public Dimension getSize() {
        Dimension s = super.getSize();
        //s.width = (int) lwc.getWidth();
        if (TestDebug||DEBUG.TEXT) out("getSize", s);
        //if (DEBUG.TEXT&&DEBUG.META) new Throwable("getSize").printStackTrace();
        return s;
    }
    public void setMaximumSize(Dimension s) {
        if (true||TestDebug) out("setMaximumSize", s);
        super.setMaximumSize(s);
    }
    public Dimension getMaximumSize() {
        Dimension s = super.getMaximumSize();
        if (TestDebug||DEBUG.TEXT) out("getMaximumSize", s);
        return s;
    }
    public void setMinimumSize(Dimension s) {
        if (true||TestDebug) out("setMinimumSize", s);
        super.setMinimumSize(s);
    }
    public Dimension getMinimumSize() {
        Dimension s = super.getMinimumSize();
        if (TestDebug||DEBUG.TEXT) out("getMinimumSize", s);
        return s;
    }

    @Override
    public void reshape(int x, int y, int w, int h) {
    //	System.out.println("RESHAPE : " + mBounds.width + " " + mBounds.height);
        if (TestDebug||DEBUG.TEXT) {
            boolean change = getX() != x || getY() != y || getWidth() != w || getHeight() != h;
            if (change) {
                out(" oldshape " + tufts.Util.out(getBounds()));
                out("  reshape " + w + "x" + h + " " + x + "," + y);
            }
            //out("  reshape " + w + "x" + h + " " + x + "," + y + (change ? "" : " (no change)"));
            if (DEBUG.META && change)
                new Throwable("reshape").printStackTrace();
        }
        boolean change = getX() != x || getY() != y || getWidth() != w || getHeight() != h;
    //    out("  reshape " + w + "x" + h + " " + x + "," + y + (change ? "" : " (no change)"));
        super.reshape(x, y, w, h);
        if (TestDebug||DEBUG.TEXT) {
            Rectangle b = getBounds();
            if (b.x != x || b.y != y || b.width != w || b.height != h)
                out("BADBOUNDS " + tufts.Util.out(b));
        }
    }

    public Rectangle2D getBoxBounds() {
        return mBounds;
    }

    public boolean boxContains(float x, float y)
    {
        return x >= mBounds.x
            && y >= mBounds.y
            && x <= mBounds.x + mBounds.width
            && y <= mBounds.y + mBounds.height;
    }
    
    public boolean boxIntersects(Rectangle2D rect)
    {
        return rect.intersects(mBounds);
    }

    public void setBoxLocation(float x, float y)
    {
        mBounds.x = x;
        mBounds.y = y;
    }
    
    public void setBoxLocation(Point2D p)
    {
        setBoxLocation((float) p.getX(), (float) p.getY());
    }

    public void setBoxCenter(float x, float y) {
        setBoxLocation(x - getBoxWidth() / 2,
                       y - getBoxHeight() / 2);
    }
    
    
    public Point2D.Float getBoxPoint()
    {
        return new Point2D.Float(mBounds.x, mBounds.y);
    }

    public float getBoxWidth() { return mBounds.width; };
    public float getBoxHeight() { return mBounds.height; }
    public float getBoxX() { return mBounds.x; };
    public float getBoxY() { return mBounds.y; }
      

    public int getHeight()
    {
    	
    	//The line height is always off by a 1 line..
    	/*if (this.isEditable() && this.getCaret().getMagicCaretPosition() != null)
    	{
    		System.out.println("MAGIC CURSOR " );
    		return super.getHeight();
    	}
    	else*/
    		return super.getHeight();
    }
    
    public void Xpaint(Graphics g) {
        super.paint(g);
        g.setColor(Color.gray);
        g.setClip(null);
        g.drawRect(0,0, getWidth(), getHeight());
    }
    
    public void paintComponent(Graphics g)
    {
        if (TestDebug||DEBUG.TEXT) out("paintComponent @ " + getX() + "," + getY() + " parent=" + getParent());

        MapViewer viewer = (MapViewer) javax.swing.SwingUtilities.getAncestorOfClass(MapViewer.class, this);
        if (viewer != null)
            ((Graphics2D)g).setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, viewer.AA_ON);
        // turn on anti-aliasing -- the cursor repaint loop doesn't
        // set anti-aliasing, so text goes jiggy around cursor/in selection if we don't do this
        super.paintComponent(g);
        if (true) {
            // draw a border (we don't want to add one because that changes the preferred size at a bad time)
            g.setColor(Color.gray);
            g.setClip(null);
            final int xpad = 1;
            final int ypad = 1;
            g.drawRect(-xpad,-ypad, getWidth()+xpad*2-1, getHeight()+ypad*2-1);
        }
      //  System.out.println(g.getClipBounds());
      //  Graphics2D g2d =((Graphics2D)g);
       // System.out.println(g2d.getDeviceConfiguration().getBounds());
        
    }  
   
    public void draw(DrawContext dc)
    {
        if (TestDebug||DEBUG.TEXT) out("draw");

        if (getParent() != null)
            System.err.println("Warning: 2nd draw of an AWT drawn component!");

        //todo: could try saving current translation or coordinates here,
        // so have EXACT last position painted at.  Tho we really should
        // be able to compute it... problem is may not be at integer
        // boundry at current translation, but have to be when we add it
        // to the map -- tho hey, LWNode could force integer boundry
        // when setting the translation before painting us.
        
        if (DEBUG.BOXES && DEBUG.META) {
            if (lwc.getLabel().indexOf('\n') < 0) {
                TextRow r = new TextRow(lwc.getLabel(), lwc.getFont(), dc.g.getFontRenderContext());
                dc.g.setColor(Color.lightGray);
                r.draw(dc, 0, 0);
            }
        }

        //boolean inverted;
        //if (dc.isBlackWhiteReversed() &&
          //  (dc.isPresenting() || lwc.isTransparent() /*|| isBlack(lwc.getFillColor())*/) &&
           // isBlack(lwc.getTextColor())) {
            //System.out.println("reversing color to white for " + this);
         //   setDocumentColor(Color.white);
           // inverted = true;
       // } else
         //   inverted = false;
        
        boolean restoreTextColor = false;
        
        if (dc.isPresenting() && lwc.isTransparent()) {
            // if the text color equals the background color when in a presentation
            // (e.g. the master slide has a black background), and the text box
            // has to fill of it's own for contrast, then temporarily swap
            // the text color to white or black so it can be seen.
            if (lwc.mTextColor.equals(dc.getBackgroundFill())) {
                restoreTextColor = true;
                if (lwc.mTextColor.brightness() > 0.5)
                    setDocumentColor(Color.black);
                else
                    setDocumentColor(Color.white);
            }
        }
        
        //super.paintBorder(g);
        super.paintComponent(dc.g);
        //super.paint(g);
        if (restoreTextColor) 
            // return document color to black
            setDocumentColor(lwc.mTextColor.get());
      //  if (inverted)
     //       setDocumentColor(Color.black);

        // draw a border for links -- why?
        // and even if, better to handle in LWLink
        /*
        if (lwc instanceof LWLink) {
            Dimension s = getSize();
            if (lwc.isSelected())
                g.setColor(COLOR_SELECTION);
            else
                g.setColor(Color.gray);
            g.setStroke(MinStroke);
            g.drawRect(0,0, s.width-1, s.height-2);
        }
        */

        Graphics2D g = dc.g;
        if (DEBUG.BOXES) {
            Dimension s = getPreferredSize();
            g.setColor(Color.red);
            dc.setAbsoluteStroke(0.5);
            //g.setStroke(MinStroke);
            g.drawRect(0,0, s.width, s.height);
            //g.drawRect(0,0, s.width-1, s.height);
        }
            
        //s = getMinimumSize();
        //g.setColor(Color.red);
        //g.setStroke(new BasicStroke(1/8f));
        //g.drawRect(0,0, s.width, s.height);

        if (DEBUG.BOXES || getParent() != null) {
            Dimension s = getSize();
            g.setColor(Color.blue);
            dc.setAbsoluteStroke(0.5);
            //g.setStroke(MinStroke);
            g.drawRect(0,0, s.width, s.height);
            //g.drawRect(0,0, s.width-1, s.height);
        }

    }

    public void overrideTextColor(Color c)
    {
    	//System.out.println("OVERRIDE TEXT COLOR : " + c.toString());
    	SimpleAttributeSet set = new SimpleAttributeSet();
   	        String colorString = "#" + Integer.toHexString(
   	                c.getRGB()).substring(2);
   	        com.lightdev.app.shtm.Util.styleSheet().addCSSAttribute(set,
   	                CSS.Attribute.COLOR, colorString);
   	        
   	            set.addAttribute(HTML.Attribute.COLOR, colorString);
   	            this.applyAttributesGlobally(set, true,false);
    }
  
    public void removeUpdate(DocumentEvent de) {
        if (TestDebug||DEBUG.TEXT)
        	out("removeUpdate " + de);
    	
        //Having both this setSize/repaint and the getParent layout causes some
        //weird state that results in an array indesx out of bounds in some cases...
        //the layout() makes sure the parent node is resized properly which wasn't
        //previously happening when just hte setSize and the lwc notify were getting called.
    		//setSize(getPreferredSize());   		
    		//lwc.notify(this, LWKey.Repaint);
    		
        if (lwc.getParent() !=null)
        {
    		lwc.getParent().layout();
      //  System.out.println("FULL CONTAINS CHILDREN : " + lwc.getParent().fullyContainsChildren());
        }
    //	setSize(getPreferredSize());
    	lwc.notify(this, LWKey.Repaint);
    }
    public void changedUpdate(DocumentEvent de) {
        if (TestDebug||DEBUG.TEXT) 
        	out("changeUpdate " + de.getType() + " len=" + de.getLength());

			//setSize(getPreferredSize());
        	//lwc.notify(this, LWKey.Repaint);
        	
        	if (lwc.getParent() !=null){
        		lwc.getParent().layout();
        
        	//System.out.println("FULL CONTAINS CHILDREN : " + lwc.getParent().fullyContainsChildren());
        	}
      //   	setSize(getPreferredSize());
        	lwc.notify(this, LWKey.Repaint);
        	
    }
    public void insertUpdate(DocumentEvent de) {
        if (TestDebug||DEBUG.TEXT) 
        	out("insertUpdate " + de);

	//	setSize(getPreferredSize());
		//lwc.notify(this, LWKey.Repaint);
		
        if (lwc.getParent() !=null){
    		lwc.getParent().layout();
       // System.out.println("FULL CONTAINS CHILDREN : " + lwc.getParent().fullyContainsChildren());
        }
    	//setSize(getPreferredSize());
    	lwc.notify(this, LWKey.Repaint);
    }
    
    public String toString()
    {
        return "RTB[" + lwc + "]";
    }
   
    private String id() {
        return Integer.toHexString(System.identityHashCode(this));
    }

    public String getRichText()
    {
    	String html = super.getText();
    	String patternStr = "size=\"(\\d*)\"";
        String replacementStr = "size=\"$1\" style=\"font-size:$1;\"";
   
        // Compile regular expression
        Pattern pattern = Pattern.compile(patternStr);
  
        // Replace all occurrences of pattern in input
        Matcher matcher = pattern.matcher(html);
        String output = matcher.replaceAll(replacementStr);
       
    	return output;
    //	return html;
    }
    
    public String getText()
    {
    	return stripHTMLTags(super.getText());
    }
    
    private String stripHTMLTags( String message ) 
    {
        StringBuffer returnMessage = new StringBuffer(message);
        int startPosition = message.indexOf("<"); // encountered the first opening brace
        int endPosition = message.indexOf(">"); // encountered the first closing braces
        while( startPosition != -1 ) {
          returnMessage.delete( startPosition, endPosition+1 ); // remove the tag
          startPosition = (returnMessage.toString()).indexOf("<"); // look for the next opening brace
          endPosition = (returnMessage.toString()).indexOf(">"); // look for the next closing brace
        }
        return org.apache.commons.lang.StringEscapeUtils.unescapeHtml(returnMessage.toString().trim());
        
      }
    
    private void out(String s) {
        System.out.println("RTB@" + id() + " [" + getText() + "] " + s);
    }
    
    private void out(String s, Dimension d) {
        out(VueUtil.pad(' ', 9, s, true) + " " + tufts.Util.out(d));
    }     
    
    private void outc(String s) {
        System.out.println(this + " " + id() + " " + s);
    }         
}