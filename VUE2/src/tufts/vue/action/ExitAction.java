/*
 * exitAction.java
 *
 * Created on April 30, 2003, 3:04 PM
 */

package euclid.VUEDevelopment.src.tufts.vue.action;

/**
 *
 * @author  akumar03
 */
import javax.swing.*;
import java.awt.event.*;


public class ExitAction extends AbstractAction {
    
    /** Creates a new instance of exitAction */
    public ExitAction() {
    }
    
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        System.exit(0);
    }
    
}
