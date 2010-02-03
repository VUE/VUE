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


/*
 * Publishable.java
 *
 * Created on June 27, 2004, 12:27 PM
 *
 *The Interface defines methods to publish to a data source and the types of publish modes.
 */

package tufts.vue;

/**
 *
 * @author  akumar03
 * @version $Revision: 1.7 $ / $Date: 2010-02-03 19:17:40 $ / $Author: mike $
 */
import java.io.IOException;
public interface Publishable {
      
    public static final int PUBLISH_NO_MODES = 0;;
    public static final int PUBLISH_MAP = 1; // just the map
    public static final int PUBLISH_CMAP = 2; // the map with selected resources in IMSCP format
    public static final int PUBLISH_ALL = 3; // all resources published to fedora and map published with pointers to resources.
    public static final int PUBLISH_SAKAI = 4; // an IMSCP to Sakai
    public static final int PUBLISH_ALL_MODES = 10; // this means that datasource can publish to any mode.
    public static final int PUBLISH_ZIP = 5; 
    /*
     */
    public int[] getPublishableModes();
    
    public boolean supportsMode(int mode);
    public void publish(int mode,LWMap map) throws IOException;
        
    
}
