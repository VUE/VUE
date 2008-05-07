package tufts.vue.gui;

import java.awt.Frame;
import java.awt.Point;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.tufts.vue.preferences.implementations.ShowAgainDialog;

public class DeleteSlideDialog extends ShowAgainDialog{

	private final JPanel panel = new JPanel();    	

	 public DeleteSlideDialog(Frame parentFrame)
	 {		 	 
		super(parentFrame,"deleteSlide","Delete Slide");
	    panel.add(new JLabel("By deleting this slide, your work on this slide will be lost."));
	    setContentPanel(panel);
	 }
}
