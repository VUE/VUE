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

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.io.File;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FileChooserUI;
import javax.swing.plaf.basic.BasicFileChooserUI;

import tufts.Util;
import tufts.vue.VueUtil;

import edu.tufts.vue.preferences.PreferencesManager;

public class VueFileChooser extends JFileChooser{

	public VueFileChooser()
	{
		super();
	
		checkForChooser();
		
		
		//an elaborate fix to avoid scrolling in the combobox drop downs in the filechooser
		
		iterateContainerAndFixComboBoxes((Container)this);
	
		
		 
	}
	public VueFileChooser(File f)
	{
		super(f);
		checkForChooser();
		
	}
	
	public static VueFileChooser getVueFileChooser()
	{
		VueFileChooser chooser = null;
		if (!Util.isMacPlatform())
    	{
    	 // TODO
    	 // This is really what I want to do for the John Bullard issue, although,
    	 // I'm not quite sure this will accomplish what I want, need to come 
    	 // up with a test case to see if this will really go.
    	 // MK
		 // try{
    		chooser = new VueFileChooser();
		 // } catch(Throwable t)
		 // {
		 //	  chooser.setUI(new javax.swing.plaf.metal.MetalFileChooserUI(chooser));
		 // }
    		if (VueUtil.isCurrentDirectoryPathSet()) 
    			chooser.setCurrentDirectory(new File(VueUtil.getCurrentDirectoryPath()));
    	}
    	else
    	{
    		
    		if (VueUtil.isCurrentDirectoryPathSet()) 
    		{
    			/*
    			 * Despite Quaqua fixes in 3.9 you can still only set the 
    			 * current directory if you set it in the constructor, 
    			 * setCurrentDirectory fails to do anything but cause the
    			 * top bar and the panels to be out of sync.... -MK 10/29
    			 */
    			chooser = new VueFileChooser(new File(VueUtil.getCurrentDirectoryPath()));
    		}
    		else
    			chooser = new VueFileChooser();

    	}
		return chooser;
	}
	
	protected JDialog createDialog(Component parent)
	{
		JDialog log = super.createDialog(parent);
		if (tufts.Util.isUnixPlatform())
		{	
			log.setAlwaysOnTop(true);
		
			log.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			log.addWindowListener(new WindowListener(){

				   
	                
					public void windowActivated(WindowEvent e) {
						// TODO Auto-generated method stub
						
					}
					public void windowClosed(WindowEvent e) {
						 System.out.println("HIDEN");DockWindow.ToggleAllVisible(); 
						
					}
					public void windowClosing(WindowEvent e) {
						// TODO Auto-generated method stub
						
					}
					public void windowDeactivated(WindowEvent e) {
						// TODO Auto-generated method stub
						
					}
					public void windowDeiconified(WindowEvent e) {
						// TODO Auto-generated method stub
						
					}
					public void windowIconified(WindowEvent e) {
						// TODO Auto-generated method stub
						
					}
					public void windowOpened(WindowEvent e) {
						DockWindow.HideAllWindows(); 
						
					}
					
						
				
				
			});
							
			
					}
		return log;
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
