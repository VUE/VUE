/*
* Copyright 2003-201 Tufts University  Licensed under the
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

import java.awt.Window;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
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
        JLabel spLabel = new JLabel(VueResources.getImageIcon("aboutVue"));
        
        String debugInfo = "";
        if (tufts.vue.DEBUG.Enabled)
            debugInfo = "<br>&nbsp;&nbsp;&nbsp;"
                + Version.Time + "/"
                + Version.User + "/"
                + Version.Platform;
        
        JLabel jtf = new JLabel("<html><font color = \"#20316A\"> <br><br>"
                                + "&nbsp;&nbsp;&nbsp;Developed by Tufts University<br> " 
                                + "&nbsp;&nbsp;&nbsp;University Information Technology<br>" 
                                + "&nbsp;&nbsp;&nbsp;Copyright &copy; 2003-2012 Tufts University<br>"
                                + "&nbsp;&nbsp;&nbsp;All Rights Reserved<br><br>"
                                + "&nbsp;&nbsp;&nbsp;Version "+VueResources.getString("vue.version")+" <br>"
                                + "&nbsp;&nbsp;&nbsp;Built " + Version.Date + " at " + Version.Time
                                + debugInfo
                                + "<br><br>"
                                + "&nbsp;&nbsp;&nbsp;<u>http://vue.tufts.edu</u><br>"
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
