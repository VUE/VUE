package edu.tufts.vue.mbs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import java.net.URLDecoder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.google.common.collect.AbstractMapEntry;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.alchemyapi.api.AlchemyAPI;
import com.alchemyapi.api.AlchemyAPI_NamedEntityParams;
import com.alchemyapi.api.AlchemyAPI_ConceptParams;

import edu.tufts.vue.metadata.VueMetadataElement;

import tufts.vue.LWComponent;

public class AlchemyAnalyzer implements LWComponentAnalyzer {

    AlchemyAPI alchemy = null;
    String alchemyAPIKey = null;
    private static final String OK_STATUS = "OK";
    private static final String ANALYZER_NAME = "Alchemy Analyzer";

    private static final org.apache.log4j.Logger log =
        org.apache.log4j.Logger.getLogger(AlchemyAnalyzer.class);


    /* (non-Javadoc)
     * @see edu.tufts.vue.mbs.LWComponentAnalyzer#analyze(tufts.vue.LWComponent, boolean)
     */
    @SuppressWarnings("unchecked")
    public List<AnalyzerResult> analyze(LWComponent c, boolean tryFallback) {
        if (isEmpty(alchemyAPIKey))
            throw new RuntimeException("AlchemyAPI Key is not specified");
        if (null == c)
            throw new IllegalArgumentException("Illegal data specified to analyze.");

        StringBuilder strBuilder = new StringBuilder();

        if (!isEmpty(c.getLabel()))
            strBuilder.append(c.getLabel()).append(". ");
        if (!isEmpty(c.getNotes()))
            strBuilder.append(c.getNotes()).append(". ");

        if (null != c.getMetadataList() && !isEmpty(c.getMetadataList().getMetadata()))
            for (VueMetadataElement element : c.getMetadataList().getMetadata())
                if (null != element && !isEmpty(element.getValue()))
                    strBuilder.append(element.getValue()).append(". ");

        if (null != c.getResource() && null != c.getResource().getProperties() &&
            !isEmpty(c.getResource().getProperties().entries()))
                for(Entry entry : c.getResource().getProperties().entries())
                    if (null != entry && entry instanceof AbstractMapEntry)
                    {
                        final AbstractMapEntry aentry = (AbstractMapEntry)entry;
                        if (null != aentry.getKey())
                        {
                            final String key = aentry.getKey().toString().trim();
                            if (!isEmpty(key) &&
                                (key.startsWith("title") || key.startsWith("date") ||
                                 key.startsWith("creator") || key.startsWith("description")))
                                    strBuilder.append("The ").append(key).append(" is ")
                                              .append(aentry.getValue().toString().trim())
                                              .append(". ");
                        }
                    }

        List<AnalyzerResult> result = new ArrayList<AnalyzerResult>();
        final String context = strBuilder.toString();
        if (!isEmpty(context))
        {
            Document doc = null;
            try {
                doc = alchemy.TextGetRankedNamedEntities(context);
            } catch (Exception e) {
                log.error("Alchemy TextGetRankedNamedEntities request failed", e);
                throw new RuntimeException(e);
            }
            NodeList nodeList = doc.getElementsByTagName("entity");
            if (null != nodeList && nodeList.getLength() > 0)
            {
                result = new ArrayList<AnalyzerResult>(nodeList.getLength());
                for (int i = 0; i < nodeList.getLength(); ++i)
                {
                    AnalyzerResult res = parseEntity(nodeList.item(i),null);
                    if (null != res)
                        result.add(res);
                }
            }
        }

        return result;
    }

    /* (non-Javadoc)
     * @see edu.tufts.vue.mbs.LWComponentAnalyzer#analyze(tufts.vue.LWComponent)
     */
    public List<AnalyzerResult> analyze(LWComponent c) {
        return analyze(c,true);
    }

    /* (non-Javadoc)
     * @see edu.tufts.vue.mbs.LWComponentAnalyzer#analyzeResource(tufts.vue.LWComponent)
     */
    public Multimap<String, AnalyzerResult> analyzeResource(LWComponent c) throws Exception {
        if (isEmpty(alchemyAPIKey))
            throw new RuntimeException("AlchemyAPI Key is not specified");
        if (null == c || null == c.getResource() || null == c.getResource().getSpec())
            throw new IllegalArgumentException("Illegal resource specified to analyze.");

        Multimap<String, AnalyzerResult> result = Multimaps.newArrayListMultimap();

        Document doc = null;
        try {
            AlchemyAPI_NamedEntityParams entityParams = new AlchemyAPI_NamedEntityParams();
            entityParams.setSourceText(AlchemyAPI_NamedEntityParams.RAW);
            doc = alchemy.URLGetRankedNamedEntities(c.getResource().getSpec(), entityParams);
        } catch (Exception e) {
             log.error("Alchemy XXXX URLGetRankedNamedEntities request failed  -----", e);
            throw new RuntimeException(e);
        }

        NodeList nodeList = doc.getElementsByTagName("entity");
        if (null != nodeList && nodeList.getLength() > 0)
            for (int i = 0; i < nodeList.getLength(); ++i)
            {
                AnalyzerResult res = parseEntity(nodeList.item(i),null);
                if (null != res)
                    result.put(res.getType(), res);
            }
            
            
        try {
            AlchemyAPI_ConceptParams conceptParams = new AlchemyAPI_ConceptParams();
            conceptParams.setSourceText(AlchemyAPI_ConceptParams.RAW);
            doc = alchemy.URLGetRankedConcepts(c.getResource().getSpec());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Alchemy XXXX URLGetRankedNamedEntities request failed " + c.getResource().getSpec() + " -----", e);
            throw new RuntimeException(e);
        }
        
        nodeList = doc.getElementsByTagName("concept");
        if (null != nodeList && nodeList.getLength() > 0)
            for (int i = 0; i < nodeList.getLength(); ++i)
            {
                AnalyzerResult res = parseEntity(nodeList.item(i),"Concept");
                if (null != res) {
                    result.put(res.getType(), res);
                }
            }

        return result;
    }

    /* (non-Javadoc)
     * @see edu.tufts.vue.mbs.LWComponentAnalyzer#getAnalyzerName()
     */
    public String getAnalyzerName() { return ANALYZER_NAME; }

    public String GetAlchemyAPIKey() { return alchemyAPIKey; }
    public boolean IsAlchemyAPIKeySet() { return !isEmpty(alchemyAPIKey); }

    public void SetAlchemyAPIKey(String key)
    {
        if (isEmpty(key))
            throw new RuntimeException("AlchemyAPI Key is not specified");

        AlchemyAPI alchemy = AlchemyAPI.GetInstanceFromString(key);
        try {
            alchemy.TextGetLanguage("Mother and Father");
        } catch (Exception e) {
            log.error("Alchemy TextGetLanguage request failed", e);
            throw new RuntimeException(e);
        }

        alchemyAPIKey = key;
        this.alchemy = alchemy;
    }

    private AnalyzerResult parseEntity(Node entity, String defaultType) {
        int count = 0;
        double relevance = .0;
        String type = defaultType;
        String value = null;
        ArrayList subtypes = null;
        Element disambiguatedNode = null;

        for (Node child = entity.getFirstChild(); null != child; child = child.getNextSibling()) {
            if (Node.ELEMENT_NODE == child.getNodeType())
            {
                if ("type".equals(child.getNodeName()))
                    type = getElementValue(child);
                else if ("text".equals(child.getNodeName()))
                    try {
                        value = URLDecoder.decode(getElementValue(child), "UTF-8");
                    }
                    catch(Exception e) {
                        value = null;
                    }
                else if ("disambiguated".equals(child.getNodeName()))
                    disambiguatedNode = (Element)child;
                else try {
                    if ("relevance".equals(child.getNodeName()))
                        relevance = Double.valueOf(getElementValue(child));
                    else if ("count".equals(child.getNodeName()))
                        count = Integer.valueOf(getElementValue(child));
                } catch (NumberFormatException ex) {}
            }
        }
         if (null != disambiguatedNode)
         {
             NodeList nodeList = disambiguatedNode.getElementsByTagName("name");
             if (null != nodeList && nodeList.getLength() > 0)
             {
                String name = getElementValue(nodeList.item(0));
                if(!isEmpty(name))
                     value = name;
             }
             nodeList = disambiguatedNode.getElementsByTagName("subType");
             if( null != nodeList && nodeList.getLength() > 0 )
             {
                subtypes = new ArrayList();
                for( int i = 0; i<nodeList.getLength(); i++ ) {
                    subtypes.add(getElementValue(nodeList.item(i)));
                }
             }
         }

         if (!isEmpty(type) && !isEmpty(value)) {
            AnalyzerResult retResults = new AnalyzerResult(type, value, relevance, count);
            if( null != subtypes ) {
                retResults.initSubtypes();
                retResults.addSubtypes(subtypes);
            }
            return retResults;
        }

         return null;
    }

    private String getElementValue(Node node)
    {
        String value = null;

        NodeList nodes = node.getChildNodes();
        if (nodes.getLength() == 1)
        {
            Node child = nodes.item(0);
            if (null != child && child.getNodeType() == Node.TEXT_NODE)
                value = child.getNodeValue();
        }

        return value;
    }

    private boolean isEmpty(String str)
    {
        return (null == str || str.length() <= 0);
    }

    private <T extends Collection<?>> boolean isEmpty(T collection) {
        return (null == collection || collection.size() <= 0);
    }
}
