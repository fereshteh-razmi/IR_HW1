

public class TermWeight implements Comparable<TermWeight> {

	public String term = "";
	public double weight = 0;
	
	public TermWeight(String term, double weight) {
		super();
		this.term = term;
		this.weight = weight;
	}
	
	@Override
	public int compareTo(TermWeight compare_termWeight) {
		//descending order
		double comparison_result = compare_termWeight.weight - this.weight;
		if(comparison_result>0)
			return 1;
		if(comparison_result<0) 
			return -1;
		return 0;	
		 
	}
}