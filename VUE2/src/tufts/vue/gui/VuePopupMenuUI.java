package tufts.vue.gui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicPopupMenuUI;
	
public class VuePopupMenuUI extends BasicPopupMenuUI 
{
	
	public void installUI(JComponent c) {
		super.installUI(c);
		
	}
	
	public Popup getPopup(JPopupMenu popup, int x, int y) {
        VuePopupFactory popupFactory = VuePopupFactory.getInstance();

        return popupFactory.getPopup(popup.getInvoker(), popup, x, y);
    }
}