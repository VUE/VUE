/*
* Copyright 2003-2010 Tufts University  Licensed under the
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






