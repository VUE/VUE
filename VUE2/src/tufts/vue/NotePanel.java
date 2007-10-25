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

/** 
 * Provides an editable note panel for an LWComponents notes.
 *
 * @version $Revision: 1.19 $ / $Date: 2007-10-25 15:54:18 $ / $Author: mike $
 */

package tufts.vue;

import tufts.vue.gui.VueTextPane;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.*;

public class NotePanel extends JPanel
{
    /** the text pane **/
    private VueTextPane mTextPane = new VueTextPane();

    public NotePanel()
    {
        setName("Notes");
        setLayout( new BorderLayout() );
		
        JScrollPane scrollPane = new JScrollPane();
	

        //scrollPane.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        //scrollPane.setLocation(new Point(8, 9));
        scrollPane.getViewport().add(mTextPane);

        add(BorderLayout.CENTER, scrollPane);

        VUE.addActiveListener(LWComponent.class, this);
    }
    
    public Dimension getMinimumSize()
    {
    	return new Dimension(200,200);    	
    }
    
    public Dimension getPreferredSize()
    {
    	return new Dimension(200,200);
    }
    public VueTextPane getTextPane()
    {
    	return mTextPane;
    }
    
    public void activeChanged(ActiveEvent e, LWComponent c) {
        load(c);
    }
    private static void setTypeName(JComponent component, LWComponent c, String suffix) {
        component.setName(c.getComponentTypeLabel() + " " + suffix);
    }
    private void load(LWComponent c) {
    
    	
        
    	if (c == null)
            mTextPane.detachProperty();
        else
        {	setTypeName(this, c, "Notes");
            mTextPane.attachProperty(c, LWKey.Notes);
        }
    }

    public String toString()
    {
        return "NotePanel[" + mTextPane + "]";
    }

    public static void main(String args[]) {
        DEBUG.Enabled = DEBUG.EVENTS = true;
        DEBUG.KEYS = true;
        NotePanel p = new NotePanel();
        p.load(new LWMap("Test Map"));
        try {
            if (args.length > 0) {
                if (args[0].endsWith(".rtf")) {
                    p.mTextPane.setContentType("text/rtf");
                    p.mTextPane.read(new java.io.FileInputStream(args[0]), "description");
                } else {
                    p.mTextPane.setPage(args[0]);
                    if (args.length > 1)
                        p.mTextPane.setEditable(false);
                }
            }
            //p.mTextPane.setPage("http://www.google.com/");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("content type: " + p.mTextPane.getContentType());
        tufts.Util.displayComponent(p, 300, 200);
    }


    


}
