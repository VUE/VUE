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
import java.awt.*;

public class UserProperty {

	///////////////
	// Statics
	////////////////
	
	public static final int STRING = 1;
	public static final int INTEGER = 2;
	public static final int NUMBER = 3;
	public static final int TAGGED = 4;
	public static final int BOOLEAN = 5;
	
	public static String [] sPropertyTypeNames = {"String", "Integer","Number","Choice" };
	
	////////////
	// Fields
	/////////////
	
	/** the id **/
	String mID = null;
	
	/** display name **/
	String mName = null;
		
	/** arrayh of UserProperty **/
	int mType = 0;;
	
	/** value **/
	String mValue = null;
	
	/** tag array for tagged property **/
	private String [] mTags = null;
	
	
	////////////////
	// Constructors
	////////////////
	
	public UserProperty() {
		super();
	}
	
	public UserProperty( String pID, String pName) {
		mID = pID;
		mName = pName;
	}
	
	public UserProperty( UserProperty pProp) {
		mID = pProp.getID();
		mName = pProp.getDisplayName();
		mType = pProp.getType();
		mValue = pProp.getValue();
		String [] tags = pProp.getTaggedValues();
		mTags = null;;
		if( tags != null) {
			mTags = new String[tags.length];
			for(int i=0; i<tags.length; i++ ) {
				mTags[i] = tags[i];
				}
			}
	}
	
	
	
	//////////////
	// Methods
	//////////////

	
	public String getID() {
		return mID;
	}
	
	public void setID( String pID) {
		mID = pID;
	}


	/**
	 * sgetDisplayName
	 * Retruns the display name of the property
	 **/
	public String getDisplayName() {
		return mName;
	}	
	
	/**
	 * setDisplayName
	 * Sets teh display name.
	 **/
	public void setDisplayName( String pName) {
		mName = pName;
	}
	
	
	
	
	
	/** 
	 * getType
	 * Gets the property type
	 **/
	public int getType() {
		return mType;
	}
	
	/**
	 * setType
	 * Sets the proeprty type
	 **/
	public void setType( int pType) {
		mType = pType;
	}
	
	
	public void setValue( String pValue) {
		mValue = pValue;
	}
	
	public String getValue() {
		return mValue;
	}
	/**
	 * getTaggedValues
	 * Gets the tagged values
	 **/
	public String [] getTaggedValues() {
		return mTags;
	}
	
	/**
	 * setTaggedValues
	 * Sets the set of tagged values for a tagged property
	 **/
	public void setTaggedValues( String [] pTags) {
		mTags = pTags;
	}
	
	
	public Component getEditor() {
		
		Component c = null;
		
		switch ( mType) {
			case STRING:
				
				break;
			case INTEGER:
				break;
			
			default:
				break;
			
			}
		return c;
	}
	
	/**
	 * equals
	 * Override of java.lang.Object equals.  This method
	 * says that a UserProeprty is equal to another user property
	 * if teh ID is the same.
	 * Or, if the string id equals the mID.  This is
	 * for use in storing user properties in Vectors or Maps.
	 **/
	public boolean equals( Object pObj)  {
		boolean retValue = false;
		if( pObj instanceof UserProperty) {
			retValue = mID.equals( ((UserProperty) pObj).getID() );
			}
		else
		if( pObj instanceof String ) {
			retValue = mID.equals( (String) pObj);
			}
		return retValue;
			}

	static public int findTypeByName( String pName) {
		int type = STRING;
		if( pName != null) {
			for( int i=0; i< sPropertyTypeNames.length; i++) {
				if( pName.equals( sPropertyTypeNames[i]) )
					return i;
				}
			}
		return type;
	}
	
	public String toString() {
		String str = getDisplayName();
		if( str == null)
			str = super.toString();
		return str;
	}
}





