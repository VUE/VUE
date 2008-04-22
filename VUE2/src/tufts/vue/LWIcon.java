/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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

import tufts.vue.gui.TextRow;
import tufts.vue.ui.ResourceIcon;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.border.*;

import edu.tufts.vue.preferences.PreferencesManager;

import java.util.Iterator;
import edu.tufts.vue.preferences.PreferencesManager;
import edu.tufts.vue.preferences.VuePrefEvent;
import edu.tufts.vue.preferences.VuePrefListener;
import edu.tufts.vue.preferences.implementations.BooleanPreference;
import edu.tufts.vue.preferences.implementations.ShowIconsPreference;
import edu.tufts.vue.preferences.interfaces.VuePreference;

/**
 * Icon's for displaying on LWComponents.
 *
 * Various icons can be displayed and stacked vertically or horizontally.
 * The icon region displays a tool-tip on rollover and may handle double-click.
 *
 * @version $Revision: 1.71 $ / $Date: 2007/11/22 07:28:50 $ / $Author: sfraize $
 *
 */

public abstract class LWIcon extends Rectangle2D.Float
    implements VueConstants
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LWIcon.class);
    
    private static final float DefaultScale = 0.045f; // scale to apply to the absolute size of our vector based icons
    private static final Color TextColor = VueResources.getColor("node.icon.color.foreground");
    private static final Color FillColor = VueResources.getColor("node.icon.color.fill");
    private static final Font FONT_ICON = VueResources.getFont("node.icon.font");
    
    private final static BooleanPreference oneClickLaunchResPref = BooleanPreference.create(
			edu.tufts.vue.preferences.PreferenceConstants.INTERACTIONS_CATEGORY,
			"oneClickResLaunch", 
			"Resource Launching", 
			"Enable launching resources on nodes with a single-click?",
			Boolean.TRUE,
			true);
    
    protected LWComponent mLWC;
    protected final Color mColor;
    protected float mMinWidth;
    protected float mMinHeight;
    
    //  ------------------------------------------------------------------
    // Preferences
    //------------------------------------------------------------------
    private static final ShowIconsPreference IconPref = new ShowIconsPreference();

    private LWIcon(LWComponent lwc, Color c) {
        // default size
        super.width = 22;
        super.height = 12;
        mLWC = lwc;
        mColor = c;
    }
    private LWIcon(LWComponent lwc) {
        this(lwc, TextColor);
    }

    public static ShowIconsPreference getShowIconPreference()
    {
    	return IconPref;
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
    
    /**
     * do we contain coords x,y?
     * Coords may be component local or map local or
     * whataver -- depends on what was handed to us
     * via @see setLocation
     */
    public boolean contains(float x, float y)
    {
        if (isShowing() && super.width > 0 && super.height > 0) {
            return x >= super.x+1
                && y >= super.y+1
                && x <= (super.x + super.width -1)
                && y <= (super.y + super.height -1);
        }
        return false;
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

//     public void setColor(Color c) {
//         mColor = c;
//     }
    
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
    abstract void doSingleClickAction();
    abstract void doDoubleClickAction();
    abstract public JComponent getToolTipComponent();
    //todo: make getToolTipComponent static & take lwc arg in case anyone else wants these

    
    //=============================================================================
    //=============================================================================
    // This is a TOTAL hack for a last minute change to VUE 2.0.1 2008-04-15 -- SMF
    //-----------------------------------------------------------------------------
    
    private static final Cursor RESOURCE_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
//     public static final Cursor RESOURCE_CURSOR = new Cursor(Cursor.HAND_CURSOR) {
//             public String getName() { return "VUE-RESOURCE-HAND"; }};
    private static tufts.vue.Resource RolloverResource;

    public static boolean hasRolloverResource(Cursor cursor) {
        return RolloverResource != null && cursor == RESOURCE_CURSOR;
    }
    public static void clearRolloverResource() {
        RolloverResource = null;
    }
    public static void displayRolloverResource() {
        if (RolloverResource != null) {
            
            // uses failsafe operation to reduce buggy side effecs of this hack: only
            // first attempt works, then we auto-clear the hacked up global state that
            // says "a resource icon is currently active to override all single clicks
            // on the map" until mouse moves over resource icon region again to be
            // reconfirmed as a viable option.
        	if (((Boolean)oneClickLaunchResPref.getValue()).booleanValue())
            RolloverResource.displayContent();
            clearRolloverResource();
        }
    }
        
    //=============================================================================
    //=============================================================================

    public static class Block extends Rectangle2D.Float
    {
        public static final boolean VERTICAL = true;
        public static final boolean HORIZONTAL = false;
        //public static final int COORDINATES_MAP = 0;
        //public static final int COORDINATES_COMPONENT  = 1;
        //public static final int COORDINATES_COMPONENT_NO_SHRINK = 2; // only currently works for blocks laid out at 0,0 of node
        
        private LWComponent mLWC;
        
        private LWIcon[] mIcons = new LWIcon[7];

        //final private boolean mCoordsNodeLocal;
        //final private boolean mCoordsNoShrink; // don't let icon's get less than 100% zoom
        final private boolean mNoShrink; // don't let icon's get less than 100% zoom
        private boolean mVertical;
        private final float mIconWidth;
        private final float mIconHeight;
               
        public Block(LWComponent lwc,
                     int iconWidth,
                     int iconHeight,
                     Color c,
                     boolean vertical)
                     //boolean noShrink)
                        //int coordStyle)
        {
            if (c == null)
                c = TextColor;

            //mCoordsNodeLocal = (coordStyle >= COORDINATES_COMPONENT);
            //mCoordsNoShrink = (coordStyle == COORDINATES_COMPONENT_NO_SHRINK);
            mNoShrink = (lwc instanceof LWImage);
            mIconWidth = iconWidth;
            mIconHeight = iconHeight;
            setOrientation(vertical);

            // todo: create these lazily
            mIcons[0] = new LWIcon.Resource(lwc, c);
     //       mIcons[1] = new LWIcon.Behavior(lwc, c);
            mIcons[1] = new LWIcon.Notes(lwc, c);
            mIcons[2] = new LWIcon.Pathway(lwc, c);
            mIcons[3] = new LWIcon.MetaData(lwc, c);
            mIcons[4] = new LWIcon.Hierarchy(lwc, c);
            mIcons[5] = new LWIcon.MergeSourceMetaData(lwc,c);
            mIcons[6] = new LWIcon.OntologicalMetaData(lwc,c);
            
            for (int i = 0; i < mIcons.length; i++) {
                mIcons[i].setSize(iconWidth, iconHeight);
                mIcons[i].setMinimumSize(iconWidth, iconHeight);
            }

            this.mLWC = lwc;
            
            // todo perf: a bit heavy weight to have every node made a listener
            // for this preference -- better to handle at the map level.
            LWIcon.getShowIconPreference().addVuePrefListener(new VuePrefListener() {
                    public void preferenceChanged(VuePrefEvent prefEvent) {
                        mLWC.layout();
                        mLWC.notify(LWKey.Repaint);
                        
                    }        	
            });

        }

        public float getIconWidth() { return mIconWidth; }
        public float getIconHeight() { return mIconHeight; }

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
                    && x <= (super.x + super.width)
                    && y <= (super.y + super.height);
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
         * width & height -- does NOT change location of the block itself
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
                        iconY += icon.height+3;
                        super.height += icon.height+3;
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
                        iconX += icon.width+3;
                        super.width += icon.width+3;
                    }
                }
            }
        }

        void draw(DrawContext dc)
        {

            // If mCoordsNoShrink is true, never let icon size get less than 100% for
            // images (tho also it shouldn't be allowed BIGGER than the object...)  This
            // is experimental in that it only currently works if the block is laid out
            // at 0,0, because we scale the DrawContext once at the top here, which will
            // offset any non-zero locations (and we don't want the location changed,
            // only the size).  You you place the block at > 0,0, the icons will be
            // moved outside the node when the scale gets small enough.

            // Also, if the scale becomes VERY small, the icon block will be drawn
            // bigger than the image itself.  TODO: fix all the above or handle this
            // some other way.
            
            if (mNoShrink && dc.zoom < 1)
                dc.setAbsoluteDrawing(true);
            
            for (int i = 0; i < mIcons.length; i++) {
                if (mIcons[i].isShowing())
                    mIcons[i].draw(dc);
            }

            if (mNoShrink && dc.zoom < 1)
                dc.setAbsoluteDrawing(false);
        }

        void checkAndHandleMouseOver(MapMouseEvent e)
        {
            final Point2D.Float localPoint = e.getLocalPoint(mLWC);
            float cx = localPoint.x;
            float cy = localPoint.y;
            
            RolloverResource = null;
            
            JComponent tipComponent = null;
            LWIcon tipIcon = null;


            for (LWIcon icon : mIcons) {
                if (!icon.isShowing())
                    continue;

                if (mNoShrink) {
                	
                    // TODO: this probably no longer quite right given local coords...
                    double zoom = e.getViewer().getZoomFactor();
                    if (zoom < 1) {
                        cx *= zoom;
                        cy *= zoom;
                    }
                }
                if (icon instanceof LWIcon.Resource) { 
                    if (icon.contains(cx, cy)) {	
                        // e.getViewer().clearTip();
                        // TODO: need cursor management system for MapViewer's that allows
                        // us to install a temporary cursor that's auto-cleared if
                        // mouse exits or re-enters the viewer
                        e.getViewer().setCursor(RESOURCE_CURSOR);
                        RolloverResource = icon.mLWC.getResource(); // hack hack hack
                    } else {
                        e.getViewer().setCursor(VueToolbarController.getActiveTool().getCursor());                		                		
                    }
                }
                
                if (icon.contains(cx, cy)) {                	                	                                	
                    tipIcon = icon;
                    break;
                } else {
                    // check: if (above condition) then: break else: reset cursor, but at the bottom of a loop?
                    e.getViewer().setCursor(VueToolbarController.getActiveTool().getCursor());        
                }
                	
            }
            
            // TODO: don't need to do this if there's already a tip showing!
            if (tipIcon != null) {
                tipComponent = tipIcon.getToolTipComponent();
                final Rectangle2D tipRegion = mLWC.transformZeroToMapRect((Rectangle2D.Float) tipIcon.getBounds2D());

                // if node, compute avoid region node+tipRegion,
                // if link avoid = label+entire tip block
                final Rectangle2D avoidRegion;

                if (mLWC instanceof LWLink) {
                    if (mLWC.hasLabel()) {
                        avoidRegion = new Rectangle2D.Float();
                        // Stay away from the link label:
                        avoidRegion.setRect(mLWC.labelBox.getBoxBounds());
                        // Also stay way from the whole icon block:
                        avoidRegion.add(this);
                    } else {
                        // Just stay way from the whole icon block:
                        avoidRegion = this;
                    }
                    mLWC.transformZeroToMapRect((Rectangle2D.Float)avoidRegion);
                } else {
                    // So it works when zoomed focus, use the transform:
                    avoidRegion = mLWC.transformZeroToMapRect(mLWC.getZeroBounds());
                    //avoidRegion = mLWC.getBounds();
                }
                
                //if (!(tipIcon instanceof LWIcon.Resource))
               // {
                    e.getViewer().setTip(tipComponent, avoidRegion, tipRegion);
               // }
            }
        }
        
        boolean handleSingleClick(MapMouseEvent e)
        {
        	boolean handled = false;
            final Point2D.Float localPoint = e.getLocalPoint(mLWC);

            for (int i = 0; i < mIcons.length; i++) {
                LWIcon icon = mIcons[i];
                if (icon.isShowing() && icon.contains(localPoint.x, localPoint.y)) {
                    icon.doSingleClickAction();
                    handled = true;
                    break;
                }
            }
            return handled;
        	
        }
        boolean handleDoubleClick(MapMouseEvent e)
        {
            boolean handled = false;
            final Point2D.Float localPoint = e.getLocalPoint(mLWC);

            for (int i = 0; i < mIcons.length; i++) {
                LWIcon icon = mIcons[i];
                if (icon.isShowing() && icon.contains(localPoint.x, localPoint.y)) {
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
    
        private TextRow mTextRow;
        private String extension;
        private Rectangle2D.Float boxBounds;
        
        Resource(LWComponent lwc) { super(lwc); }
        Resource(LWComponent lwc, Color c) {
            super(lwc, c);
            layout();
        }

        boolean isShowing() {
        	if (IconPref.getResourceIconValue())
        		return mLWC.hasResource();
        	else 
        		return false;
        	}

     //   void doDoubleClickAction() {}
         void doDoubleClickAction() {
         	if (!((Boolean)oneClickLaunchResPref.getValue()).booleanValue())
         	{
                     Log.debug("DOUBLE-CLICK " + getClass());
                     mLWC.getResource().displayContent();
         	}
         }
        void doSingleClickAction() {
        	if (((Boolean)oneClickLaunchResPref.getValue()).booleanValue())
        	{
                    Log.debug("SINGLE CLICK " + getClass() + " " + mLWC);
                    mLWC.getResource().displayContent();
        	}
        }
        
        final static String gap = "&nbsp;";
        final static String indent = "";
        //final static String indent = "&nbsp;";
        
        private JLabel ttResource;
        public JComponent getToolTipComponent()
        {
            if (ttResource == null) {
                ttResource = new AALabel("");
                //label.setFont(FONT_MEDIUM);
                // todo: "Arial Unicode MS" looks great on mac (which it maps to Hevetica, which on
                // mac supports unicode, and appears identical to Arial), but is only thing that
                // works for unicode on the PC, yet looks relaively crappy for regular text.  Check
                // into fonts avail on Win XP (this was Win2k) todo: try embedding the font name in
                // the HTML above for just the title, and not the URL & click message.
                ttResource.setFont(FONT_MEDIUM_UNICODE);
            }
            
            final tufts.vue.Resource r = mLWC.getResource();
            final boolean hasTitle = (r.getTitle() != null && !r.getTitle().equals(r.getLocationName()));
            final String prettyResource = r.getLocationName();
            ttResource.setIcon(r.getTinyIcon());
            ttResource.setVerticalTextPosition(SwingConstants.TOP);
            ttResource.setText(
                    "<html>"
                    + (hasTitle ? (indent + r.getTitle() + "&nbsp;<br>") : "")
                    + indent + prettyResource// + gap
                    //+ "<font size=-2 color=#999999><br>" + indent + "Double-click to open in new window&nbsp;"
                               );


//             final tufts.vue.Resource r = mLWC.getResource();
//             final boolean hasTitle = (r.getTitle() != null && !r.getTitle().equals(r.getSpec()));
//             final String prettyResource = r.getSpec();
//             ttResource.setIcon(r.getTinyIcon());
//             ttResource.setVerticalTextPosition(SwingConstants.TOP);
//             // already has a border -- either make compound or put in a panel
// //             if (DEBUG.BOXES)
// //                 ttResource.setBorder(new LineBorder(Color.green, 1));
// //             else
// //                 ttResource.setBorder(BorderFactory.createEmptyBorder(1,1,0,1));
//             ttResource.setText(
//                     "<html>"
//                     + (hasTitle ? (indent + r.getTitle() + "&nbsp;<br>") : "")
//                     + indent + prettyResource// + gap
//                     //+ "<font size=-2 color=#999999><br>" + indent + "Double-click to open in new window&nbsp;"
//                                );
            
                
            return ttResource;
        }
        
//         private JComponent ttResource;
//         private String ttLastString;
//         private boolean hadTitle = false;
//         private long lastAccess = 0;
//         public JComponent getToolTipComponent()
//         {
//             tufts.vue.Resource r = mLWC.getResource();
//             boolean hasTitle = (r.getTitle() != null && !r.getTitle().equals(r.getSpec()));
//             long access = 0;
//             if (r instanceof URLResource)
//                 access = ((URLResource)r).getAccessSuccessful();

//             if (ttResource == null
//                 || access > lastAccess // title may have been updated
//                 || !ttLastString.equals(mLWC.getResource().getSpec())
//                 || hadTitle != hasTitle)
//             {
//                 hadTitle = hasTitle;
//                 ttLastString = mLWC.getResource().getSpec();
//                 // todo perf: use StringBuffer
//                 String prettyURL = VueUtil.decodeURL(ttLastString);
//                 if (prettyURL.startsWith("file://") && prettyURL.length() > 7)
//                     prettyURL = prettyURL.substring(7);
//                 final String html =
//                     "<html>"
//                     + (hasTitle ? (gap + "<b>"+mLWC.getResource().getTitle()+"</b>&nbsp;<br>") : "")
//                     + gap + prettyURL + gap
//                     //+ (hasTitle?("<font size=-2><br>&nbsp;"+mLWC.getResource().getTitle()+"</font>"):"")
//                     + "<font size=-2 color=#999999><br>&nbsp;Double-click to open in new window&nbsp;"
//                     ;
                
//                 JLabel label = new AALabel(html);
//                 //label.setFont(FONT_MEDIUM);
//                 // todo: "Arial Unicode MS" looks great on mac (which it maps to Hevetica, which on
//                 // mac supports unicode, and appears identical to Arial), but is only thing that
//                 // works for unicode on the PC, yet looks relaively crappy for regular text.  Check
//                 // into fonts avail on Win XP (this was Win2k) todo: try embedding the font name in
//                 // the HTML above for just the title, and not the URL & click message.
//                 label.setFont(FONT_MEDIUM_UNICODE);

//                 if (false) {
//                     // example of button a gui component in a rollover
//                     JPanel panel = new JPanel();
//                     panel.setName("LWIcon$Resource-Action");
//                     panel.add(label);
//                     //JButton btn = new JButton(new VueAction(prettyURL) { // looks okay but funny & leaves out title...
//                     //JButton btn = new JButton(new VueAction(html) { // looks terrible
//                     AbstractButton btn = new JButton(new VueAction("Open") {
//                             // TODO: need a superclass of VueAction that doesn't add it to a global list,
//                             // as this action is very transient, and we'll want it GC'able.  And actually,
//                             // in this case, the resource object itself ought to have an action built in
//                             // that can be re-used by everyone interested in doing this.
//                             public void act() { doDoubleClickAction(); }
//                         });
                    
//                     btn.setOpaque(false);
//                     btn.setToolTipText(null);
//                     btn.setFont(FONT_SMALL_BOLD);
//                     panel.add(btn);
//                     //panel.setBackground(Color.white); // no effect
//                     ttResource = panel;
//                 } else {
//                     ttResource = label;
//                 }
//             }
            
//             lastAccess = access;
                
//             return ttResource;
//         }

//         void draw(DrawContext dc)
//         {
//             super.draw(dc);

//             if (mLWC.hasResource()) {

//                 // Draw a small image icon instead of the text "extension" icon
                
//                 final Image image;

//                 if (!dc.isInteractive() || dc.getAbsoluteScale() >= 2) {
//                     // non-interative: eg, printing or image generating
//                     image = mLWC.getResource().getLargeIconImage();
//                 } else
//                     image = mLWC.getResource().getTinyIconImage();
                
//                 if (image != null) {
//                     final double iw = image.getWidth(null);
//                     final double ih = image.getHeight(null);
//                      final AffineTransform tx = AffineTransform.getTranslateInstance(getX() + (getWidth() - 16) / 2,
//                                                                                      getY() + (getHeight() - 16) / 2);
//                     //final AffineTransform tx = AffineTransform.getScaleInstance(1.0/8.0, 1.0/8.0);
//                     //final AffineTransform tx = new AffineTransform();
//                     if (iw > 16)
//                         tx.scale(16 / iw, 16 / iw);
//                     //tx.scale(1.0/8.0, 1.0/8.0);
//                     dc.g.drawImage(image, tx, null);
//                     //return;
//                 }
//             }

//             if (mTextRow == null)
//                 return;

//             double _x = getX();
//             double _y = getY();

//             dc.g.translate(_x, _y);
//             dc.g.setColor(mColor);
//             dc.g.setFont(FONT_ICON);

//             float xoff = (super.width - mTextRow.width) / 2;
//             float yoff = (super.height - mTextRow.height) / 2;
//             mTextRow.draw(dc.g, xoff, yoff);

//             // an experiment in semantic zoom
//             // (SansSerif point size 1 MinisculeFont get's garbled on mac, so we don't do it there)
//             if (!VueUtil.isMacPlatform() && mLWC.hasResource() && dc.g.getTransform().getScaleX() >= 8.0) {
//                 dc.g.setFont(MinisculeFont);
//                 dc.g.setColor(Color.gray);
//                 dc.g.drawString(mLWC.getResource().toString(), 0, (int)(super.height));
//             }

//             dc.g.translate(-x, -y);
//         }

        void layout() {
            extension = null;
            internalLayout();
        }

        //private static final Color BoxFill = new Color(238, 238, 238);
        private static final Color BoxFill = FillColor;
        //private static final Color BoxBorder = new Color(149, 149, 149);
        
        void internalLayout()
        {
            if (extension == null) {
                if (mLWC.hasResource()) 
                    extension = mLWC.getResource().getDataType();
                
                if (extension == null || extension.length() < 1) {
                    extension = NoResource;
                } else if (extension.length() > 3) {
                    extension = extension.substring(0,3);
                }
                
//                 if (DEBUG.RESOURCE) {
//                     if (mLWC.hasResource())
//                         Log.debug("  " + mLWC.getResource() + "; yielded extension ["+extension+"]");
//                     else if (DEBUG.META)
//                         Log.debug(mLWC + "; no resource; extension = ["+extension+"]");
//                 }
                

                mTextRow = TextRow.instance(extension.toLowerCase(), FONT_ICON);
            }
            
            if (boxBounds == null)
                boxBounds = new Rectangle2D.Float();
            
            // todo: should only have to do this once, but we need to fix init
            // so that this doesn't get called till Rectangle2D.this is fully positioned
            boxBounds.setRect(this);
            final float insetW = 2;
            final float insetH = 1;
            boxBounds.x += insetW;
            boxBounds.y += insetH;
            boxBounds.width -= insetW * 2;
            boxBounds.height -= insetH * 2;

//             // Resource icon special case can override parent set width:
//             super.width = mTextRow.width;
//             if (super.width < super.mMinWidth)
//                 super.width = super.mMinWidth;
        }
        
        void draw(DrawContext dc)
        {
            if (false&&DEBUG.Enabled) {
                // test code for inserting actual icon into node icon gutter (should at least do by
                // default for local file icons)
                Icon icon = mLWC.getResource().getContentIcon();
                if (icon instanceof ResourceIcon) {
                    ((ResourceIcon)icon).setSize((int)Math.round(getWidth()-4),(int)Math.round(getHeight()));
                    icon.paintIcon(null, dc.g, (int)Math.round(getX()+2), (int)Math.round(getY()+2));
                    super.draw(dc);
                    return;
                }
            }
            
            if (true || extension == null) // TODO PERF: sometimes starts with boxBounds wrong...
                internalLayout();
            
            super.draw(dc);

            // TODO PERF: if BoxFill has alpha, pre-mix it with node.getRenderFillColor()
            dc.g.setColor(BoxFill);
            dc.g.fill(boxBounds);
            //dc.g.setColor(BoxBorder);
            final Color c = mLWC.getRenderFillColor(dc); // todo: getContrastColor(dc)
            dc.g.setColor(c == null ? Color.gray : c.darker());
            dc.g.setStroke(STROKE_HALF);
            dc.g.draw(boxBounds);
            dc.g.setColor(mColor);
            dc.g.setFont(FONT_ICON);
            

//             String extension = NoResource;
//             if (mLWC.hasResource())
//                 extension = mLWC.getResource().getExtension();
            double x = getX();
            double y = getY();
            dc.g.translate(x, y);


            
            // todo perf: listen for resource change & cache text row
            //TextRow row = new TextRow(extension, dc.g);
            final TextRow row = mTextRow;
            // Resource icon special case can override parent set width:
            if (super.width < row.width)
                super.width = row.width;
            final float xoff = (super.width - row.width) / 2;
            final float yoff = (super.height - row.height) / 2;
            row.draw(dc, xoff, yoff);

//             // an experiment in semantic zoom
//             //if (dc.zoom >= 8.0 && mLWC.hasResource()) {
//             if (mLWC.hasResource() && dc.g.getTransform().getScaleX() >= 8.0) {
//                 dc.g.setFont(MinisculeFont);
//                 dc.g.setColor(Color.gray);
//                 dc.g.drawString(mLWC.getResource().toString(), 0, (int)(super.height));
//             }

            dc.g.translate(-x, -y);
        }
        
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
        
        boolean isShowing() {
        	if (IconPref.getNotesIconValue())
        		return mLWC.hasNotes();
        	else
        		return false;
        }
        void doSingleClickAction() {}
        void doDoubleClickAction() {

            // is it faster to use Method.invoke, or to
            // cons up a new object?  Probably a new object.
            
            // When it's know that theres just one target, this could cache the result of the
            // traversal.  Tho better to never assume that: could still cache all results...  tho
            // to be *fully* dynamic, would have to update cache every time AWT hierarchy changes,
            // tho that's pretty much all the time with menu's and rollovers...
            
            //AWTEventDispatch(this, ObjectInspectorPanel.class, "setTab", ObjectInspectorPanel.NOTES_TAB);
            //new EventDispatch(this, ObjectInspectorPanel.class) {}

            /*
            new EventRaiser(this, ObjectInspectorPanel.class) {
                public void dispatch(Object target) {
                    ObjectInspectorPanel oip = (ObjectInspectorPanel) target;
                    oip.setTab(ObjectInspectorPanel.NOTES_TAB);
                    //oip.setVisible(true); need to get parent window
                    // EventRaiser could cache last Window seen...
                    EventRaiser.stop();
                }
            }.raise();
            */

            tufts.vue.gui.GUI.makeVisibleOnScreen(this, NotePanel.class);
            //VUE.ObjectInspectorPanel.setTab(ObjectInspectorPanel.NOTES_TAB);
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
					ta.setSize(ta.getPreferredSize());      
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

        boolean isShowing() {
        	if (IconPref.getPathwayIconValue())
        		return mLWC.inPathway();
        	else
        		return false;
        	}

        void doDoubleClickAction() {
            tufts.vue.gui.GUI.makeVisibleOnScreen(this, PathwayPanel.class);
            //VUE.MapInspector.showTab("Pathway");
        }
        
        private JComponent ttPathway;
        private String ttPathwayHtml;
        public JComponent getToolTipComponent()
        {
            String html = "<html>";
            Iterator i = mLWC.getPathways().iterator();
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

        void doSingleClickAction() {}
        
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

        boolean isShowing() {
        	if (IconPref.getMetaDataIconValue())
        		return mLWC.hasMetaData();
        	else
        		return false;
        	}

        void doDoubleClickAction() {
            System.out.println(this + " Meta-Data Action?");
        }
        
        void doSingleClickAction() {}
        
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

    static class MergeSourceMetaData extends LWIcon
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

        //private final static GeneralPath oval = new GeneralPath(new Ellipse2D.Float(0,0,3,3));
        private final static Ellipse2D oval = new Ellipse2D.Float(0,0,3,3);
        
        MergeSourceMetaData(LWComponent lwc, Color c) { super(lwc, c); }
        MergeSourceMetaData(LWComponent lwc) { super(lwc); }

        boolean isShowing() {
        	if (IconPref.getMetaDataIconValue())
        		return mLWC.hasMetaData(edu.tufts.vue.metadata.VueMetadataElement.OTHER);
        	else
        		return false;
        	}

        void doDoubleClickAction() {
            System.out.println(this + " Merge Source Meta-Data Action?");
        }
        
        void doSingleClickAction() {}
        
        private JComponent ttMetaData;
        private String ttMetaDataHtml;
        public JComponent getToolTipComponent()
        {
            String html = "<html>";
            html += mLWC.getMetaDataAsHTML(edu.tufts.vue.metadata.VueMetadataElement.OTHER);
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

            //dc.g.fill(oval);
            dc.g.setStroke(STROKE_HALF);
            dc.g.draw(oval);
            
            //dc.g.fill(ul);
            //dc.g.fill(ur);
            //dc.g.fill(ll);
            //dc.g.fill(lr);
            
            dc.g.translate(-x, -y);
        }
    }
    
    
    static class OntologicalMetaData extends LWIcon
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
        
        private final static GeneralPath one = new GeneralPath();
        private final static GeneralPath two = new GeneralPath();
        private final static GeneralPath three = new GeneralPath();
        private final static GeneralPath four = new GeneralPath();
        private final static GeneralPath five = new GeneralPath();
        static {
            one.moveTo(73,96);
            one.lineTo(0,81);
            one.lineTo(11,46);
            one.lineTo(79,77);
            one.lineTo(74,84);
            one.closePath();
            one.transform(t);

            two.moveTo(83,73);
            two.lineTo(76,0);
            two.lineTo(111,0);
            two.lineTo(104,75);
            two.lineTo(94,72);
            two.closePath();
            two.transform(t);

            three.moveTo(107,77);
            three.lineTo(176,47);
            three.lineTo(186,80);
            three.lineTo(113,96);
            three.lineTo(112,86);
            three.closePath();
            three.transform(t);
            
            four.moveTo(113,100);
            four.lineTo(162,156);
            four.lineTo(133,177);
            four.lineTo(95,112);
            four.lineTo(106,109);
            four.closePath();
            four.transform(t);
            
            five.moveTo(91,112);
            five.lineTo(53,177);
            five.lineTo(24,156);
            five.lineTo(74,100);
            five.lineTo(81,109);
            five.closePath();
            five.transform(t);
        }
        
        OntologicalMetaData(LWComponent lwc, Color c) { super(lwc, c); }
        OntologicalMetaData(LWComponent lwc) { super(lwc); }

        boolean isShowing() {
        	if (IconPref.getMetaDataIconValue())
        		return mLWC.hasMetaData(edu.tufts.vue.metadata.VueMetadataElement.ONTO_TYPE);
        	else
        		return false;
        	}

        void doDoubleClickAction() {
            System.out.println(this + " Ontological Meta-Data Action?");
        }
        
        void doSingleClickAction() {}
        
        private JComponent ttMetaData;
        private String ttMetaDataHtml;
        public JComponent getToolTipComponent()
        {
            String html = "<html>";
            html += mLWC.getMetaDataAsHTML(edu.tufts.vue.metadata.VueMetadataElement.ONTO_TYPE);
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

            dc.g.fill(one);
            dc.g.fill(two);
            dc.g.fill(three);
            dc.g.fill(four);
            dc.g.fill(five);
            
            dc.g.translate(-x, -y);
        }
    }

/*    static class Behavior extends LWIcon
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

        boolean isShowing() { 
        	if (IconPref.getBehaviorIconValue())
        		return mLWC.hasResource() && mLWC.getResource() instanceof AssetResource;
        	else
        		return false;
        	}

        void doDoubleClickAction() {
            //VUE.ObjectInspectorPanel.setTab(ObjectInspectorPanel.INFO_TAB);
        }
        
        private JComponent ttBehavior;
        private String ttBehaviorHtml;
        public JComponent getToolTipComponent()
        {
            //String html = "<html>Behavior info: " + mLWC.getResource().getToolTipInformation();
            final String html = "Right click on node to see disseminations.";
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
    }*/
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

        boolean isShowing() {
            // TODO performance: getting complicated: compute in layout (and check for all text nodes, not just first)
            // Will need to make sure layout() is called when removing items from nodes: only appears to be called upon adding
            if (IconPref.getHierarchyIconValue()) {
                if (mLWC.numChildren() == 1) {
                    LWComponent child0 = mLWC.getChild(0);
                    if (child0.isTextNode() || LWNode.isImageNode(mLWC))
                        return false;
                    else
                        return true;
                } else if (mLWC.hasChildren())
                    return true;
            }
            return false;
        }

//         @Override
//         void layout() {
//             Log.debug("HIERARCY LAYOUT IN " + mLWC);
//         }

        void doDoubleClickAction() {
            //VUE.ObjectInspectorPanel.setTab(ObjectInspectorPanel.TREE_TAB);
        }
        
        void doSingleClickAction() {}
        
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
            
            //int n = 0;
            for (LWComponent child : c.getChildren()) {
                if (child.isTextNode())
                    continue;
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