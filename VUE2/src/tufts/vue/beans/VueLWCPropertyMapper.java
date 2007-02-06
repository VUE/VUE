/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

package tufts.vue.beans;

import tufts.vue.*;

import java.util.*;
import java.beans.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** This class describes a DynamicPropertyMapper: an object that
* can get and set dynamic properties of dynamic beans belonging to
* Java classes or interfaces for which it has registered.
*
* This is still original S.B. code and could use reworking & better integration.
*
* Is this code still needed?
*
**/

public class VueLWCPropertyMapper
    implements VuePropertyMapper
{
    static final String [] sNodeProperties = {  LWKey.FillColor.name,
                                                LWKey.StrokeColor.name,
                                                LWKey.StrokeWidth.name,
                                                LWKey.TextColor.name,
                                                LWKey.Shape.name,
                                                LWKey.Font.name
    };
												
    static final String  []  sLinkProperties = {  LWKey.StrokeColor.name,
                                                  LWKey.StrokeWidth.name,
                                                  LWKey.TextColor.name,
                                                  LWKey.Font.name,
                                                  LWKey.LinkArrows.name,
                                                  LWKey.LinkCurves
    };
												
    static final String [] sTextProperties = {  LWKey.TextColor.name, LWKey.Font.name };
												
	
    //VueBeanInfo mLWCInfo = null;
    //VueBeanInfo mNodeInfo = null;
    //VueBeanInfo mLinkInfo = null;
	
	
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
    public Object getPropertyValue(Object pBean, String key)  {
		
        if (pBean instanceof LWComponent) {
            return ((LWComponent) pBean).getPropertyValue(key);
        } else {
            // should never happen...
            System.out.println(this + " getPropertyValue: unhandled class for " + key + " on " + pBean);
        }
        return null;
    }

    /*
    public static Object getPropertyValue(LWComponent c, String key)
    {
        if (key == LWKey.FillColor)         return c.getFillColor();
        if (key == LWKey.StrokeColor)       return c.getStrokeColor();
        if (key == LWKey.TextColor)         return c.getTextColor();
        if (key == LWKey.Font)              return c.getFont();
        if (key == LWKey.StrokeWidth)       return new Float(c.getStrokeWidth());
        if (key == LWKey.Shape)             return c.getShape();
        
        if (c instanceof LWLink) {
            LWLink link = (LWLink) c;
            if (key == LWKey.LinkArrows)
                return new Integer(link.getArrowState());
        }
        return null;
    }
    */
	
	

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
        c.setProperty(key, val);
        /*
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
        else if (key == LWKey.Hidden)           c.setHidden( ((Boolean)val).booleanValue());
        else if (key == LWKey.Size) {
            Size s = (Size) val;
            c.setSize(s.width, s.height);
        }
        //else if (key == LWKey.Size) {
          //  // Point2D used as Size2D for now
            //Point2D.Float p = (Point2D.Float) val;
            //c.setSize(p.x, p.y);
            //}
        else if (key == LWKey.Frame) {
            Rectangle2D.Float r = (Rectangle2D.Float) val;
            c.setFrame(r.x, r.y, r.width, r.height);
        }
        else if (key == LWKey.LinkArrows) {
            if (c instanceof LWLink) {
                LWLink link = (LWLink) c;
                link.setArrowState(((Integer) val).intValue());
            }
        } else {
            System.out.println("VueLWCPropertyMapper.setProperty: unknown key [" + key + "] with value [" + val + "] on " + c);
            //new Throwable().printStackTrace();
        }
        */
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
			
            if (pLWC instanceof LWNode) {
                // until we have time to clean up all this hairy property code,
                // treat text nodes just like the LWNode's they really are.
                if (((LWNode) pLWC).isTextNode())
                    mPropertyNames = sTextProperties;
                else
                    mPropertyNames = sNodeProperties;
            } else if( pLWC instanceof LWLink) {
                    mPropertyNames = sLinkProperties;
            }
			
            if (mPropertyNames != null) {
                mDescriptors = new VuePropertyDescriptor[mPropertyNames.length];
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
			
            if( pName.equals(LWKey.FillColor.name) ||
                pName.equals(LWKey.StrokeColor.name) ||
                pName.equals(LWKey.TextColor.name) ) {
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






