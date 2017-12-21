import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import utils.JSONUtils;
import entity.ContentProgressSummary;
import entity.ProgressOutput;
import entity.ProgressRequest;
import entity.ProviderContent;

/**
 * Servlet implementation class GetSQLTUTORActivity
 * @author cskamil
 */
@WebServlet("/GetSQLTutorActivity")
public class GetSQLTutorActivity extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	public GetSQLTutorActivity() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		/**
		 * Parse Json in request
		 */
		ProgressRequest contentInput = JSONUtils.parseJSONToObject(request.getInputStream(), ProgressRequest.class);
		
		ProgressOutput progressOutput = new ProgressOutput(contentInput.getUserId(), contentInput.getGroudId(), contentInput.getDateFrom());
		
		
		String query = "SELECT " + 
			    "AC.activity AS activity," +
			    "COUNT(UA.activityid) AS nattempts," +
			    "MAX(result) AS progress," +
			    "GROUP_CONCAT(CAST(UA.Result AS CHAR) " +
			        "ORDER BY UA.datentime ASC " +
			        "SEPARATOR ',') AS attemptSeq " +
			"FROM " +
			    "um2.ent_user_activity UA INNER JOIN " +
			    "um2.ent_activity AC on UA.ActivityID = AC.ActivityID INNER JOIN " +
			    "um2.ent_user usr on UA.UserID = usr.UserID " +
			"WHERE " +
			    "UA.appid = 19 " +
					"AND usr.Login = '"+ contentInput.getUserId() + "' " +
			        "AND UA.Result != - 1 ";


			if(contentInput.getDateFrom() != null && contentInput.getDateFrom().length() > 0){
				query += "AND UA.datentime > '"+contentInput.getDateFrom()+"' ";
			}
			
			query += "GROUP BY UA.activityid;";
			
		System.out.println(query);
		

		HashMap<String, String[]> sqltutor_activity = this.getSQLTUTORActivity(contentInput.getUserId(), contentInput.getGroudId(), contentInput.getDateFrom());
		System.out.println(sqltutor_activity);
		ProviderContent[] providerContentList = contentInput.getProviderContentList();
		
		for (ProviderContent providerContent : providerContentList) {
			String[] contentList = providerContent.getContentList();
			
			if(contentList.length != 0) {
				for (String content : contentList) {
					ContentProgressSummary progressSummary = createProgressSummary(content, sqltutor_activity);
					progressOutput.addContentProgress(progressSummary);
				}
			} else {
				Set<Entry<String, String[]>> activitySet = sqltutor_activity.entrySet();
				for (Entry<String, String[]> contentActivity : activitySet) {
					ContentProgressSummary progressSummary = createProgressSummary(contentActivity.getKey(), sqltutor_activity);
					progressOutput.addContentProgress(progressSummary);
				}
			}
			
		}
		
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.write(JSONUtils.writeObjectAsJSONString(progressOutput));

	}

	private ContentProgressSummary createProgressSummary(String content, HashMap<String, String[]> sqltutor_activity) {
		String[] contentActivity = sqltutor_activity.get(content);
		ContentProgressSummary contentSummary = new ContentProgressSummary(content);
		
		if(contentActivity != null){
			try{
				contentSummary.setAttempts(Double.parseDouble(contentActivity[1]));
				contentSummary.setProgress(Double.parseDouble(contentActivity[2]));
				contentSummary.setAttemptsSequence(contentActivity[3]);
				if (contentSummary.getAttempts() > 0) {
					contentSummary.setSuccessRate(contentSummary.getProgress()/contentSummary.getAttempts());
				}	  
				
			}catch(Exception e){}

		}

		return contentSummary;
	}

	public HashMap<String, String[]> getSQLTUTORActivity(String usr, String domain, String dateFrom) {
		ConfigManager cm = new ConfigManager(this);
		um2DBInterface um2_db = new um2DBInterface(cm.um2_dbstring,cm.um2_dbuser,cm.um2_dbpass);
		
		um2_db.openConnection();
		HashMap<String, String[]> sqltutorActivityMap= um2_db.getUserSQLTUTORActivity(usr, dateFrom);
		um2_db.closeConnection();
		
		return sqltutorActivityMap;
	}
}
