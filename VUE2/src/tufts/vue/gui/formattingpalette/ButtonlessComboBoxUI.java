package tufts.vue.gui.formattingpalette;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicComboBoxUI;

public class ButtonlessComboBoxUI extends BasicComboBoxUI{
	
	public ButtonlessComboBoxUI()
	{
		super();
		
	}		
	
	protected JButton createArrowButton()
	{
		JButton b = new JButton();
		b.setMinimumSize(new Dimension(0,0));
		b.setMaximumSize(new Dimension(0,0));
		b.setOpaque(false);
		b.setBorder(BorderFactory.createEmptyBorder());
		b.setPreferredSize(new Dimension(0,0));
		b.setVisible(false);
		b.setContentAreaFilled(false);
		b.setSize(new Dimension(0,0));
		return b ;
	}
		
	protected void installComponents()
	{
		if ( comboBox.isEditable() )		
			addEditor();
	
		comboBox.add( currentValuePane );
		Component[] c = comboBox.getComponents();		
	
	}
	
	  protected Rectangle rectangleForCurrentValue()
	  {
	    int w = comboBox.getWidth();
	    int h = comboBox.getHeight();
	    Insets i = comboBox.getInsets();
	
	    return new Rectangle(i.left, i.top, w -10,
	                         h - (i.top + i.left));
	  }
}
