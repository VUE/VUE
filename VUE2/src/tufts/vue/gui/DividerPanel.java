/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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
