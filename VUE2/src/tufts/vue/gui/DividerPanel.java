package tufts.vue.gui;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;

public class DividerPanel extends JPanel {

	private int height = 20;
	
	public DividerPanel(int height)
	{
		this.height = height;
		setBackground(Color.gray);
		setOpaque(true);
		
	}
	public int getHeight()
	{
		return height;
	}
	
	public int getWidth()
	{
		return 1;
	}
	public Dimension getSize()
	{
		return new Dimension(1,height);
	}
	
	public Dimension getMaximumSize()
	{
		return new Dimension(1,height);
	}
	
	public Dimension getMinimizeSize()
	{
		return new Dimension(1,height);
	}
	
	public Dimension getPreferredSize()
	{
		return new Dimension(1,height);
	}
}
