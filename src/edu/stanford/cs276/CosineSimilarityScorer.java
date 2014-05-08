package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CosineSimilarityScorer extends AScorer
{
	public CosineSimilarityScorer(Map<String,Double> idfs)
	{
		super(idfs);
	}
	
	///////////////weights///////////////////////////
    double urlweight = 1;
    double titleweight  = 4;
    double bodyweight = 4;
    double headerweight = 10;
    double anchorweight = 10;
    
    double smoothingBodyLength = 500;
    //////////////////////////////////////////
	
	public double getNetScore(Map<String,Map<String, Double>> tfs, Query q, Map<String,Double> tfQuery,Document d)
	{
		double score = 0.0;
		// Compute weighted document vector
		double[] weightedNetTfs = new double[q.queryWords.size()];
		for (String type : tfs.keySet()) {
			Map<String, Double> tfMap = tfs.get(type);
			for (int i = 0; i < q.queryWords.size(); i++) {
				String s = q.queryWords.get(i);
				
				if (tfMap.containsKey(s)) {
					if (type.equals("url")) {
						weightedNetTfs[i] += urlweight * tfMap.get(s);
					} else if (type.equals("title")) {
						weightedNetTfs[i] += titleweight * tfMap.get(s);
					} else if (type.equals("body")) {
						weightedNetTfs[i] += bodyweight * tfMap.get(s);
					} else if (type.equals("header")) {
						weightedNetTfs[i] += headerweight * tfMap.get(s);
					} else if (type.equals("anchor")) {
						weightedNetTfs[i] += anchorweight * tfMap.get(s);
					}
				}
			}
		}
		
		// Compute dot product
		for (int i = 0; i < q.queryWords.size(); i++) {
			score += tfQuery.get(q.queryWords.get(i)) * weightedNetTfs[i];
		}

//		System.out.println("Score: "+ score);
		return score;
	}

	
	public void normalizeTFs(Map<String,Map<String, Double>> tfs,Document d, Query q)
	{
		// Normalize Document Vector
		for (Map<String, Double> tfMap : tfs.values()) {
			for (String t : tfMap.keySet()) {
				tfMap.put(t, tfMap.get(t) / (d.body_length + smoothingBodyLength));
			}
		}
	}

	
	@Override
	public double getSimScore(Document d, Query q) 
	{
		
		Map<String,Map<String, Double>> tfs = this.getDocTermFreqs(d,q);
		
		this.normalizeTFs(tfs, d, q);
		
		Map<String,Double> tfQuery = getQueryFreqs(q);
		
		// Apply idf weighting to query frequencies
		for (String term : tfQuery.keySet()) {
			double idf = idfs.containsKey(term) ? idfs.get(term) : idfs.get("_NONE_");
			double score = tfQuery.get(term) * idf;
			tfQuery.put(term, score);
		}
		
        return getNetScore(tfs,q,tfQuery,d);
	}

	
	
	
	
}
