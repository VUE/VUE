package tufts.vue.gui;

import java.awt.EventQueue;
import java.io.File;

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
