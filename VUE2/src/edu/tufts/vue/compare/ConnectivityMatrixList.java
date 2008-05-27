
/*
 * Copyright 2003-2007 Tufts University  Licensed under the
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
        
    public void addLinkSourceMapMetadata(String node1,String node2,tufts.vue.LWLink link)
    {
           for(int i=0;i<size();i++)
           {
               E cm = get(i);
               if(cm instanceof ConnectivityMatrix && ((ConnectivityMatrix)cm).getConnection(node1,node2) > 0)
               {
                   String sourceLabel = ((ConnectivityMatrix)cm).getMap().getLabel();
                   
                   edu.tufts.vue.metadata.VueMetadataElement vme = new edu.tufts.vue.metadata.VueMetadataElement();
                   vme.setType(edu.tufts.vue.metadata.VueMetadataElement.OTHER);
                   // todo: likely we are going to want to the find the label(s) associated with this link
                   // will probably have to search map??
                   //vme.setObject("source: " + comp.getMap().getLabel() + "," + sourceLabel);
                   vme.setObject("source: " + sourceLabel);
                   link.getMetadataList().getMetadata().add(vme);
               }
           }
    }    
    
}
