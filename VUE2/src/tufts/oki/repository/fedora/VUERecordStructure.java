package tufts.oki.repository.fedora;

public class VUERecordStructure
implements org.osid.repository.RecordStructure
{
    private java.util.Vector partsVector = new java.util.Vector();
    private String displayName = "VUE Specific Data";
    private String description = "Provides information to be used by VUE";
    private org.osid.shared.Id id = null;
    private String schema = null;
    private String format = "Plain Text";
    private org.osid.shared.Type type = new Type("tufts.edu","recordStructure","vue");
    private org.osid.repository.PartStructure sVUEDefaultViewPartStructure = null;

    protected VUERecordStructure(Repository repository)
    throws org.osid.repository.RepositoryException
    {
        try
        {
            this.id = new PID("VUEInfoStructureId");
        }
        catch (org.osid.shared.SharedException sex)
        {
        }
        this.sVUEDefaultViewPartStructure = new VUEDefaultViewPartStructure(this, repository);
        this.partsVector.add(this.sVUEDefaultViewPartStructure);        
    }

    public String getDisplayName()
    throws org.osid.repository.RepositoryException
    {
        return this.displayName;
    }

    public void updateDisplayName(String displayName)
    throws org.osid.repository.RepositoryException
    {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }

    public String getDescription()
    throws org.osid.repository.RepositoryException
    {
        return this.description;
    }

    public String getFormat()
    throws org.osid.repository.RepositoryException
    {
        return this.format;
    }

    public org.osid.shared.Id getId()
    throws org.osid.repository.RepositoryException
    {
        return this.id;
    }

    public org.osid.repository.PartStructureIterator getPartStructures()
    throws org.osid.repository.RepositoryException
    {
        return new PartStructureIterator(this.partsVector);
    }

    public String getSchema()
    throws org.osid.repository.RepositoryException
    {
        return this.schema;
    }

    public org.osid.shared.Type getType()
    throws org.osid.repository.RepositoryException
    {
        return this.type;
    }

    public boolean isRepeatable()
    throws org.osid.repository.RepositoryException
    {
        return false;
    }

    public boolean validateRecord(org.osid.repository.Record record)
    throws org.osid.repository.RepositoryException
    {
        return true;
    }

    public org.osid.repository.PartStructure getVUEDefaultViewPartStructure()
    throws org.osid.repository.RepositoryException
    {
        if (this.sVUEDefaultViewPartStructure == null)
        {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
        }
        return this.sVUEDefaultViewPartStructure;
    }

    public static Record createVUERecord(String pid
                                       , VUERecordStructure recordStructure
                                       , Repository repository
                                       , PID objectId
                                       , FedoraObjectAssetType assetType)
    throws org.osid.repository.RepositoryException
    {
        Record record = null;
        try
        {
            record = new Record(new PID(pid),recordStructure);
            if(assetType.getKeyword().equals("TUFTS_STD_IMAGE"))
            {
                record.createPart(recordStructure.getVUEDefaultViewPartStructure().getId(),
                              repository.getFedoraProperties().getProperty("url.fedora.get") + "/"+objectId.getIdString()+"/bdef:11/getDefaultView/");
            }
            else if(assetType.getKeyword().equals("XML_TO_HTMLDOC"))
            {
                record.createPart(recordStructure.getVUEDefaultViewPartStructure().getId(),
                              repository.getFedoraProperties().getProperty("url.fedora.get")+"/"+objectId.getIdString()+"/demo:77/getDocument/");
            }
            else
            {
                record.createPart(recordStructure.getVUEDefaultViewPartStructure().getId(),
                              repository.getFedoraProperties().getProperty("url.fedora.get")+"/"+objectId.getIdString());
            }
                }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        return record;
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
