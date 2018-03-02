import java.io.IOException;
import java.util.HashMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMSimilarity;
import org.apache.lucene.util.BytesRef;

public class RM1 {
	
	
	public int RM1search(IndexSearcher searcher, IndexReader indexReader, Query query) throws Exception { //term,rel_docs,query
		int topDoc_K = 50;
		int retreivedDoc_num = 1000;
		float mu = (float) 0.2; ////????
		int vocab_size = SearchFiles.vocabSize(indexReader);
		LMSimilarity current_sim = new LMDirichletSimilarity(mu);
	    searcher.setSimilarity(current_sim); 
	    TopDocs results = searcher.search(query, retreivedDoc_num); /////Search here 1
	    ScoreDoc[] hits = results.scoreDocs;
	    //int numTotalHits = Math.toIntExact(results.totalHits);
	    //hits = searcher.search(query, numTotalHits).scoreDocs;
	    int docId = 0;
	    DocStats doc_stat;
		int sum_P_ts = 0;
		HashMap<String, Integer> boosting_factors = new HashMap<>();
	    for (int i = 0; i < topDoc_K; i++) {
	    		//Document doc = searcher.doc(hits[i].doc);
	    		docId = hits[i].doc;
	    		doc_stat = new DocStats(docId, indexReader);
	    		int P_q = 1;
	    		String[] query_text = query.toString("Text").split(" ");
	    		int query_size = query_text.length;
	    		for (int j = 0; j < query_size; j++) {
	    			System.out.println(query_text[j]);
	    			P_q *= (doc_stat.term_freq.get(query_text[j]) + 1F) / (doc_stat.docLen + vocab_size);
	    		}
	    		int term_size = doc_stat.term_freq.size();
	    		Integer[][] P_t = new Integer[term_size][topDoc_K];
	    		int j = 0;
	    		for (HashMap.Entry<String, Integer> termFreq : doc_stat.term_freq.entrySet()) {
	    		    P_t[j++][i] = termFreq.getValue() / doc_stat.docLen;
	    		    Integer factor_value = boosting_factors.get(termFreq.getKey());
	    		    if (factor_value != null) {
	    		    		boosting_factors.put(termFreq.getKey(), termFreq.getValue() + factor_value);
	    		    } else {
	    		    		boosting_factors.put(termFreq.getKey(), termFreq.getValue());
	    		    }
	    		}
	    }
		sum_P_ts = 0;
		for (Integer value : boosting_factors.values()) {
			sum_P_ts += value;
		}
		for (HashMap.Entry<String, Integer> b_f : boosting_factors.entrySet()) {
			String key = b_f.getKey();
		    Integer value = b_f.getValue();
		    boosting_factors.put(key, value/sum_P_ts);
		}
		
		return 0;
	
	}
	


}
