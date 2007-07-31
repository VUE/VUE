
/*
 *
 * * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003-2007
 * Tufts University. All rights reserved.</p>
 *
 *
 */

package edu.tufts.vue.metadata.action;

import java.awt.event.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

import tufts.vue.*;

/*
 * SearchAction.java
 *
 * Created on July 27, 2007, 2:21 PM
 *
 * @author dhelle01
 */
public class SearchAction extends AbstractAction
{
    
  private List<List<URI>> finds = null;
 
  private List<String> tags; 
  private List<LWComponent> comps;
  
  private JTextField searchInput;
  
  //public final String name = "Search";
  
        public SearchAction(JTextField searchInput)
        {
            super("Search");
            this.searchInput = searchInput;
        }
        
        public void loadKeywords(String searchString)
        {
            tags = new ArrayList<String>();
            String[] parsedQuotes = searchString.split("\"");
            for(int i=0;i<parsedQuotes.length;i++)
            {
                if(i%2 == 0)
                {
                    tags.add(parsedQuotes[i]);
                }
                else
                {
                    String[] parsedSpaces = parsedQuotes[i].split(" ");
                    for(int j=0;j<parsedSpaces.length;j++)
                    {
                      tags.add(parsedSpaces[j]);  
                    }
                }
            }
            
        }
        
        public void performSearch()
        {
          finds = new ArrayList<List<URI>>();
          
          List<URI> found = null;

          for(int i=0;i<tags.size();i++)
          {
             found = edu.tufts.vue.rdf.RDFIndex.getDefaultIndex().search(tags.get(i));
             finds.add(found);
          }     
                    
          Iterator<List<URI>> findsIterator = finds.iterator();
               
          comps = new ArrayList<LWComponent>();
               
          while(findsIterator.hasNext())
          {
            found = findsIterator.next();
            if(found !=null)
            {
              Iterator<URI> foundIterator = found.iterator();
              while(foundIterator.hasNext())
              {
                URI uri = foundIterator.next();
                LWComponent r = (LWComponent)edu.tufts.vue.rdf.VueIndexedObjectsMap.getObjectForID(uri);
                if(r!=null && r.getMap().equals(VUE.getActiveMap()))
                {
                   comps.add(r);
                }
              }
             }
           }
        }
        
        public String getName()
        {
            return "Search";
        }
        
        public void actionPerformed(ActionEvent e)
        {
            loadKeywords(searchInput.getText());
            performSearch();
            VUE.getSelection().setTo(comps.iterator());
        }
}
