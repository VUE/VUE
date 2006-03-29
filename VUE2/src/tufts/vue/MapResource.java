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



package tufts.vue;

/**
 *
 * @version $Revision: 1.44 $ / $Date: 2006-03-29 04:30:40 $ / $Author: sfraize $
 *
 * This class currently for backward compatability save files referencing "MapResource",
 * as well as old references in the codebase.
 */
public class MapResource extends URLResource
{
    public MapResource(String urlOrFile) {
        super(urlOrFile);
    }


    /** @deprecated - for persistance only */
    public MapResource() {}
    
    
    
}
