package tufts.vue.beans;


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
	
	
	////////////
	// Fields
	/////////////
	
	/** the id **/
	String mID;
	
	/** display name **/
	String mName = null;
		
	/** arrayh of UserProperty **/
	int mType;
	
	/** value **/
	String mValue;
	
	/** tag array for tagged property **/
	private String [] mTags = null;
	
	
	////////////////
	// Constructors
	////////////////
	
	
	
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

	public String toSTring() {
		String str = getDisplayName();
		if( str == null)
			str = super.toString();
		return str;
	}
}





