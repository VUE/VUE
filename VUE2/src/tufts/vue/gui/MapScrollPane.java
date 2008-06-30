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

import tufts.vue.VUE;
import tufts.vue.MapViewer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.*;
import javax.swing.BorderFactory;

/**
 * Scroll pane for MapViewer / MapViewport with a focus indicator.
 *
 * @version $Revision: 1.9 $ / $Date: 2008-06-30 20:53:06 $ / $Author: mike $
 * @author Scott Fraize
 */

public class MapScrollPane extends javax.swing.JScrollPane
{
    public final static boolean UseMacFocusBorder = false;
    
    private FocusIndicator mFocusIndicator;

    private final MapViewer mViewer;
    
    public MapScrollPane(MapViewer viewer) {
        super(viewer);

        mViewer = viewer;

        setFocusable(false);
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_ALWAYS);
        setWheelScrollingEnabled(true);
        getVerticalScrollBar().setUnitIncrement(16);
        getHorizontalScrollBar().setUnitIncrement(16);

        mFocusIndicator = new FocusIndicator(viewer);

        setCorner(LOWER_RIGHT_CORNER, mFocusIndicator);

        if (UseMacFocusBorder) {
            // Leave default installed special mac focus border.
            // The Mac Aqua focus border looks fantastic, but we have to
            // repaint the whole map every time the focus changes to
            // another map, which is slow.
        } else if (GUI.isMacAqua()) {
            if (GUI.isMacBrushedMetal())
                // use same color as mac brushed metal inactive border
                setBorder(BorderFactory.createLineBorder(new Color(155,155,155), 1));
            else
                setBorder(null); // no border at all for now for default mac look
        }
        
        //addFocusListener(viewer);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        GUI.MouseWheelRelay.addListenerOrIntercept(mViewer.getMouseWheelListener(), this);
    }

    @Override
    protected javax.swing.JViewport createViewport() {
        return new tufts.vue.MapViewport();
    }

    public java.awt.Component getFocusIndicator() {
        return mFocusIndicator;
    }


    /** a little box for the lower right of a JScrollPane indicating this viewer's focus state */
    private static class FocusIndicator extends javax.swing.JComponent {
        final Color fill;
        final Color line;
        final static int inset = 4;

        final MapViewer mViewer;
        
        FocusIndicator(MapViewer viewer) {
            mViewer = viewer;
            
            if (GUI.isMacAqua()) {
                fill = GUI.AquaFocusBorderLight;
                line = GUI.AquaFocusBorderLight.darker();
                //line = AquaFocusBorderDark;
            } else {
                fill = GUI.getToolbarColor();
                line = fill.darker();
            }
        }
        
        public void paintComponent(Graphics g) {
            //if (VUE.multipleMapsVisible() || DEBUG.Enabled || DEBUG.FOCUS)
            paintIcon(g);
        }
        
        void paintIcon(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            
            // no effect on muddling with mac aqua JScrollPane focus border
            //((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
            
            // fill a block if we own the VUE application focus (Actions apply here)
            if (VUE.getActiveViewer() == mViewer) {
                g.setColor(fill);
                g.fillRect(inset, inset, w-inset*2, h-inset*2);
            }
            
            // Draw a box if we own the KEYBOARD focus, which will appear as a border to
            // the above block assuming we have VUE app focus.  Keyboard focus effects
            // special cases such as holding down the space-bar to trigger the hand/pan
            // tool, which is not an action, but a key detected right on the MapViewer.
            // Also, e.g., the viewer loses keyboard focus when there is an
            // activeTextEdit, while keeping VUE app focus.
            
            if (mViewer.isFocusOwner()) {
                g.setColor(line);
                w--; h--;
                g.drawRect(inset, inset, w-inset*2, h-inset*2);
            }
            //if (DEBUG.FOCUS) out("painted focusIndicator");
        }
    }
    
    
}
