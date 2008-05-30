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


/*
 * DataSourceException.java
 *
 * Created on July 19, 2004, 2:47 PM
 */

package tufts.vue;

/**
 *
 * @author  akumar03
 *
 * A class to handle exceptions generated during creating and handling of DataSources
 */
public class DataSourceException extends RuntimeException {
    public static final String CREATE_MSG = "Can't create DataSource";
    
    public DataSourceException() {
        super();
    }
    
    public DataSourceException(String msg) {
        super(msg);
    }
    
    public DataSourceException(String msg, Throwable cause) {
        super(msg, cause);
    }
    
}
