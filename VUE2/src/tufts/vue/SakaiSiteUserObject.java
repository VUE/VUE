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

public class SakaiSiteUserObject
{
    private String id = null;
    private String displayName = null;

    public void setId(String id)
    {
        this.id = id;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public String getId()
    {
        return this.id;
    }

    public String toString()
    {
        return this.displayName;
    }
}