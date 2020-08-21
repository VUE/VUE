 /*
* Copyright 2003-2010 Tufts University  Licensed under the
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

import java.awt.Cursor;
import java.awt.Color;
import java.awt.Font;
import java.awt.font.FontRenderContext;

/**
 * Various constants for GUI variables.
 *
 * Is an interface so can be "implemented" as virtual java 1.5 static import.
 *
 * @version $Revision: 1.65 $ / $Date: 2010-02-03 19:17:40 $ / $Author: mike $ 
 */

// todo: rename GUI constants & move to GUI
// todo: move most of this stuff to prefs
public interface VueConstants
{
    FontRenderContext DefaultFontContext = new FontRenderContext(null, true, false);
   
    //static Font DefaultFont = new Font("SansSerif", Font.PLAIN, 18);
    Font FixedFont = new Font("Courier", Font.BOLD, 12);
    Font FixedSmallFont = new Font("Courier", Font.BOLD, 10);
    Font MediumFont = new Font("SansSerif", Font.PLAIN, 12);
    Font SmallFont = new Font("SansSerif", Font.PLAIN, 10);
    Font LargeFont = new Font("SansSerif", Font.PLAIN, 14);
    //static Font LinkLabelFont = new Font("SansSerif", Font.PLAIN, 10);

    //static Font SmallFixedFont = new Font("Lucida Sans Typewriter", Font.PLAIN, 10); // will not show special character glyphs
    //static Font SmallFixedFont = new Font("Lucida Console", Font.PLAIN, 12);
    //: new Font("Lucida Sans Typewriter", Font.PLAIN, 10); 

    Font SmallFixedFont = VueUtil.isMacPlatform()
        ? new Font("Monaco", Font.PLAIN, 11) // a fixed-width mac font that shows propertly shows special mac character glyphs (needed for Leopard)
        : new Font("Lucida Console", Font.PLAIN, 10); // will not show special character glyphs on mac, nor will be anti-aliased

    Font SmallFixedBoldFont = VueUtil.isMacPlatform()
        ? new Font("Monaco", Font.BOLD, 11)
        : new Font("Lucida Console", Font.BOLD, 10);
        
        Font JSliderSmallFixedFont = VueUtil.isMacPlatform()
        ? new Font("Lucida Console", Font.PLAIN, 10) // a fixed-width mac font that shows propertly shows special mac character glyphs (needed for Leopard)
        : new Font("SansSerif", Font.PLAIN, 11); // will not show special character glyphs on mac, nor will be anti-aliased


    // Note that "Lucida Console" on Mac OS X gets mapped to "Monaco" in any case
    
    Font LargeFixedFont = VueUtil.isMacPlatform()
        ? new Font("Monaco", Font.PLAIN, 18)
        : new Font("Lucida Console", Font.PLAIN, 18);
    
    Font LargeFixedFontBold = VueUtil.isMacPlatform()
        ? new Font("Monaco", Font.BOLD, 18)
        : new Font("Lucida Console", Font.BOLD, 18);
    
    // static Font LargeFixedFont = new Font("Lucida Sans Typewriter", Font.PLAIN, 12);

    Font FONT_DEFAULT = new Font("SansSerif", Font.PLAIN, 14);
    Font FONT_MEDIUM = new Font("SansSerif", Font.PLAIN, 12);
    Font FONT_MEDIUM_UNICODE = new Font("Arial Unicode MS", Font.PLAIN, 12);
    // TODO: above font only avail on Win2K/WinXP if Office installed (I think),
    // if this is NOT available, "Lucida Sans Unicode" is a better bet.
    // Wait: actually, this font works just as well as Lucida Sans Unicode,
    // it's just that the latter is a bolder font.  Tho this font
    // does NOT appear in the font list on default WinXP systems, it
    // appears to get mapped to something with decent unicode support,
    // except for asian fonts (chinese/japanese/korean).
    Font FONT_MEDIUM_BOLD = new Font("SansSerif", Font.BOLD,11);
    Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 10);
    Font FONT_SMALL_BOLD = new Font("SansSerif", Font.BOLD, 10);
    Font FONT_TINY = new Font("SansSerif", Font.PLAIN, 8);
    Font FONT_ICONIC = new Font("Arial Black", Font.PLAIN, 8);
    Font FONT_NARROW = new Font("Arial Narrow", Font.PLAIN, 11);

    //static Font FONT_LINKLABEL = new Font("Verdana", Font.PLAIN, 11);
    //static Font FONT_LINKLABEL = new Font("SansSerif", Font.PLAIN, 10);
    
    //static java.awt.Color COLOR_SELECTION = new java.awt.Color(74, 133, 255);
    java.awt.Color COLOR_SELECTION = VueResources.getColor("mapViewer.selection.color");
    java.awt.Color COLOR_SELECTION_CONTROL = COLOR_SELECTION.brighter(); // note: current value overflows blue, so looks very green
    //    static java.awt.Color COLOR_SELECTION_CONTROL = new java.awt.Color(74, 255, 133);
    java.awt.Color COLOR_SELECTION_NOTICE = new java.awt.Color(255, 74, 74);
    java.awt.Color COLOR_SELECTION_HANDLE = Color.white;
    java.awt.Color COLOR_SELECTION_DRAG = java.awt.Color.gray;
    java.awt.Color COLOR_INDICATION = java.awt.Color.green;
    java.awt.Color COLOR_INDICATION_ALTERNATE = java.awt.Color.yellow;
    java.awt.Color COLOR_ACTIVE_VIEWER = new java.awt.Color(74, 255, 133);
    java.awt.Color COLOR_ACTIVE_MODEL = COLOR_ACTIVE_VIEWER;
    java.awt.Color COLOR_DEFAULT = java.awt.Color.black;
    java.awt.Color COLOR_BORDER = java.awt.Color.black;
    java.awt.Color COLOR_FAINT = java.awt.Color.lightGray;
    java.awt.Color COLOR_TRANSPARENT = null;

    java.awt.Color COLOR_TEXT = java.awt.Color.black;
    java.awt.Color COLOR_FILL = java.awt.Color.gray;
    java.awt.Color COLOR_STROKE = java.awt.Color.darkGray;

    java.awt.Color COLOR_NODE_DEFAULT = new Color(200, 200, 255);
    java.awt.Color COLOR_NODE_INVERTED = new Color(225, 225, 255);

    java.awt.Color COLOR_HIGHLIGHT = VueResources.getColor("mapViewer.highlight.color");
    java.awt.Color COLOR_TOOLTIP = new Color(255,255,192);
    java.awt.Color COLOR_TOOLTIP_TRANSPARENT = new Color(255,255,192, 192);

    java.awt.Color ContrastWhite = new java.awt.Color(255,255,255,128);
    java.awt.Color ContrastBlack = new java.awt.Color(0,0,0,128);
    java.awt.Color ContrastGray = new java.awt.Color(128,128,128,128);
    java.awt.Color ContrastRed = new java.awt.Color(255,0,0,128);
    java.awt.Color ContrastGreen = new java.awt.Color(0,255,0,128);
    java.awt.Color ContrastBlue = new java.awt.Color(0,0,255,128);
    
    // todo: create our own cursors for most of these
    // named cursor types
    //static Cursor CURSOR_HAND     = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    //static Cursor CURSOR_MOVE     = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    Cursor CURSOR_WAIT     = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
    //static Cursor CURSOR_TEXT     = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
    //static Cursor CURSOR_CROSSHAIR= Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    Cursor CURSOR_DEFAULT  = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    
    // tool cursor types
    //static Cursor CURSOR_ZOOM_IN saveMap.  = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
    //static Cursor CURSOR_ZOOM_OUT = Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
    //static Cursor CURSOR_PAN      = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    //static Cursor CURSOR_ARROW    = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    //static Cursor CURSOR_SUBSELECT= Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR); // white arrow

    java.awt.BasicStroke STROKE_ZERO = new java.awt.BasicStroke(0f);
    java.awt.BasicStroke STROKE_SIXTEENTH = new java.awt.BasicStroke(0.0625f);
    java.awt.BasicStroke STROKE_EIGHTH = new java.awt.BasicStroke(0.125f);
    java.awt.BasicStroke STROKE_FOURTH = new java.awt.BasicStroke(0.25f);
    java.awt.BasicStroke STROKE_HALF = new java.awt.BasicStroke(0.5f);
    java.awt.BasicStroke STROKE_ONE = new java.awt.BasicStroke(1f);
    java.awt.BasicStroke STROKE_TWO = new java.awt.BasicStroke(2f);
    java.awt.BasicStroke STROKE_THREE = new java.awt.BasicStroke(3f);
    java.awt.BasicStroke STROKE_FIVE = new java.awt.BasicStroke(5f);
    java.awt.BasicStroke STROKE_SEVEN = new java.awt.BasicStroke(7f);
    java.awt.BasicStroke STROKE_INDICATION = new java.awt.BasicStroke(3f);
    java.awt.BasicStroke STROKE_DEFAULT = STROKE_ONE;
    java.awt.BasicStroke STROKE_SELECTION = new java.awt.BasicStroke(1f);
    java.awt.BasicStroke STROKE_SELECTION_DYNAMIC = new java.awt.BasicStroke(1f);

    int SelectionStrokeWidth = 8;
}
