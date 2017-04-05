package oracle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBManager {
	static private DBManager instance;
	private String driver="oracle.jdbc.driver.OracleDriver";
	private String url="jdbc:oracle:thin:@localhost:1521:XE";
	private String user="batman";
	private String password="1234";
	
	Connection con; //접속후 , 그 정보 담는 객체
	PreparedStatement pstmt;
	
	
	
	//다른 애들이 new를 막음
	private DBManager() {
		try {
			Class.forName(driver);
			con = DriverManager.getConnection(url,user,password);
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	static public DBManager getInstance(){
		if(instance==null){
			instance = new DBManager();
		}
		return instance;
	}
	
	//접속객체 반환
	public Connection getConnection(){
		return con;
	}
	
	//접속해제
	public void disConnect(Connection con){
		try {
			if(con!=null){
				con.close();
			}
		} catch (SQLException e) {

			e.printStackTrace();
		}
		
	}
	
}
