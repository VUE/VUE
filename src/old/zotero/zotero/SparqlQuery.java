package edu.tufts.vue.zotero;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

public class SparqlQuery {

	public static ResultSet executeQuery(String queryString, Model model)
	{
		com.hp.hpl.jena.query.Query query = QueryFactory.create(queryString);

		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet results = qe.execSelect();
		
		// Important - free up resources used running the query
//		qe.close();
		
		return results;
	}
}
