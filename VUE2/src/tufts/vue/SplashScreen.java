/*
 * SpashScreen.java
 *
 * Created on February 28, 2004, 6:28 PM
 */

package tufts.vue;

/**
 *
 * @author  akumar03
 */

import java.awt.*;
import javax.swing.*;

public class SplashScreen extends JWindow {
    
    /** Creates a new instance of SpashScreen */
    public final int width = 430;
    public final int height = 300;
    public final long sleepTime = 5000;
    public SplashScreen() {
        super();
        createSplash();
    }
    
    private void createSplash() {
        Dimension screen =  Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screen.width - width)/2;
        int y = (screen.height - height)/2;
        this.setBounds(x, y, width, height);
        JLabel logoLabel = new JLabel(VueResources.getImageIcon("splashScreen"));
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(logoLabel,BorderLayout.CENTER);
        this.setVisible(true);
        try {
          //  Thread.sleep(sleepTime);
        } catch(Exception ex) {}
        //this.setVisible(false);
    }
    
}
