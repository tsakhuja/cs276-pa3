package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class SmallestWindowScorer extends BM25Scorer
{

	/////smallest window specific hyperparameters////////
    double B = 10;    
   //double boostmod = -1;
    
    //////////////////////////////
	
	public SmallestWindowScorer(Map<String, Double> idfs,Map<Query,Map<String, Document>> queryDict) 
	{
		super(idfs, queryDict);
		handleSmallestWindow();
	}

	
	public void handleSmallestWindow()
	{
		/*
		 * @//TODO : Your code here
		 */
	}

	//Custom comparator for pqueues of indexes
	public static Comparator<TreeSet<Integer>> ListComporator = new Comparator<TreeSet<Integer>>() {
		@Override
		public int compare(TreeSet<Integer> l1, TreeSet<Integer> l2){
			if (l1.isEmpty()) return -1;
			if (l2.isEmpty()) return 1;
			return l1.first()-l2.first();
		} 
	};

	public double checkWindow(Query q,String docstr,double curSmallestWindow)
	{
		if (docstr == null) return curSmallestWindow;
		TreeSet<TreeSet<Integer>> idxList = new TreeSet<TreeSet<Integer>>(ListComporator);
		//Build index list queues
		for (String term : q.queryWords){
			TreeSet<Integer> docList = new TreeSet<Integer>();
			int idx = docstr.indexOf(term,0);
			while (idx != -1){
				docList.add(idx);
				idx = docstr.indexOf(term,idx+1);
			}
			//Term not found
			if (docList.isEmpty()) return curSmallestWindow;
			//Add to overall index lists
			else idxList.add(docList);
		}
		return Math.min(curSmallestWindow, findSmallestWindow(idxList));
	}
	
	public double checkBody(Query q, Document d, double curSmallestWindow){
		TreeSet<TreeSet<Integer>> idxList = new TreeSet<TreeSet<Integer>>(ListComporator);
		for (String term : q.queryWords){
			if (!d.body_hits.containsKey(term)) return curSmallestWindow;
			idxList.add(new TreeSet<Integer>(d.body_hits.get(term)));
		}
		return Math.min(curSmallestWindow, findSmallestWindow(idxList));
	}
	
	double findSmallestWindow(TreeSet<TreeSet<Integer>> idxQ){
		//Find max value
		double w = Integer.MAX_VALUE;
		do{
			idxQ.add(idxQ.pollFirst());
			w = Math.min(w,idxQ.last().first() - idxQ.first().first());
			idxQ.first().pollFirst();
		}while (!idxQ.first().isEmpty());
		return w+1;
	}
	
	public double getBoost(Document d, Query q){
		double curSmallestWindow = Integer.MAX_VALUE;
		//url
		curSmallestWindow = checkWindow(q, d.url, curSmallestWindow);
		//title
		curSmallestWindow = checkWindow(q, d.title, curSmallestWindow);
		//header
		if (d.headers != null){
			for (String header : d.headers){
				curSmallestWindow = checkWindow(q, header, curSmallestWindow);
			}
		}
		//body
		if (d.body_hits != null){
			curSmallestWindow = checkBody(q, d, curSmallestWindow);
		}
		//anchor
		if (d.anchors != null){
			for (String anchor : d.anchors.keySet()){
				curSmallestWindow = checkWindow(q, anchor, curSmallestWindow);
			}
		}
		if (curSmallestWindow == Integer.MAX_VALUE) return 1;
		return 1+B*Math.pow(2,q.queryWords.size()-curSmallestWindow);
	}

	@Override
	public double getSimScore(Document d, Query q) {
		Map<String,Map<String, Double>> tfs = this.getDocTermFreqs(d,q);
		
		this.normalizeTFs(tfs, d, q);
		
		Map<String,Double> tfQuery = getQueryFreqs(q);
        return getNetScore(tfs,q,tfQuery,d) * getBoost(d, q);
	}

}
