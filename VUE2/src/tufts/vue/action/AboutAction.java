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
public class AboutAction extends AbstractAction
{
    private static Window AboutWindow;
    
    public AboutAction() {
        super("About VUE");
    }
    
    public void actionPerformed(ActionEvent actionEvent)
    {
        if (AboutWindow == null)
            AboutWindow = createAboutWindow();
        VueUtil.centerOnScreen(AboutWindow);
        AboutWindow.setVisible(true);
    }

    private static Window createAboutWindow()
    {
        JDialog window = new JDialog(VUE.getRootFrame(), "About VUE", true);
        //JFrame window = new JFrame("VUE: About");
        
        JPanel backPanel = new JPanel();
        //backPanel.setBorder(new LineBorder(Color.WHITE,20));
        backPanel.setBorder(new EmptyBorder(20,20,20,20));
        backPanel.setMinimumSize(new Dimension(275,147));
        
        JPanel aboutusPanel = new JPanel();
        JLabel spLabel = new JLabel(VueResources.getImageIcon("aboutVue"));
        
        String debugInfo = "";
        if (tufts.vue.DEBUG.Enabled)
            debugInfo = "<br>&nbsp;&nbsp;&nbsp;"
                + Version.BuildTime + "/"
                + Version.BuildUser + "/"
                + Version.BuildPlatform;
        
        JLabel jtf = new JLabel("<html><font color = \"#20316A\"> <br><br>"
                                + "&nbsp;&nbsp;&nbsp;Developed by Tufts Academic Technology<br>"
                                + "&nbsp;&nbsp;&nbsp;Copyright &copy; 2004 Tufts University<br>"
                                + "&nbsp;&nbsp;&nbsp;Copyright &copy; 2004 MIT University<br>"
                                + "&nbsp;&nbsp;&nbsp;All Rights Reserved<br><br>"
                                + "&nbsp;&nbsp;&nbsp;Version 0.9+ <br>"
                                + "&nbsp;&nbsp;&nbsp;Built " + Version.BuildDay
                                + debugInfo
                                + "<br><br>"
                                + "&nbsp;&nbsp;&nbsp;<u>http://vue.tccs.tufts.edu</u><br>"
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
