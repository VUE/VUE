package edu.tufts.vue.util;

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
	public static void log(Throwable t,
					  String message) {
		System.out.println(message);
		t.printStackTrace();
	}

	public static void log(String message) {
		System.out.println(message);
	}

	public static void log(Throwable t) {
		t.printStackTrace();
	}
}