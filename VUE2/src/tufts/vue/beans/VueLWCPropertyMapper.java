
package tufts.vue.beans;


import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.beans.*;
import javax.swing.*;
import tufts.vue.*;

/** This class describes a DynamicPropertyMapper: an object that
* can get and set dynamic properties of dynamic beans belonging to
* Java classes or interfaces for which it has registered.
*
*
**/

public class VueLWCPropertyMapper
    implements VuePropertyMapper
{
    static final String [] sNodeProperties = {  LWKey.FillColor,
                                                LWKey.StrokeColor,
                                                LWKey.TextColor,
                                                LWKey.StrokeWidth,
                                                LWKey.Font
    };
												
    static final String  []  sLinkProperties = {  LWKey.StrokeColor,
                                                  LWKey.StrokeWidth,
                                                  LWKey.TextColor,
                                                  LWKey.LinkArrows,
                                                  LWKey.Font
    };
												
    static final String [] sTextProperties = {  LWKey.TextColor, LWKey.Font };
												
	
    ////////////
    // Fields
    /////////////
	
    VueBeanInfo mLWCInfo = null;
    VueBeanInfo mNodeInfo = null;
    VueBeanInfo mLinkInfo = null;
	
	
	//////////////////
	// VUePropertyMapper Interface
	//////////////////
	
	/**
	 * getPropertyValue
	 * Gets the value of the property from the specified object.
	 * @param Object - the object
	 * @param String name
	 * @return Object the value
	 **/
	public Object getPropertyValue(Object pBean, String pName)  {
		Object value = null;
		
		if(  pBean instanceof LWComponent) {
			LWComponent obj = (LWComponent) pBean;
			
			if( pName.equals(LWKey.FillColor) ) {
				return obj.getFillColor();
				}
			if( pName.equals(LWKey.StrokeColor) ) {
				return obj.getStrokeColor();
				}
			if( pName.equals(LWKey.TextColor) ) {
				return obj.getTextColor();
				}
			if( pName.equals(LWKey.StrokeWidth) ) {
				float f;
				Float width = new Float( obj.getStrokeWidth() );
				return width;
				}
			if( pName.equals(LWKey.Font) ) {
				return obj.getFont();
				}
			
			if( obj instanceof LWLink ) {
				LWLink link = (LWLink) obj;

				
				if( pName.equals(LWKey.LinkArrows) ) {
					Integer state = new Integer( link.getArrowState() );
					return state;
					}
				}
			}
		else {
			// should never happen...
			System.out.println("Error - VueLWCPropertyMapper mapped to wrong class.");
			}
		
		return value;
	}
	
	

    /**
     * setPropertyValue
     * This sets the property with of the object with the passed value
     * @param OpObject - the object
     * @param String pName the proeprty name
     * @param pValue - the value of the named property
     **/
    public void setPropertyValue(Object pBean, String pName, Object pValue)   {

        if (pBean instanceof LWComponent) {
            setProperty((LWComponent)pBean, pName, pValue);
        } else 
            throw new IllegalArgumentException("VueLWCPropertyMapper: can't handle class " + pBean + " name=" + pName + " val=" + pValue);
    }

    public static void setProperty(LWComponent c, Object key, Object val)
    {
        if (DEBUG.UNDO&&DEBUG.META) System.out.println("setProperty [" + key + "] on " + c + " with " + val);
                                           
             if (key == LWKey.FillColor)        c.setFillColor( (Color) val);
        else if (key == LWKey.TextColor)        c.setTextColor( (Color) val);
        else if (key == LWKey.StrokeColor)      c.setStrokeColor( (Color) val);
        else if (key == LWKey.StrokeWidth)      c.setStrokeWidth( ((Float) val).floatValue());
        else if (key == LWKey.Font)             c.setFont( (Font) val);
        else if (key == LWKey.Label)            c.setLabel( (String) val);
        else if (key == LWKey.Notes)            c.setNotes( (String) val);
        else if (key == LWKey.Resource)         c.setResource( (Resource) val);
        else if (key == LWKey.Location)         c.setLocation( (Point2D) val);
        else if (key == LWKey.Size) {
            // Point2D used as Size2D for now
            Point2D.Float p = (Point2D.Float) val;
            c.setSize(p.x, p.y);
        }
        else if (key == LWKey.LinkArrows) {
            if (c instanceof LWLink) {
                LWLink link = (LWLink) c;
                link.setArrowState(((Integer) val).intValue());
            }
        } else {
            System.out.println("Unknown key in setProperty: [" + key + "] with " + val + " on " + c);
            //new Throwable().printStackTrace();
        }
    }


    /**
     * getBeanInfo
     * Returns the VueBeanInfo for the object.
     * @param Object - the object
     * @return VueBeanInfo the info for the object.
     **/
    public VueBeanInfo getBeanInfo(Object pObject)  {
        VueBeanInfo beanInfo = null;
        if (pObject instanceof LWComponent)
            beanInfo = new LWCBeanInfo( (LWComponent) pObject);
        return beanInfo;
    }

	
	public class LWCBeanInfo implements VueBeanInfo {
		
		String [] mPropertyNames = null;
		VuePropertyDescriptor [] mDescriptors = null;
		Map mMap = null;
		
		
		LWCBeanInfo( LWComponent pLWC ) {
			
			if( pLWC instanceof LWNode) {
				//FIX:  add check for text node ehre.
				if( ((LWNode) pLWC).isTextNode() ) {
					mPropertyNames = sTextProperties;
					}
				else {
					mPropertyNames = sNodeProperties;
					}
				}
			else
			if( pLWC instanceof LWLink) {
				mPropertyNames = sLinkProperties;
			}
			
			if( mPropertyNames != null) {
				mDescriptors = new VuePropertyDescriptor[ mPropertyNames.length];
				mMap = new HashMap();
				VuePropertyDescriptor desc = null;
				for(int i=0; i<mPropertyNames.length; i++) {
					desc = createDescriptor( mPropertyNames[i]);
					mDescriptors[i] = desc;
					mMap.put( mPropertyNames[i], desc);
					}
				}
		}
		
		public VuePropertyDescriptor[] getPropertyDescriptors() {
	 		return mDescriptors;
	 	}


		public boolean hasProperty( String pName) {
	 		boolean hasKey = mMap.containsKey( pName);
	 	return hasKey;
	 	}
	  

		public String [] getPropertyNames() {
	 		return mPropertyNames;
	 	}


		public VuePropertyDescriptor getPropertyDescriptor( String pName) {
	 		return (VuePropertyDescriptor) mMap.get( pName);
	 	}


	 			
	
		
		private VuePropertyDescriptor createDescriptor( String pName) {
			VuePropertyDescriptor desc = null;
			String str = new String();
			Font font = new Font("Default",1,Font.PLAIN);
			Integer  i = new Integer(1);
			Color color = new Color(0,0,0);;
			Class theClass = null;
			
			if( pName.equals(LWKey.FillColor) ||
				pName.equals(LWKey.StrokeColor) ||
				pName.equals(LWKey.TextColor) ) {
				theClass = color.getClass();
				}
			else
			if( pName.equals(LWKey.StrokeWidth) ) {
				Float thefloat = new Float(0);
				theClass = thefloat.getClass();
			}
			else
			if( pName.equals(LWKey.LinkArrows) ) {
				
				theClass = i.getClass();
				}
			else
			if( pName.equals(LWKey.Font) ) {
				theClass = font.getClass();
				}
				
			desc = new VuePropertyDescriptor( pName, theClass, null);
			
			return desc;
		}
	}
	
	boolean sDebug = true;
	private void debug( String s) {
		if( sDebug)
			System.out.println( s);
	}
}






