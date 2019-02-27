/*
* Copyright 2003-2010 Tufts University  Licensed under the
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
package tufts.oki.dr.fedora;

public class DigitalRepositoryManager
implements osid.dr.DigitalRepositoryManager
{
    private osid.OsidOwner owner = null;
    private java.util.Map configuration = null;

    public osid.OsidOwner getOwner()
    throws osid.dr.DigitalRepositoryException
    {
        return this.owner;
    }

    public void updateOwner(osid.OsidOwner owner)
    throws osid.dr.DigitalRepositoryException
    {
        this.owner = owner;
    }

    public void updateConfiguration(java.util.Map configuration)
    throws osid.dr.DigitalRepositoryException
    {
        this.configuration = configuration;
    }

    public osid.dr.DigitalRepository createDigitalRepository(String displayName
                                                           , String description
                                                           , osid.shared.Type drType)
    throws osid.dr.DigitalRepositoryException
    {
        if ( (displayName == null) || (description == null) || (drType == null) )
        {
            throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.NULL_ARGUMENT);
        }
        throw new osid.dr.DigitalRepositoryException(osid.OsidException.UNIMPLEMENTED);
    }

    public void deleteDigitalRepository(osid.shared.Id drId)
    throws osid.dr.DigitalRepositoryException
    {
        if (drId == null)
        {
            throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.NULL_ARGUMENT);
        }
        throw new osid.dr.DigitalRepositoryException(osid.OsidException.UNIMPLEMENTED);
    }

    public osid.dr.DigitalRepositoryIterator getDigitalRepositories()
    throws osid.dr.DigitalRepositoryException
    {
        java.util.Vector result = new java.util.Vector();
        
        try
        {
            result.addElement(new DR("fedora.conf",
                                     "",
                                     "Tufts Digital Library",
                                     "",
                                     new java.net.URL("http","snowflake.lib.tufts.edu",8080,"fedora/"),
                                     "test",
                                     "test"));
        }
        catch (Throwable t) {}
        // insert code here to add elements to result vector
        return new DigitalRepositoryIterator(result);
    }

    public osid.dr.DigitalRepositoryIterator getDigitalRepositoriesByType(osid.shared.Type drType)
    throws osid.dr.DigitalRepositoryException
    {
        if (drType == null)
        {
            throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.NULL_ARGUMENT);
        }
        java.util.Vector result = new java.util.Vector();
        // insert code here to add elements to result vector
        return new DigitalRepositoryIterator(result);
    }

    public osid.dr.DigitalRepository getDigitalRepository(osid.shared.Id drId)
    throws osid.dr.DigitalRepositoryException
    {
        if (drId == null)
        {
            throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.NULL_ARGUMENT);
        }

        throw new osid.dr.DigitalRepositoryException(osid.OsidException.UNIMPLEMENTED);  
    }

    public osid.dr.Asset getAsset(osid.shared.Id assetId)
    throws osid.dr.DigitalRepositoryException
    {
        if (assetId == null)
        {
            throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.NULL_ARGUMENT);
        }

        throw new osid.dr.DigitalRepositoryException(osid.OsidException.UNIMPLEMENTED);  
    }

    public osid.dr.Asset getAssetByDate(osid.shared.Id assetId
                                      , java.util.Calendar date)
    throws osid.dr.DigitalRepositoryException
    {
        if (assetId == null)
        {
            throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.NULL_ARGUMENT);
        }

        throw new osid.dr.DigitalRepositoryException(osid.OsidException.UNIMPLEMENTED);  
    }

    public osid.shared.CalendarIterator getAssetDates(osid.shared.Id assetId)
    throws osid.dr.DigitalRepositoryException
    {
        if (assetId == null)
        {
            throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.NULL_ARGUMENT);
        }

        java.util.Vector result = new java.util.Vector();
        // insert code here to add elements to result vector
        try
        {
            return new CalendarIterator(result);
        }
        catch(osid.OsidException oex)
        {
            System.out.println(oex.getMessage());
            throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.OPERATION_FAILED);
        }
    }

    public osid.dr.AssetIterator getAssets(osid.dr.DigitalRepository[] repositories
                                        , java.io.Serializable searchCriteria
                                        , osid.shared.Type searchType)
    throws osid.dr.DigitalRepositoryException
    {
        if ( (repositories == null) || (searchCriteria == null) )
        {
            throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.NULL_ARGUMENT);
        }

        // just call get assets on each dr
        java.util.Vector result = new java.util.Vector();    
        try
        {
            for (int i=0, length = repositories.length; i < length; i++)
            {
                osid.dr.AssetIterator assetIterator = 
                    repositories[i].getAssetsBySearch(searchCriteria,searchType);
                while (assetIterator.hasNext())
                {
                    result.addElement(assetIterator.next());
                }
            }
        }
        catch(Exception ex)
        {
            System.out.println(ex.getMessage());
            throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.OPERATION_FAILED);
        }
        return new AssetIterator(result);
    }

    public osid.shared.Id copyAsset(osid.dr.DigitalRepository dr
                                  , osid.shared.Id assetId)
    throws osid.dr.DigitalRepositoryException
    {
        if ( (dr == null) || (assetId == null) )
        {
            throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.NULL_ARGUMENT);
        }

        throw new osid.dr.DigitalRepositoryException(osid.OsidException.UNIMPLEMENTED);
    }

    public osid.shared.TypeIterator getDigitalRepositoryTypes()
    throws osid.dr.DigitalRepositoryException
    {
        try
        {
            java.util.Vector result = new java.util.Vector();
            result.addElement(new Type("dr","tufts.edu","fedora_image",""));
            return new TypeIterator(result);
        }
        catch (osid.OsidException oex)
        {
            System.out.println(oex.getMessage());
        }
        throw new osid.dr.DigitalRepositoryException(osid.dr.DigitalRepositoryException.OPERATION_FAILED);
    }

    public void osidVersion_1_0()
    throws osid.dr.DigitalRepositoryException
    {
    }
/**
<p>MIT O.K.I&#46; SID Implementation License.
  <p>	<b>Copyright and license statement:</b>
  </p>  <p>	Copyright &copy; 2003 Massachusetts Institute of
	Technology &lt;or copyright holder&gt;
  </p>  <p>	This work is being provided by the copyright holder(s)
	subject to the terms of the O.K.I&#46; SID Implementation
	License. By obtaining, using and/or copying this Work,
	you agree that you have read, understand, and will comply
	with the O.K.I&#46; SID Implementation License.
  </p>  <p>	THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
	KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
	THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
	PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
	MASSACHUSETTS INSTITUTE OF TECHNOLOGY, THE AUTHORS, OR
	COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
	OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
	OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
	THE WORK OR THE USE OR OTHER DEALINGS IN THE WORK.
  </p>  <p>	<b>O.K.I&#46; SID Implementation License</b>
  </p>  <p>	This work (the &ldquo;Work&rdquo;), including software,
	documents, or other items related to O.K.I&#46; SID
	implementations, is being provided by the copyright
	holder(s) subject to the terms of the O.K.I&#46; SID
	Implementation License. By obtaining, using and/or
	copying this Work, you agree that you have read,
	understand, and will comply with the following terms and
	conditions of the O.K.I&#46; SID Implementation License:
  </p>  <p>	Permission to use, copy, modify, and distribute this Work
	and its documentation, with or without modification, for
	any purpose and without fee or royalty is hereby granted,
	provided that you include the following on ALL copies of
	the Work or portions thereof, including modifications or
	derivatives, that you make:
  </p>  <ul>	<li>	  The full text of the O.K.I&#46; SID Implementation
	  License in a location viewable to users of the
	  redistributed or derivative work.
	</li>  </ul>  <ul>	<li>	  Any pre-existing intellectual property disclaimers,
	  notices, or terms and conditions. If none exist, a
	  short notice similar to the following should be used
	  within the body of any redistributed or derivative
	  Work: &ldquo;Copyright &copy; 2003 Massachusetts
	  Institute of Technology. All Rights Reserved.&rdquo;
	</li>  </ul>  <ul>	<li>	  Notice of any changes or modifications to the
	  O.K.I&#46; Work, including the date the changes were
	  made. Any modified software must be distributed in such
	  as manner as to avoid any confusion with the original
	  O.K.I&#46; Work.
	</li>  </ul>  <p>	THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
	KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
	THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
	PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
	MASSACHUSETTS INSTITUTE OF TECHNOLOGY, THE AUTHORS, OR
	COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
	OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
	OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
	THE WORK OR THE USE OR OTHER DEALINGS IN THE WORK.
  </p>  <p>	The name and trademarks of copyright holder(s) and/or
	O.K.I&#46; may NOT be used in advertising or publicity
	pertaining to the Work without specific, written prior
	permission. Title to copyright in the Work and any
	associated documentation will at all times remain with
	the copyright holders.
  </p>  <p>	The export of software employing encryption technology
	may require a specific license from the United States
	Government. It is the responsibility of any person or
	organization contemplating export to obtain such a
	license before exporting this Work.
  </p>
*/
}
