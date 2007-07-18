package edu.tufts.vue.editor.text.util;

import java.awt.Component;
import javax.swing.JOptionPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import java.util.Enumeration;
import javax.swing.text.StyleConstants;

public class VueRichTextUtils {

  public static final String pct = "%";
  public static final String pt = "pt";
  public static final String px = "px";
  private static final String ERROR_TITLE = "Error Occurred";
  private static String unit = "";

  public VueRichTextUtils() {
  }

  //unravels nested attributes 
  public static AttributeSet resolveAttributes(AttributeSet style) {
    SimpleAttributeSet set = new SimpleAttributeSet();
    if(style != null) {
      Enumeration names = style.getAttributeNames();
      Object value;
      Object key;
      while(names.hasMoreElements()) {
        key = names.nextElement();
        value = style.getAttribute(key);
        
        if( (!key.equals(StyleConstants.NameAttribute)) &&
            (!key.equals(StyleConstants.ResolveAttribute)) &&
            (!key.equals(AttributeSet.ResolveAttribute)) &&
            (!key.equals(AttributeSet.NameAttribute)))
        {
          set.addAttribute(key, value);
        }
        else {
          if(key.equals(StyleConstants.ResolveAttribute) ||
             key.equals(AttributeSet.ResolveAttribute)) {        
            set.addAttributes(resolveAttributes((AttributeSet) value));
          }
        }
      }
    }
    return set;
  }

    public static float getSizeInPoints(String valStr) {
    float len = 0;
    int pos = valStr.indexOf(pt);
    if(pos > -1) {
      unit = pt;
      valStr = valStr.substring(0, pos);
      len = Float.valueOf(valStr).floatValue();
    }
    else {
      pos = valStr.indexOf(px);
      if(pos > -1) {
        unit = px;
        valStr = valStr.substring(0, pos);
        len = Float.valueOf(valStr).floatValue() * 1.3f;
      }
      else {
        pos = valStr.indexOf(pct);
        if(pos > -1) {
          unit = pct;
          valStr = valStr.substring(0, pos);
          len = Float.valueOf(valStr).floatValue() / 100f;
        }
        else {
          try {
            len = Float.valueOf(valStr).floatValue();
            unit = pt;
          }
          catch(Exception e) {
            e.printStackTrace();
          }
        }
      }
    }
    return len;
  }

  public static String getLastAttributeSizeUnit() {
    return unit;
  }

  public static float getAttrValue(Object attr) {
    float val = -1;
    if(attr != null) {
      val = getSizeInPoints(attr.toString());
    }
    return val;
  }

   //These are ripped right from the java forums pretty much.
  public static Element findElementInParent(String name, Element start) {
    Element elem = start;
    while((elem != null) && (!elem.getName().equalsIgnoreCase(name))) {
      elem = elem.getParentElement();
    }
    return elem;
  }

  public static Element findElementInChild(String name, Element parent) {
    Element foundElement = null;
    ElementIterator eli = new ElementIterator(parent);
    Element thisElement = eli.first();
    while(thisElement != null && foundElement == null) {
      if(thisElement.getName().equalsIgnoreCase(name)) {
        foundElement = thisElement;
      }
      thisElement = eli.next();
    }
    return foundElement;
  }

  public static void showErrorDialog(Component owner, String msg, Exception e) {
    if(msg != null) {
      JOptionPane.showMessageDialog(owner, msg, ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
    }
    if(e != null) {
      e.printStackTrace();
    }
  }
}