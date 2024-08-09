package eplprediction;

public class User {
private String name;
private int total;
private int matchday;

public User() {}

public User(String name, int total, int matchday) {
	super();
	this.name = name;
	this.total = total;
	this.matchday = matchday;
}

public String getName() {
	return name;
}

public void setName(String name) {
	this.name = name;
}

public int getTotal() {
	return total;
}

public void setTotal(int total) {
	this.total = total;
}

public int getMatchday() {
	return matchday;
}

public void setMatchday(int matchday) {
	this.matchday = matchday;
}

}
