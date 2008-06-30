/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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
 **  LWCFilter
 **
 **
 *********/

package tufts.vue;


import java.io.*;
import java.util.*;
import tufts.vue.filter.*;


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
 * \**/
public class LWCFilter {
    static public final String ACTION_HIDE = "Hide";
    static public final String ACTION_SHOW = "Show";
    static public final String ACTION_SELECT = "Select";
    
    /** condition constants **/
    static public final int CONTAINS = 0;
    static public final int IS = 1;
    static public final int STARTS = 2;
    static public final int ENDS = 3;
    
    /** property source types **/
    static public final int ANYWHERE = 0;
    static public final int LABEL = 1;
    static public final int NOTES = 2;
    static public final int USERTYPE= 3;
    static public final int METADATA = 4;
    static public final int USERDATA = 5;
    
    /////////////
    // Fields
    //////////////
    
    /** a vector of contraints ??? still used ??? **/
    LogicalStatement [] mStatements = null;
    
    Vector  statements = null;
    
    /** the logical op state **/
    boolean mIsAny = false;
    
    /** the filter action **/
    private Object mFilterAction = ACTION_SHOW;
    
    /** is the matching inverse of the logical?  A logical NOT **/
    private boolean mIsLogicalNot = false;
    
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
    
    /**
     * @param Vector - a Vector of FilterStatementEdtiors
     **/
    public LWCFilter( Vector pEditors) {
        this();
        if( (pEditors != null) && ( !pEditors.isEmpty() ) ) {
            
            mStatements = new LogicalStatement[ pEditors.size() ];
            
            for( int i=0; i< pEditors.size(); i++) {
                FilterStatementEditor fse = (FilterStatementEditor) pEditors.elementAt( i);
                mStatements[i] = fse.getStatement();
            }
        }
        
    }
    
    public LWCFilter( LWMap pMap) {
        this();
        setMap( pMap);
    }
    
    ////////////////////
    // Methods
    ///////////////////
    
    /**
     * setFilterOn
     * Sets the filter to be on or off
     * @param boolean true if on; false if off
     **/
    public void setFilterOn( boolean pIsOn) {
        mIsFilterOn = pIsOn;
    }
    
    /**
     * isFilterOn
     * @return true if on; false if off
     **/
    public boolean isFilterOn() {
        return mIsFilterOn;
    }

    /** @return true if the action of this filter has a persistent effect on the map
     * e.g.: SHOW/HIDE are persistent in that items remained in a changed state as long
     * as the filter is in effect, where as the SELECT action has no persistent effect:
     * it's just a one-time change of the selection */
    public boolean hasPersistentAction() {
        return mFilterAction != ACTION_SELECT;
    }
    
    public void setFilterAction(Object action) {
        if (action != ACTION_HIDE && action != ACTION_SHOW && action != ACTION_SELECT)
            throw new IllegalArgumentException(this + " illegal filter action: " + action);
        mFilterAction = action;
    }
    public Object getFilterAction() {
        return mFilterAction;
    }
    
    
    public void setStatements(Vector statements) {
        this.statements = statements;
    }
    public Vector getStatements() {
        return this.statements;
    }
    public void removeStatements(Key key) {
        Vector removeStatements = new Vector();
        Iterator i = statements.iterator();
        while(i.hasNext()) {
            Statement statement = (Statement)i.next();
            if(((String)statement.getKey().getKey()).equals(key.getKey().toString()))
                removeStatements.add(statement);
        }
        this.statements.removeAll(removeStatements);
    }
    
        /*
         * isSelecting
         * @return true if filter should select items on apply; false if not
         
        public boolean isSelecting() {
            return mFilterAction == ACTION_SELECT;
        }
         
        /**
         * isFiltering
         * @return true iff should hide or filter matches; false if not
         
        public boolean isFiltering() {
            return mFilterAction == ACTION_HIDE;
        }
         
        /**
         * setFiltering
         * @param boolean true if should filter; false if not and should select
         
        public void setIsFiltering( boolean pState) {
                mIsFiltering = pState;
        }
         **/
    
    /**
     * isLogicalNot
     * @return true if filter should be !isMatch; false if normal
     **/
    public boolean isLogicalNot() {
        return mIsLogicalNot;
    }
    
    /**
     * setLogicalNot
     * @param boolean true if filter results should be logical NOT of results.
     **/
    public void setLogicalNot( boolean pState) {
        mIsLogicalNot = pState;
    }
    
    /**
     * setMap
     * @param LWMap = the map that this filter applies to
     **/
    public void setMap( LWMap pMap) {
        mMap = pMap;
    }
    
    /**
     * getMap
     * @return LWMap the map for this filter
     **/
    public LWMap getMap() {
        return mMap;
    }
    
    /**
     * setIsAny
     * @param boolean true if match applies to any match of a logical statement
     *  false if ALL matches required for match
     **/
    public void setIsAny( boolean pIsAny) {
        mIsAny = pIsAny;
    }
    public boolean getIsAny() {
        return mIsAny;
    }
    
    public LogicalStatement createLogicalStatement() {
        return new LogicalStatement();
    }
    
    /**
     * setLogicalStatements
     * @param LogicalStatements [] the set of logical statements
     **/
    public void setLogicalStatements(  LogicalStatement [] pArray ) {
        mStatements = pArray;
    }
    /**
     * getLogicalStatements
     * @return the set of logical statements
     **/
    public LogicalStatement [] getLogicalStatements() {
        return mStatements;
    }
    
    /*
     *isMatch
     *this method checks if the componenent matches complex logic. It is not enabled in current version of VUE.
     *@param LWComponent to be checked.
     *@return true if matches the criteria
     */ 
    
    public boolean isMatch( LWComponent pLWC ) {
        
        // if any, we'll assume FALSE
        // if ALL, we'll assume TRUE
        boolean matched = !mIsAny;
        /**
         * for(int i=0; i< mStatements.length; i++ ) {
         * LogicalStatement ls = mStatements[ i];
         * boolean result = ls.isMatch( pLWC);
         *
         * if( mIsAny && result) {
         * // if looking for ANY and MATCH return true
         * return true;
         * }
         * if(  (!mIsAny) && (!result) ) {
         * // if it's ALL and no match, return false
         * return false;
         * }
         * }
         **/
        
        Iterator i = statements.iterator();
        while(i.hasNext()) {
            Statement statement = (Statement) i.next();
            boolean result = matchStatementComponent(statement,pLWC);
            
            if( mIsAny && result) {
                // if looking for ANY and MATCH return true
                return true;
            }
            if(  (!mIsAny) && (!result) ) {
                // if it's ALL and no match, return false
                return false;
            }
        }
        
        return matched;
    }
    
    private boolean matchStatementComponent(Statement statement, LWComponent pLWC) {
        if(statement.getKey().getKey().toString().equals("Anywhere"))
            return matchLabel(statement, pLWC) | matchNotes(statement, pLWC);
        else if(statement.getKey().getKey().toString().equals("Label")) {
            return  matchLabel(statement, pLWC);
        }  else if(statement.getKey().getKey().toString().equals("Notes")) {
            return  matchNotes(statement, pLWC);
        } else  {
            return matchFilter(statement,pLWC);
        }
        
        
    }
    
    
    
    private boolean matchLabel(Statement statement, LWComponent pLWC) {
        Statement labelStatement = new Statement("Label", pLWC.getLabel());
        return  labelStatement.compare(statement);
    }
    
    private boolean matchNotes(Statement statement, LWComponent pLWC) {
        Statement notesStatement = new Statement("Label", pLWC.getNotes());
        return  notesStatement.compare(statement);
    }
    
    private boolean matchFilter(Statement statement,LWComponent pLWC) {
        return pLWC.getNodeFilter().compare(statement);
    }
    
    public String toString() {
        return "LWCFilter[" + mFilterAction
            + " on=" + mIsFilterOn
            //            + " mStatements=" + (mStatements==null?"null":Arrays.asList(mStatements).toString())
            + " statements=" + statements
            + "]";
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
            
            switch (getSourceType()) {
                case ANYWHERE:  return matchAnywhere( pLWC);
                case LABEL:     return matchLabel( pLWC);
                case NOTES:     return matchNotes( pLWC);
                case USERTYPE:  return matchUserMapType( pLWC);
                case METADATA:  return matchMetaData( pLWC);
                case USERDATA:  return matchUserProperty( pLWC, false);
            }
            return state;
        }
        
        private boolean matchAnywhere( LWComponent pLWC) {
            boolean isMatch = false;
            
            if( matchLabel( pLWC) )
                return true;
            if( matchNotes( pLWC) )
                return true;
            if( matchUserMapType( pLWC) )
                return true;
            if( matchMetaData( pLWC) )
                return true;
            if( matchUserProperty( pLWC, true) )
                return true;
            
            return isMatch;
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
            
            
            UserMapType type = pLWC.getUserMapType();
            if( type != null) {
                if( pCheckAll ) {
                    
                }
                else {
                    
                }
            }
            
            return state;
        }
        
        private boolean matchUserMapType( LWComponent pLWC) {
            boolean state = false;
            
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
            
            if (pStr == null || mValue == null)
                return false;
            
            if (DEBUG) debug(" ...checking( "+pStr+" } to value: "+ mValue);
            
            String search = pStr.toLowerCase();
            String filter = mValue.toLowerCase();
            
            switch (getCondition()) {
                case CONTAINS:  return search.indexOf(filter) != -1;
                case IS:        return search.equals(filter);
                case STARTS:    return search.startsWith(filter);
                case ENDS:      return search.endsWith(filter);
            }
            
            // should never happen
            throw new IllegalStateException(this + " unknown condition " + getCondition());
        }
        
        
        public String toString() {
            return "LogicalStatement[Source: "+mSourceType+" condition: "+mCondition+" value: "+mValue + "]";
        }
    }
    private static final boolean DEBUG  = false;
    protected void debug( String str) {
        if (DEBUG) System.out.println(" -> "+str);
    }
}
