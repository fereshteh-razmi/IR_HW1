
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.LMSimilarity;
import org.apache.lucene.search.similarities.SimilarityBase;


public class LaplanceLM extends LMSimilarity {

	private int vocab_size = 0;
	
	public LaplanceLM (int vocab_size) {
		this.vocab_size = vocab_size;
	}
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "LaplaceLM " + vocab_size;
	}

	@Override
	protected float score(BasicStats stats, float freq, float docLen) {
		// TODO Auto-generated method stub
	    return (float) (stats.getBoost() *
	            Math.log((freq + 1F) / (docLen + vocab_size)));
 
	    //((LMStats)stats).getCollectionProbability()   
	}
	



}
