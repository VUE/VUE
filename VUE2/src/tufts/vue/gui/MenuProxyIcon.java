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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Icon;

import tufts.vue.DEBUG;

/**
 * Wraps an icon in a container icon that also draws a drop-down arrow at right of original
 * icon.  For indicating there's a drop-down menu with the MenuButton.
 */
public class MenuProxyIcon implements Icon {
    private static final int arrowWidth = 5; // make sure is odd #
    private static final int arrowGap = 3;
    private Icon src;
    
    public MenuProxyIcon(Icon src) {
        this.src = src;
    }

    public int getIconWidth() { return src.getIconWidth(); };
    public int getIconHeight() { return src.getIconHeight(); }
    
    public void paintIcon(Component c, Graphics g, int sx, int sy) {

        int w = src.getIconWidth();
        int h = src.getIconHeight();
        if (DEBUG.BOXES) System.out.println("proxyPaint x=" + sx + " y=" + sy + " src=" + src);
        if (c.isEnabled())
            g.setColor(Color.darkGray);
        else
            g.setColor(Color.lightGray);
       // int x = sx + w + arrowGap;
        //int y = sy + h / 2 - 1;  // src icon relative
       // int y = getHeight() / 2 - 1; // parent button relative: keeps arrows aligned across butons buttons of same height
//        for (int len = arrowWidth; len > 0; len -= 2) {
//            g.drawLine(x,y,x+len,y);
//            y++;
//            x++;
//        }
        if (!c.isEnabled()) {
            //c.setBackground(Color.white);
        //	System.out.println("PAINT ICON DISABLED");
            ((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        }
        src.paintIcon(c, g, sx, sy);
    }
    public String toString() {
        return "MenuProxyIcon[" + src + "]";
    }
}