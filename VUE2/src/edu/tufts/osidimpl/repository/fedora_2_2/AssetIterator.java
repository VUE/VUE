/*
 * Copyright 2003-2008 Tufts University  Licensed under the
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

import fedora.server.types.gen.*;

import java.util.*;

public class AssetIterator
        implements org.osid.repository.AssetIterator {
    private java.util.Iterator iterator = null;
    private SearchCriteria lSearchCriteria = null;
    private String numberToAskForString = "100";
    private int numberToAskFor = 100;
    private int numberAskedFor = 0;
    private org.osid.shared.Type searchType = null;
    private org.osid.shared.Type keywordSearchType = new Type("mit.edu","search","keyword");
    private org.osid.shared.Type multiFieldSearchType = new Type("mit.edu","search","multiField");
    private Repository repository = null;
    private int resultsCounter = 1;
    
    // allow for an empty iterator
   // public AssetIterator(java.util.Vector vector)
   // throws org.osid.repository.RepositoryException {
   //     this.iterator = vector.iterator();
   // }
    public AssetIterator(List<Asset> assetList)  throws org.osid.repository.RepositoryException {
        this.iterator = assetList.iterator();
    }
    
        /* Supports a simple and advanced search.  A simple search has the keyword search type and passes the criteria, unmodified,
                to the soap-support code.  An advanced search assumes the criteria is multi-field.  Take the multi-field XML, as defined
                by the OSID Type for multiField searches, and parse it to create Condition objects.  Pass these objects to the soap-support
                code.
         */
    public AssetIterator(Repository repository, java.io.Serializable searchCriteria, org.osid.shared.Type searchType)
    throws org.osid.repository.RepositoryException {
        this.repository = repository;
        this.searchType = searchType;
        try {
            if ( (searchCriteria instanceof String) && (searchType.isEqual(keywordSearchType)) ) {
                lSearchCriteria = new SearchCriteria();
                lSearchCriteria.setKeywords((String)searchCriteria);
                lSearchCriteria.setMaxReturns(numberToAskForString);
                lSearchCriteria.setSearchOperation(SearchCriteria.FIND_OBJECTS);
                lSearchCriteria.setResults(0);
                
                org.osid.repository.AssetIterator ai = FedoraRESTSearchAdapter.search(repository,lSearchCriteria);
                java.util.Vector v = new java.util.Vector();
                while (ai.hasNextAsset()) v.addElement(ai.nextAsset());
                this.iterator = v.iterator();
            } else if ( (searchCriteria instanceof String) && (searchType.isEqual(multiFieldSearchType)) ) {
                lSearchCriteria = new SearchCriteria();
                
                // parse criteria to get the field-value pairs
                java.util.List conditions = getConditions((String)searchCriteria);
                
                lSearchCriteria.setConditions((fedora.server.types.gen.Condition[])conditions.toArray(new Condition[0]));
                lSearchCriteria.setMaxReturns(numberToAskForString);
                lSearchCriteria.setSearchOperation(SearchCriteria.FIND_OBJECTS);
                lSearchCriteria.setResults(0);
                
                org.osid.repository.AssetIterator ai = FedoraRESTSearchAdapter.advancedSearch(repository,lSearchCriteria);
                java.util.Vector v = new java.util.Vector();
                while (ai.hasNextAsset()) v.addElement(ai.nextAsset());
                this.iterator = v.iterator();
            } else {
                this.iterator = new java.util.Vector().iterator();
            }
        } catch (Exception ex) {
            Utilities.log(ex);
            //ex.printStackTrace();
            throw new org.osid.repository.RepositoryException(org.osid.OsidException.OPERATION_FAILED);
        }
    }
    
    // parse XML with type-value pairs to Condition objects
    private java.util.List getConditions(String xml) {
        java.util.List conditions = new java.util.ArrayList();
//		System.out.println("advanced criteria " + xml);
        
        try {
            javax.xml.parsers.DocumentBuilderFactory dbf = null;
            javax.xml.parsers.DocumentBuilder db = null;
            org.w3c.dom.Document document = null;
            
            dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            document = db.parse(new java.io.ByteArrayInputStream(xml.toString().getBytes()));
            
            // for each DOC (maps 1-to-1 with Asset)
            org.w3c.dom.NodeList fields = document.getElementsByTagName("field");
            int numFields = fields.getLength();
            for (int i=0; i < numFields; i++) {
                org.w3c.dom.Element field = (org.w3c.dom.Element)fields.item(i);
                org.w3c.dom.NodeList nodeList = field.getElementsByTagName("type");
                int numNodes = nodeList.getLength();
                if (numNodes > 0) {
                    org.w3c.dom.Element e = (org.w3c.dom.Element)nodeList.item(0);
                    if (e.hasChildNodes()) {
                        String type = e.getFirstChild().getNodeValue();
                        org.w3c.dom.NodeList nodeList2 = field.getElementsByTagName("value");
                        int numNodes2 = nodeList2.getLength();
                        if (numNodes2 > 0) {
                            org.w3c.dom.Element e2 = (org.w3c.dom.Element)nodeList2.item(0);
                            if (e2.hasChildNodes()) {
                                String value = e2.getFirstChild().getNodeValue();
                                fedora.server.types.gen.Condition cond = new fedora.server.types.gen.Condition();
                                String property = Utilities.stringToType(type).getKeyword();
                                
                                // special case mapping of keywords to subject
                                if (property.equals("keywords")) {
                                    property = "subject";
                                }
                                
                                cond.setProperty(property);
                                cond.setOperator(ComparisonOperator.has);
                                cond.setValue(value);
                                conditions.add(cond);
//								System.out.println("type " + type);
//								System.out.println("value " + value);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Utilities.log(ex);
            //ex.printStackTrace();
        }
        return conditions;
    }
    
    // Results are not paged.  If the current result set is exhausted, and we used a soap-supported search, search again.
    public boolean hasNextAsset()
    throws org.osid.repository.RepositoryException {
        return iterator.hasNext();
/*
        if (this.iterator.hasNext()) {
                        return true;
                } else if (lSearchCriteria != null) {
                        // did we exhaust a full result set; in which case there might be more
                        if (numberAskedFor == numberToAskFor) {
                                numberAskedFor = 0;
                                lSearchCriteria.setSearchOperation(SearchCriteria.RESUME_FIND_OBJECTS);
                                resultsCounter += numberToAskFor;
                                lSearchCriteria.setResults(resultsCounter);
                                if (searchType.isEqual(keywordSearchType)) {
                                        org.osid.repository.AssetIterator ai = FedoraSoapFactory.search(repository,lSearchCriteria);
                                        java.util.Vector v = new java.util.Vector();
                                        while (ai.hasNextAsset()) v.addElement(ai.nextAsset());
                                        this.iterator = v.iterator();
                                } else {
                                        org.osid.repository.AssetIterator ai = FedoraSoapFactory.advancedSearch(repository,lSearchCriteria);
                                        java.util.Vector v = new java.util.Vector();
                                        while (ai.hasNextAsset()) {
                                                org.osid.repository.Asset a = ai.nextAsset();
                                                v.addElement(a);
                                                System.out.println(a.getDisplayName());
                                        }
                                        this.iterator = v.iterator();
                                }
                                //return this.iterator.hasNext();   //TO DO: Not sure how to get next set of objects....
                                return false;
                        } else {
                                return false;
                        }
                } else {
                        return false;
                }
 */
    }
    
    public org.osid.repository.Asset nextAsset()
    throws org.osid.repository.RepositoryException {
        if (this.iterator.hasNext()) {
            numberAskedFor++;
            return (org.osid.repository.Asset)this.iterator.next();
        } else {
            throw new org.osid.repository.RepositoryException(org.osid.shared.SharedException.NO_MORE_ITERATOR_ELEMENTS);
        }
    }
}
