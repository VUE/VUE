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
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class VueTextField extends JTextField implements MouseListener,FocusListener{
    
    public static final int LENGTH = 42;
    private String longText ="";
    /** Creates a new instance of VueTextField */
    
    public VueTextField() {
        super();
    }
    public VueTextField(String text) {
        super();
        setText(text);
        
    }
    
    public void setText(String text) {
        this.longText = text;
        if(text.length() > LENGTH) {
            super.setText(text.substring(0,LENGTH)+"...");
            super.setEditable(false);
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
                super.setText(longText);
                super.setEditable(true);
            }
        }
    }
    
    public void mouseEntered(MouseEvent e) {
    }
    
    public void mouseExited(MouseEvent e) {
    }
    
    public void mousePressed(MouseEvent e) {
    }
    
    public void mouseReleased(MouseEvent e) {
    }
    
    public void focusGained(FocusEvent e) {
    }
    
    public void focusLost(FocusEvent e) {
        setText(this.getText());
    }
    
}



