

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class GetExamplesActivity
 */
@WebServlet("/GetExamplesActivity")
public class GetExamplesActivity extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetExamplesActivity() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		boolean error = false;
		
		String errorMsg = "";
		um2DBInterface um2_db;
		ConfigManager cm = new ConfigManager(this); // this object gets the database connections values

		response.setContentType("application/json");
		PrintWriter out = response.getWriter();

		String usr = request.getParameter("usr"); // user name
		String output = "";
		if(usr==null || usr.length()<3){
			error = true;
			errorMsg = "user identifier not provided or invalid";
		}else{
		
			um2_db = new um2DBInterface(cm.um2_dbstring,cm.um2_dbuser,cm.um2_dbpass);
			um2_db.openConnection();
		
			HashMap<String,String[]> examplesActivity = um2_db.getUserExamplesActivity(usr);
			
			output = "{ learnerId:\""+usr+"\", content_type:\"example\", activity:[  \n";
			
			if(examplesActivity!=null){
				for (Map.Entry<String, String[]> activity : examplesActivity.entrySet()) {
					String example = activity.getKey();
					String[] details = activity.getValue();
			
					output += "  {content_name: \""+example+"\", nactions:"+details[1]+", " +
								"distinctactions:"+details[2]+", totallines:"+details[3]+","+
								"clicked:["+details[4]+"]},\n"; 
				}
			}
			output = output.substring(0,output.length()-2); 
			output +="\n]}";
			
			um2_db.closeConnection();
		}
		if (error){
			out.print("{ error: 1, errorMsg:\""+errorMsg+"\"}");
		}else{
			out.print(output);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request,response);
	}

}
