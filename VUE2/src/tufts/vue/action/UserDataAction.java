/*
 * UserDataAction.java
 *
 */


package tufts.vue.action;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import tufts.vue.*;

/**
 *
 *
 */
public class UserDataAction extends AbstractAction
{
    private boolean mIsShown = false;
   
    public UserDataAction() {
    	super("User meta-data...");
    }
    
    public void actionPerformed( ActionEvent pEvent) {
    	LWMap map = VUE.getActiveMap();
    	
    	if( (map != null) && ( !mIsShown ) ) {
    		
    		mIsShown = true;
    		
    		MapTypeDialog dialog = new MapTypeDialog( map);
    		dialog.displayDialog();
    		
    		mIsShown = false;
    		}
    }
    
}






