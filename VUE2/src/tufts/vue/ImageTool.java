 /*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue;

import java.lang.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class ImageTool extends VueTool
    implements ChangeListener
{
    public ImageTool() {
        super();
    }

    public JPanel createToolPanel() {
        JPanel p = new JPanel();
        //JSlider slider = new JSlider(0,359);
        JSlider slider = new JSlider(-180,180);
        //slider.setLabelTable(slider.createStandardLabels(45));
        slider.setMajorTickSpacing(45);
        slider.setMinorTickSpacing(15);
        //slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        slider.setSnapToTicks(true);
        slider.setBackground(VueTheme.getToolbarColor());
        slider.addChangeListener(this);
        p.add(slider);
        p.add(new JLabel("IMAGE TOOL"));
        return p;
    }

    public void stateChanged(ChangeEvent e) {
        JSlider slider = (JSlider) e.getSource();
        //if (!slider.getValueIsAdjusting())
        double radians = Math.toRadians(slider.getValue());
        if (VUE.getSelection().first() instanceof LWImage)
            ((LWImage)VUE.getSelection().first()).setRotation(radians);
    }
    
    public boolean supportsDraggedSelector(java.awt.event.MouseEvent e) { return true; }
    public boolean supportsSelection() { return true; }
    public boolean hasDecorations() { return true; }
    
}