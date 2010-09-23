package tufts.vue.gui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import tufts.vue.VUE;
import tufts.vue.VueResources;

public class HtmlOutputDialog extends JDialog implements KeyListener, ActionListener {
/*
	htmloutput.title=HTML Output Settings
htmloutput.pixel.title=Pixel Dimensions
htmloutput.ok=OK
htmloutput.cancel=Cancel
htmloutput.width=Width
htmloutput.heigh=Height
htmloutput.pixels=Pixels
 */
	private static final String TITLE = VueResources.getString("htmloutput.title");
	private JLabel heightPixels = new JLabel(VueResources.getString("htmloutput.pixels"));
	private JLabel widthPixels = new JLabel(VueResources.getString("htmloutput.pixels"));
	private JButton okButton = new JButton(VueResources.getString("htmloutput.ok"));
	private JButton cancelButton = new JButton(VueResources.getString("htmloutput.cancel"));
	private JLabel widthLabel = new JLabel(VueResources.getString("htmloutput.width"));
	private JLabel heightLabel = new JLabel(VueResources.getString("htmloutput.height"));
	private JTextField widthField = new JTextField();
	private JTextField heightField = new JTextField();
	private double widthRatio;
	private double heightRatio;
	private double width;
	private double height;
	private int returnVal = -1;

	public HtmlOutputDialog()
	{
		 super(VUE.getDialogParentAsFrame(),TITLE,true);
		 buildUI();
		 setModal(true);
         setResizable(false);  
         setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
         width = VUE.getActiveMap().getPaintBounds().getWidth();
         height = VUE.getActiveMap().getPaintBounds().getHeight();
         widthField.setText((new Integer((int)width)).toString());
         heightField.setText((new Integer((int)height)).toString());
         widthRatio = (double)((double)width/(double)height);
         heightRatio = (double)((double)height/(double)width);
        // System.out.println("w;"+width + " h;" + height + " widthRatio;" + widthRatio+" heightRatio;"+heightRatio);
         addListeners();
         Dimension screenSize =
         Toolkit.getDefaultToolkit().getScreenSize();
         Dimension labelSize = getPreferredSize();
         setLocation(screenSize.width/2 - (labelSize.width/2),
                       screenSize.height/2 - (labelSize.height/2));


	}
	

	private void addListeners()
	{
		widthField.addKeyListener(this);
		heightField.addKeyListener(this);
		okButton.addActionListener(this);
	}
	
	private void buildUI()
	{
		 JPanel panel = new JPanel(new MigLayout("fill","[80][80][80]","[20]"));
		 panel.add(new JLabel("Dimensions"),"gaptop 3");
		 panel.add(new JSeparator(),       "growx, wrap, gaptop 3");
		 panel.add(widthLabel,"gap 10");
		 panel.add(widthField,"growx");
		 panel.add(widthPixels,"gap 10, wrap");
		 panel.add(heightLabel,"gap 10");
		 panel.add(heightField,"growx");		 
		 panel.add(heightPixels,"gap 10, wrap");
		
		 JPanel p = new JPanel(new MigLayout(""));
		 p.add(cancelButton, "sg, tag cancel");
		 p.add(okButton, "sg, tag ok");
		 
		 panel.add(p,"gapbottom 1, span 3, align right");
		 this.getContentPane().add(panel);
		 this.pack();

	}
	public double getScale()
	{
		String wText = widthField.getText();
		Double w = new Double(wText);
		
		double ratio = w.doubleValue()/width; 
		
		return ratio;
	}
	
	public static void main(String[] args)
	{
		HtmlOutputDialog tml = new HtmlOutputDialog();
		tml.setVisible(true);
	}

	public void keyPressed(KeyEvent e) {
		
		
	
		
	}

	public void keyReleased(KeyEvent e) {
		if (e.getSource().equals(widthField))
		{
			String s = widthField.getText();
			Integer i = null;
			
			try
			{
				i = new Integer(s);
			}
			catch(Exception e2)
			{
				i = new Integer((int)width);
			}
			//System.out.println(i.intValue());
			int newHeight = (int)(i.intValue() * heightRatio);
			heightField.setText((new Integer(newHeight)).toString());
		}
		else if (e.getSource().equals(heightField))
		{
			String s = heightField.getText();
			Integer i = null;
			
			try
			{
				i = new Integer(s);
			}
			catch(Exception e2)
			{
				i = new Integer((int)height);
			}
			//System.out.println(i.intValue());
			int newWidth = (int)(i.intValue() * widthRatio);
			widthField.setText((new Integer(newWidth)).toString());
		}
	}

	public void keyTyped(KeyEvent e) {
		
	}
	
	public int getReturnVal()
	{
		return returnVal;
	}
	
	public void actionPerformed(ActionEvent e) {
		
		if (e.getSource().equals(okButton))
			returnVal = 1;
		
		setVisible(false);
		dispose();
		
	}
}
