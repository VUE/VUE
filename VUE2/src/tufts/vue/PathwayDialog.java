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

/*
 * PathwayDialog.java
 *
 * Created on December 3, 2003, 2:42 PM
 */

package tufts.vue;

import javax.swing.*;

import tufts.Util;

import java.awt.event.*;
import java.awt.*;

/**
 *
 * @author  Jay Briedis
 */
/**A dialog displayed when the user chooses to add a new pathway to the current map */
public class PathwayDialog extends JDialog implements ActionListener, KeyListener
{
    private JButton okButton, cancelButton;
    private JTextField textField;
    private PathwayTableModel tableModel;
    
    public PathwayDialog(Frame parentFrame, PathwayTableModel model, Point location)
    {
        super(parentFrame, VueResources.getString("presentationDialog.title"), false);
        this.tableModel = model;
        if(Util.isWindowsPlatform()){
        	setSize(300, 114);
        }else{
        	setSize(300, 112);
        }
        this.setFocusable(true);
        setLocation(location);    
        //setAlwaysOnTop(true);
        setModal(true);
        setUpUI();
        super.setResizable(false);
    }
    public void setVisible(boolean b)
    {
    	super.setVisible(b);
    	SwingUtilities.invokeLater(new Runnable() {
    	public void run()
    	{
    		textField.setFocusable(true);    		
    	    textField.requestFocusInWindow();
    	    textField.selectAll();
    		return;	
    	}}
    	);
    	
    	
    }
    private static int newcnt = 1;
    private void setUpUI()
    {
        okButton = new JButton(VueResources.getString("button.add.label"));
        cancelButton = new JButton(VueResources.getString("button.cancel.label"));

        okButton.addActionListener(this);
        Insets i = okButton.getMargin();
        i.left=i.left+12;
        i.right=i.right+12;
        //okButton.setMargin(i);
        okButton.addKeyListener(this);
        cancelButton.addActionListener(this);
        cancelButton.addKeyListener(this);
        if(Util.isWindowsPlatform()){
        	textField = new JTextField(VueResources.getString("presentationDiaaog.presentationName.text")+" " + newcnt++, 33);
        }else{
        	textField = new JTextField(VueResources.getString("presentationDiaaog.presentationName.text")+" " + newcnt++, 22);
        }
        textField.addKeyListener(this);
        //textField.setPreferredSize(new Dimension(140, 20));

        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));  
        if(!Util.isWindowsPlatform()){
        	buttons.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 3));
        }else{
        	buttons.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 6));
        }
        
        buttons.add(cancelButton);
        buttons.add(okButton);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new FlowLayout());
        textPanel.add(textField);

        Container dialogContentPane = getContentPane();
        dialogContentPane.setLayout(new BorderLayout());
        JLabel tempLbl = new JLabel("");
        tempLbl.setPreferredSize(new Dimension(20,10));
        dialogContentPane.add(tempLbl, BorderLayout.NORTH);
        dialogContentPane.add(textPanel, BorderLayout.CENTER);
        dialogContentPane.add(buttons, BorderLayout.SOUTH);
       
    }

    public void actionPerformed(java.awt.event.ActionEvent e) 
    {
        if (DEBUG.PATHWAY) System.out.println(this + " " + e);
        if (e.getSource() == okButton)
        {
            String pathLabel = textField.getText();
            addNewPathway(pathLabel);
        }
        else if (e.getSource() == cancelButton)
        {
            dispose();
        }
    }
    private void addNewPathway(String pathLabel)
    {
    	  if (tableModel.containsPathwayNamed(pathLabel)) {
              JOptionPane option = new JOptionPane(
                  VueResources.getString("presentationDialog.renamePresentation.text"),
                  JOptionPane.INFORMATION_MESSAGE);
              JDialog dialog = option.createDialog(okButton,VueResources.getString("presentationDialog.renamePresentation.title"));
              dialog.setVisible(true);
          } else {
          	LWPathway path = new LWPathway(pathLabel);
          	LWPathway activePath = VUE.getActivePathway();
          	if (activePath != null)
          	{
          		MasterSlide masterSlide = activePath.getMasterSlide();
          		//path.getMasterSlide().setStyle(masterSlide);
        		path.getMasterSlide().setTitleStyle(masterSlide.getTitleStyle());
        		path.getMasterSlide().setLinkStyle(masterSlide.getLinkStyle());
        		path.getMasterSlide().setTextStyle(masterSlide.getTextStyle());
        		path.getMasterSlide().setFillColor(masterSlide.getFillColor());

          	}
              VUE.getActiveMap().addPathway(path);                
              VUE.setActive(LWPathway.class, this, path);
              dispose();
          }
    	  return;
    }
    //key events for the dialog box
    public void keyPressed(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}

    public void keyTyped(KeyEvent e) 
    {
        if (e.getKeyChar()== KeyEvent.VK_ENTER)
        {
            if (DEBUG.PATHWAY) System.out.println(this + " ENTER");
            //if the ok button or the text field has the focus, add a designated new pathway
            if (okButton.isFocusOwner() || textField.isFocusOwner()) {    
                addNewPathway(textField.getText());                  
            } else if (cancelButton.isFocusOwner()) {
                //else if the cancel button has the focus, just aborts it
                dispose();
            }
        }
    }

    public String toString()
    {
        return "PathwayDialog[]";
    }

}
    
