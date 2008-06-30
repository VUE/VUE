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
