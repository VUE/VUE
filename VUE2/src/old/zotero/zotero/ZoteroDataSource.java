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
 * ZoteroDataSource.java
 *
 * Created on April 23, 2009
 */

package edu.tufts.vue.zotero;
import javax.swing.JComponent;

import tufts.vue.BrowseDataSource;
import tufts.vue.DataSourceException;
import tufts.vue.VueUtil;

import java.io.*;
import edu.tufts.vue.util.*;

/**
 * @version
 * @author  mikek
 */

public class ZoteroDataSource extends BrowseDataSource {
    public static final String FOLDER= VueUtil.getDefaultUserFolder().getAbsolutePath()+File.separatorChar;
    
    private String saveFile;

    // persistance create: saveFile will be set
    public ZoteroDataSource() {}
    
    // runtime create: create a new safe file
    public ZoteroDataSource(String DisplayName) throws DataSourceException {
        setDisplayName(DisplayName);
        if (saveFile == null) {
            // 
            saveFile = FOLDER+"f"+GUID.generate()+".xml";
        }
    }
    
    @Override
    public String getTypeName() {
        return "Zotero";
    }
    
    @Override
    public JComponent buildResourceViewer() {
        return new ZoteroWindow(getDisplayName(), getSaveFile());
    }
    
    public void setSaveFile(String saveFile) {
        //tufts.Util.printStackTrace("setSaveFile " + saveFile);
        this.saveFile = saveFile;
    }
    
    public String getSaveFile() {
        return this.saveFile;
    }
    

}







