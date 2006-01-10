package edu.tufts.vue.osidimpl.registry;

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
 * Id implements the O.K.I. Id service interface.
 * </p>
 * 
 * @author Massachusetts Institute of Technology
 */
public class Id
implements org.osid.shared.Id
{
    private String idString = null;

	/**
	 * Store away the input string
	*/
    protected Id(String idString)
    throws org.osid.shared.SharedException
    {
        if (idString == null)
        {
            throw new org.osid.shared.SharedException(org.osid.id.IdException.NULL_ARGUMENT);    
        }
        this.idString = idString;
    }

	/**
	 * Simply return the input string stored during construction.
	 */
    public String getIdString()
    throws org.osid.shared.SharedException
    {
        return this.idString;
    }

	/**
	 * Two Ids are equal if their string representations are equal.
	 */
    public boolean isEqual(org.osid.shared.Id id)
    throws org.osid.shared.SharedException
    {
        return id.getIdString().equals(this.idString);
    }
}
