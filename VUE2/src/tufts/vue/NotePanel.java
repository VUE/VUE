/** 
 * NotesPanel.java
 *
 * Provides an editable note panel for an LWComponents notes.
 *
 * @author scottb
 * @author Scott Fraize
 * @version February 2004
 */

package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class NotePanel extends JPanel
    implements LWComponent.Listener
{
    /** the LWComponent **/
    private LWComponent mObject = null;
	
    /** the scrollable pane for the text **/
    private JScrollPane mScrollPane = new JScrollPane();
	
    /** the text pane **/
    private JTextPane mTextPane = new JTextPane();

    /* the property of the notes **/
    //VuePropertyDescriptor mPropertyDescriptor = null;

    /** was a key pressed since we loaded the current text? */
    private boolean mKeyWasPressed = false;
	
    public NotePanel() {
        super();
        initComponents();
    }
	
    public String getName() {
        //FIX:(fix what?)
        return "Notes";
    }

    private void initComponents()  {

        this.setVisible(true);
        this.setLayout( new BorderLayout() );
		
        mScrollPane.setSize( new Dimension( 200, 400));
        mScrollPane.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        mScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
        mScrollPane.setLocation(new Point(8, 9));
        mScrollPane.setVisible(true);
		
		
		
        mTextPane.setVisible(true);
        mTextPane.setText("mTextPane");
        mTextPane.setText("Notes...");
		
        this.add( BorderLayout.CENTER, mScrollPane);
        mScrollPane.getViewport().add( mTextPane);

        setSize(new java.awt.Dimension(200, 400));
		
        mTextPane.addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent e) { saveNotes(); }
            });
        mTextPane.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) { mKeyWasPressed = true; }
            });
    }

	
    /**
     * saveNotes
     * This method saves the notes by setting the appropriate property
     * value on the property descriptor.  Will only save value if
     * a key has been pressed since we loaded the text, indicating
     * a possible change in the text.
     **/
    protected void saveNotes() {
        if (mKeyWasPressed && mObject != null) {
            mObject.setNotes( mTextPane.getText() );
            VUE.getUndoManager().mark();
        }	
        //if( mPropertyDescriptor != null) {
        //String notes = mTextPane.getText();
        //mPropertyDescriptor.setValue( notes);
        //}
    }
    
    /** 
     * Set us editing notes for @param pObj LWComponent
     **/
    public void updatePanel(LWComponent pObj) {
        System.out.println(this + " updatePanel " + pObj);
        if (true||pObj != mObject) {
            saveNotes();
            if (mObject != null && mObject != pObj)
                mObject.removeLWCListener(this);
            if (pObj != null) {
                String text = pObj.getNotes();
                if (text == null)
                    text = "";
                mTextPane.setText(text);
                mKeyWasPressed = false;
                if (pObj != mObject)
                    pObj.addLWCListener(this);
            }
            mObject = pObj;
        }
    }
	
    public void LWCChanged(LWCEvent e)
    {
        if (e.getComponent() == mObject && e.getWhat() == LWKey.Deleting)
            mObject = null;
    }

    public String toString()
    {
        return "NotePanel[" + mObject + "]";
    }


}
