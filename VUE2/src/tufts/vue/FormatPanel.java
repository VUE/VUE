package tufts.vue;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import tufts.vue.gui.GUI;
import tufts.vue.gui.VueButton;
import tufts.vue.gui.Widget;
import tufts.vue.gui.WidgetStack;
import tufts.vue.gui.formattingpalette.TextPropsPane;
import tufts.vue.gui.formattingpalette.VueColorButton;

public class FormatPanel extends JPanel
{
	//	Resource panes
    private final TextPropsPane mTextPane;
	public FormatPanel()
	{							
		super(new BorderLayout());
		this.setFocusable(false);
        setName(VueResources.getString("formatting.label"));
     
        mTextPane = new TextPropsPane();
        JPanel mNodePane = new JPanel();
        JPanel mLinkPane = new JPanel();
        JPanel mAlignmentPane = new JPanel();
        JPanel mMapPane = new JPanel();
        
        WidgetStack stack = new WidgetStack();

        stack.addPane(VueResources.getString("formatting.text.label"), mTextPane, 0f);
      //  stack.addPane(VueResources.getString("formatting.node.label"), mNodePane, 0f);
       // stack.addPane(VueResources.getString("formatting.link.label"), mLinkPane, 0f);
       // stack.addPane(VueResources.getString("formatting.alignment.label"), mAlignmentPane, 0f);
       // stack.addPane(VueResources.getString("formatting.map.label"), mMapPane, 0f);

        Widget.setExpanded(mTextPane, true);
        
        add(stack, BorderLayout.CENTER);                
	}
	
	public TextPropsPane getTextPropsPane()
    {
    	return mTextPane;
    }
}
