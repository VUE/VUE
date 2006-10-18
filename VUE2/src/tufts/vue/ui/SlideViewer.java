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

package tufts.vue.ui;

import tufts.Util;
import tufts.vue.LWComponent;
import tufts.vue.LWMap;
import tufts.vue.LWSelection;
import tufts.vue.MapViewer;
import tufts.vue.DrawContext;
import tufts.vue.VueTool;
import tufts.vue.VueToolbarController;
import tufts.vue.VueConstants;
import tufts.vue.LWCEvent;
import tufts.vue.DEBUG;

import java.awt.Color;

public class SlideViewer extends tufts.vue.MapViewer
{
    private static SlideViewer singleton;

    private final VueTool PresentationTool = VueToolbarController.getController().getTool("viewTool");

    private boolean mBlackout = false;
    private LWComponent mFocused;
    
    public SlideViewer(LWMap map) {
        super(map == null ? new LWMap("empty") : map, "SlideViewer");
        singleton = this;
        DEBUG_TIMER_ROLLOVER = false;
    }

    public void LWCChanged(LWCEvent e) {
        super.LWCChanged(e);
        //if (e.getComponent() == mFocused)
            zoomToContents();
    }

    public static void setFocused(LWComponent c) {
        singleton.loadMap(c.getMap());
        singleton.mFocused = c;
        singleton.reshapeImpl(0,0,0,0);
    }

    public void keyPressed(java.awt.event.KeyEvent e) {
        super.keyPressed(e);
        if (e.isConsumed())
            return;
        char c = e.getKeyChar();
        if (c == 'b') {
            mBlackout = !mBlackout;
            repaint();
        }
    }

    protected void drawSelection(DrawContext dc, LWSelection s) {
        // Don't draw selection if its the focused component
        if (s.size() == 1 && s.first() == mFocused)
            return;
        super.drawSelection(dc, s);
    }

    protected void drawMap(DrawContext dc) {
        if (mBlackout) {
            dc.g.setColor(Color.black);
            dc.setBlackWhiteReversed(true);
            dc.setPresenting(true);
            dc.setInteractive(false);
        } else
            dc.g.setColor(mMap.getFillColor());
        dc.g.fill(dc.g.getClipBounds());
        if (mFocused == null)
            mMap.draw(dc);
        else
            mFocused.draw(dc);

        dc.setRawDrawing();
        //dc.g.setFont(VueConstants.FONT_MEDIUM);
        dc.g.setFont(new java.awt.Font("Apple Chancery", java.awt.Font.PLAIN, 14));
        //dc.g.setColor(Color.blue);
        dc.g.setColor(new Color(89,130,203));

        int x = -getX() + 2;
        int y = -getY() + 15;
        dc.g.drawString("Header", x, y);
        dc.g.drawString("Footer", x, -getY() + getHeight() - 3);
        
    }

    protected void zoomToContents() {
        LWComponent focus = (mFocused == null ? mMap : mFocused);
            //if (mFocused != null)
        tufts.vue.ZoomTool.setZoomFitRegion(this,
                                            focus.getBounds(),
                                            DEBUG.MARGINS ? 0 : 20,
                                            false);

        //tufts.vue.ZoomTool.setZoomFit(this);
    }
    
    protected void reshapeImpl(int x, int y, int w, int h) {
        zoomToContents();
    }
}
