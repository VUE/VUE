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

package tufts.vue;

import tufts.Util;
import tufts.vue.gui.GUI;
import tufts.macosx.MacOSX;

import java.awt.*;
import java.util.*;
import javax.swing.*;

/**
 * Display the VUE splash screen.  Show the VUE splash graphic with current version
 * text drawn on top of it,
 *
 * @version $Revision: 1.19 $ / $Date: 2009-09-21 21:31:20 $ / $Author: sfraize $ 
 * @author  akumar03
 */

public class SplashScreen extends Frame
{
	private final Color charcoal = new Color(36,36,36);
    public SplashScreen() {
        setName(tufts.vue.gui.GUI.OVERRIDE_REDIRECT); // ignore textured background if we can
        setUndecorated(true);
        setFocusableWindowState(false);
        createSplash();

        if (GUI.isMacAqua()) {
            if (Util.isMacCocoaSupported()) {
                pack(); // ensure peer created for MacOSX
                MacOSX.setTransparent(SplashScreen.this);
            } else { //if (tufts.Util.getJavaVersion() >= 1.6) {
                // this doesn't work: it sets the alpha of the window AND the contents
                // we only want the window to be invisible (non-opaque in the NSWindow world,
                // as opposed to having an alpha)  Apple hasn't added a special property
                // for that.
                //getRootPane().putClientProperty("Window.alpha", Float.valueOf(0));
                setBackground(Color.white);
            }
        } else {
            // This will give it a transparent "look"
            // if no other window's are open on the
            // users desktop.
          //  setBackground(SystemColor.desktop);
        	setBackground(charcoal);
        }
        
        setVisible(true);

        if (DEBUG.INIT) System.out.println("SplashScreen: visible");
    }
    
    private void createSplash() {
        Dimension screen =  Toolkit.getDefaultToolkit().getScreenSize();
        
        String splashProp = null;
        if (Util.isMacCocoaSupported())
            splashProp = "splashScreen";
        else
            splashProp = "splashScreenNoTransparency";
        ImageIcon icon = new ImageIcon(VueResources.getURL(splashProp)) {
          public void paintIcon(Component c, Graphics g, int x, int y) {
              Calendar calendar = new GregorianCalendar();
              super.paintIcon(c,g,x,y);
              g.setColor(charcoal);
              g.setFont(new Font("Verdana", Font.PLAIN, 11));
              g.drawString(VueResources.getString("splashscreen.title"),172,165);
              g.drawString(VueResources.getString("splashscreen.developedby"),202,185);
              g.drawString((char)169+" "+VueResources.getString("vue.build.date")+" Tufts University", 240,205);
              g.drawString("Version "+VueResources.getString("vue.version"),95,215);
              
          } 
        };
        final int width = icon.getIconWidth();
        final int height = icon.getIconHeight();
        int x = (screen.width - width)/2;
        int y = (screen.height - height)/2;
        this.setBounds(x, y, width, height);
        JLabel logoLabel = new JLabel(icon);

        logoLabel.setOpaque(false);

        add(logoLabel);
    }

    public static void main(String args[]) {
        VUE.init(args);
        new SplashScreen();
    }
    
}
