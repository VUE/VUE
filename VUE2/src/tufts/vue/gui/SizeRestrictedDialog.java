package tufts.vue.gui;

import java.awt.Frame;
import java.awt.event.*;
import javax.swing.*;

import tufts.vue.VUE;

public class SizeRestrictedDialog extends JDialog 
{
	private static final long serialVersionUID = -1569884045611370290L;
	private int minWidth;
	private int minHeight;

	public SizeRestrictedDialog(Frame owner, String title, boolean modal)
	{	
		super(owner,title,modal);		
	}
	
	public void setMinSizeRestriction(int minW, int minH)
	{
		minWidth = minW;
		minHeight = minH;

		addComponentListener( new ComponentAdapter()
				{
				  public void componentResized(ComponentEvent e)
				  {
					  int width = getWidth();
					  int height = getHeight();

					  if (width < minWidth) width = minWidth;					  

					  if (height < minHeight) height = minHeight;
				
					  setSize(width, height);
				  }
				});
	
	}
}
