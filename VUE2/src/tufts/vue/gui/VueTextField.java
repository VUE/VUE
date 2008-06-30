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
/*
 * VueTextField.java
 *
 * Created on June 30, 2004, 11:02 AM
 *
 *This is a gui class for rendering TextField in VUEComponents
 */


package tufts.vue.gui;

/**
 *
 * @author  akumar03
 */
import tufts.vue.DEBUG;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.Border;

public class VueTextField extends JTextField
    implements MouseListener, FocusListener
               //, java.beans.PropertyChangeListener, CaretListener
{
    public static final String ActiveBorderKey = "VueActiveBorder";
    public static final String InactiveBorderKey = "VueInactiveBorder";
    
    public static final int LENGTH = 42;
    private String longText ="";
    
    public VueTextField() {
        this("");
    }
    public VueTextField(String text) {
        this(text, text.length() + 2);
    }
    public VueTextField(String text, int length) {
        super(text, length);
        setToolTipText(null);
        //addMouseListener(this);
        addFocusListener(this);

        setDragEnabled(false); // keep things simple for now

        // background -- appears to work only when manually calling
        // getCaret().setSelectionVisible as we do in our hack.
        //setSelectionColor(Color.red);
        
        //setSelectedTextColor(Color.red); // text foreground
        
        //addPropertyChangeListener(this);
        //addCaretListener(this);
    }

    public Container X_getFocusCycleRootAncestor() {
        tufts.Util.printStackTrace(this + " getFocusCycleRootAncestor");
        Container r = super.getFocusCycleRootAncestor();
        System.err.println("FOCUS-ANCESTOR " + r
                           + "\n\t  isShowing: " + r.isShowing()
                           + "\n\tisFocusable: " + r.isFocusable()
                           + "\n\t  isEnabled: " + r.isEnabled());
	return r;
    }

    public boolean X_requestFocus(boolean temporary) {
        tufts.Util.printStackTrace(this + " requestFocus, temporary=" + temporary + " isFocusable=" + isFocusable());
        return super.requestFocus(temporary);
    }


    public void X_setCaretPosition(int p) {
        tufts.Util.printStackTrace(this + " setCaretPosition " + p);
        super.setCaretPosition(p);
    }
    
    public void X_moveCaretPosition(int p) {
        tufts.Util.printStackTrace(this + " moveCaretPosition " + p);
        super.moveCaretPosition(p);
    }

    public void X_select(int start, int end) {
        tufts.Util.printStackTrace(this + " select, start=" + start + " end=" + end);
        super.select(start, end);
    }

    public void X_paint(Graphics g) {
        tufts.Util.printStackTrace("paint");
        //System.out.println(this + " paint");
        super.paint(g);
    }

    public void X_caretUpdate(CaretEvent e) {
        System.out.println(this + " " + e);
        getCaret().setVisible(true);
        
    }

    public void X_propertyChange(java.beans.PropertyChangeEvent e) {
        System.out.println(this + " " + e);
    }

    public void X_setBorder(Border border) {
        //tufts.Util.printStackTrace("setBorder " + border);
        super.setBorder(border);
    }
                                          
    
    public void mousePressed(MouseEvent e) {
        System.out.println(this + " " + e);
        //requestFocus();
    }
    public boolean isFocusable() {
	return true;
    }

    public void setText(String text) {
        this.longText = text;
        if(text.length() > LENGTH) {
            super.setText(text.substring(0,LENGTH)+"...");
            super.setEditable(false);
            super.setOpaque(false);
            Border b = (Border) getClientProperty(InactiveBorderKey);
            if (b != null)
                setBorder(b);
            this.addMouseListener(this);
            this.addFocusListener(this);
        }else {
            super.setText(text);
            super.setEditable(true);
        }
        super.setToolTipText(longText);
        
    }
    
    public void mouseClicked(MouseEvent e) {
        if(longText.length() > LENGTH) {
            if((e.getButton() == MouseEvent.BUTTON1) && (e.getClickCount() == 2)) {
                unlock();
                super.setText(longText);
            }
        }
    }

    
    private void unlock() {
        System.out.println(this + " is unlocking");
        Border b = (Border) getClientProperty(ActiveBorderKey);
        if (b != null) {
            //System.out.println("found native border " + b);
            setBorder(b);
        }
        setFocusable(true);
        setEditable(true);
        setOpaque(true);
        //repaint();
        requestFocus();
    }

    
    public void mouseEntered(MouseEvent e) {
    }
    
    public void mouseExited(MouseEvent e) {
    }
    
    public void mouseReleased(MouseEvent e) {
    }
    
    public void focusGained(FocusEvent e) {
        //tufts.Util.printStackTrace(this + " focusGained: hasFocus=" + hasFocus() + " " + e);
        if (DEBUG.FOCUS) System.out.println("\t" + this + " focusGained: hasFocus=" + hasFocus());
    }
    
    public void focusLost(FocusEvent e) {
        //tufts.Util.printStackTrace(this + " focusLost: hasFocus=" + hasFocus() + " " + e);
        if (DEBUG.FOCUS) System.out.println("\t" + this + " focusLost: hasFocus=" + hasFocus());
        //if(super.isEditable())
        //    setText(this.getText());
    }

    public String toString() {
        String text = "<uninitialized>";
        try {
            text = getText();
        } catch (Exception e) {}
        return "VueTextField@" + Integer.toHexString(hashCode()) + "[" + text + "]";
    }
    
}



