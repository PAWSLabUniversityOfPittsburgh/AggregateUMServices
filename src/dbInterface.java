import java.sql.*; 
import java.util.*;

import com.jcraft.jsch.Session;
public class dbInterface {
	protected String dbstring;
	protected String dbuser;
	protected String dbpass;
	
	protected Connection conn;
	protected Statement stmt = null; 
	protected ResultSet rs = null;
	private static Session session;
	
	public dbInterface(String connurl, String user, String pass){
		dbstring = connurl;
		dbuser = user;
		dbpass = pass;
		
	}
	
	public boolean openConnection(){
		try{
			
			if(session == null || session.isConnected()) {
				session = DBConnectionManager.getSession();
			}
			
			String schemaName = dbstring.substring(dbstring.lastIndexOf("/")+1);
			
			conn = DBConnectionManager.getConnection(session, schemaName);
			
			//Class.forName("com.mysql.jdbc.Driver").newInstance();
			//System.out.println(dbstring+"?"+ "user="+dbuser+"&password="+dbpass);
			//conn = DriverManager.getConnection(dbstring+"?"+ "user="+dbuser+"&password="+dbpass);
		
		}catch (Exception ex) {
			System.out.println("SQLException: " + ex.getMessage()); 
			closeConnection();
			return false;
		}
		
		return true; 
	}
	
	public  void closeConnection(){
		releaseStatement(stmt, rs);
		if (conn != null){
			DBConnectionManager.close(conn, session);
		}
	}
	
	
	public  void releaseStatement(Statement stmt, ResultSet rs){
		if (rs != null) {
			try { 
				rs.close();
			}catch (SQLException sqlEx) { sqlEx.printStackTrace(); } 
			rs = null;
		}
		if (stmt != null) {
			try{
				stmt.close();
			}catch (SQLException sqlEx) { sqlEx.printStackTrace(); } 
			stmt = null;
		}
	}
	
	
	
}
