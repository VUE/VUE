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

/*******
** Filter Statement Editor
**
**
*********/

package tufts.vue;


import java.io.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;


	/**
	 * FilterStatementEditor
	 * This is the editor to create/modify logical filter statements.
	 *
	 **/
	public class FilterStatementEditor extends JPanel {
		
		////////////
		// Statics
		///////////
		
		static private String [] sConditions = {"contains","equals","starts with","ends with" };
		
		static private String [] sSources = { "All ", "Label","Notes","User Type","Resource","User Data" };
		
		protected SourceItem mAnywhereSource = new SourceItem( sSources[0], 0, null);
		private SourceItem mLabelSource = new SourceItem( sSources[1], 1, null);		
		private SourceItem mNotesSource = new SourceItem( sSources[2],2,null );
		private SourceItem mUserTypeSource = new SourceItem( sSources[3], 3, null);
		
		private  SourceItem [] mDefaultSourceItems = {  mAnywhereSource,
														mLabelSource,
														mNotesSource,
														mUserTypeSource };
		// end of array
				
		///////////
		// Fields
		////////////
		
		
		/** the horizontal layout fox **/
		Box mBox = null;
		
		/** the list of match conditions **/
		JComboBox mCondition = null;
		
		/** the value text edit field. **/
		JTextField mValue = null;	
		
		/** the list of source types **/
		JComboBox mSource = null;
		
		
		/** the logical statement **/
		LWCFilter.LogicalStatement mStatement = null;
		
		
		// FIX:  may want to add this restriction later...
		/** The popup of map types **/
		JComboBox mMapTypes = null;
		
		/** the LWCFilter that this statement belongs to **/
		LWCFilter mFilter = null;
		
		
		
		
		//////////////
		// Constuctors
		///////////////
		
		
		/**
		 * FilterStatementEditor
		 *
		 **/
		public FilterStatementEditor( LWCFilter pFilter, LWCFilter.LogicalStatement pStatement) {
			
			mBox = Box.createHorizontalBox();
			mCondition = new JComboBox( sConditions );
			mSource = new JComboBox();
			mFilter = pFilter;
			mValue = new JTextField();
			
			setLayout( new BorderLayout() );
			setBorder( new EmptyBorder(4,4,4,4) );
			
			buildSourcesCombo( mFilter.getMap() );
		
		
			mBox.add( mSource);
			mBox.add( mCondition);
		
			add( BorderLayout.WEST, mBox );
			add( BorderLayout.CENTER, mValue );
			setStatement( pStatement);
		}
		
		
		/**
		 * getStatement
		 * This returns the logical statement
		 * @return LogicalStatement the statement
		 **/
		public LWCFilter.LogicalStatement getStatement() {
			
			SourceItem item =(SourceItem)  mSource.getSelectedItem();
			mStatement.setSourceType(  item.getType() );
			mStatement.setSourceID( item.getSourceID() );
			mStatement.setValue( mValue.getText() );
			mStatement.setCondition( mCondition.getSelectedIndex() );
			
			return mStatement;
		}
		 
		public void setStatement( LWCFilter.LogicalStatement pStatement) {
			mStatement = pStatement;
			
			mValue.setText( mStatement.getValue() );
			mCondition.setSelectedIndex( mStatement.getCondition() );
			setSourceCombo( mStatement.getSourceType(), mStatement.getSourceID() );
		}
		
		public void refreshFields( LWMap pMap) {
			// update the display
		}
		
		private void setSourceCombo( int pSource, String pID) {
			if( pSource < mDefaultSourceItems.length ) {
				mSource.setSelectedIndex( pSource);
				}
			else
			if( pSource == LWCFilter.USERDATA ) {
				// FIX:  search user metadata to set
				System.out.println("!!! FIX:  need to search user meta-data.");
				}
			
		}
		
		public void buildSourcesCombo( LWMap pMap) {
		
			mSource.removeAllItems();
			for(int i=0; i< mDefaultSourceItems.length; i++) {
				mSource.addItem( mDefaultSourceItems[i] );
				}
			
			//UserMapType [] pMap.getUserMapTypes();
			
		
		}
	
	
	///////////
	// Inner Classes
	///////////////////
	
	public class SourceItem {
	
		String mName = null;
		int mSourceType = 0;
		String mSourceID = null;
		
		public SourceItem( String pName, int pType, String pID) {
			mName = pName;
			mSourceType = pType;
			mSourceID = pID;
		}
		
		public int getType()  {
			return mSourceType;
		}
		public String getSourceID() {
			return mSourceID;
		}
		
		public String toString() {
			return mName;	
		}
	}
}
	
	
