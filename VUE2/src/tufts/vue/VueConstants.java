package tufts.vue;

import java.awt.Cursor;
import java.awt.Color;
import java.awt.Font;

public interface VueConstants
{
    // todo: move most of this stuff to prefs

    static String installDir = "C:\\anoop\\euclid\\VUEDevelopment\\src\\";
    
    
    static Font DefaultFont = new Font("SansSerif", Font.PLAIN, 18);
    static Font MediumFont = new Font("SansSerif", Font.PLAIN, 12);
    static Font SmallFont = new Font("SansSerif", Font.PLAIN, 10);
    static Font LinkLabelFont = new Font("SansSerif", Font.PLAIN, 10);

    static Font FONT_DEFAULT = new Font("SansSerif", Font.PLAIN, 18);
    static Font FONT_MEDIUM = new Font("SansSerif", Font.PLAIN, 12);
    static Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 10);
    static Font FONT_LINKLABEL = new Font("SansSerif", Font.PLAIN, 10);
    
    static java.awt.Color COLOR_LINK_LABEL = java.awt.Color.darkGray;
    static java.awt.Color COLOR_SELECTION = new java.awt.Color(74, 133, 255);
    //static java.awt.Color COLOR_SELECTION_HANDLE = new java.awt.Color(74, 255, 133);
    static java.awt.Color COLOR_SELECTION_HANDLE = Color.white;
    static java.awt.Color COLOR_SELECTION_DRAG = java.awt.Color.gray;
    static java.awt.Color COLOR_INDICATION = java.awt.Color.red;
    static java.awt.Color COLOR_DEFAULT = java.awt.Color.black;
    static java.awt.Color COLOR_BORDER = java.awt.Color.black;
    static java.awt.Color COLOR_FAINT = java.awt.Color.lightGray;
    static java.awt.Color COLOR_TRANSPARENT = null;

    static java.awt.Color COLOR_TEXT = java.awt.Color.black;
    static java.awt.Color COLOR_FILL = java.awt.Color.gray;
    static java.awt.Color COLOR_STROKE = java.awt.Color.black;

    static java.awt.Color COLOR_NODE_DEFAULT = new Color(200, 200, 255);
    static java.awt.Color COLOR_NODE_INVERTED = new Color(225, 225, 255);
    
    // todo: create our own cursors for most of these
    // named cursor types
    static Cursor CURSOR_HAND     = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    static Cursor CURSOR_MOVE     = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    static Cursor CURSOR_WAIT     = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    static Cursor CURSOR_CROSSHAIR= Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    static Cursor CURSOR_DEFAULT  = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    
    // tool cursor types
    static Cursor CURSOR_ZOOM_IN  = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
    static Cursor CURSOR_ZOOM_OUT = Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
    static Cursor CURSOR_PAN      = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    static Cursor CURSOR_ARROW    = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    static Cursor CURSOR_SUBSELECT= Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR); // white arrow

    static java.awt.BasicStroke STROKE_ZERO = new java.awt.BasicStroke(0f);
    static java.awt.BasicStroke STROKE_ONE = new java.awt.BasicStroke(1f);
    static java.awt.BasicStroke STROKE_TWO = new java.awt.BasicStroke(2f);
    static java.awt.BasicStroke STROKE_INDICATION = new java.awt.BasicStroke(3f);
    static java.awt.BasicStroke STROKE_DEFAULT = STROKE_ONE;
    static java.awt.BasicStroke STROKE_SELECTION = new java.awt.BasicStroke(1f);
    static java.awt.BasicStroke STROKE_SELECTION_DYNAMIC = new java.awt.BasicStroke(1f);

    static boolean DEBUG_CONTAINMENT = false;
    static boolean DEBUG_PARENTING = false;
    static boolean DEBUG_LAYOUT = false;
    static boolean DEBUG_EVENTS = false;
    
}
