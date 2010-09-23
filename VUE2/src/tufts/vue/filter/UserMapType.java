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

package tufts.vue.filter;

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
	Vector mProperties = new Vector();
	
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
	
	public UserMapType( UserMapType pType) {
		mID = pType.getID();
		mName = pType.getDisplayName();
		UserProperty [] props = pType.getUserProperties();
		if( props != null) {
			UserProperty [] newProps = new UserProperty[ props.length ];
			for( int i=0; i<props.length; i++) {
				newProps[i] = new UserProperty( props[i] );
				}
			setUserProperties( newProps);
			}
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
	 * createUserProperty
	 * Creates a user Property with the given display name
	 * @param String the display name
	 **/
	public UserProperty createUserProperty( String pName) {
		String id = makeUserPropertyID( pName);
		
		UserProperty prop = new UserProperty( id, pName);
		return prop;	
	}
	
	
	private String makeUserPropertyID( String pName) {

		if( pName == null) {
			pName = "userProperty";
			}
		UserProperty [] types = getUserProperties() ;
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
		if( pMap == null)
			return null;
			
		return findUserMapType( pMap.getUserMapTypes(), pID);
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
	
	/**
	 * getAsHTML
	 * Returns a string formated in HTML format of the meta data
	 * @return an HTML body string 
	 *
	 * NOTE:  FIX: needs work
	 *
	 */
	public String getAsHTML( Map pValues) {
		String str = "Metadata: <p>" ;
		
		Iterator it = pValues.keySet().iterator();
		while( it.hasNext() ) {
			Object key = it.next();
			Object value = pValues.get( key);
			if( value != null) {
				str = str + " <br> "+key.toString()+" - "+value.toString() ;
				}
			}
		return str;
	}
	
	public String toString() {
		String str = getDisplayName();
		
		if( str == null) {
			str = super.toString();
			}
		return str;
	}

	
}
