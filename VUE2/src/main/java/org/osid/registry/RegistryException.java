package org.osid.registry;

/**
* OsidException or one of its subclasses is thrown by all methods of all
 * interfaces of an Open Service Interface Definition (OSID). This requires
 * the caller of an OSID package method handle the OsidException. Since the
 * application using an OSID can not determine where an implementation method
 * will ultimately execute, it must assume a worst case scenerio and protect
 * itself. OSID Implementations should throw their own subclasses of
 * OsidException and limit exception messages to those predefined by their own
 * OsidException or its superclasses. This approach to exception messages
 * allows Applications and OSID implementations using an OsidException's
 * predefined messages to handle exceptions in an interoperable way.
 * 
 * <p>
 * OSID Version: 2.0
 * </p>
 * 
 * <p>
 * Licensed under the {@link org.osid.SidImplementationLicenseMIT MIT
	 * O.K.I&#46; OSID Definition License}.
 * </p>
 */
public class RegistryException extends org.osid.shared.SharedException
{
    public RegistryException(String message) {
        super(message);
    }
}
/**
* <p>
 * MIT O.K.I&#46; SID Definition License.
 * </p>
 * 
 * <p>
 * <b>Copyright and license statement:</b>
 * </p>
 * 
 * <p>
 * Copyright &copy; 2003 Massachusetts Institute of     Technology &lt;or
 * copyright holder&gt;
 * </p>
 * 
 * <p>
 * This work is being provided by the copyright holder(s)     subject to
 * the terms of the O.K.I&#46; SID Definition     License. By obtaining,
 * using and/or copying this Work,     you agree that you have read,
 * understand, and will comply     with the O.K.I&#46; SID Definition
 * License.
 * </p>
 * 
 * <p>
 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY     KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO     THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A     PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL     MASSACHUSETTS INSTITUTE OF
 * TECHNOLOGY, THE AUTHORS, OR     COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR     OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT     OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH     THE WORK OR THE USE OR OTHER DEALINGS IN THE WORK.
 * </p>
 * 
 * <p>
 * <b>O.K.I&#46; SID Definition License</b>
 * </p>
 * 
 * <p>
 * This work (the &ldquo;Work&rdquo;), including any     software,
 * documents, or other items related to O.K.I&#46;     SID definitions, is
 * being provided by the copyright     holder(s) subject to the terms of
 * the O.K.I&#46; SID     Definition License. By obtaining, using and/or
 * copying     this Work, you agree that you have read, understand, and
 * will comply with the following terms and conditions of     the
 * O.K.I&#46; SID Definition License:
 * </p>
 * 
 * <p>
 * You may use, copy, and distribute unmodified versions of     this Work
 * for any purpose, without fee or royalty,     provided that you include
 * the following on ALL copies of     the Work that you make or
 * distribute:
 * </p>
 * 
 * <ul>
 * <li>
 * The full text of the O.K.I&#46; SID Definition License in a location
 * viewable to users of the redistributed Work.
 * </li>
 * </ul>
 * 
 * 
 * <ul>
 * <li>
 * Any pre-existing intellectual property disclaimers, notices, or terms
 * and conditions. If none exist, a short notice similar to the following
 * should be used within the body of any redistributed Work:
 * &ldquo;Copyright &copy; 2003 Massachusetts Institute of Technology. All
 * Rights Reserved.&rdquo;
 * </li>
 * </ul>
 * 
 * <p>
 * You may modify or create Derivatives of this Work only     for your
 * internal purposes. You shall not distribute or     transfer any such
 * Derivative of this Work to any location     or any other third party.
 * For purposes of this license,     &ldquo;Derivative&rdquo; shall mean
 * any derivative of the     Work as defined in the United States
 * Copyright Act of     1976, such as a translation or modification.
 * </p>
 * 
 * <p>
 * THE WORK PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,     EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE     WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR     PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL     MASSACHUSETTS INSTITUTE OF
 * TECHNOLOGY, THE AUTHORS, OR     COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR     OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT     OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH     THE WORK OR THE USE OR OTHER DEALINGS IN THE WORK.
 * </p>
 * 
 * <p>
 * The name and trademarks of copyright holder(s) and/or     O.K.I&#46; may
 * NOT be used in advertising or publicity     pertaining to the Work
 * without specific, written prior     permission. Title to copyright in
 * the Work and any     associated documentation will at all times remain
 * with     the copyright holders.
 * </p>
 * 
 * <p>
 * The export of software employing encryption technology     may require a
 * specific license from the United States     Government. It is the
 * responsibility of any person or     organization contemplating export
 * to obtain such a     license before exporting this Work.
 * </p>
 */

	