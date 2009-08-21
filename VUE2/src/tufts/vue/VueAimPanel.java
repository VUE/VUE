package tufts.vue;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import edu.tufts.vue.collab.im.BasicConn;
import edu.tufts.vue.collab.im.ChatConn;
import edu.tufts.vue.collab.im.ChatConnListener;
import edu.tufts.vue.collab.im.VUEAim;
import edu.tufts.vue.layout.TabularLayout;

import net.kano.joscar.net.ClientConnEvent;
import net.kano.joscar.net.ClientConnListener;
import net.kano.joscar.snaccmd.FullUserInfo;
import net.kano.joscar.snaccmd.chat.ChatMsg;
import sun.net.ProgressSource.State;
import tufts.vue.gui.GUI;

/**
 * VueAimPanel
 *
 **/
public class VueAimPanel extends JPanel implements ActionListener, ClientConnListener, ItemListener
{
 
    private JTextField mUsernameEditor = null;
    private JPasswordField mPasswordEditor = null;   
    private PropertyPanel mPropPanel = null;
    private PropertiesEditor propertiesEditor = null;
    private VUEAim aim = null;
    private JButton loginButton = null;
    private JButton resetApproveButton = null;
    private JCheckBox ignoreCheckbox = null;
    private JCheckBox approveCheckbox = null;
    private JCheckBox assignColorCheckbox = null;
    private JCheckBox nodeWidthCheckbox = null;
    private JCheckBox makeTableCheckbox = null;
    private JCheckBox zoomCheckbox = null;
    private JCheckBox enforceColCheckbox = null;
    String[] colStrings = { "2", "3", "4", "5", "6", "7", "8", "9", "10" };

    //Create the combo box, select item at index 4.
    //Indices start at 0, so 4 specifies the pig.
    JComboBox colList = new JComboBox(colStrings);

    
    public VueAimPanel() {                    
        
        JPanel innerPanel = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
    
        //BoxLayout boxLayout = new BoxLayout(innerPanel,BoxLayout.Y_AXIS);
        innerPanel.setLayout(gridbag);
        //mTitleEditor = new JTextField();
        mUsernameEditor = new JTextField();
        
        mPasswordEditor = new JPasswordField();
        assignColorCheckbox = new JCheckBox(VueResources.getString("im.button.assigncolor")); 
        ignoreCheckbox = new JCheckBox(VueResources.getString("im.button.ignore")); 
        approveCheckbox = new JCheckBox(VueResources.getString("im.button.approve"),true);
        nodeWidthCheckbox = new JCheckBox(VueResources.getString("im.button.nodewidth"),true); 
        makeTableCheckbox = new JCheckBox(VueResources.getString("im.button.maketable"),true); 
        zoomCheckbox = new JCheckBox(VueResources.getString("im.button.zoom"),true); 
        enforceColCheckbox = new JCheckBox(VueResources.getString("im.button.enforce"),false); 

        resetApproveButton = new JButton(VueResources.getString("im.approve.reset"));
        resetApproveButton.addActionListener(this);
        ignoreCheckbox.addItemListener(this);
        approveCheckbox.addItemListener(this);
        nodeWidthCheckbox.addItemListener(this);
        makeTableCheckbox.addItemListener(this);
        assignColorCheckbox.addItemListener(this);
        zoomCheckbox.addItemListener(this);
        enforceColCheckbox.addItemListener(this);
        
        mPropPanel  = new PropertyPanel();
        //mPropPanel.addProperty( "Label:", mTitleEditor); // initially Label was title
        mPropPanel.addProperty(VueResources.getString("im.username"), mUsernameEditor); //added through metadata
        mPropPanel.addProperty(VueResources.getString("im.password"), mPasswordEditor);

        JLabel titleLabel = new JLabel(VueResources.getString("im.title"));
        loginButton = new JButton(VueResources.getString("im.button.login"));
        loginButton.addActionListener(this);
        
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(titleLabel,c);
        innerPanel.add(titleLabel);
        
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(mPropPanel,c);
        innerPanel.add(mPropPanel);
        
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(ignoreCheckbox,c);
        innerPanel.add(ignoreCheckbox);
        
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(approveCheckbox,c);
        innerPanel.add(approveCheckbox);

        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(assignColorCheckbox,c);
        innerPanel.add(assignColorCheckbox);
        
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(nodeWidthCheckbox,c);
        innerPanel.add(nodeWidthCheckbox);
        
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(makeTableCheckbox,c);
        innerPanel.add(makeTableCheckbox);
        
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(zoomCheckbox,c);
        innerPanel.add(zoomCheckbox);
        
        
        c.gridx=0;
        c.gridy=9;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(enforceColCheckbox,c);
        innerPanel.add(enforceColCheckbox);
       
        
        c.gridx=1;
        c.gridy=9;
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        gridbag.setConstraints(colList,c);
        colList.setSelectedItem("4");
        innerPanel.add(colList);
        colList.addItemListener(this);
        
        c.gridx=0;
        c.gridy=11;
        c.weightx = 1.0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        gridbag.setConstraints(resetApproveButton,c);
        innerPanel.add(resetApproveButton);
        
        c.gridy=12;
        c.weightx = 1.0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        gridbag.setConstraints(loginButton,c);
        innerPanel.add(loginButton);
        
        /**
         * JPanel metaDataLabelPanel  = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
         * metaDataLabelPanel.add(new JLabel("Metadata"));
         *
         * innerPanel.add(metaDataLabelPanel);
         */
        
        
        JPanel linePanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                g.setColor(Color.DARK_GRAY);
                g.drawLine(0,15, this.getSize().width, 15);
            }
        };
        
        //c.gridwidth = GridBagConstraints.REMAINDER;
        //c.fill = GridBagConstraints.HORIZONTAL;
        //gridbag.setConstraints(linePanel,c);
        //innerPanel.add(linePanel);
        //linePanel.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
        propertiesEditor = new PropertiesEditor(true);
        JPanel metadataPanel = new JPanel(new BorderLayout());
//         if(tufts.vue.ui.InspectorPane.META_VERSION == tufts.vue.ui.InspectorPane.OLD)
//         {
//           metadataPanel.add(propertiesEditor,BorderLayout.CENTER);
//         }
        //metadataPanel.setBorder(BorderFactory.createEmptyBorder(0,9,0,6));
        
        
         mUsernameEditor.setFont(GUI.LabelFace);
        
         // VUE 1001
         //mLocation.setFont(GUI.LabelFace);
         mPasswordEditor.setFont(GUI.LabelFace);
         
       
        
        
        c.weighty = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        gridbag.setConstraints(metadataPanel,c);
        innerPanel.add(metadataPanel);
        //innerPanel.add(mInfoScrollPane,BorderLayout.CENTER);
        //mInfoScrollPane.setSize( new Dimension( 200, 400));
        //mInfoScrollPane.getViewport().setLayout(new BorderLayout());
        //mInfoScrollPane.getViewport().add( innerPanel,BorderLayout.CENTER);
        //mInfoScrollPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        setLayout(new BorderLayout());
        //setLayout(new BorderLayout());
        //setBorder( new EmptyBorder(4,4,4,4) );
        //add(mInfoScrollPane,BorderLayout.NORTH);
        add(innerPanel,BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(10,10,0,6));
       
    }

	public void actionPerformed(ActionEvent e) {
	if (e.getSource().equals(loginButton))
	{
		if (aim == null)
		{
			aim = new VUEAim(mUsernameEditor.getText(),mPasswordEditor.getText());
			aim.addConnectionListener(this);
		
		}
		if (aim.isConnected())
			aim.disconnect();
		
		else
		{
			aim.connect();						
			aim.ignoreIMs(ignoreCheckbox.isSelected());
			aim.requireApprovalToCollaborate(approveCheckbox.isSelected());
			aim.assignColorsToContributors(assignColorCheckbox.isSelected());
			aim.forceUniformNodeWidth(nodeWidthCheckbox.isSelected());
			aim.forceNodesIntoTable(makeTableCheckbox.isSelected());
			aim.keepMapAtOneToOne(zoomCheckbox.isSelected());
		}
	}
	else if (e.getSource().equals(resetApproveButton))
	{
		if (aim !=null)
			aim.resetApprovalList();
	}
		
	}

	public void stateChanged(ClientConnEvent arg0) {
		if (arg0.getNewState().equals(BasicConn.STATE_CONNECTED))
			loginButton.setText(VueResources.getString("im.button.logout"));
		else if (arg0.getNewState().equals(BasicConn.STATE_NOT_CONNECTED))
			loginButton.setText(VueResources.getString("im.button.login"));
		
	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(ignoreCheckbox))
		{
			if (((JCheckBox)e.getSource()).isSelected() && aim !=null)
				aim.ignoreIMs(true);
			else if (!((JCheckBox)e.getSource()).isSelected() && aim !=null)
				aim.ignoreIMs(false);
		}
		else if (e.getSource().equals(approveCheckbox))
		{
			if (((JCheckBox)e.getSource()).isSelected() && aim !=null)
				aim.requireApprovalToCollaborate(true);
			else if (!((JCheckBox)e.getSource()).isSelected() && aim !=null)
				aim.requireApprovalToCollaborate(false);
		}
		else if (e.getSource().equals(assignColorCheckbox))
		{
			if (((JCheckBox)e.getSource()).isSelected() && aim !=null)
				aim.assignColorsToContributors(true);
			else if (!((JCheckBox)e.getSource()).isSelected() && aim !=null)
				aim.assignColorsToContributors(false);
		}
		else if (e.getSource().equals(nodeWidthCheckbox))
		{
			if (((JCheckBox)e.getSource()).isSelected() && aim !=null)
				aim.forceUniformNodeWidth(true);
			else if (!((JCheckBox)e.getSource()).isSelected() && aim !=null)
				aim.forceUniformNodeWidth(false);
		}
		else if (e.getSource().equals(makeTableCheckbox))
		{
			if (((JCheckBox)e.getSource()).isSelected() && aim !=null)
				aim.forceNodesIntoTable(true);
			else if (!((JCheckBox)e.getSource()).isSelected() && aim !=null)
				aim.forceNodesIntoTable(false);
		}
		else if (e.getSource().equals(zoomCheckbox))
		{
			if (((JCheckBox)e.getSource()).isSelected() && aim !=null)
				aim.keepMapAtOneToOne(true);
			else if (!((JCheckBox)e.getSource()).isSelected() && aim !=null)
				aim.keepMapAtOneToOne(false);
		}
		else if (e.getSource().equals(enforceColCheckbox))
		{
			if (((JCheckBox)e.getSource()).isSelected())
			{
				TabularLayout.useStrictColumnCount(true);
				TabularLayout.overrideDefaultColumnCount(new Integer((String)colList.getSelectedItem()).intValue());
			}
			else if (!((JCheckBox)e.getSource()).isSelected())
			{
				TabularLayout.useStrictColumnCount(false);
			}
		}
		else if (e.getSource().equals(colList))
		{
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				Integer i = new Integer((String)colList.getSelectedItem());
				TabularLayout.overrideDefaultColumnCount(i.intValue());
			}
			
		}
			
		
	}

	
}
