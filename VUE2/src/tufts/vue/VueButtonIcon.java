package tufts.vue;

import java.awt.Component;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.AlphaComposite;
import javax.swing.Icon;
import javax.swing.AbstractButton;
import javax.swing.border.EtchedBorder;

// TODO: replace ToolIcon with this or subclass of this.
public class VueButtonIcon implements Icon
{
    public static final int UP = 0;        // unselected/default
    public static final int PRESSED = 1;   // only while being held down by a mouse press
    public static final int SELECTED = 2;  // selected (after mouse click)
    public static final int DISABLED = 3;  // disabled
    public static final int ROLLOVER = 4;  // rollover
    public static final int MENU = 5;              // sub-menus: default (palette menu)
    public static final int MENU_SELECTED = 6;     // sub-menus: rollover (palette menu)
            
    public static void installGenerated(AbstractButton b, Icon raw) {
        b.setIcon(new VueButtonIcon(raw, UP));
        b.setPressedIcon(new VueButtonIcon(raw, PRESSED));
        b.setSelectedIcon(new VueButtonIcon(raw, SELECTED));
        b.setDisabledIcon(new VueButtonIcon(raw, DISABLED));
        b.setRolloverIcon(new VueButtonIcon(raw, ROLLOVER));
    }

    private static final Color sButtonColor = new Color(222,222,222);
    private static final Color sOverColor = Color.gray;
    private static final Color sDownColor = sOverColor;
    private static final EtchedBorder sEtchedBorder = new EtchedBorder();
    
    final int width = 22;
    final int height = 22;

    private Insets insets = new Insets(0,0,0,0);
    private int mType = UP;
    private Color mColor = sButtonColor;

    private final Icon mRawIcon;
    private final boolean isPressIcon;
    private boolean isRadioButton = false; // if in an exclusive button-group

    // OffsetWhenDown: nudge the icon when in the down state.
    // Set to true of "up" state appears as a button -- can
    // turn on otherwise but will need to adjust whole button so
    // icon stays centered.
    private final static boolean OffsetWhenDown = false;
    private final static boolean debug = false;

    protected VueButtonIcon(Icon rawIcon, int t)
    {
        mRawIcon = rawIcon;
        mType = t;
        isPressIcon = (t == PRESSED || t == SELECTED || t == MENU_SELECTED);
        //mPaintGradient = isPressIcon || t == ROLLOVER;
        //if (mPaintGradient)
        if (isPressIcon)
            mColor = Color.lightGray;
        //mColor = Color.gray;
        //mColor = ToolbarColor;
        if (debug) {
            if (t == MENU) mColor = Color.pink;
            if (t == MENU_SELECTED) mColor = Color.magenta;
            if (t == ROLLOVER) mColor = Color.green;
        }
        //if (t >= MENU) insets = new Insets(-1,-1,1,1);
    }
            
    public int getIconWidth() { return width; }
    public int getIconHeight() { return height; }

	
    /**
     * paint the entire button as an icon plus it's visible icon graphic
     */
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (debug) System.out.println("painting " + mRawIcon + " type = " + mType + " on " + c);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (mType >= MENU) {
            // the drop-down menus have GC bugs on the PC such
            // that we need to be sure to paint something in
            // the entire region, or we appear to get another
            // version of the icon painted *under* us.
            g2.setColor(c.getBackground());
            g2.fillRect(0,0, width,height);
        }

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
            sEtchedBorder.paintBorder(c, g, 0, 0, w, h);
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
        int ix = (w - mRawIcon.getIconWidth()) / 2;
        int iy = (h - mRawIcon.getIconHeight()) / 2;
        if (mType == DISABLED)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        if (DEBUG.BOXES) {
            g2.setColor(Color.red);
            g2.fillRect(ix, iy, mRawIcon.getIconWidth(), mRawIcon.getIconHeight());
        }
        drawGraphic(c, g2, ix, iy);
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
    
