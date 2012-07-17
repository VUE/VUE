
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

package edu.tufts.vue.compare;

/*
 * ConnectivityMatrixList.java
 *
 * Created on May 27, 2008, 10:22 AM
 *
 * @author dhelle01
 */
public class ConnectivityMatrixList<E> extends java.util.ArrayList<E> {
        
    private static final boolean DEBUG_LOCAL = false;
    
    public void addLinkSourceMapMetadata(Object key1, Object key2, tufts.vue.LWLink link)
    {
        
           for(int i=0;i<size();i++)
           {
               
               E cm = get(i);
               if(cm instanceof ConnectivityMatrix && ((ConnectivityMatrix)cm).getConnection(key1,key2) > 0)
               {
                   String sourceLabel = ((ConnectivityMatrix)cm).getMap().getLabel();
                   
                   edu.tufts.vue.metadata.VueMetadataElement vme = new edu.tufts.vue.metadata.VueMetadataElement();
                   vme.setType(edu.tufts.vue.metadata.VueMetadataElement.OTHER);
                   
                   java.util.Iterator<tufts.vue.LWLink> le = ((ConnectivityMatrix)cm).getMap().getLinkIterator();
                   
                   while(le.hasNext())
                   {
                       tufts.vue.LWLink currLink = le.next();
                       
                       if(DEBUG_LOCAL)
                       {
                           System.out.println("CML: current link --> " + currLink);
                           System.out.println("CML: current link label --> " + currLink.getLabel());
                       }
                       
                       tufts.vue.LWComponent head = currLink.getHead(); 
                       tufts.vue.LWComponent tail = currLink.getTail();
                       
                       if(DEBUG_LOCAL)
                       {
                           System.out.println("CML: found h " + head);
                           System.out.println("CML: found t " + tail);
                       }
                       
                       if(head == null || tail == null) // currently shouldn't be happening
                                                        // connectivity matrix only counts links with both
                                                        // but do nothing, just in case
                       {
                           
                       }
                       else
                       {
                          Object headMP = Util.getMergeProperty(head);
                          Object tailMP = Util.getMergeProperty(tail);
                          
                          int arrowState = currLink.getArrowState();
                          
                          boolean matches = false;
                          
                          //switch(arrowState) 
                          //{
                          //    case tufts.vue.LWLink.ARROW_HEAD:
                          //        matches = headMP.equals(key2) && tailMP.equals(key1);
                          //        break;
                          //    case tufts.vue.LWLink.ARROW_TAIL:
                          //        matches = headMP.equals(key1) && tailMP.equals(key2);
                          //        break;
                          //    case tufts.vue.LWLink.ARROW_BOTH: case tufts.vue.LWLink.ARROW_NONE:
                                  matches = (headMP.equals(key2) && tailMP.equals(key1)) ||
                                            (headMP.equals(key1) && tailMP.equals(key2)) ;
                          //        break;
                          //    default:
                          //        matches = false;

                          //}
                          
                          if(DEBUG_LOCAL)
                          {
                              System.out.println("CML: matches " + matches);
                          }
                          
                          if(matches)
                          {
                              String label = currLink.getLabel();
                              
                              //String label = currLink.getLabelBox().getText();
                              
                              if(DEBUG_LOCAL)
                              {
                                  System.out.println("CML -- there was a match -- label --> " + label);
                              }
                              
                              if(label != null)
                              {
                                  sourceLabel += "," + label;
                                  if(DEBUG_LOCAL)
                                  {
                                      System.out.println("CML: updating sourcelabel to: " + sourceLabel);
                                  }
                              }
                           
                       }
                       
                   }

               }
                   
               vme.setObject("source: " + sourceLabel);
               link.getMetadataList().getMetadata().add(vme);
           }
               
       }

    }    
    
}
