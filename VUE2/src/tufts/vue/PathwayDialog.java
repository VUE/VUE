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
        super(parentFrame, "New Pathway Name", true);
        this.tableModel = model;
        setSize(250, 100);
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

        textField = new JTextField("New Pathway " + newcnt++, 18);
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
                    "Please rename this pathway.",
                    JOptionPane.INFORMATION_MESSAGE);
                JDialog dialog = option.createDialog(okButton, "Pathway Name Exists");
                dialog.show();
            } else {
                VUE.getActiveMap().addPathway(new LWPathway(pathLabel));
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
    
