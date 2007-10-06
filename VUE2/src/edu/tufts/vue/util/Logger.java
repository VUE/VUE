package edu.tufts.vue.util;

import tufts.Util;
import tufts.vue.DEBUG;

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
 * <p>The entire file consists of original code.  Copyright &copy; 2006 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/**
  Prints throwable using stack trace and prints message.  Output is to System.out.
  */
public class Logger
{
    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(Logger.class);
    
    // just to mark stuff coming from this logger
    private static final String Tag = "[ETVUL] ";
    
	public static void log(Throwable t, String message) {

            if (DEBUG.Enabled) {
                Util.printStackTrace(t, message);
            } else {
                Log.info(Tag + t + "; " + message);
                //t.printStackTrace();
            }
            
		//System.out.println(message);
		//t.printStackTrace();
	}

	public static void log(String message) {
            Log.info(Tag + message);
            //if (DEBUG.Enabled) VUE.Log.info(message);
            //System.out.println(message);
	}

	public static void log(Throwable t) {
            if (DEBUG.Enabled) {
                Util.printStackTrace(t);
            } else {
                Log.info(Tag + t);
                t.printStackTrace();
            }
		//t.printStackTrace();
		//System.out.println(t.getMessage());
	}
}