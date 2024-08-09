package eplprediction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class EplPrediction {

	public static void main(String[] args) throws IOException {

		dropUsersTable();
		createUsersTable();

		int matchday = 1;
		countPoints(matchday);
		printTable(matchday);

		countUserTotalPoints("Scarhead");
	}


	static void dropUsersTable() {
		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.postgresql.Driver");
			c = DriverManager
					.getConnection("jdbc:postgresql://localhost:5432/epldb",
							"postgres", "");
			System.out.println("Opened database successfully");

			stmt = c.createStatement();
			String sql = "DROP TABLE USERS;";
			stmt.executeUpdate(sql);
			stmt.close();
			c.close();
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName()+": "+ e.getMessage() );
			System.exit(0);
		}
		System.out.println("Table Users dropped successfully");
	}


	static void createUsersTable() {
		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.postgresql.Driver");
			c = DriverManager
					.getConnection("jdbc:postgresql://localhost:5432/epldb",
							"postgres", "");
			System.out.println("Opened database successfully");

			stmt = c.createStatement();
			String sql = "CREATE TABLE USERS " +
					"(NAME           TEXT    NOT NULL, " +
					" TOTAL          INT     NOT NULL, " + 
					" MATCHDAY       INT     NOT NULL)";
			stmt.executeUpdate(sql);
			stmt.close();
			c.close();
		} catch ( Exception e ) {
			System.err.println( e.getClass().getName()+": "+ e.getMessage() );
			System.exit(0);
		}
		System.out.println("Table Users created successfully");
	}


	static HashMap<String, String> createMatchesHashMap(int matchday) {
		Resource r = new ClassPathResource("matchdayBeans.xml");
		BeanFactory factory = new XmlBeanFactory(r);

		Matchday md =(Matchday)factory.getBean("md" + matchday);
		Map<String,String> matches = md.getMatches();

		matches = matches.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey().replaceAll("[^a-zA-Z]", ""), Map.Entry::getValue));

		return (HashMap<String, String>) matches;
	}


	static String readPredictionsString(int matchday) throws IOException {
		Path fileName= Path.of("predictions_" + matchday + ".txt");
		String str = Files.readString(fileName);

		return str;
	}


	static void countPoints(int matchday) throws IOException {
		HashMap<String, String> matches = createMatchesHashMap(matchday);
		String predictionsString = readPredictionsString(matchday);

		String[] predictions = predictionsString.split("QUOTE");

		Resource r = new ClassPathResource("applicationContext.xml");
		BeanFactory factory = new XmlBeanFactory(r);
		UserDao dao=(UserDao)factory.getBean("udao");

		dao.resetUserMatchday();

		try {
			File myObj = new File(matchday + "_voor.html");
			if (myObj.createNewFile()) {
				System.out.println("File created: " + myObj.getName());
			} else {
				System.out.println("File already exists.");
			}

			FileWriter myWriter = new FileWriter(matchday + "_voor.html");
			myWriter.write("<!DOCTYPE html>\r\n"
					+ "<html>\r\n"
					+ "<head>\r\n"
					+ "<style>\r\n"
					+ "table {\r\n"
					+ "  border: 1px solid black;\r\n"
					+ "  border-collapse: collapse;\r\n"
					+ "}\r\n"
					+ "th, td {\r\n"
					+ "  text-align: center;\r\n"
					+ "  padding: 4px;\r\n"
					+ "  border: 1px solid black;\r\n"
					+ "  border-collapse: collapse;\r\n"
					+ "}\r\n"
					+ "tr.green td{\r\n"
					+ "  background-color: green;\r\n"
					+ "  color: white;\r\n"
					+ "}\r\n"
					+ "</style>\r\n"
					+ "</head>\r\n"
					+ "<body>\r\n");

			myWriter.write("<h1>" + matchday + ". voor</h1>\r\n");

			for (int i = 1; i < predictions.length; i++) {

				String name = predictions[i].substring(1, predictions[i].indexOf(";"));
				System.out.println(i + ". " + name);
				int totalPoints = getUserTotal(name);
				int matchdayPoints = 0;
				int predictedMatches = 0;

				myWriter.write("<table>\r\n"
						+ "<tr><th>" + name + "</th><th title=\"Ennustus\">E</th><th title=\"Tulemus\">T</th></tr>\r\n");

				String[] userPredictions =  predictions[i].substring(predictions[i].indexOf("]")+1).strip().split("\\r?\\n|\\r");
				for (int j = 0; j < userPredictions.length; j++) {
					String predictedMatchDescription = userPredictions[j].substring(0, userPredictions[j].lastIndexOf(" "));
					String predictedMatchPrediction = userPredictions[j].substring(userPredictions[j].lastIndexOf(" ")+1).toLowerCase();

					predictedMatchDescription = predictedMatchDescription.replaceAll("[^a-zA-Z]", "");
					predictedMatchPrediction = predictedMatchPrediction.substring(0, 1);

					if (matches.get(predictedMatchDescription).equals(predictedMatchPrediction)){
						matchdayPoints++;					
						System.out.println(matchdayPoints + ": " + predictedMatchDescription);

						myWriter.write("<tr class=\"green\"><td>" + predictedMatchDescription + "</td><td colspan=\"2\">"+ predictedMatchPrediction +"</td></tr>\r\n");

					} else {
						myWriter.write("<tr><td>" + predictedMatchDescription + "</td><td>"+ predictedMatchPrediction +"</td><td>"+ matches.get(predictedMatchDescription) +"</td></tr>\r\n");
					}

					predictedMatches++;
				}

				totalPoints = totalPoints + matchdayPoints ;
				System.out.println("Total: " + totalPoints + ", predicted: " + predictedMatches);

				dao.updateUser(new User(name, totalPoints, matchdayPoints));

				myWriter.write("<tr ><td colspan=\"3\">"+ matchdayPoints +" p</td></tr>\r\n");
				myWriter.write("</table><br><br>\r\n");

				System.out.println();
			}

			myWriter.write("</body>\r\n</html>");
			myWriter.close();

		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

	}


	static void countUserTotalPoints(String name) throws IOException {
		int totalPoints = 0;

		for (int matchday = 1; matchday < 39; matchday++) {

			HashMap<String, String> matches = createMatchesHashMap(matchday);
			String predictionsString = readPredictionsString(matchday);
			String userPredictionsString = predictionsString.substring(predictionsString.indexOf(name));

			if (userPredictionsString.indexOf("QUOTE") > 0) {
				userPredictionsString = userPredictionsString.substring(0, userPredictionsString.indexOf("QUOTE"));
			}

			String[] userPredictions =  userPredictionsString.strip().split("\\r?\\n|\\r");
			for (int k = 1; k < userPredictions.length; k++) {
				String predictedMatchDescription = userPredictions[k].substring(0, userPredictions[k].lastIndexOf(" "));
				String predictedMatchPrediction = userPredictions[k].substring(userPredictions[k].lastIndexOf(" ")+1).toLowerCase();

				predictedMatchDescription = predictedMatchDescription.replaceAll("[^a-zA-Z]", "");
				predictedMatchPrediction = predictedMatchPrediction.substring(0, 1);

				if (matches.get(predictedMatchDescription).equals(predictedMatchPrediction)){
					totalPoints++;								
				}
			}
		}

		System.out.println();
		System.out.println(name + " : " + totalPoints);
		System.out.println();
	}



	static int getUserTotal(String name) {
		Connection c = null;
		Statement stmt = null;
		int total = 0;

		try {
			Class.forName("org.postgresql.Driver");
			c = DriverManager
					.getConnection("jdbc:postgresql://localhost:5432/epldb",
							"postgres", "");
			c.setAutoCommit(false);

			stmt = c.createStatement();

			String query = "SELECT TOTAL FROM USERS WHERE NAME='" + name + "';";
			ResultSet rs = stmt.executeQuery( query);
			if(rs.next()){
				total  = rs.getInt("total");
			} else {
				String sql = "INSERT INTO USERS (NAME,TOTAL,MATCHDAY) VALUES ('" + name + "', 0, 0);";
				stmt.executeUpdate(sql);
				c.commit();

				System.out.println("Inserted user: " + name);

				total = 0;
			}
			stmt.close();
			rs.close();			
			c.close();

		} catch ( Exception e ) {
			System.err.println( e.getClass().getName()+": "+ e.getMessage() );
			System.exit(0);
		}

		return total;
	}


	static void printTable(int matchday) {
		Resource r = new ClassPathResource("applicationContext.xml");
		BeanFactory factory = new XmlBeanFactory(r);
		UserDao dao=(UserDao)factory.getBean("udao");

		if (matchday < 38 ) {
			System.out.println("\nTabel pärast " + matchday + ". vooru:\n");
		} else {
			System.out.println("\nLõplik tabel:\n");
		}

		List<User> list=dao.getUsers();  

		int curTotalStart = 0;
		int curTotalEnd = 0;
		int curTotal = 0;
		String curTotalUsers = "";

		for(User u:list)   {
			String  name = u.getName();
			int totalPoints  = u.getTotal();
			int matchdayPoints  = u.getMatchday();

			if (totalPoints != curTotal) {
				if (curTotalEnd != curTotalStart) {
					System.out.println(curTotalStart + "-" + curTotalEnd + ". " + curTotalUsers);
				} else if (curTotalStart > 0){
					System.out.println(curTotalStart + ". " + curTotalUsers);
				}
				if (matchday > 1) {
					if (matchdayPoints > 0){
						curTotalUsers = name + " " + totalPoints + "p (+" + matchdayPoints + ")";
					} else if (matchdayPoints == 0){
						curTotalUsers = name + " " + totalPoints + "p (0)";
					} else {
						curTotalUsers = name + " " + totalPoints + "p (-)";
					}
				} else {
					curTotalUsers = name + " " + totalPoints + "p";
				}				

				curTotal = totalPoints;
				curTotalStart = curTotalEnd + 1;
				curTotalEnd = curTotalStart;
			} else {
				if (matchday > 1) {
					if (matchdayPoints > 0) {
						curTotalUsers = curTotalUsers + ", " + name + " " + totalPoints + "p (+" + matchdayPoints + ")";
					} else if (matchdayPoints == 0){
						curTotalUsers = curTotalUsers + ", " + name + " " + totalPoints + "p (0)";
					} else {
						curTotalUsers = curTotalUsers + ", " + name + " " + totalPoints + "p (-)";
					}
				} else {
					curTotalUsers = curTotalUsers + ", " + name + " " + totalPoints + "p";
				}

				curTotalEnd++; 
			}
		}

		if (curTotalEnd != curTotalStart) {
			System.out.println(curTotalStart + "-" + curTotalEnd + ". " + curTotalUsers);
		} else {
			System.out.println(curTotalStart + ". " + curTotalUsers);
		}
	}
}
