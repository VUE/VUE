package tufts.vue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.geom.RectangularShape;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;

import tufts.vue.IBISNodeTool.IBISSubTool.IBISNodeIcon;
import tufts.vue.NodeToolPanel.LinkMenuButton;
import tufts.vue.NodeToolPanel.ShapeMenuButton;
import tufts.vue.NodeToolPanel.ShapeMenuButton.ShapeComboRenderer;
import tufts.vue.gui.GUI;
import tufts.vue.gui.VueComboMenu;
import tufts.vue.ibisicon.*;
import tufts.vue.ibisimage.*;

public class IBISNodeToolPanel extends ToolPanel {
	
    private final IBISMenuButton mIBISButton;

	public IBISNodeToolPanel() {
        this.mIBISButton = new IBISMenuButton();
	}
	
	public void buildBox()
    {
        GridBagConstraints gbc = new GridBagConstraints();
     	    
        gbc.gridx = 0;
 		gbc.gridy = 0;    		
 		gbc.gridwidth = 1;
 		gbc.gridheight=1;
 		gbc.insets= new Insets(0,3,0,0);
 		gbc.fill = GridBagConstraints.VERTICAL; // the label never grows
 		gbc.anchor = GridBagConstraints.EAST;
 		
 		JLabel imageLabel = new JLabel(VueResources.getString("IBISnodetoolpanel.image"));
                imageLabel.setLabelFor(mIBISButton);
 		imageLabel.setForeground(new Color(51,51,51));
 		imageLabel.setFont(tufts.vue.VueConstants.SmallFont);
 		getBox().add(imageLabel,gbc);
         
     	gbc.gridx = 1;
 		gbc.gridy = 0;    				
 		gbc.fill = GridBagConstraints.NONE; // the label never grows
 		gbc.insets = new Insets(1,1,1,5);
 		gbc.anchor = GridBagConstraints.WEST;
         getBox().add(mIBISButton, gbc);
         
    }
    public boolean isPreferredType(Object o) {
        return o instanceof LWIBISNode;
    }
	
    static class IBISMenuButton extends VueComboMenu<Class<? extends IBISImage>>
	{
	    public IBISMenuButton() {
	    	// HO 08/11/2010 BEGIN ***************
	    	// HO 11/11/2010 BEGIN ***************
	        // super(LWKey.IBISSymbol, IBISNodeTool.getTool().getAllIconClasses());
	    	super(LWKey.IBISSymbol, IBISNodeTool.getTool().getAllImageClasses());
	        // HO 11/11/2010 END ***************
	    	// super(LWKey.IBISSymbol, IBISNodeTool.getTool().getAllIconValues());
	        // HO 08/11/2010 END ***************
	        setToolTipText(VueResources.getString("IBISnodetoolpanel.nodeimage.tooltip"));
	        setRenderer(new IBISComboRenderer());
	        // setRenderer(new IBISIconRenderer());
	        this.setMaximumRowCount(10);
	    }
	
	    // @Override
	    // HO 18/11/2010 BEGIN **********************
	    protected Icon makeIcon(Class<? extends IBISImage> imageClass) {
	    //protected ImageIcon makeIcon(Class<? extends IBISImage> imageClass) {
	    	// HO 18/11/2010 END **********************
	        try {
	            // HO 16/11/2010 BEGIN ****************
	        	//return new GUI.ProxyEnabledIcon(new IBISNodeTool.IBISSubTool.IBISNodeIcon(imageClass.newInstance().getIcon().getClass().newInstance()).getIcon(), IBISMenuButton.this);
	        	return new IBISNodeTool.IBISSubTool.IBISNodeIcon(imageClass.newInstance().getIcon().getClass().newInstance());
	        	// HO 16/11/2010 END ****************
	        } catch (Throwable t) {
	            tufts.Util.printStackTrace(t);
	        }
	        return null;
	    }
	
	    class IBISComboRenderer extends javax.swing.DefaultListCellRenderer {
	    	
	        public IBISComboRenderer() {
	            setHorizontalAlignment(CENTER);
	            setVerticalAlignment(CENTER);
	            setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
	        }
	        
	        @Override
	        public Component getListCellRendererComponent(
	                                                      JList list,
	                                                      Object value,
	                                                      int index,
	                                                      boolean isSelected,
	                                                      boolean cellHasFocus)
	        {
	        	
	        	Color bg = GUI.getTextHighlightColor();
	        	if (isSelected) {
	        		// HO 15/11/2010 BEGIN *************
	        		
	        		// HO 15/11/2010 END *************
	                setBackground(bg);
	                setForeground(list.getSelectionForeground());
	            } else {
	                setBackground(Color.white);
	                setForeground(Color.green);
	            }
	            
	            //setEnabled(IBISMenuButton.this.isEnabled());
	            // the combo box will NOT repaint our icon when it becomes disabled!
	            // Tho this works fine for the image-icons in the LinkMenuButton below -- ??
	            
	            setOpaque(true);
	            // okay first get back the Icon object
	        	Icon icon = getIconForValue(value);
	            // now make sure we really have the icon
				System.out.println(icon.toString());

				// now get the icon we want from it
				ImageIcon imgIcon = null;
				if (icon.getClass().equals(IBISNodeIcon.class)) {
					IBISNodeIcon ndIcon = (IBISNodeIcon)icon;
					imgIcon = ndIcon.getIcon();
					// now make sure we really have the ImageIcon
					System.out.println(imgIcon.toString());
					// now make sure the ImageIcon really has an image
					Image tehImg = imgIcon.getImage();
					System.out.println(tehImg.toString()); 
				}

	        	// now set the icon to be the ImageIcon
	            //setIcon(icon);
	            setIcon(imgIcon);      

	            if (DEBUG.TOOL && !isEnabled())
	                System.out.println("RENDERER SET DISABLED ICON: " + icon + " for value " + value);
	            if (DEBUG.TOOL && DEBUG.META) setText(value.toString());
	
	            return this;
	        }
	    } 
	    
	    /**
	     * HO 18/11/2010 BEGIN 
	     * What it says on the tin - a routine to turn an Icon into an Image
	     * @param icon, the Icon to be turned into an Image
	     * @return an Image made from the Icon param
	     */
	    static Image iconToImage(Icon icon) {
	    	if (icon instanceof ImageIcon) {
	    		return ((ImageIcon)icon).getImage();
	    	} else {
	    		int w = icon.getIconWidth();
	    		int h = icon.getIconHeight();
	    		GraphicsEnvironment ge = 
	    			GraphicsEnvironment.getLocalGraphicsEnvironment();
	    		GraphicsDevice gd = ge.getDefaultScreenDevice();
	    		GraphicsConfiguration gc = gd.getDefaultConfiguration();
	    		BufferedImage image = gc.createCompatibleImage(w, h);
	    		Graphics2D g = image.createGraphics();
	    		icon.paintIcon(null, g, 0, 0);
	    		g.dispose();
	    		return image;
	    	}
	    }
	
	}
    

	

}
