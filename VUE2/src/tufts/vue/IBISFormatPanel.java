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

public class IBISFormatPanel extends JPanel
{
	//	Resource panes
    private final IBISNodeToolPanel ibisNodeToolPanel;
    // private final FillToolPanel fillToolPanel;
    // private final LinkToolPanel linkToolPanel;
    // private final ArrowToolPanel arrowToolPanel;
    // private final TextToolPanel textToolPanel;
    
	public IBISFormatPanel()
	{									
		super();
		this.setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
		this.setFocusable(false);
     
		ibisNodeToolPanel = new IBISNodeToolPanel();
		// fillToolPanel = new FillToolPanel();
		// linkToolPanel = new LinkToolPanel();
		// arrowToolPanel = new ArrowToolPanel();
		// textToolPanel = new TextToolPanel();
		
        this.add(ibisNodeToolPanel);
        // this.add(new JSeparator(SwingConstants.VERTICAL));
        
        // this.add(fillToolPanel);
        // this.add(new JSeparator(SwingConstants.VERTICAL));
        
        // this.add(linkToolPanel);       
        // this.add(new JSeparator(SwingConstants.VERTICAL));
        
        // this.add(arrowToolPanel);
        // this.add(new JSeparator(SwingConstants.VERTICAL));
        
        // this.add(textToolPanel);
	}
	
	/* public TextToolPanel getTextPropsPane()
    {
    	return textToolPanel;
    } */
}

