package tufts.vue.gui;

import java.awt.Component;
import java.awt.Point;
import java.awt.Window;

import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import javax.swing.MenuElement;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;

public class VuePopupFactory extends PopupFactory {
    private static final int offset = 5;
    private PopupFactory wrapped;
    private static VuePopupFactory fact;
    
    public VuePopupFactory (PopupFactory wrapped) {
        this.wrapped = wrapped;
    }

    public Popup getPopup(Component owner, Component contents, int x, int y) throws IllegalArgumentException {
        if (contents == null) {
            throw new IllegalArgumentException();
        }
      
        return VueHeavyweightPopup.getInstance (owner, contents, new Point(x,y));
    }
    
    public static VuePopupFactory getInstance()
    {
    	if (fact == null)
    		fact = new VuePopupFactory(PopupFactory.getSharedInstance());
    	
    	return fact;
    }
   
}