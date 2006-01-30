package tufts.vue.gui;

import tufts.vue.Actions;
import tufts.vue.action.*;
import javax.swing.Box;
import javax.swing.JButton;

/**
 * Toolbar for general VUE actions.
 *
 * @version $Revision: 1.1 $ / $Date: 2006-01-30 05:57:21 $ / $Author: sfraize $ 
 */
public class VueActionBar extends javax.swing.Box
{
    public VueActionBar()
    {
	super(javax.swing.BoxLayout.X_AXIS);
        add(Actions.NewMap);
        add(new OpenAction());
        add(new SaveAction());
        add(new PrintAction()); // deal with print singleton issue / getactioncommand is null here
        //addSeparator(); // not doing much
        add(Actions.Undo);
        add(Actions.Redo);
        //add(Actions.Group);
        //add(Actions.Ungroup);
        add(Actions.ZoomIn);
        add(Actions.ZoomOut);
        add(Actions.ZoomFit);
        add(Actions.Delete);
        add(Box.createHorizontalStrut(3));
    }

    public JButton add(javax.swing.Action a) {
        JButton b = makeButton(a);
        super.add(b);
        return b;
    }

    private static JButton makeButton(javax.swing.Action a) {
        VueButton b = new VueButton(a);
        b.setAsToolbarButton(true);
        return b;
    }
}

