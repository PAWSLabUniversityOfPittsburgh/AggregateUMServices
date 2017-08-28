

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
 * Servlet implementation class GetParsonsActivity
 */
@WebServlet("/GetParsonsActivity")
public class GetParsonsActivity extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static boolean verbose = false;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetParsonsActivity() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	@SuppressWarnings("unchecked")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		um2DBInterface um2_db;
		ConfigManager cm = new ConfigManager(this);
		
		/**
		 * Parse Json in request
		 */
		//Regard request as a InputSteam
		InputStreamReader reader = new InputStreamReader(request.getInputStream());
		
		
		
		//Use org.json.simple here
		JSONParser jsonParser = new JSONParser();
		JSONObject jsonObject;
		String usr = null;
		String grp = null;
		String domain = null;
		String dateFrom = null;
		//each provider has a content list
		ArrayList<String> contentList = new ArrayList<String>();
		//the key of map is provider name, the ArrayList stores content list of this provider
		Map<String, ArrayList<String>> provider_contentListMap = new HashMap<String, ArrayList<String>>();
		//Map<String, Double[]> 
		try {
			/**
			
{
	"user-id" : "adl01", 
	"group-id" : "ADL", 
	"domain" : "java", 
	"content-list-by-provider" : [
						{ "provider-id" : "parsons", "content-list" : [ "ps_hello","ps_simple_function","ps_simple_params","ps_return_bigger_or_none","ps_python_addition","ps_python_iteration_addition","ps_python_iteration_multiplication","ps_python_calculate_function","ps_python_nested_calls","ps_python_recursive_factorial","ps_python_class_person","ps_python_modulo_is_even","ps_python_list_iteration_zoo","ps_python_nested_lists_indexing","ps_python_list_indexing" ] }
	] 
}

			 */
			//Use parser to convert InputStreamReader to whole Json Object
			jsonObject = (JSONObject) jsonParser.parse(reader);
			usr = (String) jsonObject.get("user-id");
			grp = (String)jsonObject.get("group-id");
			domain = (String)jsonObject.get("domain");
			dateFrom = (String)jsonObject.get("date-from");
			if(verbose){
				System.out.println("The usr is: " + usr);
				System.out.println("The grp is: " + grp);
				System.out.println("The domain is: " + domain);
				
			}
			
			/**
			 * make output Json
			 */
			JSONObject totalObject = new JSONObject();
			totalObject.put("user-id", usr);
			totalObject.put("group-id", grp);
			if(dateFrom != null) totalObject.put("date-from", dateFrom);
			
			JSONArray outputCntListArray = new JSONArray();
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			
			JSONArray provider_cntListArray = (JSONArray)jsonObject.get("content-list-by-provider");
			if(verbose) System.out.println(provider_cntListArray);
			
			um2_db = new um2DBInterface(cm.um2_dbstring,cm.um2_dbuser,cm.um2_dbpass);
			um2_db.openConnection();
			HashMap<String, Object[]> parsons_activity = um2_db.getUserParsonsActivity(usr, domain, dateFrom);
			um2_db.closeConnection();
			
			
			
			//if(verbose) System.out.println(content_list_by_provider.toString());
			//For each object in this JSONArray
			Iterator ir = provider_cntListArray.iterator();
			while (ir.hasNext()) {
				contentList.clear();
				JSONObject each_content_list_by_provider = (JSONObject)ir.next();
				System.out.println(each_content_list_by_provider.toString());
				String provider_id = (String)each_content_list_by_provider.get("provider-id");
				//each_content_list_by_provider.
				JSONArray cntListArray = (JSONArray)each_content_list_by_provider.get("content-list");
				if (verbose) {
					System.out.println(provider_id);
					System.out.println(cntListArray.toJSONString());					
				}
				
				
				
				//Because there is no key in this array, we handle it in this way
				//http://stackoverflow.com/questions/23393312/parse-simple-json-array-without-key
				for (int i = 0; i < cntListArray.size(); i++) {
					  String content =  (String) cntListArray.get(i);
					  contentList.add(content);
					  
					  
					  if(verbose) System.out.println(content);
					  
					  Object[] currentParsonsAct = parsons_activity.get(content);
					  
					  JSONObject cntSummaryObj = new JSONObject();
					 
					  double progress = 0;
					  int nAttempts = 0;
					  double maxScore = 0.0;
					  double firstScore  = 0.0;
					  double succRate = 0.0;
					  String attemptSeq = "";
					    // get activity of salt content containing an Object array with the following columns:
					    // [0] Integer : attempts
					    // [1] Double  : first score 
					    // [2] Double  : maximum score 
					    // [3] Double  : succrate 
					    // [4] String  : attempts results (comma-separated)

					  if(currentParsonsAct == null){
						  if(verbose) System.out.println("no activity for parsons problem "+content);
					  }else{
						  if(verbose) System.out.println(Arrays.toString(currentParsonsAct));
						  try{
							  nAttempts = (Integer) currentParsonsAct[0];
							  firstScore = (Double) currentParsonsAct[1];
							  maxScore = (Double) currentParsonsAct[2];
							  succRate = (Double) currentParsonsAct[3];
							  attemptSeq = (String) currentParsonsAct[4];
							  
						  }catch(Exception e){}
						  
					  }
					  
					  cntSummaryObj.put("content-id", content);
					  cntSummaryObj.put("progress", maxScore);
					  cntSummaryObj.put("attempts", nAttempts);
					  cntSummaryObj.put("success-rate", succRate);
					  cntSummaryObj.put("annotation-count", -1);//??
					  cntSummaryObj.put("like-count", -1);
					  cntSummaryObj.put("time-spent", -1);
					  cntSummaryObj.put("sub-activities", -1);
					  cntSummaryObj.put("attempts-seq", attemptSeq);
					  outputCntListArray.add(cntSummaryObj);
					}
				//System.out.println(contentListArray);
				provider_contentListMap.put(provider_id, contentList);
			}
			
			/**
			 * continue make output Json
			 */
			totalObject.put("content-list", outputCntListArray);
			
			if (verbose) {
				System.out.println(totalObject.toString());
			}
			out.write(totalObject.toJSONString());
			
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}//end of doPost


}
