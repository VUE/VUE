/*
* Copyright 2003-2011 Tufts University  Licensed under the
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
package tufts.vue.action;

import tufts.vue.VUE;
import tufts.vue.VueUtil;
import tufts.vue.VueResources;
import tufts.vue.Version;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.border.*;
    

/**
 * Display an About Box for VUE.
 */
/*
 * From what i've learned today the version # in the about dialog gets inserted by
 * the shell script on releases.atech.tufts.edu not from the vueresources.properties
 * like you may think if you were reading the code.
 */
public class AboutAction extends tufts.vue.VueAction
{
    private static Window AboutWindow;
    
    public AboutAction() {
        super(VueResources.getString("aboutaction.about") + VUE.getName());
    }
    
    public void act()
    {
        if (AboutWindow == null)
            AboutWindow = createAboutWindow();
        VueUtil.centerOnScreen(AboutWindow);
        AboutWindow.setVisible(true);
    }

    private static Window createAboutWindow()
    {
        final JDialog window = new JDialog(VUE.getApplicationFrame(), "About " + VUE.getName(), true);
        
        JPanel backPanel = new JPanel();
        //backPanel.setBorder(new LineBorder(Color.WHITE,20));
        backPanel.setBorder(new EmptyBorder(20,20,20,20));
        backPanel.setMinimumSize(new Dimension(275,147));
        
        JPanel aboutusPanel = new JPanel();
        // HO 31/10/2011 BEGIN *******
        final Color charcoal = new Color(36,36,36);
        ImageIcon icon = new ImageIcon(VueResources.getURL("aboutVue")) {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                super.paintIcon(c,g,x,y);
                g.setColor(charcoal);
                // HO 28/10/2011 BEGIN ***********
                g.setFont(new Font("Verdana", Font.PLAIN, 20));
                g.drawString("design", 132, 45); 
            }
        };
        // JLabel spLabel = new JLabel(VueResources.getImageIcon("aboutVue"));
        JLabel spLabel = new JLabel(icon);
        // HO 31/10/2011 END ******************
                
        String debugInfo = "";
        if (tufts.vue.DEBUG.Enabled)
            debugInfo = "<br>&nbsp;&nbsp;&nbsp;"
                + Version.Time + "/"
                + Version.User + "/"
                + Version.Platform;
        
        JLabel jtf = new JLabel("<html><font color = \"#20316A\"> <br><br>"
        						// HO 31/10/2011 BEGIN ***********
                				+ "&nbsp;&nbsp;&nbsp;designVUE beta version 1.04<br>"
                				+ "&nbsp;&nbsp;&nbsp;Developed by Design Engineering Group<br>"
                				+ "&nbsp;&nbsp;&nbsp;Imperial College London<br>"
                				+ "&nbsp;&nbsp;&nbsp;Copyright &copy; 2010-2012 Imperial College London<br>"
                				+ "&nbsp;&nbsp;&nbsp;All Rights Reserved<br><br>"
                				+ "&nbsp;&nbsp;&nbsp;VISUAL UNDERSTANDING ENVIRONMENT<br>"
                				+ "&nbsp;&nbsp;&nbsp;Developed by Tufts University<br>"
                				+ "&nbsp;&nbsp;&nbsp;Copyright &copy; 2003-2012 Tufts University<br>"
                				+ "&nbsp;&nbsp;&nbsp;All Rights Reserved<br><br>"
                				// HO 31/10/2011 END ***********
                                + "</font></html>");
  
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BorderLayout());
        labelPanel.add(jtf,BorderLayout.CENTER);
          
        aboutusPanel.setLayout(new BorderLayout());
        //spLabel.setBorder(new LineBorder(Color.white, 5));
        //spLabel.setBackground(Color.white);
        aboutusPanel.add(spLabel,BorderLayout.CENTER);
         
        jtf.addMouseListener(new javax.swing.event.MouseInputAdapter() {
                public void mouseClicked(MouseEvent evt) {
                    try {
                        VueUtil.openURL("http://vue.tccs.tufts.edu");
                        window.setVisible(false);
                        window.dispose();
                    } catch (Exception ex) {}
                }
            });
       
        //labelPanel.setBackground(Color.WHITE);
        aboutusPanel.add(labelPanel,BorderLayout.SOUTH);
        
        backPanel.setLayout(new BorderLayout());
        backPanel.add(aboutusPanel,BorderLayout.CENTER);
        window.setContentPane(backPanel);
        window.pack();
        return window;
    }

    public static void main(String[] args) {
        new AboutAction().actionPerformed(null);
    }
}
