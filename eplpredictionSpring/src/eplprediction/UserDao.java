package eplprediction;
import java.sql.ResultSet;  
import java.sql.SQLException;  
import java.util.ArrayList;  
import java.util.List;  
import org.springframework.dao.DataAccessException;  
import org.springframework.jdbc.core.JdbcTemplate;  
import org.springframework.jdbc.core.ResultSetExtractor;  

public class UserDao {
	private JdbcTemplate jdbcTemplate;

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}


	public int insertUser(String name){
		String query="insert into users values('"+name+"',0,-1')";
		return jdbcTemplate.update(query);
	}


	public int updateUser(User u){
		String query="update users set total='"+u.getTotal()+"',matchday='"+u.getMatchday()+"' where name='"+u.getName()+"' ";
		return jdbcTemplate.update(query);
	}


	public int resetUserMatchday(){
		String query="update users set matchday=-1";
		return jdbcTemplate.update(query);
	}


	public List<User> getUsers(){
		return jdbcTemplate.query("select * from users order by total desc",new ResultSetExtractor<List<User>>(){  
			@Override  
			public List<User> extractData(ResultSet rs) throws SQLException,  
			DataAccessException {  

				List<User> list = new ArrayList<User>();  
				while(rs.next()){  
					User u = new User();  
					u.setName(rs.getString(1));
					u.setTotal(rs.getInt(2)); 
					u.setMatchday(rs.getInt(3)); 
					list.add(u);  
				}  
				return list;  
			}  
		});  
	}  
}
