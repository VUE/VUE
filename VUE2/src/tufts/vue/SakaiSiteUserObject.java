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
package tufts.vue;

public class SakaiSiteUserObject
{
    private String id = null;
    private String displayName = null;
    private org.osid.repository.Asset asset = null;
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
     public void setAsset(org.osid.repository.Asset  asset)
    {
        this.asset  = asset;
    }
    public org.osid.repository.Asset  getAsset()
    {
        return this.asset;
    }
    
    public String toString()
    {
        return this.displayName;
    }
    
    
}