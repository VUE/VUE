/*******
 **  LWCFilter
 **
 **
 *********/

package tufts.vue;


import java.io.*;
import java.util.*;
import tufts.vue.beans.*;
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
    
    /** a vector of contraints **/
    LogicalStatement [] mStatements = null;
    
    Vector  statements = null;
    
    /** the logical op state **/
    boolean mIsAny = false;
    
    /** the filter action **/
    private Object mFilterAction = ACTION_SHOW;
    
    /** is the matching inverse of the logical?  A logical NOT **/
    private boolean mIsLogicalNot = false;
    
    /** is the map filter currenlty on **/
    boolean mIsFilterOn = true;
    
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
    
    
    /**
     * applyFilter
     * Applies this filter to the map.  It uses
     * the isFiltering, isSelecting, isLogicalNot, isAny to
     * constuct the terms and cations for logical expression.
     * isFiltering causes Map items to be hidden or shown
     * isSelecting causes the current selection to change
     * isLogicalNot inverses the result set
     * isAny is a logical OR, !isAny is a logcal AND for statements
     *
     **/
    public void applyFilter() {
        
        if( mMap == null) {
            return;
        }
        if (DEBUG) debug("LWCFilter.applyFilter()");
        
        if (getFilterAction() == ACTION_SELECT)
            VUE.getSelection().clear();
        
        java.util.List list = mMap.getAllDescendents();
        Iterator it = list.iterator();
        while (it.hasNext()) {
            LWComponent c = (LWComponent) it.next();
            if( (c instanceof LWNode) || (c instanceof LWLink) ) {
                boolean state = isMatch( c);
                if (DEBUG) debug("  FINAL: "+c.getLabel()+"  is  "+ state  );
                if( isLogicalNot() ) {
                    state = !state;
                }
                if (getFilterAction() == ACTION_HIDE)
                    c.setIsFiltered(state);
                else if (getFilterAction() == ACTION_SHOW)
                    c.setIsFiltered(!state);
                else if (getFilterAction() == ACTION_SELECT) {
                    if (state)
                        VUE.getSelection().add(c);
                }
            }
        }
        
        // repaint
        mMap.notify(this, "repaint");
        
    }
    
    
    /**
     * showComponent
     * This method returns true if the the component should be filtered
     *
     * @return boolean true - if this should be shown; false if filtered
     **/
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
            return "LWCFilter[Source: "+mSourceType+" condition: "+mCondition+" value: "+mValue + "]";
        }
    }
    private static final boolean DEBUG  = false;
    protected void debug( String str) {
        if (DEBUG) System.out.println(" -> "+str);
    }
}
