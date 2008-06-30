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

/*
 * CastorDR.java
 *
 * Created on October 21, 2003, 6:21 PM
 */

package  tufts.vue;

/**
 *
 * @author  akumar03
 */
import java.net.URL;
import osid.dr.*;
import tufts.oki.dr.fedora.*;
public class CastorDR {
    DR dr;
    String id;
    String displayName;
    String description;
    String address;
    String userName;
    String password;
    String conf;
    
    public CastorDR() {
    }
    
    public CastorDR(DR dr) {
        try {
            this.id = dr.getId().getIdString();
            this.displayName = dr.getDisplayName();
            this.description = dr.getDescription();
            this.address = dr.getAddress();
            this.userName = dr.getUserName();
            this.password  = dr.getPassword();
            this.conf = dr.getConf();
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    public DR getDR() {
        try {
            dr = new DR(conf,id, displayName, description,new URL("http",address,8080,"fedora/"),userName,password);
            return dr;
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    public void setId(String id) {
        this.id  = id;
    }
    public String getId() {
        return this.id;
    }
    public void setdisplayName(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName(){
        return this.displayName;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    public String getDescription(){
        return this.description;
        
    }
    public String getAddress() {
        return this.address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public String getUserName() {
        return this.userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getPassword() {
        return this.password;
    }
    public void setPassword() {
        this.password = password;
    }
    public String getConf() {
        return this.conf;
    }
    public void setConf(String conf) {
        this.conf = conf;
    }
}