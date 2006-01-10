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
 * ProviderIterator implements a "straw-man" registry interface.
 * </p>
 * 
 * @author Massachusetts Institute of Technology
 */
public class ProviderIterator implements org.osid.registry.ProviderIterator {
    java.util.Iterator mIterator = null;

    /**
	 * Store away the input.
	 */
	protected ProviderIterator(java.util.Vector vector) {
        mIterator = vector.iterator();
    }

    /**
	 * Test if we have any more Providers to return.
	 */
    public boolean hasNextProvider()
		throws org.osid.registry.RegistryException
	{
        return mIterator.hasNext();
    }

    /**
	 * Return the next Provider from the original input.
	 */
    public org.osid.registry.Provider nextProvider()
		throws org.osid.registry.RegistryException
	{
        try {
            return (org.osid.registry.Provider) mIterator.next();
        } catch (java.util.NoSuchElementException e) {
            throw new org.osid.registry.RegistryException(org.osid.shared.SharedException.NO_MORE_ITERATOR_ELEMENTS);
        }
    }
}
