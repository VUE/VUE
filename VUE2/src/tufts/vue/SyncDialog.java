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

package tufts.vue;

import javax.swing.*;

import java.awt.event.*;
import java.awt.*;


public class SyncDialog extends JDialog implements ActionListener, KeyListener
{
    private JButton okButton, cancelButton;
    private static int newcnt = 1;
    private final JRadioButton nodeToSlideButton = new JRadioButton(VueResources.getString("jbutton.nodetoslide.label"));
    private final JRadioButton slideToNodeButton = new JRadioButton(VueResources.getString("jbutton.slidetonode.label"));
    private final JRadioButton syncAllButton = new JRadioButton(VueResources.getString("jbutton.bothways.label"));    
    
    public SyncDialog(Frame parentFrame, Point location)
    {
        super(parentFrame, VueResources.getString("dialog.syncresource.title"), true);
        
        setSize(390, 200);
        this.setFocusable(true);
        setLocation(location);
        setAlwaysOnTop(true);
        setUpUI();
    }    
    private void setUpUI()
    {
        okButton = new JButton(VueResources.getString("button.sync.lable"));
        cancelButton = new JButton(VueResources.getString("button.cancel.lable"));

        okButton.addActionListener(this);
        okButton.addKeyListener(this);
        cancelButton.addActionListener(this);
        cancelButton.addKeyListener(this);

        

        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 40));
        buttons.add(cancelButton);
        buttons.add(okButton);
        
        Container dialogContentPane = getContentPane();
        dialogContentPane.setLayout(new BorderLayout());
        JLabel label = new JLabel(VueResources.getString("syncresource.label"));
                        
        ButtonGroup group = new ButtonGroup();
        group.add(nodeToSlideButton);
        group.add(slideToNodeButton);
        group.add(syncAllButton);
        
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbConstraints = new GridBagConstraints();
        gbConstraints.insets=new Insets(0,0,10,0);
        panel.add(label,gbConstraints);
        gbConstraints.insets=new Insets(0,0,0,0);
        gbConstraints.gridy=1;
        gbConstraints.anchor=GridBagConstraints.WEST;
        panel.add(nodeToSlideButton,gbConstraints);
        gbConstraints.gridy=2;
        panel.add(slideToNodeButton,gbConstraints);
        gbConstraints.gridy=3;
        panel.add(syncAllButton,gbConstraints);
        gbConstraints.gridy=4;
    group.setSelected(syncAllButton.getModel(), true);
        dialogContentPane.add(panel,BorderLayout.CENTER);
        dialogContentPane.add(buttons, BorderLayout.SOUTH);
    }

    public void actionPerformed(java.awt.event.ActionEvent e) 
    {
        if (e.getSource() == okButton) {

            if (!fireAction())
                return;

            dispose();
        } else if (e.getSource() == cancelButton) {
            dispose();
        }
    }

    private boolean fireAction() {

        final LWPathway.Entry entry = VUE.getActiveEntry();
        if (entry == null || entry.isMapView()) {
            tufts.Util.printStackTrace(this + ": no entry or not allowed to sync virtual slides; action should have been disabled; entry=" + entry);
            return false;
        }

        // must call action fire method in order for undo code to be invoked:
        if (nodeToSlideButton.isSelected())
            Actions.SyncToSlide.fire(this);
        else if (slideToNodeButton.isSelected())
            Actions.SyncToNode.fire(this);
        else if (syncAllButton.isSelected())
            Actions.SyncAll.fire(this);
        else
            return false;

        return true;
    }

    //key events for the dialog box
    public void keyPressed(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}

    public void keyTyped(KeyEvent e) 
    {
        if (e.getKeyChar()== KeyEvent.VK_ENTER)
        {
            if (okButton.isFocusOwner()) 
            {    
                
                if (!fireAction())
                    return;
                
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
    
