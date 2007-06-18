 /*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/*
 * PathwayDialog.java
 *
 * Created on December 3, 2003, 2:42 PM
 */

package tufts.vue;

import javax.swing.*;
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
        super(parentFrame, VueResources.getString("presentationDialog.title"), true);
        this.tableModel = model;
        setSize(250, 100);
        this.setFocusable(true);
        setLocation(location);
        setUpUI();
    }

    private static int newcnt = 1;
    private void setUpUI()
    {
        okButton = new JButton("Ok");
        cancelButton = new JButton("Cancel");

        okButton.addActionListener(this);
        okButton.addKeyListener(this);
        cancelButton.addActionListener(this);
        cancelButton.addKeyListener(this);

        textField = new JTextField(VueResources.getString("presentationDiaaog.presentationName.text")+" " + newcnt++, 18);
        textField.addKeyListener(this);
        textField.setPreferredSize(new Dimension(40, 20));

        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout());
        buttons.add(okButton);
        buttons.add(cancelButton);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new FlowLayout());
        textPanel.add(textField);

        Container dialogContentPane = getContentPane();
        dialogContentPane.setLayout(new BorderLayout());

        dialogContentPane.add(textPanel, BorderLayout.CENTER);
        dialogContentPane.add(buttons, BorderLayout.SOUTH);
    }

    public void actionPerformed(java.awt.event.ActionEvent e) 
    {
        if (DEBUG.PATHWAY) System.out.println(this + " " + e);
        if (e.getSource() == okButton)
        {
            String pathLabel = textField.getText();
            if (tableModel.containsPathwayNamed(pathLabel)) {
                JOptionPane option = new JOptionPane(
                    VueResources.getString("presentationDialog.renamePresentation.text"),
                    JOptionPane.INFORMATION_MESSAGE);
                JDialog dialog = option.createDialog(okButton,VueResources.getString("presentationDialog.renamePresentation.title"));
                dialog.setVisible(true);
            } else {
            	LWPathway path = new LWPathway(pathLabel);
                VUE.getActiveMap().addPathway(path);                             
                dispose();
            }
        }
        else if (e.getSource() == cancelButton)
        {
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
            if (DEBUG.PATHWAY) System.out.println(this + " ENTER");
            //if the ok button or the text field has the focus, add a designated new pathway
            if (okButton.isFocusOwner() || textField.isFocusOwner()) {    
                VUE.getActiveMap().addPathway(new LWPathway(textField.getText()));
                dispose();                  
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
    
