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
/*
 * one day we should come back here and add drop shadows to the menus like they appear
 * on the mac, that'd be a nice touch.
 */
public class VuePopupFactory extends PopupFactory {
    private static final int offset = 5;

    private static VuePopupFactory fact;
    
    public Popup getPopup(Component owner, Component contents, int x, int y) throws IllegalArgumentException {
        if (contents == null) {
            throw new IllegalArgumentException();
        }
        return VueHeavyweightPopup.getInstance (owner, contents, new Point(x,y));
    }
    
    public static VuePopupFactory getInstance()
    {
    	if (fact == null)
    		fact = new VuePopupFactory();
    	
    	return fact;
    }
   
}