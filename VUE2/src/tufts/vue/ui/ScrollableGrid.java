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
package tufts.vue.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Scrollable;

import tufts.vue.DEBUG;
import tufts.vue.VUE;
import tufts.vue.gui.GUI;

/**
 * A JPanel that implements Scrollable, and can temporarily have it's painting turned off,
 * which is useful for preventing repaints during batch updates and improving visual performance.
 */
//public class ScrollableGrid extends tufts.Util.JPanelAA implements Scrollable
public class ScrollableGrid extends JPanel implements Scrollable
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(ScrollableGrid.class);
    
    private JComponent delegate;
    private int vertScrollUnit;

    // hack till scroll-pane updating can be made saner:
    // if value > 0, don't paint
    private int paintDisabled = 0;
    
    
    ScrollableGrid(JComponent delegate, int vertScrollUnit) {
        super(new GridBagLayout());
        //setBackground(Color.red);
        this.delegate = delegate;
        this.vertScrollUnit = vertScrollUnit;
    }

    synchronized void setPaintDisabled(boolean disabled) {
        if (disabled)
            paintDisabled++;
        else
            paintDisabled--;
            
    }

    public void validate() {
        if (DEBUG.SCROLL) Log.debug("ScrollableGrid; validate");
        super.validate();
    }
    public void doLayout() {
        if (DEBUG.SCROLL) Log.debug("ScrollableGrid: doLayout");
        super.doLayout();
    }
    public void paint(Graphics g) {
        if (paintDisabled > 0)
            return;
        if (DEBUG.SCROLL) Log.debug("ScrollableGrid: paint " + g.getClip());
        super.paint(g);
    }
//     public void paintComponent(Graphics g) {
//         VUE.Log.debug("ScrollableGrid: paintComponent");
//         super.paintComponent(g);
//     }
    public Dimension getPreferredScrollableViewportSize() {
        Dimension d = delegate.getSize();
        d.height = getHeight();
        if (DEBUG.SCROLL) Log.debug(GUI.name(delegate) + " viewportSize " + GUI.name(d));
        return d;
    }

    /** clicking on the up/down arrows of the scroll bar use this */
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return vertScrollUnit;
        // TODO: can make this the full height of the component given the direction vertically
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return vertScrollUnit*2; // what triggers this?
    }

    public boolean getScrollableTracksViewportWidth() { return true; }
    public boolean getScrollableTracksViewportHeight() { return false; }
    
}
