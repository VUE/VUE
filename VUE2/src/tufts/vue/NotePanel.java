/** 
 * NotesPanel.java
 *
 * Description:	This is an editable notes panel
 * @author			
 * @version			
 */

package tufts.vue;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class NotePanel extends JPanel
    implements LWComponent.Listener
{
	//////////////////
	//  Fields
	//////////////////
	
	/** the LWComponent **/
	LWComponent mObject = null;
	
	/** the scrollable pane for the text **/
	JScrollPane mScrollPane = new JScrollPane();
	
	/** the text pane **/
	JTextPane mTextPane = new JTextPane();

	/** the property of the notes **/
	//VuePropertyDescriptor mPropertyDescriptor = null;

        /** was a key pressed since we loaded the current text? */
        private boolean mKeyWasPressed = false;
	
	///////////////////
	// Constructors
	////////////////////
	
	public NotePanel() {
            super();
            initComponents();
	}
	
	
	///////////////////////
	// Methods
	////////////////////
	
	
	public String getName() {
		//FIX:
		return "Notes";
	}
	/**
	 * initComponents()
	 *
	 **/
	public void initComponents()  {

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
		
		mTextPane.addFocusListener( new NoteFocusListener() );
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
    public void saveNotes() {
        debug("   Saving notes...");
        
        if (mKeyWasPressed && mObject != null) {
            mObject.setNotes( mTextPane.getText() );
        }	
        /*******
                if( mPropertyDescriptor != null) {
                
                String notes = mTextPane.getText();
                //mPropertyDescriptor.setValue( notes);
                }
        *********/
    }
    
    /** 
     * updatePanel
     *
     **/
    public void updatePanel( LWComponent pObj) {
        if (pObj != mObject)  {
            saveNotes();
            if (mObject != null)
                mObject.removeLWCListener(this);
            mObject = pObj;
            if (pObj != null) {
                String text = pObj.getNotes();
                if (text == null)
                    text = "";
                mTextPane.setText(text);
                mKeyWasPressed = false;
                pObj.addLWCListener(this);
            }
        }
    }
	
    public void LWCChanged(LWCEvent e)
    {
        if (e.getComponent() == mObject && e.getWhat() == LWCEvent.Deleting)
            mObject = null;
    }
    
	/**
	 * NoteFocusListener
	 * This inner cclass is used to listen to focus to save any changes to
	 * the users notes.
	 **/
	public class NoteFocusListener implements FocusListener {
	
		public NoteFocusListener() {
		
		}
		
		
		public void focusGained( FocusEvent pEvent) {
		
		}
		
		public void focusLost( FocusEvent pEvent) {
			saveNotes();
		}
		
	}

	static private boolean  sDebug = false;
	private void debug( String str) {
		if( sDebug) {
			System.out.println( str + " [NotePanel]");
			}
	}

}
