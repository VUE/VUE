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
package  edu.tufts.osidimpl.repository.fedora_2_2;

public class ImageRecordStructure
        implements org.osid.repository.RecordStructure {
    public static final String MEDIUM_RES = "bdef:TuftsImage/getMediumRes/";
    public static final String THUMBNAIL = "bdef:TuftsImage/getThumbnail/";
    public static final String FULLVIEW = "bdef:AssetDef/getFullView/";
    private java.util.Vector partsVector = new java.util.Vector();
    private String displayName = "Image Specific Data";
    private String description = "Provides information about and image (thumbnail and view URLs)";
    private org.osid.shared.Id id = null;
    private String schema = null;
    private String format = "Plain Text";
    private org.osid.shared.Type type = new Type("mit.edu","recordStructure","image");
    private org.osid.repository.PartStructure sThumbnailPartStructure = null;
    private org.osid.repository.PartStructure sURLPartStructure = null;
    private org.osid.repository.PartStructure sMediumImagePartStructure = null;
    
    protected ImageRecordStructure(Repository repository)
    throws org.osid.repository.RepositoryException {
        try {
            this.id = new PID("ImageRecordStructureId");
        } catch (org.osid.shared.SharedException sex) {
        }
        this.sThumbnailPartStructure = new ThumbnailPartStructure(this, repository);
        this.sURLPartStructure = new URLPartStructure(this, repository);
        this.sMediumImagePartStructure = new MediumImagePartStructure(this,repository);
        
        this.partsVector.add(this.sThumbnailPartStructure);
        this.partsVector.add(this.sURLPartStructure);
        this.partsVector.add(this.sMediumImagePartStructure);
		
		this.partsVector.add(ContributorPartStructure.getInstance());
		this.partsVector.add(CoveragePartStructure.getInstance());
		this.partsVector.add(CreatorPartStructure.getInstance());
		this.partsVector.add(DatePartStructure.getInstance());
		this.partsVector.add(DescriptionPartStructure.getInstance());
		this.partsVector.add(FormatPartStructure.getInstance());
		this.partsVector.add(IdentifierPartStructure.getInstance());
		this.partsVector.add(LanguagePartStructure.getInstance());
		this.partsVector.add(LargeImagePartStructure.getInstance());
		this.partsVector.add(PublisherPartStructure.getInstance());
		this.partsVector.add(RelationPartStructure.getInstance());
		this.partsVector.add(RightsPartStructure.getInstance());
		this.partsVector.add(SourcePartStructure.getInstance());
		this.partsVector.add(SubjectPartStructure.getInstance());
		this.partsVector.add(TitlePartStructure.getInstance());
		this.partsVector.add(TypePartStructure.getInstance());
    }
    
    public String getDisplayName()
    throws org.osid.repository.RepositoryException {
        return this.displayName;
    }
    
    public void updateDisplayName(String displayName)
    throws org.osid.repository.RepositoryException {
        throw new org.osid.repository.RepositoryException(org.osid.OsidException.UNIMPLEMENTED);
    }
    
    public String getDescription()
    throws org.osid.repository.RepositoryException {
        return this.description;
    }
    
    public String getFormat()
    throws org.osid.repository.RepositoryException {
        return this.format;
    }
    
    public org.osid.shared.Id getId()
    throws org.osid.repository.RepositoryException {
        return this.id;
    }
    
    public org.osid.repository.PartStructureIterator getPartStructures()
    throws org.osid.repository.RepositoryException {
        return new PartStructureIterator(this.partsVector);
    }
    
    public String getSchema()
    throws org.osid.repository.RepositoryException {
        return this.schema;
    }
    
    public org.osid.shared.Type getType()
    throws org.osid.repository.RepositoryException {
        return this.type;
    }
    
    public boolean isRepeatable()
    throws org.osid.repository.RepositoryException {
        return false;
    }
    
    public boolean validateRecord(org.osid.repository.Record record)
    throws org.osid.repository.RepositoryException {
        return true;
    }
    
    public org.osid.repository.PartStructure getThumbnailPartStructure()
    throws org.osid.repository.RepositoryException {
        if (this.sThumbnailPartStructure == null) {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
        }
        return this.sThumbnailPartStructure;
    }
    
    public org.osid.repository.PartStructure getURLPartStructure()
    throws org.osid.repository.RepositoryException {
        if (this.sURLPartStructure == null) {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
        }
        return this.sURLPartStructure;
    }
    
    public org.osid.repository.PartStructure getMediumImagePartStructure()
    throws org.osid.repository.RepositoryException {
        if (this.sThumbnailPartStructure == null) {
            throw new org.osid.repository.RepositoryException(org.osid.repository.RepositoryException.OPERATION_FAILED);
        }
        return this.sMediumImagePartStructure;
    }
    
    public static Record createImageRecord(String pid
										   , ImageRecordStructure recordStructure
										   , Repository repository
										   , PID objectId
										   , FedoraObjectAssetType assetType
										   , String displayName
										   , String identifier)
		throws org.osid.repository.RepositoryException {
        Record record = null;
        try {
            record = new Record(new PID(pid),recordStructure);
            //System.out.println("creating " + recordStructure.getType().getKeyword() + " " + recordStructure.getDisplayName());
            if(assetType.getKeyword().equals("tufts/image/archival")) {
                record.createPart(recordStructure.getThumbnailPartStructure().getId(), Utilities.formatObjectUrl(objectId.getIdString(), THUMBNAIL, repository));
                record.createPart(recordStructure.getURLPartStructure().getId(), Utilities.formatObjectUrl(objectId.getIdString(), FULLVIEW, repository));
                record.createPart(recordStructure.getMediumImagePartStructure().getId(), Utilities.formatObjectUrl(objectId.getIdString(),MEDIUM_RES,repository));
                record.createPart(LargeImagePartStructure.getInstance().getId(), Utilities.formatObjectUrl(objectId.getIdString(),MEDIUM_RES,repository));
				
				// get the XML for the Dublin Core metadata for this asset; parse it to fill in part structures
				String dcURL = Utilities.formatObjectUrl(objectId.getIdString(),"bdef:TuftsMetadata/getDublinCore/",repository);				
				String contributor = "";
				String coverage = "";
				String creator = "";
				String date = "";
				String description = "";
				String format = "";
				String language = "";
				String publisher = "";
				String relation = "";
				String rights = "";
				String source = "";
				String subject = "";
				String type = "";

				try 
				{
					java.net.URL url = new java.net.URL(dcURL);
					java.net.URLConnection connection = url.openConnection();
					java.net.HttpURLConnection http = (java.net.HttpURLConnection)connection;
					java.io.InputStreamReader in = new java.io.InputStreamReader(http.getInputStream());
					StringBuffer xml = new StringBuffer();
					try
					{
						int i = 0;
						while ( (i = in.read()) != -1 )
						{
							xml.append(Character.toString((char)i));
						}
					}
					catch (Throwable t) {}
					//System.out.println("xml " + xml);
					
					javax.xml.parsers.DocumentBuilderFactory dbf = null;
					javax.xml.parsers.DocumentBuilder db = null;
					org.w3c.dom.Document document = null;
					
					dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
					db = dbf.newDocumentBuilder();
					document = db.parse(new java.io.ByteArrayInputStream(xml.toString().getBytes()));
					
					// for each DOC (maps 1-to-1 with Asset)
					org.w3c.dom.NodeList dcs = document.getElementsByTagName("dc:contributor");
					int numDCs = dcs.getLength();
					for (int i=0; i < numDCs; i++)
					{
						org.w3c.dom.Element dc = (org.w3c.dom.Element)dcs.item(i);
						if (dc.hasChildNodes()) 
						{
							contributor = dc.getFirstChild().getNodeValue();
							record.createPart(ContributorPartStructure.getInstance().getId(),contributor);				
						}
					}
					dcs = document.getElementsByTagName("dc:creator");
					numDCs = dcs.getLength();
					for (int i=0; i < numDCs; i++)
					{
						org.w3c.dom.Element dc = (org.w3c.dom.Element)dcs.item(i);
						if (dc.hasChildNodes()) 
						{
							creator = dc.getFirstChild().getNodeValue();
							record.createPart(CreatorPartStructure.getInstance().getId(),creator);				
						}
					}
					dcs = document.getElementsByTagName("dc:date");
					numDCs = dcs.getLength();
					for (int i=0; i < numDCs; i++)
					{
						org.w3c.dom.Element dc = (org.w3c.dom.Element)dcs.item(i);
						if (dc.hasChildNodes()) 
						{
							date = dc.getFirstChild().getNodeValue();
							record.createPart(DatePartStructure.getInstance().getId(),date);				
						}
					}
					dcs = document.getElementsByTagName("dc:description");
					numDCs = dcs.getLength();
					for (int i=0; i < numDCs; i++)
					{
						org.w3c.dom.Element dc = (org.w3c.dom.Element)dcs.item(i);
						if (dc.hasChildNodes()) 
						{
							description = dc.getFirstChild().getNodeValue();
							record.createPart(DescriptionPartStructure.getInstance().getId(),description);				
						}
					}
					dcs = document.getElementsByTagName("dc:format");
					numDCs = dcs.getLength();
					for (int i=0; i < numDCs; i++)
					{
						org.w3c.dom.Element dc = (org.w3c.dom.Element)dcs.item(i);
						if (dc.hasChildNodes()) 
						{
							format = dc.getFirstChild().getNodeValue();
							record.createPart(FormatPartStructure.getInstance().getId(),format);				
						}
					}
					dcs = document.getElementsByTagName("dc:language");
					numDCs = dcs.getLength();
					for (int i=0; i < numDCs; i++)
					{
						org.w3c.dom.Element dc = (org.w3c.dom.Element)dcs.item(i);
						if (dc.hasChildNodes()) 
						{
							language = dc.getFirstChild().getNodeValue();
							record.createPart(LanguagePartStructure.getInstance().getId(),language);				
						}
					}
					dcs = document.getElementsByTagName("dc:publisher");
					numDCs = dcs.getLength();
					for (int i=0; i < numDCs; i++)
					{
						org.w3c.dom.Element dc = (org.w3c.dom.Element)dcs.item(i);
						if (dc.hasChildNodes()) 
						{
							publisher = dc.getFirstChild().getNodeValue();
							record.createPart(PublisherPartStructure.getInstance().getId(),publisher);				
						}
					}
					dcs = document.getElementsByTagName("dc:relation");
					numDCs = dcs.getLength();
					for (int i=0; i < numDCs; i++)
					{
						org.w3c.dom.Element dc = (org.w3c.dom.Element)dcs.item(i);
						if (dc.hasChildNodes()) 
						{
							relation = dc.getFirstChild().getNodeValue();
							record.createPart(RelationPartStructure.getInstance().getId(),relation);				
						}
					}
					dcs = document.getElementsByTagName("dc:rights");
					numDCs = dcs.getLength();
					for (int i=0; i < numDCs; i++)
					{
						org.w3c.dom.Element dc = (org.w3c.dom.Element)dcs.item(i);
						if (dc.hasChildNodes()) 
						{
							rights = dc.getFirstChild().getNodeValue();
							record.createPart(RightsPartStructure.getInstance().getId(),rights);				
						}
					}
					dcs = document.getElementsByTagName("dc:source");
					numDCs = dcs.getLength();
					for (int i=0; i < numDCs; i++)
					{
						org.w3c.dom.Element dc = (org.w3c.dom.Element)dcs.item(i);
						if (dc.hasChildNodes()) 
						{
							creator = dc.getFirstChild().getNodeValue();
							record.createPart(SourcePartStructure.getInstance().getId(),source);				
						}
					}
					dcs = document.getElementsByTagName("dc:subject");
					numDCs = dcs.getLength();
					for (int i=0; i < numDCs; i++)
					{
						org.w3c.dom.Element dc = (org.w3c.dom.Element)dcs.item(i);
						if (dc.hasChildNodes()) 
						{
							creator = dc.getFirstChild().getNodeValue();
							record.createPart(SubjectPartStructure.getInstance().getId(),subject);
						}
					}
					dcs = document.getElementsByTagName("dc:type");
					numDCs = dcs.getLength();
					for (int i=0; i < numDCs; i++)
					{
						org.w3c.dom.Element dc = (org.w3c.dom.Element)dcs.item(i);
						if (dc.hasChildNodes()) 
						{
							type = dc.getFirstChild().getNodeValue();
							record.createPart(TypePartStructure.getInstance().getId(),type);				
						}
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return record;
    }
    
    
    
}
