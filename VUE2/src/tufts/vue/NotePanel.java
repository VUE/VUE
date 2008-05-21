 /*
 * Copyright 2003-2007 Tufts University  Licensed under the
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

/** 
 * Provides an editable note panel for an LWComponents notes.
 *
 * @version $Revision: 1.22 $ / $Date: 2008-05-21 03:45:16 $ / $Author: sfraize $
 */

package tufts.vue;

import tufts.vue.gui.VueTextPane;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.*;
import java.awt.*;

public class NotePanel extends JPanel
{
    /** the text pane **/
    private VueTextPane mTextPane = new VueTextPane();

    public NotePanel(boolean autoAttach)
    {
        setName("Notes");
        setLayout(new BorderLayout());
		
        if (false) {
            JScrollPane scrollPane = new JScrollPane();
            
            //scrollPane.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            //scrollPane.setLocation(new Point(8, 9));
            scrollPane.getViewport().add(mTextPane);
            add(BorderLayout.CENTER, scrollPane);
        } else {
            if (tufts.Util.isMacPlatform()) {
                // be sure to fetch and include the existing border, to get the special mac focus hilighting border
                final int b = 2;
                mTextPane.setBorder(new CompoundBorder(new MatteBorder(b,b,b,b,SystemColor.control),
                                                       mTextPane.getBorder()));
            } else {
                mTextPane.setBorder(new EmptyBorder(2,4,2,4));
            }
            add(BorderLayout.CENTER, mTextPane);
        }

        if (autoAttach)
            VUE.addActiveListener(LWComponent.class, this);
    }

    public NotePanel() {
        this(true);
    }
    
    
    @Override
    public Dimension getMinimumSize() {
        return minSize("getMinimumSize");
    }
    
    @Override
    public Dimension getPreferredSize() {
        return minSize("getPreferredSize");
    }

    private static final int MinNoteHeight = 46;

    private Dimension minSize(String src) {
        Dimension d = super.getPreferredSize();
        if (d.height < MinNoteHeight)
            d.height = MinNoteHeight;
        //System.out.format("NotePanel %16s %s\n", src, tufts.Util.fmt(d));
    	return d;
    }
    
    public VueTextPane getTextPane() {
    	return mTextPane;
    }
    
    public void activeChanged(ActiveEvent e, LWComponent c) {
        load(c);
    }
    private static void setTypeName(JComponent component, LWComponent c, String suffix) {
        component.setName(c.getComponentTypeLabel() + " " + suffix);
    }
    
    public void attach(LWComponent c) {
    	if (c == null) {
            mTextPane.detachProperty();
        } else {
            mTextPane.attachProperty(c, LWKey.Notes);
        }
    }
    
    private void load(LWComponent c) {
        attach(c);
        if (c != null)
            setTypeName(this, c, "Notes");
    }

    @Override
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
