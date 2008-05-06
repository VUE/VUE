/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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

package tufts.vue.gui;

import tufts.vue.DEBUG;
import tufts.vue.LWPropertyChangeEvent;
import tufts.vue.RecentlyUsedColorsManager;
import tufts.vue.RichTextBox;
import tufts.vue.VueResources;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JWindow;

import java.awt.Window;
import javax.swing.SwingUtilities;

import edu.tufts.vue.preferences.implementations.ColorPreference;

//import com.sun.codemodel.JLabel;

/**
 * ColorMenuButton
 *
 * This class provides a popup menu of items that supports named color values
 * with a corresponding color swatch.
 *
 * @version $Revision: 1.28 $ / $Date: 2008-05-06 15:42:41 $ / $Author: mike $
 * @author csb
 * @author Scott Fraize
 */

public class ColorMenuButton extends JButton
implements ActionListener, tufts.vue.LWEditor
{
    public static final String COLOR_POPUP_NAME = "ColorPopupMenu";
    
    /** The currently selected Color item--if any **/
    //protected Color mCurColor = new Color(0,0,0);
    protected Color mCurColor = null;
    private Icon mButtonIcon;
    protected Object mPropertyKey;
    protected Object mCurrentValue;
    private Window popupWindow;
    private final RecentlyUsedColorsManager colorManager = RecentlyUsedColorsManager.getInstance();
    /**
     *  Creates a new ColorMenuButton with the passed array of items
     * as it's palette menu.
     * 
     *  It will preselect the first item in the array as
     *  its default selection and use its images for its own view.
     *
     * @param pItems  an array of ColorMenuButtonItems for the menu.
     **/
    private JPanel colorArrayPanel = null;
    public ColorMenuButton(Color[] pColors, boolean pHasCustom) {
        // create default color swatch icon: override with setButtonIcon if want different
        setButtonIcon(new BlobIcon(16,16, true)); // can we live with no default? clean up init style...
        colorArrayPanel = buildMenu(pColors, pHasCustom);
        setBorder(null);
        addActionListener(this);
        // add components to main panel
        
 
        // use frame
        if (tufts.Util.isUnixPlatform())
	        popupWindow = new JWindow();
	    else
	    	popupWindow = new JFrame();
        if (tufts.Util.isWindowsPlatform())
        	popupWindow.setAlwaysOnTop(true);
        popupWindow.setName(COLOR_POPUP_NAME);
    	if (!tufts.Util.isUnixPlatform())
	        ((JFrame)popupWindow).setUndecorated(true);
    //    popupWindow.setAlwaysOnTop(true);

        Component c = null;
        if (!tufts.Util.isUnixPlatform())
        	c= ((JFrame)popupWindow).getGlassPane();
        else
        	c = ((JWindow)popupWindow).getGlassPane();
        c.setVisible(true);
        c.addMouseListener(new MouseAdapter()
        {                 
            
            public void mousePressed(final MouseEvent e)
            {
            	Component c = null;
                if (!tufts.Util.isUnixPlatform())
                	c= ((JFrame)popupWindow).getGlassPane();
                else
                	c = ((JWindow)popupWindow).getGlassPane();
                
            	java.awt.Container contentPane = null;
            	
                if (!tufts.Util.isUnixPlatform())
                	contentPane= ((JFrame)popupWindow).getContentPane();
                else
                	contentPane = ((JWindow)popupWindow).getContentPane();
//            	 get the mouse click point relative to the content pane
                Point containerPoint = SwingUtilities.convertPoint(popupWindow,
                    e.getPoint(),contentPane);

                // find the component that under this point
                Component component = SwingUtilities.getDeepestComponentAt(
                            contentPane,
                            containerPoint.x,
                            containerPoint.y);

                // return if nothing was found
                if (component == null) {             
                    return;
                }

                // convert point relative to the target component
                Point componentPoint = SwingUtilities.convertPoint(
                    popupWindow,
                    e.getPoint(),
                    component);

                // redispatch the event
                component.dispatchEvent(new MouseEvent(component,
                    e.getID(),
                    e.getWhen(),
                    e.getModifiers(),
                    componentPoint.x,
                    componentPoint.y,
                    e.getClickCount(),
                    e.isPopupTrigger()));
            }	
            
            public void mouseReleased(final MouseEvent e)
            {
            	Component c = null;
                if (!tufts.Util.isUnixPlatform())
                	c= ((JFrame)popupWindow).getGlassPane();
                else
                	c = ((JWindow)popupWindow).getGlassPane();
                
            	java.awt.Container contentPane = null;
            	
                if (!tufts.Util.isUnixPlatform())
                	contentPane= ((JFrame)popupWindow).getContentPane();
                else
                	contentPane = ((JWindow)popupWindow).getContentPane();
                
//            	 get the mouse click point relative to the content pane
                Point containerPoint = SwingUtilities.convertPoint(popupWindow,
                    e.getPoint(),contentPane);

                // find the component that under this point
                Component component = SwingUtilities.getDeepestComponentAt(
                            contentPane,
                            containerPoint.x,
                            containerPoint.y);

                // return if nothing was found
                if (component == null) {
             
                    return;
                }
             

                // convert point relative to the target component
                Point componentPoint = SwingUtilities.convertPoint(
                    popupWindow,
                    e.getPoint(),
                    component);

                // redispatch the event
                component.dispatchEvent(new MouseEvent(component,
                    e.getID(),
                    e.getWhen(),
                    e.getModifiers(),
                    componentPoint.x,
                    componentPoint.y,
                    e.getClickCount(),
                    e.isPopupTrigger()));
            }
            
        	
        	public void mouseExited(final MouseEvent e)
        	{        		
        		SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (popupWindow.isVisible())
                            doClick();
                    }
                });
        	}
        });
        popupWindow.addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent e) {

            }

            public void windowLostFocus(WindowEvent e) {
            	//System.out.println("Opposite component" + e.getOppositeWindow().getClass().toString());
            	
            	if (e.getOppositeWindow() != null && ((e.getOppositeWindow().getClass() == VueFrame.class)))
            	{            		
            		return;
            	}
            		
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        if (popupWindow.isVisible())
                            doClick();
                    }
                });
            }
        });

        // add some components to window
        java.awt.Container contentPane = null;
    	
        if (!tufts.Util.isUnixPlatform())
        	contentPane= ((JFrame)popupWindow).getContentPane();
        else
        	contentPane = ((JWindow)popupWindow).getContentPane();
        
        ((JComponent)contentPane).setLayout(new BorderLayout());
        ((JComponent)contentPane).setBorder(BorderFactory.createEtchedBorder());
        
        
        contentPane.add(colorArrayPanel);
        popupWindow.pack();
    }
    private JPanel colorPanel = new JPanel();
    
    public Window getPopupWindow()
    {
    	return popupWindow;
    }
    
    public void rebuildMenu()
    {
    	popupWindow.remove(colorArrayPanel);
    	colorArrayPanel = buildMenu(colorsToRebuild,customRebuild);
    	  // add some components to window
        
    	java.awt.Container contentPane = null;
    	
        if (!tufts.Util.isUnixPlatform())
        	contentPane= ((JFrame)popupWindow).getContentPane();
        else
        	contentPane = ((JWindow)popupWindow).getContentPane();
        
        contentPane.add(colorArrayPanel);
        popupWindow.pack();
    }
    private Color[] colorsToRebuild;
    private boolean customRebuild;
    public JPanel buildMenu(Color[] colors, boolean custom)
    {    	
    	// 4 x 15
    	JPanel parent = new JPanel();
    	parent.setLayout(new BorderLayout());
    	
    	colorsToRebuild =colors.clone();
    	customRebuild = custom;
    	colorPanel.removeAll();
    	colorPanel.setLayout(new GridLayout(0,4,2,2));
    	Color c = this.getColor();
    	for (int i = 0; i < colors.length ; i++)
    	{
    		JButton colorButton = new JButton();
    		colorButton.setFocusable(false);
    		colorButton.setBorderPainted(false);
    		colorButton.setContentAreaFilled(false);
    		 Icon icon = makeIcon(colors[i]);
    		colorButton.setIcon(icon);
    		colorButton.setPreferredSize(new Dimension(20,20));
    		colorButton.addActionListener(this);
    		Color blobColor = colors[i];
    		
    		if (c != null && blobColor != null && blobColor.getRed() == c.getRed() && blobColor.getBlue() == c.getBlue() && blobColor.getGreen() == c.getGreen() && blobColor.getAlpha() == c.getAlpha())
    		{
    			//System.out.println("MATCH");
    			colorButton.setBorderPainted(true);
    			
    			colorButton.setBorder(BorderFactory.createLineBorder(Color.gray));
    		}
    		
    		colorPanel.add(colorButton);
    	}
    	
    	ColorPreference customColor = null;
    	
    	
    	List colorList = colorManager.getRecentlyUsedColors();
    	
    	//for (int i=0; i < colorList.size(); i++};    	    	
    	   for (int p = 0 ; p< colorList.size(); p++)
           {		 
    		JButton colorButton = new JButton();
    		colorButton.setFocusable(false);
    		colorButton.setBorderPainted(false);
    		colorButton.setContentAreaFilled(false);
    		
    		//System.out.println(color);
    		StringTokenizer tokens = new StringTokenizer((String)colorList.get(p),",");
    		Color color = new Color(Integer.parseInt((String)tokens.nextElement()),Integer.parseInt((String)tokens.nextElement()),Integer.parseInt((String)tokens.nextElement()));
    		
    		Icon icon = makeIcon(color);
    		colorButton.setIcon(icon);
    		colorButton.setPreferredSize(new Dimension(20,20));
    		colorButton.addActionListener(this);
    		Color blobColor = color;
    		
    		if (c != null && blobColor != null && blobColor.getRed() == c.getRed() && blobColor.getBlue() == c.getBlue() && blobColor.getGreen() == c.getGreen() && blobColor.getAlpha() == c.getAlpha())
    		{
    			//System.out.println("MATCH");
    			colorButton.setBorderPainted(true);
    			
    			colorButton.setBorder(BorderFactory.createLineBorder(Color.gray));
    		}
    		colorPanel.add(colorButton);
           }
    	
    	JButton item = new JButton("other...");
    	item.setFocusable(false);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) 
            { 
            	Color c =(Color)runCustomChooser();
            	colorManager.updateRecentlyUsedColors(c.getRed()+","+c.getGreen()+","+c.getBlue());
            	handleValueSelection(c); 
            	doClick();
            	if (tufts.Util.isWindowsPlatform())
            		popupWindow.setAlwaysOnTop(true);
            	
            //	rebuildMenu();
            }
            
            });
    	item.setBorderPainted(false);
		item.setContentAreaFilled(false);		
		parent.add(colorPanel,BorderLayout.CENTER);
		parent.add(item,BorderLayout.SOUTH);
    	return parent;
    }
    
    public ColorMenuButton(Color[] pColors) {
        this(pColors, false);
    }
	  
	
    /**
     * Sets a the current color.
     */
    public void setColor(Color c) {    	
        if (DEBUG.TOOL) System.out.println(this + " setColor " + c);
        if (c == null && mPropertyKey == null)
        	return;
        if (c == null || c.getAlpha() == 0)
            setToolTipText("No Fill");
        else if (c.getAlpha() == 255)
            setToolTipText(String.format("RGB: %d,%d,%d", c.getRed(), c.getGreen(), c.getBlue()));
        else
            setToolTipText(String.format("RGB: %d,%d,%d @ %.0f%%", c.getRed(), c.getGreen(), c.getBlue(),
                                         100f * ((float)c.getAlpha()) / 255f));
        mCurColor = c;
        Icon i = getButtonIcon();
        if (i instanceof BlobIcon)
            ((BlobIcon)i).setColor(c);
    //    if (c == null)
    //        mPopup.setSelected(super.mEmptySelection);
        
        int count = colorPanel.getComponentCount();            
        for (int p = 0 ; p< count; p++)
        {
        	Component comp = colorPanel.getComponent(p);
        	if (comp instanceof JButton)
            {
            	JButton b = ((JButton)comp);
            	if (b.getIcon() instanceof BlobIcon)
            	{
            		BlobIcon blob = ((BlobIcon)b.getIcon());            	
            		//System.out.println(String.format("RGB: %d,%d,%d @ %.0f%%", c.getRed(), c.getGreen(), c.getBlue(),
                    //        100f * ((float)c.getAlpha()) / 255f));
            		//System.out.println(String.format("RGB BLOB: %d,%d,%d @ %.0f%%", blob.getColor().getRed(),  blob.getColor().getGreen(),  blob.getColor().getBlue(),
                    //        100f * ((float) blob.getColor().getAlpha()) / 255f));
            		Color blobColor = blob.getColor();
            		
            		if (c != null && blobColor != null && blobColor.getRed() == c.getRed() && blobColor.getBlue() == c.getBlue() && blobColor.getGreen() == c.getGreen() && blobColor.getAlpha() == c.getAlpha())
            		{
            			//System.out.println("MATCH");
            			b.setBorderPainted(true);
            			
            			b.setBorder(BorderFactory.createLineBorder(Color.gray));
            		}
            		else
            		{ //System.out.println(" NO MATCH");
            			
            			b.setBorderPainted(false);
            		}
            	}
            }
        }
        repaint();
    }
	 
    public void setButtonIcon(Icon i) {
        if (DEBUG.BOXES||DEBUG.TOOL) System.out.println(this + " setButtonIcon " + i);
        //if (DEBUG.Enabled) new Throwable().printStackTrace();
        _setIcon(mButtonIcon = i);
    }
    /** return the default button size for this type of button: subclasses can override */
    protected Dimension getButtonSize() {
        return new Dimension(32,22); // better at 22, but get clipped 1 pix at top in VueToolbarController! todo: BUG
    }
    
    private void _setIcon(Icon i) {
        /*
            super.setIcon(i);
            super.setRolloverIcon(new VueButtonIcon(i, VueButtonIcon.ROLLOVER));
        */
        /*
          final int pad = 7;
          Dimension d = new Dimension(i.getIconWidth()+pad, i.getIconHeight()+pad);
          if (d.width < 21) d.width = 21; // todo: config
          if (d.height < 21) d.height = 21; // todo: config
        */
        //if (DEBUG.BOXES||DEBUG.TOOL) System.out.println(this + " _setIcon " + i);
        Dimension d = getButtonSize();
        if (true || !GUI.isMacAqua()) {
            if (false)
                VueButtonIcon.installGenerated(this, i, d);
            else
                VueButtonIcon.installGenerated(this, new MenuProxyIcon(i), d);
            //System.out.println(this + " *** installed generated, setPreferredSize " + d);
        }
        setPreferredSize(d);
    }
    protected Icon getButtonIcon() {
        return mButtonIcon;
    }
    
    /**
     * Gets the current color
     */
    public Color getColor() {
        return mCurColor;
    }

    public void displayValue(Object o) {    	
    		setColor((Color)o);
    }
	 
    public Object produceValue() {
        return getColor();
    }
    /** Simulate a user value selection */
    public void selectValue(Color value) {
        handleValueSelection(value);
    }
    
    protected void handleValueSelection(Object newPropertyValue) {
        if (DEBUG.TOOL) System.out.println(this + " handleValueSelection: newPropertyValue=" + newPropertyValue);
        // TODO: this is getting fired twice, once for ItemEvent stateChange=DESELECTED, and
        // then the one we really want, with itemState=SELECTED.  We want to ignore the former,
        // as it's generating extra property sets on the selection that are immediately
        // overriden by the SELECTED value.  This should actually be harmless, but
        // it's definitely unexpected internal behaviour.  -- SMF 2007-05-01 14:55.37
        
        if (newPropertyValue == null) // could be result of custom chooser
            return;
        // even if we were build from actions, in which case the LWComponents
        // have already been changed via that action, call setPropertyValue
        // here so any listening LWCToolPanels can update their held state,
        // and so subclasses can update their displayed selected icons

        // Okay, do NOT call this with the action?  But what happens if nothing is selected?
        if (newPropertyValue instanceof Action) {
            System.out.println("Skipping setPropertyValue & firePropertyChanged for Action " + newPropertyValue);
        } else {
            Object oldValue = produceValue();
            displayValue(newPropertyValue);
          //  System.out.println(newPropertyValue.toString());
            firePropertyChanged(oldValue, newPropertyValue);
        }
        repaint();
    }
    
    /** fire a property change event even if old & new values are the same */
    // COULD USE Component.firePropertyChange!  all this is getting us is diagnostics!
    protected void firePropertyChanged(Object oldValue, Object newValue)
    {
        if (getPropertyKey() != null) {
            PropertyChangeListener[] listeners = getPropertyChangeListeners();
            if (listeners.length > 0) {
                PropertyChangeEvent event = new LWPropertyChangeEvent(this, getPropertyKey(), oldValue, newValue);
                for (int i = 0; i< listeners.length; i++) {
                    if (DEBUG.TOOL && (DEBUG.EVENTS || DEBUG.META)) System.out.println(this + " fires " + event + " to " + listeners[i]);
                    listeners[i].propertyChange(event);
                }
            }
        }
    }
    /** factory for superclass buildMenu */
    protected Icon makeIcon(Object value) {
        return new BlobIcon(16,16, (Color) value);
    }

    protected Object runCustomChooser() {
    	if (tufts.Util.isWindowsPlatform())
    		popupWindow.setAlwaysOnTop(false);
        Color c =  tufts.vue.VueUtil.runColorChooser("Select Custom Color", getColor(), this);
        
        return c;
        // todo: set up own listeners for color change in chooser
        // --that way way can actually tweak color on map as they play
        // with it in the chooser
    }

    ColorMenuButton() { this(sTestColors, true); }
    /*private static String[] sTestNames = { "Black",
                                           "White",
                                           "Red",
                                           "Green",
                                           "Blue" };*/ 
    private static Color[] sTestColors = { new Color(0,0,0),
                                           new Color(255,255,255),
                                           new Color(255,0,0),
                                           new Color(0,255,0),
                                           new Color(0,0,255)  };

	public void actionPerformed(ActionEvent arg0) {
	    // set popup window visibility
	
        if (!popupWindow.isVisible()) {
            // set location relative to button
            Point location = getLocation();
            SwingUtilities.convertPointToScreen(location, getParent());
            if (false) {
                // this code was causing the menu to drop too low on Mac OS X Leopard 
                // (so it would dissapear when mousing down from the color button to the pop-up menu).
                // SMF 2008-03-12 -- See VUE-821
                location.translate(0, getHeight()
                                   + (getBorder() == null ? 0
                                      : getBorder().getBorderInsets(this).bottom));
            } else {
                // this makes things work on Leopard, but does it break things on Windows or Linux?  SMF 2008-03-12
                location.translate(0, getHeight());
            }
            rebuildMenu();
        
            System.out.println(location);
            GUI.keepLocationOnScreen(location, new Dimension(popupWindow.getWidth(),popupWindow.getHeight()));
            System.out.println("G ? H : " + getWidth() + " " + getHeight());
            System.out.println(location);
            popupWindow.setLocation(location); 
            if (tufts.Util.isUnixPlatform())
            {
            	popupWindow.setAlwaysOnTop(true);
            	//popupWindow.set
            	//popupWindow.toFront();
            }
            else
            {
            	popupWindow.setAlwaysOnTop(false);
            }
            popupWindow.setVisible(true);                        
            popupWindow.requestFocus();
        } else {
            // hide it otherwise         
            popupWindow.setVisible(false);
            if (arg0.getSource() instanceof JButton)
            {
            	JButton b = ((JButton)(arg0).getSource());
            	if (b.getIcon() instanceof BlobIcon)
            	{
            		BlobIcon blob = ((BlobIcon)b.getIcon());            	
            		selectValue(blob.getColor());
            	}
            }
        }
        
		
	}


	 public void setPropertyKey(Object key) {
	        mPropertyKey = key;
	    }
	 public Object getPropertyKey() {
	        return mPropertyKey;
	    }
	
    
}

