/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.io.File;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FileChooserUI;
import javax.swing.plaf.basic.BasicFileChooserUI;

import edu.tufts.vue.preferences.PreferencesManager;

public class VueFileChooser extends JFileChooser{

	public VueFileChooser()
	{
		super();
		checkForChooser();
		//an elaborate fix to avoid scrolling in the combobox drop downs in the filechooser
		iterateContainerAndFixComboBoxes((Container)this);
	
		
		 
	}
	public void iterateContainerAndFixComboBoxes(Container c)
	{
		for (int i=0;i < c.getComponentCount(); i++)
		{
			if (c.getComponent(i) instanceof Container)
			{
				iterateContainerAndFixComboBoxes((Container)c.getComponent(i));
			}
			else
			{
				System.out.println(c.getComponent(i));
			}
			if (c instanceof JComboBox)
			{
				((JComboBox)c).setMaximumRowCount(12);
			}
				
		}
	}
	public VueFileChooser(File f)
	{
		super(f);
		checkForChooser();
	}
	
	private void checkForChooser()
	{
		boolean enhancedChooserEnabled = PreferencesManager.getBooleanPrefValue(edu.tufts.vue.preferences.implementations.EnhancedFileChooserPreference.getInstance());
		if (!enhancedChooserEnabled)
			putClientProperty("FileChooser.useShellFolder", Boolean.FALSE);
	}
	/**
	 * Hack to get around Java Bug
	 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4678049
	 * See : 4956530, 5095197,4678049
	 */
	public void setFileFilter(FileFilter filter) {

		super.setFileFilter(filter);
	              
		

		final BasicFileChooserUI ui = (BasicFileChooserUI) this.getUI();

		final String name = ui.getFileName().trim();

		if ((name == null) || (name.length() == 0)) {
			return;
		}
		
		EventQueue.invokeLater(new Thread() {
			public void run() {
				String currentName = ui.getFileName();
				if ((currentName == null) || (currentName.length() == 0)) {
					ui.setFileName(name);
				}
			}
		});
	}

}
