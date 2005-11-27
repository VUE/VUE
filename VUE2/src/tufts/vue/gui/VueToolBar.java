package tufts.vue.gui;

import tufts.vue.VUE;
import tufts.vue.Actions;

import java.awt.Insets;
import javax.swing.Action;
import javax.swing.JButton;

import tufts.vue.action.*;

/**
 * Main VUE application toolbar.
 *
 * @version $Revision: 1.1 $ / $Date: 2005-11-27 16:15:35 $ / $Author: sfraize $ 
 */
public class VueToolBar extends javax.swing.JToolBar
// public static class VueToolBar extends com.jidesoft.action.CommandBar // JIDE ENABLE
{
    public VueToolBar()
    {
        super("VUE Toolbar");
        add(Actions.NewMap);
        add(new OpenAction());
        add(new SaveAction());
        add(new PrintAction()); // deal with print singleton issue / getactioncommand is null here
        //addSeparator(); // not doing much
        add(Actions.Undo);
        add(Actions.Redo);
        add(Actions.Group);
        add(Actions.Ungroup);
        add(Actions.ZoomIn);
        add(Actions.ZoomOut);
        add(Actions.ZoomFit);
        add(Actions.Delete);

        //setRollover(true);
        setMargin(new Insets(0,0,0,0));

        /* JIDE ENABLE
           if (JIDE_TEST) {
           // DockableBar calls
           setFloatable(true);
           setRearrangable(true);
           setAllowedDockSides(15);
           //setStretch(true);

           //final JButton b = new JButton("r");
           final JButton b = add(new VueAction("rootWindow", null, ":general/New") {
           public boolean enabled() { return true; }
           });
                
           b.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
           //System.out.println(e);
           Component c = SwingUtilities.getRoot(b);
           System.out.println("root="+c + "\nparent=" + c.getClass().getSuperclass());
           }
           });
           }
        */
    }

    public JButton add(Action a) {
        JButton b = makeButton(a);
        if (false&&VUE.JIDE_TEST) {
            // toolbars can go only go vertical in JIDE if we do this...
            b = (JButton) super.add(a);
            System.out.println("added " + b.getClass() + " " + b);
        } else {
            b = makeButton(a);
            super.add(b);
        }
        return b;
    }

    private static JButton makeButton(Action a) {
        VueButton b = new VueButton(a);
        b.setAsToolbarButton(true);
        return b;
    }
}

