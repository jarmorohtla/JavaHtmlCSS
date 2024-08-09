package eplprediction;
import java.util.Map;

public class Matchday {
	private Map<String,String> matches;


	public Map<String, String> getMatches() {
		return matches;
	}


	public Matchday() {}
	public Matchday(Map<String, String> matches) {
		super();
		this.matches = matches;
	}

}
