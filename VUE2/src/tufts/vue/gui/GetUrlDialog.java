package tufts.vue.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import tufts.vue.RecentlyOpenedUrlManager;
import tufts.vue.VueResources;

public class GetUrlDialog extends JDialog implements ActionListener {

final RecentlyOpenedUrlManager roum = RecentlyOpenedUrlManager.getInstance();
	JTextField fUrlField = AutoCompleteDocument.createAutoCompleteTextField(roum.getRecentlyOpendUrlsArray());
	Container fContainer;
	GridBagLayout fGridBag;
	GridBagConstraints fGBC = new GridBagConstraints();
	JButton fButtonOK;
	JButton fButtonCancel;
	private static String url = new String();
	
	
	public GetUrlDialog() {
		// Call the JDialog constructor.
		super(tufts.vue.VUE.getApplicationFrame(), VueResources.getString("dialog.openurl.title"), true);
	
		fContainer = this.getContentPane();
		fButtonOK = new JButton(VueResources.getString("button.ok.label"));
		fButtonOK.addActionListener(this);

		fButtonCancel = new JButton(VueResources.getString("button.cancel.lable"));
		fButtonCancel.addActionListener(this);

		JPanel button_panel = new JPanel();
		button_panel.add(fButtonOK);
		button_panel.add(fButtonCancel);

		// Use a gridbag layout for the contentpane
		fGridBag = new GridBagLayout();
		fContainer.setLayout(fGridBag);
		// Give the labels a small weightx and a
		// width of 1 to push them to the left
		fGBC.fill = GridBagConstraints.BOTH;
		fGBC.weightx = 0.10;
 		fGBC.weighty = 1.0;
		fGBC.gridwidth = 1;
		fGBC.gridheight = 1;
		fGBC.insets = new Insets(5, 5, 8, 5);
		JLabel label = new JLabel(VueResources.getString("openurl.label"),SwingConstants.LEFT);
		addGB(label, 0, 0);


		
		fGBC.gridwidth = 6;
		fGBC.gridheight = 1;
		fGBC.weightx = 1;
		fGBC.weighty = 1;
		fGBC.insets = new Insets(2, 20, 2, 20);

		addGB(button_panel, 0, 2);

		
		// Give the textfields a large weightx and a
		// width of 5 so they will have a long x width
		fGBC.weightx = 1.0;
		fGBC.weighty = 1.0;
		fGBC.insets = new Insets(10, 1, 2, 1);
		fGBC.gridwidth = 5;
		fGBC.gridheight = 1;
		addGB(fUrlField, 0, 1);


		// Resize the dialog box and position it at the center of
		// the applet.
		setSize(350, 120);
		setResizable(false);
		Dimension dialogDim = getSize();
	
		Dimension frameDim = null;
		if (!tufts.vue.VUE.isApplet())
			frameDim = tufts.vue.VUE.getApplicationFrame().getSize();
		else
			frameDim = tufts.vue.VUE.getRootWindow().getSize();
			
		Dimension screenSize = getToolkit().getScreenSize();
		Point location = null; 
		
		if (!tufts.vue.VUE.isApplet())
			location = tufts.vue.VUE.getApplicationFrame().getLocation();
		else
			location = tufts.vue.VUE.getRootWindow().getLocation();
		location.translate((frameDim.width - dialogDim.width) / 2,
				(frameDim.height - dialogDim.height) / 2);
		location.x = Math.max(0, Math.min(location.x, screenSize.width
				- getSize().width));
		location.y = Math.max(0, Math.min(location.y, screenSize.height
				- getSize().height));

		setLocation(location.x, location.y);
		this.getRootPane().setDefaultButton(fButtonOK);
	
	    //Make textField get the focus whenever frame is activated.
	    this.addWindowFocusListener(new WindowAdapter() {
	        public void windowGainedFocus(WindowEvent e) {
	            fUrlField.requestFocusInWindow();
	        }
	    });


		setVisible(true);
	}
	
	public static String getURL()
	{
		return url;
		
	}
	
	public void addGB(Component component, int x, int y) {
		fGBC.gridx = x;
		fGBC.gridy = y;
		fGridBag.setConstraints(component, fGBC);
		fContainer.add(component);
	}

	public void actionPerformed(ActionEvent e) {
		 if (e.getSource () == fButtonOK)  
		 {
			roum.updateRecentlyOpenedUrls(fUrlField.getText());
			url = fUrlField.getText();
		 }
		 
		 if (e.getSource() == fButtonCancel)
		 {
			 url = null;
		 }
			 
		this.dispose();
	}

}
