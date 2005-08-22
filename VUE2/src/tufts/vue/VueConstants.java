 /*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */


package tufts.vue;

import java.awt.Cursor;
import java.awt.Color;
import java.awt.Font;

public interface VueConstants
{
    // todo: move most of this stuff to prefs

   
    //static Font DefaultFont = new Font("SansSerif", Font.PLAIN, 18);
    static Font FixedFont = new Font("Courier", Font.BOLD, 12);
    static Font MediumFont = new Font("SansSerif", Font.PLAIN, 12);
    static Font SmallFont = new Font("SansSerif", Font.PLAIN, 10);
    //static Font LinkLabelFont = new Font("SansSerif", Font.PLAIN, 10);

    static Font FONT_DEFAULT = new Font("SansSerif", Font.PLAIN, 14);
    static Font FONT_MEDIUM = new Font("SansSerif", Font.PLAIN, 12);
    static Font FONT_MEDIUM_UNICODE = new Font("Arial Unicode MS", Font.PLAIN, 12);
    static Font FONT_MEDIUM_BOLD = new Font("SansSerif", Font.BOLD,11);
    static Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 10);
    static Font FONT_SMALL_BOLD = new Font("SansSerif", Font.BOLD, 10);
    static Font FONT_TINY = new Font("SansSerif", Font.PLAIN, 8);
    static Font FONT_ICONIC = new Font("Arial Black", Font.PLAIN, 8);
    //static Font FONT_LINKLABEL = new Font("Verdana", Font.PLAIN, 11);
    //static Font FONT_LINKLABEL = new Font("SansSerif", Font.PLAIN, 10);
    
    //static java.awt.Color COLOR_SELECTION = new java.awt.Color(74, 133, 255);
    static java.awt.Color COLOR_SELECTION = VueResources.getColor("mapViewer.selection.color");
    static java.awt.Color COLOR_SELECTION_CONTROL = new java.awt.Color(74, 255, 133);
    static java.awt.Color COLOR_SELECTION_NOTICE = new java.awt.Color(255, 74, 74);
    static java.awt.Color COLOR_SELECTION_HANDLE = Color.white;
    static java.awt.Color COLOR_SELECTION_DRAG = java.awt.Color.gray;
    static java.awt.Color COLOR_INDICATION = java.awt.Color.red;
    static java.awt.Color COLOR_ACTIVE_VIEWER = new java.awt.Color(74, 255, 133);
    static java.awt.Color COLOR_ACTIVE_MODEL = COLOR_ACTIVE_VIEWER;
    static java.awt.Color COLOR_DEFAULT = java.awt.Color.black;
    static java.awt.Color COLOR_BORDER = java.awt.Color.black;
    static java.awt.Color COLOR_FAINT = java.awt.Color.lightGray;
    static java.awt.Color COLOR_TRANSPARENT = null;

    static java.awt.Color COLOR_TEXT = java.awt.Color.black;
    static java.awt.Color COLOR_FILL = java.awt.Color.gray;
    static java.awt.Color COLOR_STROKE = java.awt.Color.black;

    static java.awt.Color COLOR_NODE_DEFAULT = new Color(200, 200, 255);
    static java.awt.Color COLOR_NODE_INVERTED = new Color(225, 225, 255);

    static java.awt.Color COLOR_HIGHLIGHT = new Color(255,255,0, 128);
    static java.awt.Color COLOR_TOOLTIP = new Color(255,255,192);
    static java.awt.Color COLOR_TOOLTIP_TRANSPARENT = new Color(255,255,192, 192);
    
    // todo: create our own cursors for most of these
    // named cursor types
    //static Cursor CURSOR_HAND     = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    //static Cursor CURSOR_MOVE     = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    static Cursor CURSOR_WAIT     = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    //static Cursor CURSOR_TEXT     = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
    //static Cursor CURSOR_CROSSHAIR= Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    static Cursor CURSOR_DEFAULT  = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    
    // tool cursor types
    //static Cursor CURSOR_ZOOM_IN saveMap.  = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
    //static Cursor CURSOR_ZOOM_OUT = Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
    //static Cursor CURSOR_PAN      = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    //static Cursor CURSOR_ARROW    = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    //static Cursor CURSOR_SUBSELECT= Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR); // white arrow

    static java.awt.BasicStroke STROKE_ZERO = new java.awt.BasicStroke(0f);
    static java.awt.BasicStroke STROKE_SIXTEENTH = new java.awt.BasicStroke(0.0625f);
    static java.awt.BasicStroke STROKE_EIGHTH = new java.awt.BasicStroke(0.125f);
    static java.awt.BasicStroke STROKE_FOURTH = new java.awt.BasicStroke(0.25f);
    static java.awt.BasicStroke STROKE_HALF = new java.awt.BasicStroke(0.5f);
    static java.awt.BasicStroke STROKE_ONE = new java.awt.BasicStroke(1f);
    static java.awt.BasicStroke STROKE_TWO = new java.awt.BasicStroke(2f);
    static java.awt.BasicStroke STROKE_INDICATION = new java.awt.BasicStroke(3f);
    static java.awt.BasicStroke STROKE_DEFAULT = STROKE_ONE;
    static java.awt.BasicStroke STROKE_SELECTION = new java.awt.BasicStroke(1f);
    static java.awt.BasicStroke STROKE_SELECTION_DYNAMIC = new java.awt.BasicStroke(1f);

    static int SelectionStrokeWidth = 8;
}
