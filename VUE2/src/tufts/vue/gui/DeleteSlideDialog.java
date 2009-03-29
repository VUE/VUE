package tufts.vue.gui;

import java.awt.Frame;

import javax.swing.JLabel;
import javax.swing.JPanel;

import tufts.vue.VueResources;
import edu.tufts.vue.preferences.implementations.ShowAgainDialog;

public class DeleteSlideDialog extends ShowAgainDialog{

	private final JPanel panel = new JPanel();    	

	 public DeleteSlideDialog(Frame parentFrame)
	 {		 	 
		super(parentFrame,"deleteSlide",VueResources.getString("deleteslidedialog.deleteslides"),VueResources.getString("deleteslidedialog.delete"),VueResources.getString("deleteslidedialog.cancel"));
	    panel.add(new JLabel(VueResources.getString("jlabel.deletingthisslide")));
	    setContentPanel(panel);
	 }
}
