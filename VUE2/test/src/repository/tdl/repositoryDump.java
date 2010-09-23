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

package repository.tdl;

public class repositoryDump
{
    private boolean showManadatoryRecordStructureDetail = false;
    private boolean showStructureDetail = false;
    private boolean showAssetDetail = false;
    private boolean showAssets = false;
    private int assetNumber = 0;

    public static void main(String args[])
    {
        try
        {
            org.osid.OsidContext context = new org.osid.OsidContext();
                      
            org.osid.repository.RepositoryManager repositoryManager = (org.osid.repository.RepositoryManager)org.osid.OsidLoader.getManager(
                "org.osid.repository.RepositoryManager",
                "tufts.oki.repository.fedora",
                context,
                new java.util.Properties());     
            repositoryDump dump = new repositoryDump( repositoryManager );
            System.exit(0);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
    
    private void assetsByRepository(org.osid.repository.RepositoryManager repositoryManager)
    {
        try
        {
			System.out.println("about to get repositories");
            org.osid.repository.RepositoryIterator repositoryIterator = repositoryManager.getRepositories();
			System.out.println("got repositories");
            while (repositoryIterator.hasNextRepository())
            {
                org.osid.repository.Repository repository = repositoryIterator.nextRepository();
                System.out.println("Repository: " + repository.getDisplayName());
                org.osid.shared.TypeIterator ti = repository.getSearchTypes();
                while (ti.hasNextType()) System.out.println(ti.nextType().getKeyword());
                org.osid.repository.AssetIterator assetIterator = repository.getAssetsBySearch("Jumbo",new Type("mit.edu","search","keyword"),new SharedProperties());
                while (assetIterator.hasNextAsset())
                {
                    org.osid.repository.Asset asset = assetIterator.nextAsset();
                    this.assetNumber++;
                    System.out.println("Asset# " + this.assetNumber + ": " + asset.getDisplayName());

                    System.out.println("Description " + asset.getDescription());
					System.out.println("Id " + asset.getId().getIdString());
					showType(asset.getAssetType());
/*					
					org.osid.repository.RecordIterator recordIterator = asset.getRecords();
					while (recordIterator.hasNextRecord()) {
					org.osid.repository.Record sourceRecord = recordIterator.nextRecord();
						org.osid.repository.PartIterator partIterator = sourceRecord.getParts();
						while (partIterator.hasNextPart()) {
							org.osid.repository.Part sourcePart = partIterator.nextPart();
							if (sourcePart.getValue() != null) {
								System.out.println();
								System.out.println("Part Type:");
								showType(sourcePart.getPartStructure().getType());
								System.out.println("Value: " + sourcePart.getValue());
							}
						}
					}
					System.out.println("Content " + asset.getContent());
*/
                }
                System.out.println();
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
		System.exit(0);
    }

    protected repositoryDump (org.osid.repository.RepositoryManager repositoryManager)
    {
/*=================================================================================================================*/
        assetsByRepository(repositoryManager);

        org.osid.shared.TypeIterator repositoryTypeIterator = null;
        try
        {
            repositoryTypeIterator = repositoryManager.getRepositoryTypes();
            System.out.println(" Repository Types are ");
            try
            {
                while (repositoryTypeIterator.hasNextType())
                {
                    showType(repositoryTypeIterator.nextType());
                }
            }
            catch (Throwable t)
            {
                showException(t,"org.osid.repository.RepositoryIterator.hasNextType or nextType");
            }
        }
        catch (Throwable t)
        {
            showException(t,"org.osid.repository.RepositoryManager.getRepositoryTypes");
        }
/*=================================================================================================================*/
        org.osid.repository.RepositoryIterator repositoryIterator = null;
        try
        {
            repositoryIterator = repositoryManager.getRepositories();
        }
        catch (Throwable t)
        {
            showException(t,"org.osid.repository.RepositoryManager.getRepositories");
            System.exit(1);
        }
/*=================================================================================================================*/            
        org.osid.repository.Repository repository = null;        
        try
        {
            while (repositoryIterator.hasNextRepository())
            {
/*=================================================================================================================*/
                try
                {
                    repository = repositoryIterator.nextRepository();
                }
                catch (Throwable t)
                {
                    showException(t,"org.osid.repository.RepositoryIterator.next");
                }

/*=================================================================================================================*/
                
                String displayName = null;
                try
                {
                    displayName = repository.getDisplayName();
                }
                catch (Throwable t)
                {
                    showException(t,"org.osid.repository.Repository.getDisplayName");
                }
                
/*=================================================================================================================*/
                String description = null;
                try
                {
                    description = repository.getDescription();
                }
                catch (Throwable t)
                {
                    showException(t,"org.osid.repository.Repository.getDescription");
                }
    
                System.out.println("Found  Repository " + repository.getDisplayName() + " " + repository.getDescription());
                System.out.println();
                 
/*=================================================================================================================*/
                org.osid.shared.Type repositoryType = null;
                try
                {
                    repositoryType = repository.getType();
                    System.out.println(" Repository Type is ");
                    showType(repositoryType);
                }
                catch (Throwable t)
                {
                    showException(t,"org.osid.repository.Repository.getType");
                }
                
/*=================================================================================================================*/
                org.osid.shared.Id id = null;
                try
                {
                    id = repository.getId();
                    System.out.println(" Repository Id is " + id.getIdString());
                }
                catch (Throwable t)
                {
                    showException(t,"org.osid.repository.Repository.getId");
                }
                
/*=================================================================================================================*/
                org.osid.shared.TypeIterator searchTypeIterator = null;
                try
                {
                    searchTypeIterator = repository.getSearchTypes();
                    System.out.println("Search Types are ");
                    try
                    {
                        while (searchTypeIterator.hasNextType())
                        {
                            showType(searchTypeIterator.nextType());
                        }
                    }
                    catch (Throwable t)
                    {
                        showException(t,"org.osid.repository.SearchTypeIterator.hasNextType or next");
                    }
                }
                catch (Throwable t)
                {
                    showException(t,"org.osid.repository.Repository.getSearchTypes");
                }
/*=================================================================================================================*/
                org.osid.shared.TypeIterator statusTypeIterator = null;
                try
                {
                    statusTypeIterator = repository.getStatusTypes();
                    System.out.println("Status Types are ");
                    try
                    {
                        while (statusTypeIterator.hasNextType())
                        {
                            showType(statusTypeIterator.nextType());
                        }
                    }
                    catch (Throwable t)
                    {
                        showException(t,"org.osid.repository.StatusTypeIterator.hasNextType or nextType");
                    }
                }
                catch (Throwable t)
                {
                    showException(t,"org.osid.repository.Repository.getStatusTypes");
                }
/*=================================================================================================================*/
                org.osid.repository.RecordStructureIterator structureIterator = null;
                try
                {
                    structureIterator = repository.getRecordStructures();
                    System.out.println("RecordStructures are:");
                    try
                    {
                        while (structureIterator.hasNextRecordStructure())
                        {
                            showRecordStructure(structureIterator.nextRecordStructure());
                        }
                    }
                    catch (Throwable t)
                    {
                        showException(t,"org.osid.repository.RecordStructureIterator.hasNextRecordStructure or nextRecordStructure");
                    }
                }
                catch (Throwable t)
                {
                    showException(t,"org.osid.repository.Repository.getRecordStructures");
                }
/*=================================================================================================================*/
                org.osid.shared.TypeIterator assetTypeIterator = null;
                try
                {
                    assetTypeIterator = repository.getAssetTypes();
                    try
                    {
                        while (assetTypeIterator.hasNextType())
                        {
                            org.osid.shared.Type nextAssetType = assetTypeIterator.nextType();
                            org.osid.repository.RecordStructureIterator mandatorystructureIterator = null;
                            try
                            {
                                System.out.println("Mandatory RecordStructures for type ");
                                showType(nextAssetType);
                                System.out.println("are:");
                                if (!this.showManadatoryRecordStructureDetail)
                                {
                                    this.showStructureDetail = false;
                                }
                                mandatorystructureIterator = repository.getMandatoryRecordStructures(nextAssetType);
                                try
                                {
                                    while (mandatorystructureIterator.hasNextRecordStructure())
                                    {
                                        showRecordStructure(mandatorystructureIterator.nextRecordStructure());
                                    }
                                }
                                catch (Throwable t)
                                {
                                    showException(t,"org.osid.repository.RecordStructureIterator.hasNextRecordStructure or nextRecordStructure");
                                }
                            }
                            catch (Throwable t)
                            {
                                showException(t,"org.osid.repository.Repository.getMandatoryRecordStructures");
                            }
                        }
                    }
                    catch (Throwable t)
                    {
                        showException(t,"org.osid.repository.AssetTypeIterator.hasNextType or nextType");
                    }
                }
                catch (Throwable t)
                {
                    showException(t,"org.osid.repository.Repository.getAssetTypes");
                }
                if (showAssets) 
                {
                    repositoryDumpAssets(repository);
                    repositoryDumpMetadata(repository);
                }
            }
        }
        catch (Throwable t)
        {
            showException(t,"org.osid.repository.RepositoryIterator.next");
        }
    }

    private void repositoryDumpAssets(org.osid.repository.Repository repository)
    {
/*=================================================================================================================*/
        org.osid.repository.AssetIterator ai = null;
        try
        {
            ai = repository.getAssets();

//            ai = dr.getAssetsBySearch("Jumbo",new org.osid.types.mit.KeywordSearchType());
        }
        catch (Throwable t)
        {
            showException(t,"org.osid.repository.Repository.getAssets");
        }
/*=================================================================================================================*/
        try
        {
            while (ai.hasNextAsset())
            {
                org.osid.repository.Asset asset = null;
                try
                {
                    asset = ai.nextAsset();
                    showAsset(asset,repository);
                    this.showAssetDetail = false;
                }
                catch (Throwable t)
                {
                    showException(t,"org.osid.repository.AssetIterator.nextAsset");
                }
            }
        }
        catch (Throwable t)
        {
            showException(t,"org.osid.repository.AssetIterator.hasNextAsset");
        }
    }

    private void repositoryDumpMetadata(org.osid.repository.Repository repository)
    {
/*=================================================================================================================*/
        org.osid.repository.AssetIterator ai = null;
        try
        {
            ai = repository.getAssets();
        }
        catch (Throwable t)
        {
            showException(t,"org.osid.repository.Repository.getAssets");
        }
/*=================================================================================================================*/
        try
        {
            while (ai.hasNextAsset())
            {
                org.osid.repository.Asset asset = null;
                try
                {
                    asset = ai.nextAsset();
                    showAsset(asset,repository);
                    try
                    {
                        org.osid.repository.RecordIterator recordIterator = asset.getRecords();
                        try
                        {
                            while (recordIterator.hasNextRecord())
                            {
                                org.osid.repository.Record record = recordIterator.nextRecord();
                                showRecord(record);
                            }
                        }
                        catch (Throwable t)
                        {
                            showException(t,"org.osid.repository.RecordIterator.hasNextRecord or nextRecord");
                        }
                    }
                    catch (Throwable t)
                    {
                        showException(t,"org.osid.repository.Asset.getRecords");
                    }
                }
                catch (Throwable t)
                {
                    showException(t,"org.osid.repository.AssetIterator.nextAsset");
                }
            }
        }
        catch (Throwable t)
        {
            showException(t,"org.osid.repository.AssetIterator.hasNextAsset");
        }
    }

    private void showType(org.osid.shared.Type type)
    {
        System.out.println("Authority: " + type.getAuthority());
        System.out.println("Domain: " + type.getDomain());
        System.out.println("Keyword: " + type.getKeyword());
        System.out.println("Description: " + type.getDescription());
        System.out.println();
    }

    private void showException(Throwable t
                             , String methodName)
    {
        System.out.println("Exception in method " + methodName);
        System.out.println(t);
        System.out.println();
    }

    private void showRecordStructure(org.osid.repository.RecordStructure recordStructure)
    {
        try
        {
            System.out.println("Display Name: " + recordStructure.getDisplayName());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.RecordStructure.getDisplayName"); 
        }
        if (!this.showStructureDetail) return;
        try
        {
            System.out.println("Description: " + recordStructure.getDescription());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.RecordStructure.getDescription"); 
        }
        try
        {
            System.out.println("Format: " + recordStructure.getFormat());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.RecordStructure.getFormat"); 
        }
        try
        {
            System.out.println("Schema: " + recordStructure.getSchema());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.RecordStructure.getSchema"); 
        }
        try
        {
            System.out.println("Id: " + recordStructure.getId().getIdString());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.RecordStructure.getId"); 
        }
        System.out.println();
        
        try
        {
            org.osid.repository.PartStructureIterator partIterator = recordStructure.getPartStructures();
            try
            {
                while (partIterator.hasNextPartStructure())
                {
                    org.osid.repository.PartStructure part = partIterator.nextPartStructure();
                    showPartStructure(part);
                }
            }
            catch (Throwable t)
            {
                showException(t,"org.osid.repository.PartStructureIterator.hasNexttPartStructure or nexttPartStructure"); 
            }
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.PartStructureIterator.getPartStructures"); 
        }
        System.out.println();
    }

    private void showPartStructure(org.osid.repository.PartStructure partStructure)
    {
        try
        {
            System.out.println("Display Name: " + partStructure.getDisplayName());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.PartStructure.getDisplayName"); 
        }
        try
        {
            System.out.println("Description: " + partStructure.getDescription());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.PartStructure.getDescription"); 
        }
        try
        {
            System.out.println("Id: " + partStructure.getId().getIdString());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.PartStructure.getId"); 
        }
        try
        {
            System.out.println("isMandatory? : " + partStructure.isMandatory());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.PartStructure.isMandatory"); 
        }
        try
        {
            System.out.println("isPopulatedByRepository? : " + partStructure.isPopulatedByRepository());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.PartStructure.isPopulatedByRepository"); 
        }
        try
        {
            System.out.println("isRepeatable? : " + partStructure.isRepeatable());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.PartStructure.isRepeatable"); 
        }
        System.out.println();
    }

    private void showRecord(org.osid.repository.Record record)
    {
        try
        {
            System.out.println("Id: " + record.getId().getIdString());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.record.getId"); 
        }
        try
        {
            System.out.println("Structure: ");
            showRecordStructure(record.getRecordStructure());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.Record.getRecordStructure"); 
        }
        System.out.println();
        
        try
        {
            org.osid.repository.PartIterator partIterator = record.getParts();
            try
            {
                while (partIterator.hasNextPart())
                {
                    org.osid.repository.Part field = partIterator.nextPart();
                    showPart(field);
                }
            }
            catch (Throwable t)
            {
                showException(t,"org.osid.repository.PartIterator.hasNextPart or nextPart"); 
            }
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.Record.getParts"); 
        }
        System.out.println();
    }

    private void showPart(org.osid.repository.Part part)
    {
        try
        {
            System.out.println("Id: " + part.getId().getIdString());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.part.getId"); 
        }
        try
        {
            System.out.println("Part: ");
            showPartStructure(part.getPartStructure());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.part.getPartStructure"); 
        }
        try
        {
            System.out.println("Value's toString(): " + part.getValue().toString());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.part.getValue"); 
        }
        System.out.println();
    }

    private void showAsset(org.osid.repository.Asset asset
                         , org.osid.repository.Repository dr)
    {
        try
        {
            this.assetNumber++;
            System.out.println("Asset # " + this.assetNumber + " Display Name: " + asset.getDisplayName());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.Asset.getDisplayName"); 
        }
        if (!this.showAssetDetail) return;
        try
        {
            System.out.println("Description: " + asset.getDescription());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.Asset.getDescription"); 
        }
        try
        {
            org.osid.shared.Id assetId = asset.getId();
            System.out.println("Id: " + assetId.getIdString());
            try
            {
                System.out.println("Asset status:");
                showType(dr.getStatus(assetId));
            }
            catch (Throwable t)
            {
                showException(t,"org.osid.repository.getStatus"); 
            }
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.Asset.getId"); 
        }
        try
        {
            long timeInMillis =  asset.getEffectiveDate();
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.setTimeInMillis(timeInMillis);
            System.out.println("Effective Date: " + calendar.getTime());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.Asset.getEffectiveDate"); 
        }
        try
        {
            long timeInMillis =  asset.getExpirationDate();
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.setTimeInMillis(timeInMillis);
            System.out.println("Expiration Date: " + calendar.getTime());
        }
        catch (Throwable t)
        {
           showException(t,"org.osid.repository.Asset.getExpirationDate"); 
        }
        System.out.println();
        
        try
        {
            org.osid.repository.RecordStructureIterator structureIterator = asset.getRecordStructures();
            try
            {
                while (structureIterator.hasNextRecordStructure())
                {
                    try
                    {
                        org.osid.repository.RecordStructure structure = structureIterator.nextRecordStructure();
                        showRecordStructure(structure);
                    }
                    catch (Throwable t)
                    {
                        showException(t,"org.osid.repository.RecordStructureIterator.nextRecordStructure"); 
                    }
                }
            }
            catch (Throwable t)
            {
                showException(t,"org.osid.repository.RecordStructureIterator.hasNextRecordStructure"); 
            }
        }
        catch (Throwable t)
        {
            showException(t,"org.osid.repository.Asset.getRecordStructures"); 
        }
        
        try
        {
            org.osid.repository.RecordStructure structure = asset.getContentRecordStructure();
            showRecordStructure(structure);
        }
        catch (Throwable t)
        {
            showException(t,"org.osid.repository.RecordStructureIterator.getContentRecordStructure"); 
        }
        System.out.println();

        try
        {
            org.osid.repository.RecordIterator recordIterator = asset.getRecords();
            try
            {
                while (recordIterator.hasNextRecord())
                {
                    try
                    {
                        org.osid.repository.Record record = recordIterator.nextRecord();
                        showRecord(record);
                    }
                    catch (Throwable t)
                    {
                        showException(t,"org.osid.repository.RecordIterator.nextRecord"); 
                    }
                }
            }
            catch (Throwable t)
            {
                showException(t,"org.osid.repository.RecordIterator.hasNextRecord"); 
            }
        }
        catch (Throwable t)
        {
            showException(t,"org.osid.repository.Asset.getRecords"); 
        }
    }
}