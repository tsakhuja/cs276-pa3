package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BM25Scorer extends AScorer
{
	Map<Query,Map<String, Document>> queryDict;
	
	public BM25Scorer(Map<String,Double> idfs,Map<Query,Map<String, Document>> queryDict)
	{
		super(idfs);
		this.queryDict = queryDict;
		this.calcAverageLengths();
	}

	
	///////////////weights///////////////////////////
    double urlweight = -1;
    double titleweight  = -1;
    double bodyweight = -1;
    double headerweight = -1;
    double anchorweight = -1;
    
    ///////bm25 specific weights///////////////
    double burl=-1;
    double btitle=-1;
    double bheader=-1;
    double bbody=-1;
    double banchor=-1;

    double k1=-1;
    double pageRankLambda=-1;
    double pageRankLambdaPrime=-1;

    ////////////Page rank function//////////////////////
    int pageRankFunc = 0;//0-Log
    
    
    ////////////bm25 data structures--feel free to modify ////////
    
    Map<Document,Map<String,Double>> lengths;
    Map<String,Double> avgLengths;
    Map<Document,Double> pagerankScores;
    
    //////////////////////////////////////////
    
    //sets up average lengths for bm25, also handles pagerank
    public void calcAverageLengths()
    {
    	lengths = new HashMap<Document,Map<String,Double>>();
    	avgLengths = new HashMap<String,Double>();
    	pagerankScores = new HashMap<Document,Double>();
    	
    	//get lengths for each document
    	for (Query q : queryDict.keySet()){
    		for (String url : queryDict.get(q).keySet()){
    			Map<String,Double> docLengths = new HashMap<String, Double>();
    			Document d = queryDict.get(q).get(url);
    			for (String tfType : this.TFTYPES){
    				double l = 0;
    				if (tfType == "url"){
    					l = (double) parseUrl(d).length;
    				} else if (tfType == "title"){
    					l = (double) parseTitle(d).length;
    				} else if (tfType == "body"){
    					l = (double) d.body_length;
    				} else if (tfType == "body"){
    					l = (double) parseHeaders(d).length;
    				} else if (tfType == "anchors"){
        				//anchor
    					Map<String, Double> anchors = parseAnchors(d);
    					for (Double a : anchors.values()){
    						l+=a;
    					}
    				}
    				docLengths.put(tfType,l);
    				//Update overall counts
    				if (avgLengths.containsKey(tfType)){
    					avgLengths.put(tfType,avgLengths.get(tfType)+ l);
    				} else {
    					avgLengths.put(tfType,l);
    				}
    			}
    			//pagerank
    			pagerankScores.put(d, getPageRankScore(d.page_rank));
    			lengths.put(d,docLengths);
    		}

    	}
    	//normalize avgLengths
		for (String tfType : this.TFTYPES)
		{
			avgLengths.put(tfType,avgLengths.get(tfType)/lengths.keySet().size());
		}
    }

    ////////////////////////////////////
    double getPageRankScore(int pageRank){
    	switch(pageRankFunc){
    		default:
    			return Math.log(pageRankLambdaPrime + pageRank);
    	}
    }
    
    
	public double getNetScore(Map<String,Map<String, Double>> tfs, Query q, Map<String,Double> tfQuery,Document d)
	{
		double score = 0.0;
		//For each term
		for (String term : q.queryWords){
			//Weight each field
			double W = 0.0;
			for (String field : tfs.get(term).keySet()){
				//Get field parameter
				if (field == "url"){
					W += urlweight*tfs.get(term).get(field);
				} else if (field == "header"){
					W += headerweight*tfs.get(term).get(field);
				} else if (field == "body"){
					W += bodyweight*tfs.get(term).get(field);
				} else if (field == "title"){
					W += titleweight*tfs.get(term).get(field);
				} else if (field == "anchor"){
					W += anchorweight*tfs.get(term).get(field);
				}
			}
			//Score the term
			score += W/(k1+W)*tfQuery.get(term)+pageRankLambda*pagerankScores.get(d);
		}
		return score;
	}

	//do bm25 normalization
	public void normalizeTFs(Map<String,Map<String, Double>> tfs,Document d, Query q)
	{
		//For each term
		for (String term : q.queryWords){
			//For each field
			for (String field : tfs.get(term).keySet()){
				//Get field parameter
				double B = 0.0;
				if (field == "url"){
					B = burl;
				} else if (field == "header"){
					B = bheader;
				} else if (field == "body"){
					B = bbody;
				} else if (field == "title"){
					B = btitle;
				} else if (field == "anchor"){
					B = banchor;
				}
				//Normalize
				double denom = 1 + B*(lengths.get(d).get(field)/avgLengths.get(field)-1);
				tfs.get(term).put(field,tfs.get(term).get(field)/denom);
			}
		}
	}

	
	@Override
	public double getSimScore(Document d, Query q) 
	{
		
		Map<String,Map<String, Double>> tfs = this.getDocTermFreqs(d,q);
		
		this.normalizeTFs(tfs, d, q);
		
		Map<String,Double> tfQuery = getQueryFreqs(q);
		
        return getNetScore(tfs,q,tfQuery,d);
	}

	
	
	
}
