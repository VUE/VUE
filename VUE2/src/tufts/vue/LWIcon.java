package tufts.vue;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.*;
import java.util.Iterator;

/**
 * LWIcon.java
 *
 * Icon's for displaying on LWComponents
 */

public abstract class LWIcon extends Rectangle2D.Float
    implements VueConstants
{
    private static final float DefaultScale = 0.045f;
    static final Font FONT_ICON = VueResources.getFont("node.icon.font");
    static final Color DefaultColor = new Color(61, 0, 88);
    
    protected LWComponent mLWC;
    protected Color mColor;
    protected float mMinWidth;
    protected float mMinHeight;

    private LWIcon(LWComponent lwc, Color c) {
        // default size
        super.width = 22;
        super.height = 12;
        mLWC = lwc;
        mColor = c;
    }
    private LWIcon(LWComponent lwc) {
        this(lwc, DefaultColor);
    }

    
    public void setLocation(float x, float y)
    {
        super.x = x;
        super.y = y;
    }

    public void setSize(float w, float h)
    {
        super.width = w;
        super.height = h;
    }

    public void setMinimumSize(float w, float h)
    {
        mMinWidth = w;
        mMinHeight = h;
        if (super.width < w)
            super.width = w;
        if (super.height < h)
            super.height = h;
    }

    public void setColor(Color c) {
        mColor = c;
    }
    
    void draw(DrawContext dc)
    {
        if (DEBUG.BOXES) {
            dc.g.setColor(Color.red);
            dc.g.setStroke(STROKE_SIXTEENTH);
            dc.g.draw(this);
        }
    }

    void layout() {} // subclasses can check for changes in here
    
    abstract boolean isShowing();
    abstract void doDoubleClickAction();
    abstract public JComponent getToolTipComponent();
    //todo: make getToolTipComponent static & take lwc arg in case anyone else wants these

    public static class Block extends Rectangle2D.Float
    {
        public static final boolean VERTICAL = true;
        public static final boolean HORIZONTAL = false;
        public static final boolean COORDINATES_MAP = false;
        public static final boolean COORDINATES_COMPONENT  = true;
        
        private LWComponent mLWC;
        
        private LWIcon[] mIcons = new LWIcon[6];

        private boolean mVertical = true;
        private boolean mCoordsLocal;
        private float mIconWidth;
        private float mIconHeight;
        
        public Block(LWComponent lwc,
                     int iconWidth,
                     int iconHeight,
                     Color c,
                     boolean vertical,
                     boolean coord_local)
        {
            if (c == null)
                c = DefaultColor;

            mCoordsLocal = coord_local;
            mIconWidth = iconWidth;
            mIconHeight = iconHeight;
            setOrientation(vertical);

            mIcons[0] = new LWIcon.Resource(lwc, c);
            mIcons[1] = new LWIcon.Behavior(lwc, c);
            mIcons[2] = new LWIcon.Notes(lwc, c);
            mIcons[3] = new LWIcon.Pathway(lwc, c);
            mIcons[4] = new LWIcon.MetaData(lwc, c);
            mIcons[5] = new LWIcon.Hierarchy(lwc, c);

            for (int i = 0; i < mIcons.length; i++) {
                mIcons[i].setSize(iconWidth, iconHeight);
                mIcons[i].setMinimumSize(iconWidth, iconHeight);
            }

            this.mLWC = lwc;
        }

        public void setOrientation(boolean vertical)
        {
            mVertical = vertical;
            if (vertical)
                super.width = mIconWidth;
            else
                super.height = mIconHeight;
        }
        
        /**
         * do we contain coords x,y?
         * Coords may be component local or map local or
         * whataver -- depends on what was handed to us
         * via @see setLocation
         */
        public boolean contains(float x, float y)
        {
            if (isShowing() && super.width > 0 && super.height > 0) {
                return x >= super.x
                    && y >= super.y
                    && x <= super.x + super.width
                    && y <= super.y + super.height;
            }
            return false;
        }

        public String toString()
        {
            return "LWIcon.Block[" + super.x+","+super.y + " " + super.width+"x"+super.height + " " + mLWC + "]";
        }

        //public float getWidth() { return super.width; }
        //public float getHeight() { return super.height; }

        boolean isShowing() {
            return super.width > 0 && super.height > 0;
        }

        void setLocation(float x, float y)
        {
            super.x = x;
            super.y = y;
            layout();
        }
        
        /** Layout whatever is currently relevant to show, computing
         * width & height -- does NOT change location
         */
        void layout()
        {
            if (mVertical) {
                super.height = 0;
                float iconY = super.y;
                for (int i = 0; i < mIcons.length; i++) {
                    LWIcon icon = mIcons[i];
                    if (icon.isShowing()) {
                        icon.layout();
                        icon.setLocation(x, iconY);
                        iconY += icon.height;
                        super.height += icon.height;
                    }
                }
            } else {
                super.width = 0;
                float iconX = super.x;
                for (int i = 0; i < mIcons.length; i++) {
                    LWIcon icon = mIcons[i];
                    if (icon.isShowing()) {
                        icon.layout();
                        icon.setLocation(iconX, y);
                        iconX += icon.width;
                        super.width += icon.width;
                    }
                }
            }
        }

        void draw(DrawContext dc)
        {
            for (int i = 0; i < mIcons.length; i++) {
                if (mIcons[i].isShowing())
                    mIcons[i].draw(dc);
            }
        }


        void checkAndHandleMouseOver(MapMouseEvent e)
        {
            float cx = 0, cy = 0;

            if (mCoordsLocal) {
                cx = e.getComponentX();
                cy = e.getComponentY();
            } else {
                cx = e.getMapX();
                cy = e.getMapY();
            }
            JComponent tipComponent = null;
            LWIcon tipIcon = null;

            for (int i = 0; i < mIcons.length; i++) {
                LWIcon icon = mIcons[i];
                if (icon.isShowing() && icon.contains(cx, cy)) {
                    tipIcon = icon;
                    break;
                }
            }
            
            // TODO: don't need to do this if there's already a tip showing!
            if (tipIcon != null) {
                tipComponent = tipIcon.getToolTipComponent();
                Rectangle2D.Float tipRegion = (Rectangle2D.Float) tipIcon.getBounds2D();
                if (mCoordsLocal) {
                    // translate tipRegion from component to map coords
                    float s = mLWC.getScale();
                    if (s != 1) {
                        tipRegion.x *= s;
                        tipRegion.y *= s;
                        tipRegion.width *= s;
                        tipRegion.height *= s;
                    }
                    tipRegion.x += mLWC.getX();
                    tipRegion.y += mLWC.getY();
                }

                // if node, compute avoid region node+tipRegion,
                // if link avoid = label+entire tip block
                Rectangle2D avoidRegion = null;
                if (mLWC instanceof LWLink) {
                    float w = 1, h = 1;
                    
                    if (mLWC.hasLabel()) {
                        w = mLWC.labelBox.getMapWidth();
                        h = mLWC.labelBox.getMapHeight();
                        // Stay away from the link label:
                        avoidRegion = new Rectangle2D.Float(mLWC.getLabelX(), mLWC.getLabelY(), w,h);
                        // Stay way from the whole icon block:
                        Rectangle2D.union(avoidRegion, this, avoidRegion);
                    } else
                        // Stay way from the whole icon block:
                        avoidRegion = this;
                } else {
                    avoidRegion = mLWC.getShapeBounds();
                }
                
                e.getViewer().setTip(tipComponent, avoidRegion, tipRegion);
            }
        }

        boolean handleDoubleClick(MapMouseEvent e)
        {
            float cx = 0, cy = 0;
            boolean handled = false;

            if (mCoordsLocal) {
                cx = e.getComponentX();
                cy = e.getComponentY();
            } else {
                cx = e.getMapX();
                cy = e.getMapY();
            }

            for (int i = 0; i < mIcons.length; i++) {
                LWIcon icon = mIcons[i];
                if (icon.isShowing() && icon.contains(cx, cy)) {
                    icon.doDoubleClickAction();
                    handled = true;
                    break;
                }
            }
            return handled;
        }
    }
    
    /**
     * AALabel: A JLabel that forces anti-aliasing -- use this if
     * you want a tool-tip to be anti-aliased on the PC,
     * because there's no way to set it otherwise.
     * (This is redundant on the Mac which does it automatically)
     */
    class AALabel extends JLabel
    {
        AALabel(String s) { super(s); };
        public void paintComponent(Graphics g) {
            ((Graphics2D)g).setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                                             java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            super.paintComponent(g);
        }
    }
    
    static class Resource extends LWIcon
    {
        private final static String NoResource = VueUtil.isMacPlatform() ? "---" : "__";
        // On PC, two underscores look better than "---" in default Trebuchet font,
        // which leaves the dashes high in the box.
    
        TextRow mTextRow;
        
        Resource(LWComponent lwc) { super(lwc); }
        Resource(LWComponent lwc, Color c) {
            super(lwc, c);
            layout();
        }

        boolean isShowing() { return mLWC.hasResource(); }

        void doDoubleClickAction() {
            mLWC.getResource().displayContent();
        }
        
        private JComponent ttResource;
        private String ttLastString;
        private boolean hadTitle = false;
        public JComponent getToolTipComponent()
        {
            tufts.vue.Resource r = mLWC.getResource();
            boolean hasTitle = (r.getTitle() != null && !r.getTitle().equals(r.getSpec()));
            if (ttResource == null
                || !ttLastString.equals(mLWC.getResource().getSpec())
                || hadTitle != hasTitle)
            {
                hadTitle = hasTitle;
                ttLastString = mLWC.getResource().getSpec();
                // todo perf: use StringBuffer
                ttResource = new AALabel("<html>&nbsp;<b>"
                                         + ttLastString + "</b>"
                                         + (hasTitle?("<font size=-2><br>&nbsp;"+mLWC.getResource().getTitle()+"</font>"):"")
                                         + "<font size=-2 color=#999999><br>&nbsp;Double-click to open in new window&nbsp;");
                ttResource.setFont(FONT_MEDIUM);
            }
            return ttResource;
        }

        void layout()
        {
            String extension = NoResource;
            if (mLWC.hasResource())
                extension = mLWC.getResource().getExtension();
            mTextRow = new TextRow(extension, FONT_ICON);
            // Resource icon special case can override parent set width:
            super.width = mTextRow.width;
            if (super.width < super.mMinWidth)
                super.width = super.mMinWidth;
        }
        
        void draw(DrawContext dc)
        {
            super.draw(dc);

            double x = getX();
            double y = getY();

            dc.g.translate(x, y);
            dc.g.setColor(mColor);
            dc.g.setFont(FONT_ICON);

            float xoff = (super.width - mTextRow.width) / 2;
            float yoff = (super.height - mTextRow.height) / 2;
            mTextRow.draw(dc.g, xoff, yoff);

            // an experiment in semantic zoom
            if (mLWC.hasResource() && dc.g.getTransform().getScaleX() >= 8.0) {
                dc.g.setFont(MinisculeFont);
                dc.g.setColor(Color.gray);
                dc.g.drawString(mLWC.getResource().toString(), 0, (int)(super.height));
            }

            dc.g.translate(-x, -y);
        }

        /*        
        void draw(DrawContext dc)
        {
            super.draw(dc);
            //dc.g.setColor(Color.black);
            dc.g.setColor(mColor);
            dc.g.setFont(FONT_ICON);
            String extension = NoResource;
            if (mLWC.hasResource())
                extension = mLWC.getResource().getExtension();
            double x = getX();
            double y = getY();
            dc.g.translate(x, y);

            // todo perf: listen for resource change & cache text row
            TextRow row = new TextRow(extension, dc.g);
            // Resource icon special case can override parent set width:
            if (super.width < row.width)
                super.width = row.width;
            float xoff = (super.width - row.width) / 2;
            float yoff = (super.height - row.height) / 2;
            row.draw(xoff, yoff);

            // an experiment in semantic zoom
            //if (dc.zoom >= 8.0 && mLWC.hasResource()) {
            if (mLWC.hasResource() && dc.g.getTransform().getScaleX() >= 8.0) {
                dc.g.setFont(MinisculeFont);
                dc.g.setColor(Color.gray);
                dc.g.drawString(mLWC.getResource().toString(), 0, (int)(super.height));
            }

            dc.g.translate(-x, -y);
        }
        */
        
    }

    static class Notes extends LWIcon
    {
        private final static float MaxX = 155;
        private final static float MaxY = 212;

        private final static float scale = DefaultScale;
        private final static AffineTransform t = AffineTransform.getScaleInstance(scale, scale);

        private final static Stroke stroke = new BasicStroke(0.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

        static float iconWidth = MaxX * scale;
        static float iconHeight = MaxY * scale;


        //-------------------------------------------------------
        
        private final static GeneralPath pencil_body = new GeneralPath();
        private final static GeneralPath pencil_point = new GeneralPath();
        private final static GeneralPath pencil_tip = new GeneralPath();


        //static float iconXoff = (super.width - iconWidth) / 2f;
        //static float iconYoff = (super.height - iconHeight) / 2f;

        static {
            pencil_body.moveTo(0,31);
            pencil_body.lineTo(55,0);
            pencil_body.lineTo(150,155);
            pencil_body.lineTo(98,187);
            pencil_body.closePath();
            pencil_body.transform(t);

            pencil_point.moveTo(98,187);
            pencil_point.lineTo(150,155);
            pencil_point.lineTo(150,212);
            pencil_point.closePath();

            /*pencil_point.moveTo(150,155);
            pencil_point.lineTo(150,212);
            pencil_point.lineTo(98,187);
            */
            
            pencil_point.transform(t);

            pencil_tip.moveTo(132,203);
            pencil_tip.lineTo(150,192);
            pencil_tip.lineTo(150,212);
            pencil_tip.closePath();
            pencil_tip.transform(t);
        }

        Notes(LWComponent lwc, Color c) { super(lwc, c); }
        Notes(LWComponent lwc) { super(lwc); }
        
        boolean isShowing() { return mLWC.hasNotes(); }
        
        void doDoubleClickAction() {
            VUE.objectInspectorPanel.activateNotesTab();
            VUE.objectInspector.setVisible(true);
        }
    
        private JComponent ttNotes;
        private String ttLastNotes;
        public JComponent getToolTipComponent()
        {
            // todo: would be more efficent to list for note change
            // events instead of comparing the whole string every time
            // -- especially for big notes (this goes for all the other
            // LWIcon tool tips also)
            if (ttNotes == null || !ttLastNotes.equals(mLWC.getNotes())) {
                ttLastNotes = mLWC.getNotes();
                int size = ttLastNotes.length();
                //System.out.println("width="+width);

                if (size > 50 || ttLastNotes.indexOf('\n') >= 0) {
                    JTextArea ta = new JTextArea(ttLastNotes, 1, 30);
                    ta.setFont(FONT_SMALL);
                    ta.setLineWrap(true);
                    ta.setWrapStyleWord(true);
                    //System.out.println("    size="+ta.getSize());
                    //Dimension ps = ta.getPreferredSize();
                    //System.out.println("prefsize="+ps);
                    //System.out.println(" minsize="+ta.getMinimumSize());
                    ttNotes = ta;
                } else {
                    ttNotes = new JLabel(ttLastNotes);
                    ttNotes.setFont(FONT_SMALL);
                }
            }
            return ttNotes;
        }

        
        public void draw(DrawContext dc)
        {
            super.draw(dc);
            double x = getX();
            double y = getY();
            
            dc.g.translate(x, y);

            // an experiment in semantic zoom
            /*
            if (dc.zoom >= 8.0) {
                dc.g.setFont(MinisculeFont);
                dc.g.setColor(Color.gray);
                dc.g.drawString(this.node.getNotes(), 0, (int)(super.height));
                }*/

            double x2 = (getWidth() - iconWidth) / 2;
            double y2 = (getHeight() - iconHeight) / 2;
            dc.g.translate(x2, y2);
            x += x2;
            y += y2;

            dc.g.setColor(mColor);
            dc.g.fill(pencil_body);
            dc.g.setStroke(stroke);
            dc.g.setColor(Color.white);
            dc.g.fill(pencil_point);
            dc.g.setColor(mColor);
            dc.g.draw(pencil_point);
            dc.g.fill(pencil_tip);

            dc.g.translate(-x, -y);
        }
    }

    static class Pathway extends LWIcon
    {
        private final static float MaxX = 224;
        private final static float MaxY = 145;

        private final static double scale = DefaultScale;
        private final static double scaleInv = 1/scale;

        private final static Stroke stroke = new BasicStroke((float)(0.5/scale));

        static float iconWidth = (float) (MaxX * scale);
        static float iconHeight = (float) (MaxY * scale);

        //-------------------------------------------------------

        private final static Line2D line1 = new Line2D.Float( 39,123,  92, 46);
        private final static Line2D line2 = new Line2D.Float(101, 43, 153,114);
        private final static Line2D line3 = new Line2D.Float(163,114, 224, 39);

        private final static Ellipse2D dot1 = new Ellipse2D.Float(  0,95, 62,62);
        private final static Ellipse2D dot2 = new Ellipse2D.Float( 65, 0, 62,62);
        private final static Ellipse2D dot3 = new Ellipse2D.Float(127,90, 62,62);

        
        Pathway(LWComponent lwc, Color c) { super(lwc, c); }
        Pathway(LWComponent lwc) { super(lwc); }

        boolean isShowing() { return mLWC.inPathway(); }

        void doDoubleClickAction() {
            VUE.sMapInspector.setVisible(true); //TODO: show the right panel
        }
        
        private JComponent ttPathway;
        private String ttPathwayHtml;
        public JComponent getToolTipComponent()
        {
            String html = "<html>";
            Iterator i = mLWC.pathwayRefs.iterator();
            int n = 0;
            while (i.hasNext()) {
                //tufts.vue.Pathway p = (tufts.vue.Pathway) i.next();
                LWPathway p = (LWPathway) i.next();
                // todo perf: use StringBuffer
                if (n++ > 0)
                    html += "<br>";
                html += "&nbsp;In path: <b>" + p.getLabel() + "</b>&nbsp;";
            }
            if (ttPathwayHtml == null || !ttPathwayHtml.equals(html)) {
                ttPathway = new AALabel(html);
                ttPathway.setFont(FONT_MEDIUM);
                ttPathwayHtml = html;
            }
            return ttPathway;
        }

        
        void draw(DrawContext dc)
        {
            super.draw(dc);
            double x = getX();
            double y = getY();
            
            dc.g.translate(x, y);

            double x2 = (getWidth() - iconWidth) / 2;
            double y2 = (getHeight() - iconHeight) / 2;
            dc.g.translate(x2, y2);
            x += x2;
            y += y2;
            
            dc.g.scale(scale,scale);

            dc.g.setColor(mColor);
            dc.g.fill(dot1);
            dc.g.fill(dot2);
            dc.g.fill(dot3);
            dc.g.setStroke(stroke);
            dc.g.draw(line1);
            dc.g.draw(line2);
            dc.g.draw(line3);

            dc.g.scale(scaleInv,scaleInv);
            dc.g.translate(-x, -y);
        }
    }
    
    static class MetaData extends LWIcon
    {
        //private final static int w = 28;
        private final static int w = 16;
        private final static float MaxX = 221;
        private final static float MaxY = 114+w;

        private final static double scale = DefaultScale;
        private final static double scaleInv = 1/scale;
        private final static AffineTransform t = AffineTransform.getScaleInstance(scale, scale);

        private static float iconWidth = (float) (MaxX * scale);
        private static float iconHeight = (float) (MaxY * scale);

        //-------------------------------------------------------

        private final static GeneralPath ul = new GeneralPath();
        private final static GeneralPath ll = new GeneralPath();
        private final static GeneralPath ur = new GeneralPath();
        private final static GeneralPath lr = new GeneralPath();
        static {
            ul.moveTo(0,58);
            ul.lineTo(96,0);
            ul.lineTo(96,w);
            ul.lineTo(0,58+w);
            ul.closePath();
            ul.transform(t);

            ll.moveTo(0,58);
            ll.lineTo(96,114);
            ll.lineTo(96,114+w);
            ll.lineTo(0,58+w);
            ll.closePath();
            ll.transform(t);

            ur.moveTo(125,0);
            ur.lineTo(221,58);
            ur.lineTo(221,58+w);
            ur.lineTo(125,w);
            ur.closePath();
            ur.transform(t);
            
            lr.moveTo(221,58);
            lr.lineTo(125,114);
            lr.lineTo(125,114+w);
            lr.lineTo(221,58+w);
            lr.closePath();
            lr.transform(t);
        }
        
        MetaData(LWComponent lwc, Color c) { super(lwc, c); }
        MetaData(LWComponent lwc) { super(lwc); }

        boolean isShowing() { return mLWC.hasMetaData(); }

        void doDoubleClickAction() {
            System.out.println(this + ": ACTION");
        }
        
        private JComponent ttMetaData;
        private String ttMetaDataHtml;
        public JComponent getToolTipComponent()
        {
            String html = "<html>";
            html += mLWC.getMetaDataAsHTML();
            if (ttMetaDataHtml == null || !ttMetaDataHtml.equals(html)) {
                ttMetaData = new AALabel(html);
                ttMetaData.setFont(FONT_MEDIUM);
                ttMetaDataHtml = html;
            }
            return ttMetaData;
        }

        
        void draw(DrawContext dc)
        {
            super.draw(dc);
            double x = getX() + (getWidth() - iconWidth) / 2;
            double y = getY() + (getHeight() - iconHeight) / 2;
            
            dc.g.translate(x, y);
            dc.g.setColor(mColor);

            dc.g.fill(ul);
            dc.g.fill(ur);
            dc.g.fill(ll);
            dc.g.fill(lr);
            
            dc.g.translate(-x, -y);
        }
    }

    static class Behavior extends LWIcon
    {
        private final static float sMaxX = 194;
        private final static float sMaxY = 155;

        private final static double scale = DefaultScale;
        private final static double scaleInv = 1/scale;

        static float iconWidth = (float) (sMaxX * scale);
        static float iconHeight = (float) (sMaxY * scale);

        //-------------------------------------------------------

        
        private final static int pw = 15; // plus-sign "stroke" width
        private final static int bw = 10; // bracket "stroke" width
        private final static int sl = 52; // bracket stub length
        private final static Rectangle2D plus_vert = new Rectangle2D.Float(89,16,  pw,123);
        private final static Rectangle2D plus_horz = new Rectangle2D.Float(37,70,  123,pw);
        private final static Rectangle2D bracket_left = new Rectangle2D.Float(0,bw-1, bw,sMaxY-bw*2+2);
        private final static Rectangle2D bracket_right= new Rectangle2D.Float(sMaxX-bw,bw-1, bw,sMaxY-bw*2+2);
        private final static Rectangle2D bracket_ul = new Rectangle2D.Float(0,0,                sl,bw);
        private final static Rectangle2D bracket_ur = new Rectangle2D.Float(sMaxX-sl,0,         sl,bw);
        private final static Rectangle2D bracket_ll = new Rectangle2D.Float(0,sMaxY-bw,         sl,bw);
        private final static Rectangle2D bracket_lr = new Rectangle2D.Float(sMaxX-sl,sMaxY-bw,  sl,bw);

        
        Behavior(LWComponent lwc, Color c) { super(lwc, c); }
        Behavior(LWComponent lwc) { super(lwc); }

        boolean isShowing() { return mLWC.hasResource() && mLWC.getResource() instanceof AssetResource;  }

        void doDoubleClickAction() {
            System.out.println("Behavior action?");
        }
        
        private JComponent ttBehavior;
        private String ttBehaviorHtml;
        public JComponent getToolTipComponent()
        {
            String html = "<html>Behavior from: " + mLWC.getResource().getToolTipInformation();
            if (ttBehaviorHtml == null || !ttBehaviorHtml.equals(html)) {
                ttBehavior = new AALabel(html);
                ttBehavior.setFont(FONT_SMALL);
                ttBehaviorHtml = html;
            }
            return ttBehavior;
        }
        
        void draw(DrawContext dc)
        {
            super.draw(dc);
            double x = getX() + (getWidth() - iconWidth) / 2;
            double y = getY() + (getHeight() - iconHeight) / 2;
            
            dc.g.translate(x, y);
            dc.g.scale(scale,scale);

            dc.g.setColor(mColor);
            dc.g.fill(plus_vert);
            dc.g.fill(plus_horz);
            dc.g.fill(bracket_left);
            dc.g.fill(bracket_right);
            dc.g.fill(bracket_ul);
            dc.g.fill(bracket_ll);
            dc.g.fill(bracket_ur);
            dc.g.fill(bracket_lr);

            dc.g.scale(scaleInv,scaleInv);
            dc.g.translate(-x, -y);
        }
    }
    static class Hierarchy extends LWIcon
    {
        private final static float MaxX = 220;
        private final static float MaxY = 155;

        private final static double scale = DefaultScale;
        private final static double scaleInv = 1/scale;

        private final static Stroke stroke = STROKE_TWO;

        static float iconWidth = (float) (MaxX * scale);
        static float iconHeight = (float) (MaxY * scale);

        //-------------------------------------------------------

        private final static Line2D line1 = new Line2D.Float(101, 16, 141, 16); // top horiz
        private final static Line2D line2 = new Line2D.Float( 70, 76, 141, 76); // middle long horiz
        private final static Line2D line3 = new Line2D.Float(101,136, 141,136); // bottom horiz
        private final static Line2D line4 = new Line2D.Float(101, 16, 101,136); // vertical

        private final static Rectangle2D box = new Rectangle2D.Float(0,51, 56,56);
        private final static Rectangle2D rect1 = new Rectangle2D.Float(150,  0, 70,33);
        private final static Rectangle2D rect2 = new Rectangle2D.Float(150, 63, 70,33);
        private final static Rectangle2D rect3 = new Rectangle2D.Float(150,122, 70,33);

        
        Hierarchy(LWComponent lwc, Color c) { super(lwc, c); }
        Hierarchy(LWComponent lwc) { super(lwc); }

        boolean isShowing() { return mLWC.hasChildren(); }

        void doDoubleClickAction() {
            VUE.objectInspectorPanel.activateTreeTab();
            VUE.objectInspector.setVisible(true);
        }
        
        private JLabel ttTree;
        private String ttTreeHtml;
        public JComponent getToolTipComponent()
        {
            if ((mLWC instanceof LWContainer) == false)
                return new JLabel("no children: no hierarchy");
            
            // todo perf: use StringBuffer
            String html = "<html>" + getChildHtml(mLWC, 1);
            if (html.endsWith("<br>"))
                html = html.substring(0, html.length()-4);
            //System.out.println("HTML [" + html + "]");
            if (ttTreeHtml == null || !ttTreeHtml.equals(html)) {
                ttTree = new AALabel(html);
                ttTree.setFont(FONT_MEDIUM);
                ttTreeHtml = html;
            }
            return ttTree;
        }

        private static final String Indent = "&nbsp;&nbsp;&nbsp;&nbsp;";
        private static final String RightMargin = Indent;
        //private static final String RightMargin = "&nbsp;&nbsp;&nbsp;";
        
        private String getChildHtml(LWComponent c, int indent)
        {
            // todo perf: use StringBuffer
            String label = null;
            if (indent == 1)
                label = "&nbsp;<b>" + c.getDisplayLabel() + "</b>";
            else
                label = c.getDisplayLabel();
            
            String html = label + RightMargin + "<br>";

            if (!(c instanceof LWContainer))
                return html;
            
            Iterator i = ((LWContainer)c).getChildIterator();
            int n = 0;
            while (i.hasNext()) {
                LWComponent child = (LWComponent) i.next();
                //if (n++ > 0) html += "<br>";
                for (int x = 0; x < indent; x++)
                    html += Indent;
                if (indent % 2 == 0)
                    html += "- ";
                else
                    html += "+ ";
                html += getChildHtml(child, indent + 1);
            }
            return html;
        }
        

        void draw(DrawContext dc)
        {
            super.draw(dc);
            double x = getX() + (getWidth() - iconWidth) / 2;
            double y = getY() + (getHeight() - iconHeight) / 2;
            
            dc.g.translate(x, y);
            dc.g.scale(scale,scale);

            dc.g.setColor(mColor);
            dc.g.fill(box);
            dc.g.fill(rect1);
            dc.g.fill(rect2);
            dc.g.fill(rect3);
            dc.g.setStroke(stroke);
            dc.g.draw(line1);
            dc.g.draw(line2);
            dc.g.draw(line3);
            dc.g.draw(line4);

            dc.g.scale(scaleInv,scaleInv);
            dc.g.translate(-x, -y);
        }
    }
    
    private static Font MinisculeFont = new Font("SansSerif", Font.PLAIN, 1);
    //private static Font MinisculeFont = new Font("Arial Narrow", Font.PLAIN, 1);

    
}

