/*******
**  LWCFilter
**
**
*********/

package tufts.vue;


import java.io.*;
import java.util.*;
import tufts.vue.beans.*;


/**
* LWCFilterPanel
*
* This class describes a logical filter that can be applied to a
* set of LWComponents.
*
* A filter can be on or off and contains a list of logical statements
* that describe a condition.  If there are more than one conditions listed, a
* global AND (meaning all conditions must be met) or a logical OR (meaning
* (one or any of the conditons should be met) can be specified.
*
* Any object interested in displaying an item, should check to see if
* the filter by the boolean showComponenet( LWComponent) method.
*
\**/
public class LWCFilter  
{


	/////////////
	// Statics
	//////////////
	
	/** condition constants **/
	static public final int CONTAINS = 0;
	static public final int IS = 1;
	static public final int STARTS = 2;
	static public final int ENDS = 3;

	/** property source types **/
	static public final int ANYWHERE = 0;
	static public final int LABEL = 1;
	static public final int METADATA = 2;
	static public final int USERDATA = 3;
	static public final int NOTES = 4;
	
	/////////////
	// Fields
	//////////////
	
	/** a vector of contraints **/
	LogicalStatement [] mStatements = null;
	
	/** the logical op state **/
	boolean mIsAny = false;
	
	/** is the map filter currenlty on **/
	boolean mIsFilterOn = false;
	
	/** LWMap **/
	private LWMap mMap = null;
	
	///////////////////
	// Constructors
	////////////////////
	
	public LWCFilter() {
		super();
		mStatements = new LogicalStatement[1];
		mStatements[0] = new LogicalStatement();
		
	}
	
	
	
	////////////////////
	// Methods
	///////////////////
	
	/**
	 * 
	 **/
	public void setFilterOn( boolean pIsOn) {
		mIsFilterOn = pIsOn;
	}
	
	public boolean isFilterOn() {
		return mIsFilterOn;
	}
	
	public void setMap( LWMap pMap) {
		mMap = pMap;
	}
	
	public LWMap getMap() {
		return mMap;
	}
	
	public void setIsAny( boolean pIsAny) {
		mIsAny = pIsAny;
	}
	public boolean getIsAny() {
		return mIsAny;
	}
	
	public void setLogicalStatements(  LogicalStatement [] pArray ) {
		mStatements = pArray;
	}
	
	public LogicalStatement [] getLogicalStatements() {
		return mStatements;
	}
	
	
	/**
	 * showComponent
	 * This method returns true if the the component should be filtered
	 *
	 * @return boolean true - if this should be shown; false if filtered
	 **/
	public boolean showComponenent( LWComponent pLWC ) {
		
		boolean showItem = true;
		if( mIsFilterOn )   {
			showItem = !mIsAny;
			
			for(int i=0; i< mStatements.length; i++ ) {
				LogicalStatement ls = mStatements[ i];
				boolean subState = ls.isMatch( pLWC);
				if( mIsAny && subState) {
					// if looking for ANY and MATCH return true
					return true;
					}
				if( !mIsAny && !subState) {
					// if it's ALL and no match, return false
					return false;
					}
				}
			
			}
		return showItem;
	}
	
	
	
	
	
	
	
	
	/**
	 * LogicalStatement
	 * This class represents a logical statement for a search filter
	 *
	 **/
	public class LogicalStatement {
	
		/** the value to search for **/
		String mValue = "";
		
		/** the condition for the match (equals, start with, etc) **/
		int mCondition = CONTAINS;
		
		/** the source type, (Label, Notes, meta data, etc) **/
		int mSourceType = ANYWHERE;
		
		/** the sourceID (if any), could be property name **/
		String mSourceID = null;
		
		
		///////////////////
		// Constructor
		////////////////////
		public LogicalStatement() {
			super();
		}
		
		
		///////////
		// Methods
		//////////////
		
		public void setValue( String pValue) {
			mValue = pValue;
		}
		public String getValue() {
			return mValue;
		}
		
		public void setCondition( int pValue) {
			mCondition = pValue;
		}
		public int getCondition() {
			return mCondition;
		}
		
		public void setSourceType( int pType) {
			mSourceType = pType;
		}
		public int getSourceType() {
			return mSourceType;
		}
		
		public void setSourceID( String pID) {
			mSourceID = pID;
		}
		public String getSourceID() {
			return mSourceID;
		}
	
		
		/**
		 * isMatch
		 * This method determines if the LWC matches the conditions
		 * of the this statement.
		 *
		 * @param LWComponent the component to check
		 * @returns true if it matches; false if not
		 **/
		public boolean isMatch( LWComponent pLWC) {
			boolean state = false;
			
			return state;
		}
		
		private boolean matchLabel( LWComponent pLWC) {
			boolean state = false;
			String str = pLWC.getLabel();
			state = isMatch( str);
			return state;
		}
		
		private boolean matchNotes( LWComponent pLWC) {
			boolean state = false;
			String str = pLWC.getNotes();
			state = isMatch( str);
			return state;
		}
		
		public boolean matchMetaData( LWComponent pLWC ) {
			boolean state = false;
			Resource r = pLWC.getResource();
			if( r != null) {
				
				}
			return state;
		}
		
		public boolean matchUserProperty( LWComponent pLWC, boolean pCheckAll) {
			boolean state = false;
			
			String typeID = pLWC.getUserMapType();
			UserMapType type = UserMapType.findUserMapType( getMap(), typeID);
			if( type != null) {
				if( pCheckAll ) {
					
					}
				else {
					
					}
				}
				
			return state;
		}
		
		
		
		/**
		 * isMathc
		 * Determines if the passed string meets the statement requirements
		 * from the getValue and getCondition values of this statement
		 *
		 * @returns true if match; false if no match
		 **/
		private boolean isMatch( String pStr) {
			
			boolean state = false;
			
			if( ( pStr == null) || (mValue == null)) {
				return false;
				}
				
			switch ( getCondition() ) {
				case CONTAINS:
					state = (pStr.indexOf( mValue) != -1);
					break;
					
				case IS:
						state = pStr.equals( mValue);
					break;
					
				case STARTS:
						state = pStr.startsWith( mValue);
					break;
					
				case ENDS:
						state = pStr.endsWith( mValue);
					break;
					
				default:
					// should never happen
					break;
				}
			
		return state;
		}
	}
}