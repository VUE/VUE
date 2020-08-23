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

package tufts.vue;

import java.util.List;
import tufts.Util;

import edu.tufts.vue.metadata.VueMetadataElement;
import edu.tufts.vue.metadata.action.SearchAction;
import edu.tufts.vue.metadata.action.SearchAction.ResultOp;
import edu.tufts.vue.metadata.action.SearchAction.Operator;
import tufts.vue.gui.GUI;

/**
 * For saving and restoring searches.  Also used a runtime as a reference to the
 * saved search.  TODO: some of these fields are using LOCALIZED save values,
 * which is a bug.  It means a map with saved searches from a different locale
 * than VUE currently has set will not properly restore the searches.
 */
public class SearchData {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(SearchData.class);    

    private String searchSaveName;
    private String searchType;
    private String mapType;
    private Operator opLogical;
    private ResultOp opResult;
    private List<VueMetadataElement> dataList;

    public SearchData() {}

    // public SearchData(String searchSaveName, String searchType, String mapType, String resultType,
    //                   String andOrType, List<VueMetadataElement> dataList) {
    //     super();
    //     this.searchSaveName = searchSaveName;
    //     this.searchType = searchType;
    //     this.mapType = mapType;
    //     this.resultType = resultType;
    //     this.andOrType = andOrType;
    //     this.dataList = dataList;
    // }
	
    public String getSearchSaveName() {
        return searchSaveName;
    }
    public void setSearchSaveName(String searchSaveName) {
        this.searchSaveName = searchSaveName;
    }
    public String getSearchType() {
        return searchType;
    }
    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }
    public String getMapType() {
        return mapType;
    }
    public void setMapType(String mapType) {
        this.mapType = mapType;
    }
    /** @deprecated -- for castor persistance only */
    public String getResultType() {
        return opResult.key; 
    }
    /** for runtime -- no bean naming convention so castor will ignore */
    public ResultOp resultOp() {
        return opResult;
    }
    /** for runtime -- no bean naming convention so castor will ignore */
    public void putResultOp(ResultOp resultKey) {
        this.opResult = resultKey;
    }
    /** @deprecated -- for castor persistance only */
    public void setResultType(String savedValue) {
        putResultOp(decodeSavedResultOp(savedValue));
    }
    /**
     * All saved searches up till summer 2012 used the LOCALIZED string value of the result-action
     * type! That was a bug -- restoring searches properly only ever worked if we're in the same locale.
     *
     * RA_SELECT is returned as a default if the string cannot be understood.
     */
    private static ResultOp decodeSavedResultOp(String saved)
    {
        ResultOp matched;

        // first try key (2012 saves onward)
        matched = ResultOp.KeyMap.get(saved);
        //if (matched != null) Log.debug("matched key " + Util.tags(savedString) + " to " + GUI.name(matched));

        // then try matching against localized values (all save files prior to summer 2012)
        if (matched == null) {
            matched = ResultOp.ValueMap.get(saved);
            //if (matched != null) Log.debug("matched localized value " + Util.tags(savedString) + " to " + GUI.name(matched));
        }
        return matched == null ? SearchAction.RA_SELECT : matched;
    }
    
    /** for runtime -- no bean naming convention so castor will ignore */
    public Operator logicalOp() {
        return this.opLogical;
    }

    /** for runtime -- no bean naming convention so castor will ignore */
    public void putLogicalOp(Operator op) {
        this.opLogical = op;
    }
    /** @deprecated -- for castor persistance only */
    public String getAndOrType() {
        return this.opLogical.key;
    }
    /** @deprecated -- for castor persistance only */
    public void setAndOrType(String savedValue) {
        this.opLogical = decodeSavedLogicalOp(savedValue);
    }
    /** check keys, then localized values -- backward compat for old save files.  Defaults to OR on failures */
    private static Operator decodeSavedLogicalOp(String saved) {
        if (Operator.AND.key.equals(saved) || Operator.AND.localized.equals(saved))
            return Operator.AND;
        else
            return Operator.OR;
    }
    
    public List<VueMetadataElement> getDataList() {
        return dataList;
    }
    public void setDataList(List<VueMetadataElement> dataList) {
        this.dataList = dataList;
    }
    public String toString() {
        final StringBuilder b = new StringBuilder();
        //b.append("SearchData");
        b.append("[type=");
        b.append(Util.tags(getSearchType()));
        b.append(" loc=");
        b.append(Util.tags(getMapType()));
        b.append(" action=");
        b.append(Util.tags(getResultType()));
        b.append(" op=");
        b.append(Util.tags(getAndOrType()));
        b.append(" name=");
        b.append(Util.tags(searchSaveName));
        b.append(" terms:");
        for (VueMetadataElement vme : getDataList()) {
            b.append("\n\t\t");
            b.append(vme);
        }
        //b.append("]");
        return b.toString();
    }

}