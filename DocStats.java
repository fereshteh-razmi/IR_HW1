import java.util.HashMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;


public class DocStats implements Comparable<DocStats> {

	public HashMap<String, Integer> term_freq = null;
	public int docLen = 0;
	public int doc_index;
	public String DocId;
	public double score;
	
	public DocStats(int doc_index, IndexReader indexReader, String DocId) throws Exception {
		  super();
		  this.doc_index = doc_index;
		  this.DocId = DocId;
	      Terms ts = indexReader.getTermVector(doc_index, "Text");
	      HashMap<String, Integer> term_freq = new HashMap<>();
	      TermsEnum te = ts.iterator();
	      BytesRef br = null;
	      int docLen = 0;
	      while ((br = te.next()) != null) {
	            term_freq.put(br.utf8ToString(), (int) te.totalTermFreq());
	            docLen += (int) te.totalTermFreq();
	      }
	      this.term_freq = term_freq;
	      this.docLen = docLen;
	}
	
	public void set_score(double score) {
		this.score = score;
	}

	@Override
	public int compareTo(DocStats compare_docStat) {
		//descending order
		double comparison_result = compare_docStat.score - this.score;
		if(comparison_result>0)
			return 1;
		if(comparison_result<0) 
			return -1;
		return 0;	
		 
	}

}
