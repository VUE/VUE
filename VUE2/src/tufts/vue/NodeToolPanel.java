package tufts.vue;

import tufts.vue.beans.*;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.geom.RectangularShape;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.Icon;
import javax.swing.BorderFactory;
import javax.swing.border.*;

/**
 * NodeToolPanel
 * This creates an editor panel for LWNode's
 */
 
public class NodeToolPanel extends LWCToolPanel
{
     public NodeToolPanel() {
         JLabel label = new JLabel("   Node:");
         label.setFont(VueConstants.FONT_SMALL);
         getBox().add(new ShapeMenuButton(), 0);
         getBox().add(label, 0);
     }
         
    public static boolean isPreferredType(Object o) {
        return o instanceof LWNode;
    }
    
    public static void main(String[] args) {
        System.out.println("NodeToolPanel:main");
        VUE.initUI(true);
        LWCToolPanel.debug = true;
        VueUtil.displayComponent(new NodeToolPanel());
    }


    private static class ShapeMenuButton extends MenuButton
    {
        //private static BlobIcon  sIcon = new BlobIcon( 16,16, new Color(1,1,244) );
        private BlobIcon  sIcon = new BlobIcon(20,16, true);

        /** The currently selected shape */
        protected RectangularShape mShape;
			
        /** the BlobIcon for the swatch **/
        private BlobIcon mBlobIcon = null;

        public ShapeMenuButton() {
            //setPropertyName(LWKey.Shape);
            //setBorder(new CompoundBorder(getBorder(), new EmptyBorder(1,1,2,1)));
            //setBorder(new MatteBorder(3,3,4,3, Color.red));
            setBorder(new EmptyBorder(3,3,3,3));
            buildMenu(NodeTool.getTool().getShapeSetterActions());
            setIcon(((AbstractButton)super.mPopup.getComponents()[0]).getIcon());
        }


        protected void handleMenuSelection(ActionEvent e) {
            setIcon(((AbstractButton)e.getSource()).getIcon());
        }
        
        public void setShape(RectangularShape shape) {
            mShape = shape;
            /*
            if (mBlobIcon != null)
                mBlobIcon.setColor(pColor);
            if (pColor == null)
                mPopup.setSelected(super.mEmptySelection);
            */
        }
	 
        public RectangularShape getShape() {
            return mShape;
        }

        public void setPropertyValue(Object o) {
            System.out.println(this + "setProp " + o);
            //setShape((RectangularShape)o);
        }
	 
        public Object getPropertyValue() {
            return getShape();
        }
	 
        public void X_setIcon( Icon pIcon) {
            super.setIcon(pIcon);
            if (pIcon instanceof BlobIcon)
                mBlobIcon = (BlobIcon) pIcon;
            else
                mBlobIcon = null;
        }
	
    }
}
