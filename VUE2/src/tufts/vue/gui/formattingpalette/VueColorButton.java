package tufts.vue.gui.formattingpalette;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.border.*;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

public class VueColorButton extends JPanel{
	
	public VueColorButton(Dimension size)
	{
		setPreferredSize(size);
		setBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.LOWERED));
				
	}
	
	public void setColor(Color c)
	{
		this.setBackground(c);
		repaint();
	}
}
