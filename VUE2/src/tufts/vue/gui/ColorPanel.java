package tufts.vue.gui;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Color;

public class ColorPanel extends JPanel{
	
	public ColorPanel()
	{
		super();
	}
	
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Color c = g.getColor();
		g.setColor(Color.gray);
		g.drawLine(4, 263, this.getWidth()-4,263);
		g.setColor(c);
	}
}
