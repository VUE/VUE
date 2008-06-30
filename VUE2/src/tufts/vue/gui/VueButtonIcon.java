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


package tufts.vue.gui;

import tufts.vue.DEBUG;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.AlphaComposite;
import javax.swing.Icon;
import javax.swing.AbstractButton;
import javax.swing.border.*;

/**
 * VueButtonIcon
 *
 * Icon class to support the various states of VUE buttons (rollover, pressed, disabled, etc).
 * Takes a raw icon which is used for painting the body of all the button states, but adjust borders,
 * transparency, etc, based on button state.  Can also handle a null raw icon for just drawing borders.
 * Can install, via installGenerated, a set of icons into any AbstractButton using our default VUE GUI scheme.
 *
 * @version $Revision: 1.14 $ / $Date: 2008-06-30 20:53:06 $ / $Author: mike $
 * @author Scott Fraize
 */

public class VueButtonIcon implements Icon
{
    public static final int UP = 0;        // unselected/default
    public static final int PRESSED = 1;   // only while being held down by a mouse press
    public static final int SELECTED = 2;  // selected (for toggle buttons)
    public static final int DISABLED = 3;  // disabled
    public static final int ROLLOVER = 4;  // rollover
    public static final int MENU = 5;              // sub-menus: default (palette menu)
    public static final int MENU_SELECTED = 6;     // sub-menus: rollover (palette menu)
            
    public static void installGenerated(AbstractButton b, Icon raw, Dimension s) {
    	installGenerated(b,raw,raw,s);
    }
    
    public static void installGenerated(AbstractButton b, Icon raw, Icon selected,Dimension s) {
        installGenerated(b,raw,selected,raw,s);
    }
    
    public static void installGenerated(AbstractButton b, Icon raw, Icon selected,Icon rollover,Dimension s) {
        if (DEBUG.INIT||DEBUG.TOOL) System.out.println(b + " generating button states from " + raw);
        if (s == null)
            s = new Dimension(0,0);

        if (GUI.isMacAqua() && b instanceof MenuButton) {
            b.setIcon(raw);
        } else {

            b.setIcon(new VueButtonIcon(raw, UP, s));
            b.setPressedIcon(new VueButtonIcon(raw, PRESSED, s));
            b.setSelectedIcon(new VueButtonIcon(selected, SELECTED, s));
            b.setDisabledIcon(new VueButtonIcon(raw, DISABLED, s));
            b.setRolloverIcon(new VueButtonIcon(rollover, ROLLOVER, s));
        }
    }

    private static final Color sButtonColor = new Color(222,222,222);
    private static final Color sOverColor = Color.gray;
    private static final Color sDownColor = sOverColor;
    private static final AbstractBorder sRolloverBorder = new EtchedBorder();
    //private static final AbstractBorder sRolloverBorder = new EtchedBorder(EtchedBorder.RAISED);
    //private static final AbstractBorder sRolloverBorder = new BevelBorder(BevelBorder.LOWERED);
    //private static final AbstractBorder sRolloverBorder = new SoftBevelBorder(BevelBorder.LOWERED);
    private static final AlphaComposite DisabledAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f);
    
    protected final int width;
    protected final int height;

    private static Insets insets = new Insets(0,0,0,0);
    private Color mColor = sButtonColor;

    private final int mType;
    private final Icon mRawIcon;
    private final boolean isPressIcon;
    protected boolean isRadioButton = false; // if in an exclusive button-group

    // OffsetWhenDown: nudge the icon when in the down state.
    // Set to true of "up" state appears as a button -- can
    // turn on otherwise but will need to adjust whole button so
    // icon stays centered.
    private final static boolean OffsetWhenDown = false;
    private final static boolean debug = false;

    public VueButtonIcon(Icon rawIcon, int t, int width, int height)
    {
    	
        this.mRawIcon = rawIcon;
        this.mType = t;
        if (rawIcon != null) {
            if (tufts.Util.isMacLeopard()) {
                // TODO: not sure why we need this -- may have to do with clipping
                this.width = width <= 0 ? rawIcon.getIconWidth() + 6 : width;
                this.height = height <= 0 ? rawIcon.getIconHeight() + 6 : height;
            } else {
                this.width = width <= 0 ? rawIcon.getIconWidth() + 4 : width;
                this.height = height <= 0 ? rawIcon.getIconHeight() + 4 : height;
            }
        } else {
            this.width = width;
            this.height = height;
        }
        this.isPressIcon = (t == PRESSED || t == SELECTED || t == MENU_SELECTED);
        
        if (isPressIcon)
            mColor = Color.lightGray;
        if (debug) {
            if (t == MENU) mColor = Color.pink;
            if (t == MENU_SELECTED) mColor = Color.magenta;
            if (t == ROLLOVER) mColor = Color.green;
        }
        //if (t >= MENU) insets = new Insets(-1,-1,1,1);
    }
            
    public VueButtonIcon(Icon rawIcon, int t, Dimension size) {
        this(rawIcon, t, size.width, size.height);
    }
    public VueButtonIcon(Icon rawIcon, int t) {
        this(rawIcon, t, 0, 0);
    }
        
    public int getIconWidth() { return width; }
    public int getIconHeight() { return height; }

    private final static String _types[] = {"Up","Pressed","Selected","Disabled","Rollover","Menu","Menu Selected"};
	
    /**
     * paint the entire button as an icon plus it's visible icon graphic
     */
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (debug||DEBUG.BOXES) System.out.println("painting "
                                                   + mRawIcon + " type " + mType + "(" + _types[mType]
                                                   + ") on " + c + " bg=" + c.getBackground());
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //   if (mType >= MENU) {
            // the drop-down menus have GC bugs on the PC such
            // that we need to be sure to paint something in
            // the entire region, or we appear to get another
            // version of the icon painted *under* us.
            g2.setColor(c.getBackground());
            g2.fillRect(0,0, width,height);
//        }

        if (debug) {
            g2.setColor(Color.red);
            g2.fillRect(0,0, 99,99);
        }
        x += insets.top;
        y += insets.left;
        //if (VueUtil.isMacPlatform()) { x += 2; y += 2; }// try now that attend to x/y above
        g2.translate(x,y);

        int w = width - (insets.left + insets.right);
        int h = height - (insets.top + insets.bottom);
                
        float gw = width;
        //GradientPaint gradient = new GradientPaint(gw/2,0,Color.white,gw/2,h/2,mColor,true);
        //GradientPaint gradient = new GradientPaint(gw/6,0,Color.white,gw/2,h/2,mColor,true);
        GradientPaint gradient = new GradientPaint(gw/6,0,Color.white,gw*.33f,h/2,mColor,true);
        // Set gradient for the whole button.

        if (isPressIcon) {
            // Draw the 3d button border -- raised/lowered depending on down state                
            g2.setColor(c.getBackground());
            g2.draw3DRect(0,0, w-1,h-1, !isPressIcon);
        } else if (mType == ROLLOVER) {
            // Draw an etched rollover border:

            if (DEBUG.BOXES) {
                g.setColor(Color.green);
                g.fillRect(0,0, w, h);
            }

            if (true) {
                if (tufts.Util.isMacLeopard()) {
                    // TODO: not sure why we need this -- may have to do with clipping
                    sRolloverBorder.paintBorder(c, g, 0, 2, w, h-2);
                } else {
                    sRolloverBorder.paintBorder(c, g, 0, 0, w, h);
                }
            } else {
                // experiment in separate border for drop-down portions
                sRolloverBorder.paintBorder(c, g, 0, 0, w-8, h);
                sRolloverBorder.paintBorder(c, g, w-7, 0, 7, h);
            }
            
            // this make it look like button-pressed:
            //g2.draw3DRect(0,0, w-1,h-1, false);
        }

        // now fill the icon, but don't fill if we're just holding the mouse down on a grouped toggle button
        // (so the group won't ever show two buttons in the fully selected state at once)
        if (isPressIcon && (!isRadioButton || mType != PRESSED)) {
            g2.setPaint(gradient);
            g2.fillRect(1,1, w-2,h-2);
        }
        else
            g2.setColor(mColor);
        //g2.fillRect(1,1, w-2,h-2);
        // skipping the fill here creates the flush-look
                
        // if we're down, nudge icon
        if (OffsetWhenDown && isPressIcon)
            g2.translate(1,1);

        // now draw the actual graphic in the center
        if (mRawIcon != null) {
            int ix = (w - mRawIcon.getIconWidth()) / 2;
            int iy = (h - mRawIcon.getIconHeight()) / 2;
            if (mType == DISABLED)
                g2.setComposite(DisabledAlpha);
            if (DEBUG.BOXES) {
                g2.setColor(Color.red);
                g2.fillRect(ix, iy, mRawIcon.getIconWidth(), mRawIcon.getIconHeight());
            }
            drawGraphic(c, g2, ix, iy);
        }
    }

    // can be overriden to do anything really fancy
    void drawGraphic(Component c, Graphics2D g, int x, int y)
    {
        mRawIcon.paintIcon(c, g, x, y);
    }

    public String toString()
    {
        return "VueButtonIcon[" + mType + " " + mRawIcon + "]";
    }
}
    
