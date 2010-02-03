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
