package tufts.vue.gui;

import java.io.File;

import javax.swing.JFileChooser;

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
}
