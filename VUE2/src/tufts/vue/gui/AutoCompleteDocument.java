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

package tufts.vue.gui;

import javax.swing.text.*;
import javax.swing.*;

import tufts.vue.RecentlyOpenedUrlManager;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
 
public class AutoCompleteDocument extends PlainDocument {
 
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<String> dictionary = new ArrayList<String>();
    private JTextComponent comp;
    public static String  z;
  
    public AutoCompleteDocument( JTextComponent field, String[] aDictionary ) {
        comp = field;
        dictionary.addAll( Arrays.asList( aDictionary ) );
    }
 
    public void addDictionaryEntry( String item ) {
        dictionary.add( item );
    }
 
    public void insertString(int offs, String str, AttributeSet a) 
        throws BadLocationException {
        super.insertString( offs, str, a );
        String word = autoComplete( getText( 0, getLength() ) );
        if( word != null ) {
            super.insertString( offs + str.length(), word, a );
            comp.setCaretPosition( offs + str.length() );
            comp.moveCaretPosition( getLength() );
        }
    }
 
    public String autoComplete( String text ) {
        for( Iterator i = dictionary.iterator(); i.hasNext(); ) {
            String word = (String) i.next();
            if( word.startsWith( text ) ) {
                return word.substring( text.length() );
            }
        }
        return null;
    }
 
    /**
     * Creates a auto completing JTextField.
     *
     * @param dictionary an array of words to use when trying auto completion.
     * @return a JTextField that is initialized as using an auto 
     * completing textfield.
     */
    public static JTextField createAutoCompleteTextField( String[] dictionary ) 
    {
        JTextField field = new JTextField();
        AutoCompleteDocument doc = new AutoCompleteDocument( field,dictionary );
        field.setDocument(doc);
        return field;
    }
    
	public static void main(String args[]) 
	{
        javax.swing.JFrame frame = new javax.swing.JFrame("foo");
        frame.setDefaultCloseOperation( javax.swing.JFrame.EXIT_ON_CLOSE );
        String[] dict = { "Team", "meeting", "project", "review","client","call","orginization","team" };           
        JTextField field = AutoCompleteDocument.createAutoCompleteTextField( dict );
        BoxLayout layout = new BoxLayout( frame.getContentPane(),BoxLayout.X_AXIS );
        frame.getContentPane().setLayout( layout );
        frame.getContentPane().add( new javax.swing.JLabel("Text Field: ") );
        frame.getContentPane().add(field);
        frame.show();
    }
}
 
