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






