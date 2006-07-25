package tufts.vue.gui;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JToolTip;

public class VueLabel extends JLabel 
{
	public VueLabel(ImageIcon imageIconResource) {
		super(imageIconResource);
	}
	
	public VueLabel()
	{
		super();
	}
	
	public JToolTip createToolTip()
	{
		JMultiLineToolTip tip = new JMultiLineToolTip();
		tip.setColumns(20);
		return  tip;
	}
}
