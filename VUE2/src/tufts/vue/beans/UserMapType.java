package tufts.vue.beans;


import java.lang.*;
import java.io.*;
import java.util.*;
import tufts.vue.*;

public class UserMapType {


	////////////
	// Fields
	/////////////
	
	/** the id **/
	String mID;
	
	/** display name **/
	String mName = null;
	
	/** arrayh of UserProperty **/
	Vector mProperties = null;
	
	////////////////
	// Constructors
	////////////////
	
	public UserMapType( LWMap pMap, String pName) {
		if( pName == null) {
			pName = "New Property";
			}
			
		mName = pName;
		
	}
	
	
	public UserMapType() {
		super();
	}
	
	
	
	//////////////
	// Methods
	//////////////

	/**
	 * getID
	 **/
	public void setID( String pID) {
		mID = pID;
	}
	
	/**
	 * getID
	 * Gets teh unique id for this type
	 **/
	public String	getID() {
		return mID;
	}
	
	/**
	 * setName
	 **/
	public void setDisplayName( String pName) {
		mName = pName;
	}
	
	/**
	 * getDisplayName
	 * returns teh displayname for teh type
	 **/
	public String getDisplayName() {
		return mName;
	}
	
	
	/**
	 * setUserProperties
	 **/
	public void setUserProperties( UserProperty [] pProperties ) {
		
		Vector v = new Vector();
		
		if( pProperties != null) {
			for( int i=0; i< pProperties.length; i++) {
				v.add( pProperties[i]);
				}
			}
		
		mProperties = v;
		
	}
	
	/**
	 * getProperties
	 * 
	 **/
	public UserProperty [] getUserProperties() {
		UserProperty [] retValue = null;
		
		if( (mProperties != null) && (!mProperties.isEmpty() )) {
			retValue = new UserProperty[ mProperties.size() ];
			for( int i=0; i<mProperties.size(); i++) {
				retValue[i] = (UserProperty) mProperties.elementAt( i);
				}
			}
		return retValue;
	}
	
	
	public void addUserProperty( UserProperty pProperty ) {
		mProperties.add( pProperty);
	}
	
	public void removeUserProperty( UserProperty pProp) {
		mProperties.remove( pProp);
	}
	
	
	/**
	 * nudgeUserProeprty
	 **/
	public void nudgeUserProperty( UserProperty pProp, boolean pUp) {
		Object swapper = null;
		int index = mProperties.indexOf( pProp);
		if( index < 0)
			return;
		if( (pUp) && (index > 0) ) {
			swapper = mProperties.elementAt( index - 1);
			mProperties.setElementAt( pProp, index-1);
			mProperties.setElementAt( swapper, index );
			}
		else
		if( index < (mProperties.size() -1)  ){
			swapper = mProperties.elementAt( index + 1);
			mProperties.setElementAt(pProp,  index + 1);
			mProperties.setElementAt( swapper, index);
			}
	}
	
	
	
	/**
	 * findUserPropertyByID
	 * Finds the UserProperty with the given ID.
	 * @param String id
	 * @return the UserProperty with id, or null if not found
	 **/
	public UserProperty findUserPropertyByID( String pID) {
		UserProperty property = null;
		if( (mProperties != null) && (!mProperties.isEmpty() ) ) {
			for( int i=0; ( (i< mProperties.size()) && (property == null)); i++ ) {
				if( pID.equals( ((UserProperty) mProperties.elementAt(i)).getID() ) ){
					property = (UserProperty) mProperties.elementAt(i);
					}	
				}
			}
		return property;
	}
	
	
	
	/**
	 * createPropertyID
	 * Creates a unique proeprty id based on the suggested name passed in.
	 **/
	public String createPropertyID( String pName) {
		if( pName == null) {
			pName = "userProperty";
			}
		String id = pName.trim();
		boolean done = false;
		if( findUserPropertyByID( id) != null) {
			int i = 0;
			String root = id;
			while( !done) {
				id = root + i;
				if( findUserPropertyByID( id) == null) {
					done = true;
					}
				i++;
				}
			}
		return id;
	}
	
	
	/**
	 * createMapTypeID
	 * Creates a unique proeprty id based on the suggested name passed in.
	 **/
	public String createMapTypeID( String pName, UserMapType [] pTypes) {
		if( pName == null) {
			pName = "mapType";
			}
		UserMapType [] types = pTypes;
		String id = pName.trim();
		boolean done = false;
		String root = pName;
		if( types != null) {
			int suffix = 0;
			while( !done) {
				boolean alreadyExists = false;
				for(int i=0; (!alreadyExists) && (i < types.length); i++) {
					alreadyExists = id.equals( types[i].getID() );
					}
				if( !alreadyExists ) {
					done = true;
					}
				else {
					suffix++;
					id = root + suffix;
					}
				}
			}
		return id;
	}
	
	static public UserMapType findUserMapType( LWMap pMap, String pID) {

            return null;
		//return findUserMapType( pMap.getUserMapTypes(), pID);
	}
	
	static public UserMapType findUserMapType( UserMapType [] pTypes, String pID) {

		if( (pTypes == null) || (pID == null) ) {
			return null;
			}
		for( int i=0; i< pTypes.length; i++) {
			if( pID.equals( pTypes[i].getID() ) ) {
				return pTypes[i];
				}
			}
		return null;
	}
	
	public String toString() {
		String str = getDisplayName();
		if( str == null) {
			str = super.toString();
			}
		return str;
	}
}
