package org.okip.service.filing.impl.rfs;

/*
   Copyright (c) 2002 Massachusetts Institute of Technology

   This work, including any software, documents, or other related items
   (the "Work"), is being provided by the copyright holder(s) subject to
   the terms of the MIT OKI(TM) API Definition License. By obtaining,
   using and/or copying this Work, you agree that you have read,
   understand, and will comply with the following terms and conditions of
   the MIT OKI(TM) API Definition License:

   You may use, copy, and distribute unmodified versions of this Work for
   any purpose, without fee or royalty, provided that you include the
   following on ALL copies of the Work that you make or distribute:

    *  The full text of the MIT OKI(TM) API Definition License in a
       location viewable to users of the redistributed Work.

    *  Any pre-existing intellectual property disclaimers, notices, or
       terms and conditions. If none exist, a short notice similar to the
       following should be used within the body of any redistributed
       Work: "Copyright (c) 2002 Massachusetts Institute of Technology. All
       Rights Reserved."

   You may modify or create Derivatives of this Work only for your
   internal purposes. You shall not distribute or transfer any such
   Derivative of this Work to any location or any other third party. For
   purposes of this license, "Derivative" shall mean any derivative of
   the Work as defined in the United States Copyright Act of 1976, such
   as a translation or modification.

   THIS WORK PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
   IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
   CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
   TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE WORK
   OR THE USE OR OTHER DEALINGS IN THE WORK.

   The name and trademarks of copyright holder(s) and/or MIT may NOT be
   used in advertising or publicity pertaining to the Work without
   specific, written prior permission. Title to copyright in the Work and
   any associated documentation will at all times remain with the
   copyright holders.

*/

/*
 * $Source: /home/svn/cvs2svn-2.1.1/at-cvs-repo/VUE2/src/oki/old/filing/RfsType.java,v $
 */

/**
 * Type object for this implementation.
 *<p>
 * Licensed under the {@link org.okip.service.ApiLicense MIT OKI&#153; API Definition License}.
 *
 * @version $Revision: 1.1 $ / $Date: 2003-04-14 20:48:28 $
*/

public class RfsType
extends org.okip.service.shared.api.Type {

  private static final String DOMAIN    = "Filing";
  private static final String AUTHORITY = "MIT";
  private static final String KEYWORD   = "RemoteFileSystem2";

  /**
   * Constructor RfsType
   *
   *
   */
  public RfsType() {
    super(DOMAIN, AUTHORITY, KEYWORD);
  }

  protected RfsType(String domain, String authority, String keyword) {
    super(domain, authority, keyword);
  }
}
