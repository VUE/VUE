package tufts.vue;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import tufts.vue.gui.GUI;
import tufts.vue.gui.VueButton;
import tufts.vue.gui.Widget;
import tufts.vue.gui.WidgetStack;
import tufts.vue.gui.formattingpalette.TextPropsPane;
import tufts.vue.gui.formattingpalette.VueColorButton;

public class FormatPanel extends JPanel
{
	//	Resource panes
    private final NodeToolPanel nodeToolPanel;
    private final LinkToolPanel linkToolPanel;
    private final TextToolPanel textToolPanel;
    
	public FormatPanel()
	{									
		super();
		this.setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
		this.setFocusable(false);
       // setName(VueResources.getString("formatting.label"));
     
		nodeToolPanel = new NodeToolPanel();
		linkToolPanel = new LinkToolPanel();
		textToolPanel = new TextToolPanel();
		
        
        JPanel mAlignmentPane = new JPanel();
        JPanel mMapPane = new JPanel();
        
      //  WidgetStack stack = new WidgetStack();

   //     stack.addPane(VueResources.getString("formatting.text.label"), mTextPane, 0f);
      //  stack.addPane(VueResources.getString("formatting.node.label"), mNodePane, 0f);
       // stack.addPane(VueResources.getString("formatting.link.label"), mLinkPane, 0f);
       // stack.addPane(VueResources.getString("formatting.alignment.label"), mAlignmentPane, 0f);
       // stack.addPane(VueResources.getString("formatting.map.label"), mMapPane, 0f);
        
        // c.gridwidth = GridBagConstraints.RELATIVE; // next-to-last in row


        this.add(nodeToolPanel);
        this.add(new JSeparator(SwingConstants.VERTICAL));
        
        this.add(linkToolPanel);
        
        this.add(new JSeparator(SwingConstants.VERTICAL));
        this.add(textToolPanel);
	}
	
	public TextPropsPane getTextPropsPane()
    {
    	return null;
    }
}
