

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Servlet implementation class GetLastActivity
 */
@WebServlet("/GetLastActivity")
public class GetLastActivity extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static boolean verbose = false;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetLastActivity() {
        super();
        // TODO Auto-generated constructor stub
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	@SuppressWarnings("unchecked")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String usr = request.getParameter("usr"); 
		String dateFrom = request.getParameter("dateFrom"); 
		String domain = request.getParameter("domain"); 
		
		
		ArrayList<String[]> userActivity = this.getUserActivity(usr, domain, dateFrom);
		String output = "{\"user-id\":\""+usr+"\", \"domain\":\""+domain+"\", \"date-from\":\""+dateFrom+"\", \"activity\":[";
		if(userActivity != null && userActivity.size() > 0){
			for(String[] s : userActivity){
				output += "{\"content-id\":\""+s[0]+"\", \"r\":"+s[1]+"},";
			}
			output = output.substring(0,output.length()-1);
		}
		output += "]}";
		
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();

		out.write(output);


	

	}//end of doPost

	public ArrayList<String[]> getUserActivity(String usr, String domain, String dateFrom) {
		ArrayList<String[]> userActivity = new ArrayList<String[]>();
		boolean error = false;
		String errorMsg = "";
		um2DBInterface um2_db;
		ConfigManager cm = new ConfigManager(this);
		if(usr==null || usr.length()<3){
			error = true;
			errorMsg = "user identifier not provided or invalid";
		}else{	
			um2_db = new um2DBInterface(cm.um2_dbstring,cm.um2_dbuser,cm.um2_dbpass);
			um2_db.openConnection();
			userActivity = um2_db.getUserActivity(usr, domain, dateFrom);
			um2_db.closeConnection();
		}
		return userActivity;
	}

}
