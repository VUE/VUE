package tufts.vue;

/**
 * TextBox.java
 *
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
 * while it's in it's activated edit state.
 *
 * We use a JTextPane because it supports a StyledDocument, which is
 * what we need to be able to set left/center/right aligment for all
 * the paragraphs in the document.  This is a bit heavy weight for our
 * uses right now as we only make use of one font at a time for the whole
 * document (this is the heaviest weight text component in Swing).
 * JTextArea would have worked for us, except it only supports it's
 * fixed default of left-aligned text.  However, eventually we're
 * probably going to want to suport intra-string formatting (fonts,
 * colors, etc) and so we'll be ready for that, with the exception of
 * the hack mentioned above to handle zooming (tho we could
 * theoretically iterate through the whole document, individually
 * deriving zoomed fonts for every font found in the Document.)
 *
 * @author Scott Fraize
 * @version July 2003
 *
 */

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.geom.Rectangle2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;

class TextBox extends JTextPane
    implements VueConstants
               , FocusListener
               , KeyListener
               , DocumentListener
{
    static final Color SelectionColor = VueResources.getColor("mapViewer.textBox.selection.color");
    static final boolean debug = false;
    static final boolean debug_box = false;
    
    private LWComponent lwc;
    private float mapX;
    private float mapY;
    private float mapWidth;
    private float mapHeight;
    private boolean wasOpaque; /** were we opaque before we started an edit? */
        
    TextBox(LWComponent lwc)
    {
        this(lwc, null);
    }

    TextBox(LWComponent lwc, String text)
    {
        this.lwc = lwc;
        //setBorder(javax.swing.border.LineBorder.createGrayLineBorder());
        // don't set border -- adds small margin that screws us up, especially
        // at high scales
        setBorder(null);
        if (text != null)
            setText(text);
        setMargin(null);
        setOpaque(false); // don't bother to paint background
        setVisible(true);
        //setFont(SmallFont);
        // PC text pane will pick this font up up as style for
        // document, but mac ignores.
            
        //setAlignmentX(1f);//nobody's paying attention to this

        addKeyListener(this);
        addFocusListener(this);
        getDocument().addDocumentListener(this);
        setSize(getPreferredSize());
        if (VueUtil.isWindowsPlatform() && SelectionColor != null)
            setSelectionColor(SelectionColor);
        
        if (debug) System.out.println("new TextBox[" + text + "] " + getSize());
    }

    LWComponent getLWC()
    {
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
    private Font savedFont = null;
    private String savedText;
    private boolean keyWasPressed = false;
    private static final int MinEditSize = 11; // todo: prefs
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


    void saveCurrentText()
    {
        savedText = getText();
    }
    public void addNotify()
    {
        if (getText().length() < 1)
            setText("<label>");
        keyWasPressed = false;
        super.addNotify();
        // note: we get a a flash/move if we add the border before the super.addNotify()
        setBorder(javax.swing.border.LineBorder.createGrayLineBorder());
        java.awt.Container parent = getParent();
        if (parent instanceof MapViewer) { // todo: could be a scroller!
            double zoom = ((MapViewer)parent).getZoomFactor();
            // todo: also account for getScale of children!
            zoom *= lwc.getScale();
            if (zoom != 1.0) {
                Font f = lwc.getFont();
                float pointSize = (float) (f.getSize() * zoom);
                if (pointSize < MinEditSize)
                    pointSize = MinEditSize;
                savedFont = f;
                setDocumentFont(f.deriveFont(f.getStyle(), pointSize));
                setSize(getPreferredSize());
            } else {
                setDocumentFont(lwc.getFont());
            }
        }
            
        wasOpaque = isOpaque();
        if (lwc instanceof LWNode) {
            Color c = lwc.getFillColor();
            if (c == null && lwc.getParent() != null && lwc.getParent() instanceof LWNode)
                c = lwc.getParent().getFillColor();
            // todo: could also consider using map background if the node itself
            // is transpatent (has no fill color)
            if (c != null) {
                // note that if we set opaque to false, interaction speed is
                // noticably slowed down in edit mode because it has to consider
                // repainting the entire map each cursor blink as the object
                // is transparent, and thus it's background is the displayed
                // map.  So if we can guess at a reasonable fill color in edit mode,
                // we temporarily set us to opaque.
                setOpaque(true);
                setBackground(c);
            }
        }
        if (debug) System.out.println("addNotify: insets="+getInsets());
    }
    
    /*
     * Return to the regular transparent state.
     */
    public void removeNotify()
    {
        super.removeNotify();
        setBorder(null);
        if (savedFont != null) {
            setDocumentFont(savedFont);
            savedFont = null;
            setSize(getPreferredSize());
        }
        if (wasOpaque != isOpaque())
            setOpaque(wasOpaque);
        if (debug) System.out.println("removeNotify: insets="+getInsets());
    }

    public void setText(String text)
    {
        if (getDocument() == null) {
            System.out.println("TextBox: creating new document");
            setStyledDocument(new DefaultStyledDocument());
        }
        /*try {
            doc.insertString(0, text, null);
        } catch (Exception e) {
            System.err.println(e);
            }*/ 
        if (debug) System.out.println("TextBox.setText[" + text + "]");
        super.setText(text);
        copyStyle(this.lwc);
        setSize(getPreferredSize());
    }

    public void doLayout()
    {
        if (getParent() instanceof MapViewer) {
            // Automatic layout (e.g. FlowLayout)
            // produces two layout passes -- perhaps
            // this is why we need to call this TWICE
            // here so that the box size doesn't
            // temporarily flash bigger on every update.
            setSize(getPreferredSize());
            setSize(getPreferredSize());
        }
        //super.layout();
        //new Throwable("layout").printStackTrace();
    }
    
    void handleChange()
    {
        //lwc.setLabel0(getText(), false);
        //invalidate();
        // appears to be happening too late -- current character isnt include
        /*
        if (getParent() == null) {
            setSize(getPreferredSize());
            setSize(getPreferredSize());
        }
        */
    }

    public void removeUpdate(DocumentEvent de) 
    {
        if (debug) System.out.println("removeUpdate " + de);
        handleChange();
    }
    public void changedUpdate(DocumentEvent de) 
    {
        if (debug) System.out.println("changeUpdate " + de);
        handleChange();
    }
    public void insertUpdate(DocumentEvent de) 
    {
        if (debug) System.out.println("insertUpdate " + de);
        handleChange();
    }

    public void keyPressed(KeyEvent e)
    {
        //System.out.println("TextBox: " + e);
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            e.consume();
            setText(savedText);
            getParent().remove(this);
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER &&
                   (e.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD
                    || (e.getModifiersEx() != 0 && (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != InputEvent.SHIFT_DOWN_MASK))) {
            // if we hit enter key either on numpad, or with any modifier down except a shift alone (in case of caps lock)
            // complete the edit.
            keyWasPressed = true;
            e.consume();
            getParent().remove(this); // will trigger a save
        } else
            keyWasPressed = true;
    }

    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e)
    {
        // todo: would be nice if centered labels stayed center as you typed them
        
        //setLocation((int)lwc.getLabelX(), (int)lwc.getLabelY());
        // needs something else, plus can't work at zoom because
        // width isn't updated till the end (because width at + zoom
        // is overstated based on temporarily scaled font)
        // Man, it would be REALLY nice if we could paint the
        // real component in a scaled GC w/out the font tweaking --
        // problems like this would go away.
    }
    public void focusLost(FocusEvent e)
    {
        if (debug) System.out.println("TextBox focusLost to " + e.getOppositeComponent());
        if (getParent() != null)
            getParent().remove(this);
        if (keyWasPressed) {
            // only do this if they typed something (so we don't wind up with "label"
            // for the label on an accidental edit activation)
            lwc.setLabel0(getText(), false);
            System.out.println("TextBox: key was pressed; label set to: [" + getText() + "]");
        }
    }
    public void focusGained(FocusEvent e)
    {
        if (debug) System.out.println("TextBox focusGained from " + e.getOppositeComponent());
    }

    private void setDocumentFont(Font f)
    {
        SimpleAttributeSet a = new SimpleAttributeSet();
        setFontAttributes(a, f);
        StyledDocument doc = getStyledDocument();
        doc.setParagraphAttributes(0, doc.getEndPosition().getOffset(), a, false);
        setSize(getPreferredSize());
        setSize(getPreferredSize());
    }

    private void setFontAttributes(MutableAttributeSet a, Font f)
    {
        StyleConstants.setFontFamily(a, f.getFamily());
        StyleConstants.setFontSize(a, f.getSize());
        StyleConstants.setItalic(a, f.isItalic());
        StyleConstants.setBold(a, f.isBold());
    }

    
    void copyStyle(LWComponent c)
    {
        SimpleAttributeSet a = new SimpleAttributeSet();
        //StyleConstants.setAlignment(a, StyleConstants.ALIGN_RIGHT);
        StyleConstants.setAlignment(a, StyleConstants.ALIGN_CENTER);//todo: from node or LWC
        StyleConstants.setForeground(a, c.getTextColor());
        setFontAttributes(a, c.getFont());
        StyledDocument doc = getStyledDocument();
        doc.setParagraphAttributes(0, doc.getEndPosition().getOffset(), a, false);
        setSize(getPreferredSize());
        setSize(getPreferredSize());
    }


    public void setPreferredSize(Dimension preferredSize) {
        if (debug) System.out.println("MTP setPreferred " + preferredSize);
        super.setPreferredSize(preferredSize);
    }
    public Dimension getPreferredSize() {
        Dimension s = super.getPreferredSize();
        //s.width = (int) lwc.getWidth();
        //System.out.println("MTP lwcWidth " + lwc.getWidth());
        s.width += 1; // leave room for cursor, which at least on mac gets clipped if at EOL
        //if (getParent() != null)
        //s.width += 10;
        if (debug) System.out.println("MTP lwc " + lwc);
        if (debug) System.out.println("MTP getPreferred " + s);
        //new Throwable("getPreferredSize").printStackTrace();
        return s;
    }
    public void setSize(Dimension s) {
        if (debug) System.out.println("MTP setSize " + s);
        super.setSize(s);
        if (savedFont == null) {
            // savedFont only set if we had to zoom the font
            this.mapWidth = s.width;
            this.mapHeight = s.height;
        }
    }
    public Dimension getSize() {
        Dimension s = super.getSize();
        //s.width = (int) lwc.getWidth();
        if (debug) System.out.println("MTP getSize " + s);
        //new Throwable("getSize").printStackTrace();
        return s;
    }
    public void setMaximumSize(Dimension s) {
        if (debug) System.out.println("MTP setMaximumSize " + s);
        super.setMaximumSize(s);
    }
    public Dimension getMaximumSize() {
        Dimension s = super.getMaximumSize();
        if (debug) System.out.println("MTP getMaximumSize " + s);
        return s;
    }
    public void setMinimumSize(Dimension s) {
        if (debug) System.out.println("MTP setMinimumSize " + s);
        super.setMinimumSize(s);
    }
    public Dimension getMinimumSize() {
        Dimension s = super.getMinimumSize();
        if (debug) System.out.println("MTP getMinimumSize " + s);
        return s;
    }

    public float getMapWidth()
    {
        return mapWidth;
    }
    public float getMapHeight()
    {
        return mapHeight;
    }

    public void setMapLocation(float x, float y)
    {
        this.mapX = x;
        this.mapY = y;
    }

    public boolean intersectsMapRect(Rectangle2D rect)
    {
        return rect.intersects(mapX, mapY, mapWidth, mapHeight);
    }

    // todo: this doesn't account for scaling if laid
    // out in a child...
    public boolean containsMapLocation(float x, float y)
    {
        return
            x >= mapX && y >= mapY &&
            x <= mapX + mapWidth &&
            y <= mapY + mapHeight;
    }

    /*
    void resizeToWidth(float w)
    {
        int width = (int) (w + 0.5f);
        setSize(new Dimension(width, 999));  // can set height to 1 as we're ignore the set-size
        // now the preferred height will be set to the real
        // total text height at that width -- pull it back out and set actual size to same
        Dimension ps = getPreferredSize();
        setSize(new Dimension(width, (int)ps.getHeight()));
    }
    */

    public void paintComponent(Graphics g)
    {
        MapViewer viewer = (MapViewer) javax.swing.SwingUtilities.getAncestorOfClass(MapViewer.class, this);
        ((Graphics2D)g).setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, viewer.AA_ON);
        // turn on anti-aliasing -- the cursor repaint loop doesn't
        // set anti-aliasing, so text goes jiggy around cursor/in selection if we don't do this
        super.paintComponent(g);
        
        // todo: ser hilite color to non-purple (yellow) on PC -- is
        // virtually identical to our default node color!

        // todo: draw a 1 pixel border?
    }

    private static final BasicStroke MinStroke = new BasicStroke(1/8f);
    private static final BasicStroke MinStroke2 = new BasicStroke(1/24f);
    public void draw(Graphics2D g)
    {
        if (getParent() != null)
            System.err.println("Warning: 2nd draw of an AWT drawn component!");

        //todo: could try saving current translation or coordinates here,
        // so have EXACT last position painted at.  Tho we really should
        // be able to compute it... problem is may not be at integer
        // boundry at current translation, but have to be when we add it
        // to the map -- tho hey, LWNode could force integer boundry
        // when setting the translation before painting us.
        
        //super.paintBorder(g);
        super.paintComponent(g);
        //super.paint(g);

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
        
        if (debug || debug_box) {
            Dimension s = getPreferredSize();
            g.setColor(Color.blue);
            g.setStroke(MinStroke);
            g.drawRect(0,0, s.width-1, s.height);
        }
            
        //s = getMinimumSize();
        //g.setColor(Color.red);
        //g.setStroke(new BasicStroke(1/8f));
        //g.drawRect(0,0, s.width, s.height);

        if (debug || debug_box || getParent() != null) {
            Dimension s = getSize();
            g.setColor(Color.red);
            g.setStroke(MinStroke2);
            g.drawRect(0,0, s.width-1, s.height);
        }

    }
 /*   
    public void changedUpdate(javax.swing.event.DocumentEvent documentEvent) {
    }
    
    public void focusGained(java.awt.event.FocusEvent focusEvent) {
    }
    
    public void focusLost(java.awt.event.FocusEvent focusEvent) {
    }
    
    public void insertUpdate(javax.swing.event.DocumentEvent documentEvent) {
    }
    
    public void keyPressed(java.awt.event.KeyEvent keyEvent) {
    }
    
    public void keyReleased(java.awt.event.KeyEvent keyEvent) {
    }
    
    public void keyTyped(java.awt.event.KeyEvent keyEvent) {
    }
    
    public void removeUpdate(javax.swing.event.DocumentEvent documentEvent) {
    }
   */ 
}
