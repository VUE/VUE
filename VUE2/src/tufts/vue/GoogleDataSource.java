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
package tufts.vue;

/*
 * GoogleDataSource.java
 *
 * Created on October 15, 2003, 5:28 PM
 */


import javax.swing.*;
import java.net.URL;
/**
 *
 * @author  rsaigal
 */


public class GoogleDataSource extends VueDataSource {
    
    private String site;
    private String client;
    private String url;
    
    public GoogleDataSource(){
        
    }
    public GoogleDataSource(String DisplayName, String address) throws DataSourceException{ 
        this.setDisplayName(DisplayName);
        this.setAddress(address);
    }
    
    public GoogleDataSource(String displayName,String url,String site,String client)  throws DataSourceException{
        this.client = client;
        this.site =site;
        this.url = url;
        this.setDisplayName(displayName);
        setAddress(createAddress());
        
    }
    
    @Override
    public javax.swing.JComponent buildResourceViewer() {
        return new TuftsGoogle(getDisplayName(), getAddress());
    }
    
    public String getUrl() {
        return this.url;
    }
    public void setUrl(String url)  throws DataSourceException {
        this.url = url;
        setAddress(createAddress());
    }
    public String getSite() {
        return this.site;
    }
    public void setSite(String site)  throws DataSourceException{
        this.site = site;
        setAddress(createAddress());
    }
    public String getClient() {
        return this.client;
    }
    public void setClient(String client)  throws DataSourceException{
        this.client = client;
        setAddress(createAddress());
    }
    
    
    private String createAddress() {
        if((url != null) ) {
            if((url.indexOf("http://") >=0 && url.length() > 7))
                url = url.substring(7);
        } else {
            url ="";
        }
        String address = "http://"+url+"/search?site="+site+"&client="+client+"&output=xml_no_dtd";
        return address;
    }
    
    
    
}













