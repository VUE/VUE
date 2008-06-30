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

import tufts.vue.gui.GUI;

import java.awt.Dimension;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Tool for working with LWImage's
 *
 * @version $Revision: 1.16 $ / $Date: 2007/04/26 18:09:04 $ / $Author: mike $
 *
 */
public class ImageTool extends VueTool
{
    public ImageTool() {
        super();
    }
    
    public boolean supportsDraggedSelector(java.awt.event.MouseEvent e) { return true; }
    public boolean supportsSelection() { return true; }
    public boolean hasDecorations() { return true; }
    public Class getSelectionType() { return LWImage.class; }

    public JPanel createToolPanel() {
        return
            new LWCToolPanel() {
                protected void buildBox() {
                //    addComponent(mStrokeColorButton);
                //    addComponent(mStrokeButton);
                    JSlider slider = new RotationSlider();
                    //addComponent(slider);
                    slider.addPropertyChangeListener(this) ;
                    ////slider.getModel().addPropertyChangeListener(this); 
                    ////addComponent(new RotationSlider());
                    ////addComponent(ImageTool.mSlider);
                }
            };
    }

    private static class RotationSlider extends JSlider
        implements /*LWPropertyProducer,*/ ChangeListener

    {
        private final static int SnapIncrement = 5;
        
        public RotationSlider() {
            super(0,360,0);
            //setLabelTable(slider.createStandardLabels(45));
            setMajorTickSpacing(45);
            //setMinorTickSpacing(15);
            setMinorTickSpacing(SnapIncrement);
            //setPaintLabels(true);
            //setPaintTicks(true);
            setSnapToTicks(true);
            setBackground(GUI.getToolbarColor());
            // listen to the model or we only *sometimes* get
            // multiple final values while snap is being
            // sorted out
            getModel().addChangeListener(this);
            //Dimension d = getPreferredSize();
            //setMaximumSize(new Dimension(d.width,12));
            setFocusable(true);
        }

        public void XsetValue(int n) {
            super.setValue(n);
            System.out.println("SETVALUE " + n);
        }
        

        // we can't make this a property producer until it can communicate
        // to LWCToolPanel about rapidly changing values, or have it
        // handle them itself (we're changing the property value directly
        // here, whereas LWCToolPanel expects to be doing that).  Not
        // being a producer means for now the slider won't take on the rotation
        // value of the selected item.  
        public Object getPropertyKey() { return LWImage.KEY_Rotation; }
        public Object getPropertyValue() { return new Double(Math.toRadians(getValue())); }
        public void setPropertyValue(Object value) {
            setValue((int) (Math.toDegrees(((Double)value).doubleValue()) + 0.5));
        }

        private boolean isSnappedValue(int value) {
            return value % SnapIncrement == 0;
        }

        public void stateChanged(ChangeEvent e) {
            if (DEBUG.TOOL) System.out.println("RotationSlider: stateChanged: " + e.getSource().getClass()
                               + " value is " + getValue()
                               + " ok2snap " + isSnappedValue(getValue()));

            boolean isFinalValue = false;
            if (getValueIsAdjusting() == false) {
                // if snap-to-ticks is on, and we're not at a snapped value,
                // we'll get TWO change events claiming no values adjusting,
                // so ignore the first.
                if (!getSnapToTicks() || isSnappedValue(getValue()))
                    isFinalValue = true;
            }

            // todo: snap the value here so image only rotates to snapped values
            
            if (VUE.getSelection().first() instanceof LWImage) {
                double radians = Math.toRadians(getValue());
                LWImage image = (LWImage) VUE.getSelection().first();
                //if (!isFinalValue) image.getChangeSupport().setEventsSuspended();
                try {
                    image.setRotation(radians);
                } finally {
                    if (false && !isFinalValue) {
                        image.getChangeSupport().setEventsResumed();
                        image.notify(LWKey.RepaintComponent);
                    }
                }
            }

            if (isFinalValue) {
                if (DEBUG.TOOL) System.out.println("DONE: marking");
                //new Throwable("MARKING").printStackTrace();
                VUE.getUndoManager().mark();
            }
            
            setToolTipText(getValue() + " degrees");
        }
        
    }

}