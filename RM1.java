import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMSimilarity;


public class RM1 {
	
	public IndexSearcher searcher;
	public IndexReader indexReader;
	
	/***  RM1 parameters ***/
	public int topDoc_K; //How many files to be used for query expansion
	public int retreivedDoc_num; //How many files to be ranked
	public int retreiveTerm_num; //How many term from topK docs to be included in expanded query
	//Default values. Can be changed in 
	public float mu; //for Drichlet LM 
	
	public RM1(IndexSearcher searcher, IndexReader indexReader, int topDoc_K, int retreivedDoc_num, int retreiveTerm_num) {
		this.searcher = searcher;
		this.indexReader = indexReader;
		this.topDoc_K = topDoc_K;
		this.retreivedDoc_num = retreivedDoc_num;
		this.retreiveTerm_num = retreiveTerm_num;
	}

	public ArrayList<DocStats> search(float mu_, Query query) throws Exception { //term,rel_docs,query
		
		/***  RM1 parameters ***/
		mu = mu_; 
		
		/***  Variables ***/
	    int doc_index = 0; //index (in Lucene library)
	    String DocId = ""; //field
	    DocStats doc_stat;
	    // Sum of P_t over all topK documents, its size is as large as distinct terms in topK docs
		HashMap<String, Double> all_boosting_factors = new HashMap<>(); 
		// Sorted sum of P_t over all topK documents, its size is retreiveTerm_num
		// It's basically our "new query set" + their weights
		HashMap<String, Double> top_boosting_factors = new HashMap<>(); 
		//find vocabulary size (number of words in the whole collection) in Text field
		int vocab_size = SearchFiles.vocabSize(indexReader);
		
		/***  Initial search ***/
		LMSimilarity current_sim = new LMDirichletSimilarity(mu);
	    searcher.setSimilarity(current_sim); 
	    TopDocs results = searcher.search(query, retreivedDoc_num); /////Search here 1
	    ScoreDoc[] hits = results.scoreDocs; //top docs indexes(ids) found by initial search
		double P_q = 1; //multiplication of P_q_i s for all query terms over each doc
	    for (int i = 0; i < topDoc_K; i++) {
	    		doc_index = hits[i].doc;
	    		Document doc = searcher.doc(doc_index);
	    		DocId = doc.get("DocId");
	    		doc_stat = new DocStats(doc_index, indexReader, DocId);
	    		String[] query_text = query.toString("Text").split(" ");
	    		int query_size = query_text.length;
	    		P_q = 1;
	    		for (int j = 0; j < query_size; j++) {
	    			Integer tf = doc_stat.term_freq.get(query_text[j]);
	    			tf = tf == null ? 0 : tf;
	    			P_q *= (tf + 1D) / (doc_stat.docLen + vocab_size);
	    		}
	    		
	    		/*** boosting_factors --- Sum of P_t over all documents***/
	    		for (HashMap.Entry<String, Integer> termFreq : doc_stat.term_freq.entrySet()) {
	    			double P_t_i = (double) termFreq.getValue() / doc_stat.docLen;
	    			String key = termFreq.getKey();
	    		    Double factor_value = all_boosting_factors.get(key);
	    		    if (factor_value != null) {
	    		    		all_boosting_factors.put(key, (P_t_i * P_q) + factor_value);
	    		    } else {
	    		    		all_boosting_factors.put(key, (P_t_i * P_q));
	    		    }
	    		}
	    }
	    

	    /***  top boosting_factors --- retreiveTerm_num selected top terms + their: Sum of P_t over all documents ***/	    
	    ArrayList<TermWeight> term_weights = new ArrayList<TermWeight>();
	    for (HashMap.Entry<String, Double> tw : all_boosting_factors.entrySet()) {
	    		term_weights.add(new TermWeight(tw.getKey(),tw.getValue()));
	    }
		Collections.sort(term_weights);
		int term_num = 1;
		for (TermWeight tw : term_weights) {
			if (term_num > retreiveTerm_num)
				break;
			//System.out.println(term_num+". "+tw.weight+", "+tw.term);
			top_boosting_factors.put((String) tw.term, (double) tw.weight);
			term_num ++;
		}

	    /***  Rerank documents ***/
        double doc_weight;
        String new_P_q_term = "";
        double new_P_q_val = 0;
        double boosting_val = 0;
        ArrayList<DocStats> final_docStats = new ArrayList<DocStats>(); //Final Docs to print, size is retreivedDoc_num
        for (int i = 0; i < retreivedDoc_num; i++) {
	        	doc_index = hits[i].doc;
	        	Document doc = searcher.doc(doc_index);
	        	DocId = doc.get("DocId");
	        	DocStats current_docStat = new DocStats(doc_index, indexReader, DocId);
	    		doc_weight = 0;
    	        Set set = top_boosting_factors.entrySet();
    	        Iterator iterator = set.iterator();
    	        while(iterator.hasNext()) {
    	        		Map.Entry me = (Map.Entry)iterator.next();
    	        		boosting_val = (double) me.getValue(); //boosting factor
    	        		new_P_q_term = (String) me.getKey(); //new query terms
    	    			Integer tf = current_docStat.term_freq.get(new_P_q_term);
    	    			tf = tf == null ? 0 : tf;
    	        		new_P_q_val = (tf + 1D) / (current_docStat.docLen + vocab_size); //new P_q_i
    	        		//System.out.println("boost:"+boosting_val+"log:"+Math.log(1 + new_P_q_val));
    	        		doc_weight += (boosting_val * Math.log(1 + new_P_q_val));
    	        }  
    	        //System.out.println(doc_weight);
    	        current_docStat.set_score(doc_weight);
    	        final_docStats.add(current_docStat);
        }
        Collections.sort(final_docStats);
        
        return final_docStats;

		
	}
}



