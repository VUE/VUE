package edu.tufts.vue.preferences.implementations;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import tufts.vue.DEBUG;
import tufts.vue.LWPathway;
import tufts.vue.MasterSlide;
import tufts.vue.PathwayTableModel;
import tufts.vue.VUE;
import tufts.vue.VueResources;

public class ShowAgainDialog  extends JDialog implements ActionListener, KeyListener
{
    private JButton okButton, cancelButton;       
    private boolean okCancel = true;
    //private JLabel showAgain = new JLabel("don't show again ");
    private JCheckBox showAgainBox = new JCheckBox();
    private BooleanPreference showAgainPref = null; 
    public ShowAgainDialog(Frame parentFrame, String prefName, String title)
    {
    	super(parentFrame, title, true);
    	this.setDefaultCloseOperation(javax.swing.JFrame.DO_NOTHING_ON_CLOSE);
    	showAgainPref = BooleanPreference.create(
    		edu.tufts.vue.preferences.PreferenceConstants.INTERACTIONS_CATEGORY,
    		prefName, 
    		"Not Used", 
    		"Not Used",
    		true,
    		false);
     
        //setSize(250, 100);
 
        this.setResizable(false);        
        
     
    }

    public void setContentPanel(JPanel contentPanel)
    {
    	   setUpUI(contentPanel);	
    }
    public boolean showAgain()
    {
    	return (((Boolean)showAgainPref.getValue()).booleanValue());
    }
    private static int newcnt = 1;
    private void setUpUI(JPanel panel)
    {
        okButton = new JButton("Delete");
        cancelButton = new JButton("Cancel");

        
        Insets i = okButton.getMargin();
        i.left=i.left+6;
        i.right=i.right+6;
        okButton.setMargin(i);        
        okButton.addKeyListener(this);
        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
        cancelButton.addKeyListener(this);     
        
        Container dialogContentPane = getContentPane();
        GridBagLayout gbl = new GridBagLayout();
        dialogContentPane.setLayout(gbl);
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridx=0;
        gbc.gridy=0;
        gbc.gridwidth=4;
        gbc.weightx=4.0;
        gbc.insets = new Insets(15,10,10,10);
        gbc.anchor=GridBagConstraints.CENTER;
        gbc.fill=GridBagConstraints.BOTH;                   	
        dialogContentPane.add(panel, gbc);

        ///////////////////////////////////////////////////////
        
        gbc.weightx=1.0;
        this.getRootPane().setDefaultButton(okButton);
        gbc.ipadx=0;
        gbc.gridx=0;
        gbc.gridy=1;
        gbc.gridwidth=1;
        //gbc.weightx=0.5;
        gbc.insets = new Insets(2,10,15,1);
        gbc.anchor=GridBagConstraints.EAST;
        gbc.fill=GridBagConstraints.BOTH;
        showAgainBox.setText("don't show again");
        showAgainBox.setSelected(!((Boolean)showAgainPref.getValue()).booleanValue());
        dialogContentPane.add(showAgainBox,gbc);

        gbc.gridx=1;
        gbc.gridy=1;
        gbc.gridwidth=2;
        gbc.weightx=14.0;
       // gbc.insets = new Insets(1,110,10,5);
        gbc.anchor=GridBagConstraints.EAST;
        gbc.fill=GridBagConstraints.NONE;
        gbc.insets = new Insets(2,20,15,2);
        dialogContentPane.add(cancelButton,gbc);
     
        gbc.weightx=1.0;
        gbc.gridx=3;
        gbc.gridy=1;
        gbc.gridwidth=1;
        gbc.insets = new Insets(2,2,15,10);
        
       // gbc.insets = new Insets(1,5,10,110);
        gbc.anchor=GridBagConstraints.EAST;
        gbc.fill=GridBagConstraints.REMAINDER;               
        dialogContentPane.add(okButton,gbc);
        
        
        ////////////////////////////////////////////////////////
        
                this.pack();
    
        
    }

    public void actionPerformed(java.awt.event.ActionEvent e) 
    {
        if (DEBUG.PATHWAY) System.out.println(this + " " + e);
        if (e.getSource() == okButton)
        {
        	okCancel = true;
        	showAgainPref.setValue(new Boolean(!showAgainBox.isSelected()));
        	dispose();
        }
        else if (e.getSource() == cancelButton)
        {
        	okCancel = false;
            dispose();
        }
    }
        //key events for the dialog box
    public void keyPressed(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}

    public void keyTyped(KeyEvent e) 
    {
        if (e.getKeyChar()== KeyEvent.VK_ENTER)
        {
        	
            if (okButton.isFocusOwner()) {
            	okCancel = true;
            	showAgainPref.setValue(new Boolean(!showAgainBox.isSelected()));
                dispose();             
            } else if (cancelButton.isFocusOwner()) {
                //else if the cancel button has the focus, just aborts it
            	okCancel = false;
                dispose();
            }
            
        }
    }
    
    public boolean getOkCanel()
    {
    	return okCancel;
    }
    
    public String toString()
    {
        return "ShowAgainDialog[]";
    }
    
    public static void main(String[] args)    
    {
    	JPanel panel = new JPanel();    	
    	panel.add(new JLabel("By deleting this slide, your work on this slide will be lost."));
    	ShowAgainDialog sad = new ShowAgainDialog(null,"blah","Delete Pathway");
    	sad.setContentPanel(panel);
 //   	sad.setSize(375, 130);
    	sad.pack();
    	sad.setVisible(true);
    }
}
    
