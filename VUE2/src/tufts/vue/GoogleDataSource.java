package tufts.vue;
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


public class GoogleDataSource extends VueDataSource{
    
    private String site;
    private String client;
    private String url;
    private JComponent resourceViewer;
    
    public GoogleDataSource(){
        
    }
    public GoogleDataSource(String DisplayName, String address){
        this.setDisplayName(DisplayName);
        this.setAddress(address);
        this.setResourceViewer();
    }
    
    public GoogleDataSource(String displayName,String url,String site,String client) {
        this.client = client;
        this.site =site;
        this.url = url;
        this.setDisplayName(displayName);
        setAddress(createAddress());
        
    }
    
    
    public void  setResourceViewer(){
        
        try{
            this.resourceViewer = new TuftsGoogle(this.getDisplayName(),this.getAddress());
            DataSourceViewer.refreshDataSourcePanel(this);
        }catch (Exception ex){VueUtil.alert(null,ex.getMessage(),"Error Setting Reseource Viewer");};
    }
    
    public JComponent getResourceViewer(){
        return this.resourceViewer;
    }
    public void setAddress(String address) {
        this.address = address;
        setResourceViewer();
    }
    
    public String getUrl() {
        return this.url;
    }
    public void setUrl(String url) {
        this.url = url;
        setAddress(createAddress());
    }
    public String getSite() {
        return this.site;
    }
    public void setSite(String site) {
        this.site = site;
        setAddress(createAddress());
    }
    public String getClient() {
        return this.client;
    }
    public void setClient(String client) {
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













