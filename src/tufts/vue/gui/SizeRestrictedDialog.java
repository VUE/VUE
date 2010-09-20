/*
* Copyright 2003-2010 Tufts University  Licensed under the
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
