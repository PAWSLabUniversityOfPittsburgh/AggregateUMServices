

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
 * Servlet implementation class GetWEActivity
 */
@WebServlet("/GetSaltActivity")
public class GetSaltActivity extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static boolean verbose = false;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetSaltActivity() {
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
		//each provider has a content list
		ArrayList<String> contentList = new ArrayList<String>();
		//the key of map is provider name, the ArrayList stores content list of this provider
		Map<String, ArrayList<String>> provider_contentListMap = new HashMap<String, ArrayList<String>>();
		//Map<String, Double[]> 
		try {
			/**
			 * {
					"user­-id" : "dguerra", 
					"group­-id" : "test", 
					"domain" : "java", 
					"content­-list-­by­provider" : 
					[
						{ "provider-­id" : "WE", "content-­list" : [ "ex1","ex2" ] }, 
						{ "provider­-id" : "AE", "content­-list" : [ "ae1" ] }
					] 
				}
			 */
			//Use parser to convert InputStreamReader to whole Json Object
			jsonObject = (JSONObject) jsonParser.parse(reader);
			usr = (String) jsonObject.get("user-id");
			grp = (String)jsonObject.get("group-id");
			domain = (String)jsonObject.get("domain");
			
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
			JSONArray outputCntListArray = new JSONArray();
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			
			JSONArray provider_cntListArray = (JSONArray)jsonObject.get("content-list-by-provider");
			if(verbose) System.out.println(provider_cntListArray);
			
			um2_db = new um2DBInterface(cm.um2_dbstring,cm.um2_dbuser,cm.um2_dbpass);
			um2_db.openConnection();
			HashMap<String, Object[]> salt_activity = um2_db.getUserSaltActivity(usr, domain);
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
					  
					  //double[] cntSummary = getContentSummary(usr, domain, provider_id, content);
					  String lessletId = content.split("_")[1];
					  
					  if(verbose) System.out.print(content + "   ");
					  if(verbose) System.out.println(lessletId);
					  
					  Object[] currentLessletAct = salt_activity.get(lessletId);
					  
					  
					  
					  JSONObject cntSummaryObj = new JSONObject();

					 
					  double progress = 0;
					  int nDesc = 0;
					  int nExample = 0;
					  int nTest = 0;
					  double maxScore = 0.0;
					  double firstScore  = 0.0;
					  
					    // get activity of salt content containing an Object array with the following columns:
					    // [0] Integer : description views
					    // [1] Integer : example views
					    // [2] Integer : test views
					    // [3] Integer : test answers count
					    // [4] Double  : first score 
					    // [5] Double  : maximum score 
					    // [6] String  : test answers (comma-separated)
					  if(currentLessletAct == null){
						  if(verbose) System.out.println("no activity for lesslet "+lessletId);
					  }else{
						  if(verbose) System.out.println(Arrays.toString(currentLessletAct));
						  try{
							  nDesc = (Integer) currentLessletAct[0];
							  nExample = (Integer) currentLessletAct[1];
							  nTest = (Integer) currentLessletAct[2];
							  firstScore = (Double) currentLessletAct[4];
							  maxScore = (Double) currentLessletAct[5];
							  
//							  nDistAct = Double.parseDouble(currentExampleAct[2]);
//							  totalLines = Double.parseDouble(currentExampleAct[3]);
							  if(nDesc>0) progress = 0.15;
							  if(nDesc>0 && nExample>0) progress = 0.3;
							  if(nDesc>0 && nExample>0 && nTest>0) progress = 0.5;
							  if(firstScore > 0.75) progress = 1.0; 
							  else if(maxScore > 0.5) progress = 0.75;
							  
						  }catch(Exception e){}
						  
					  }
					  
					  cntSummaryObj.put("content-id", content);
					  cntSummaryObj.put("progress", progress);
					  cntSummaryObj.put("attempts", nDesc);
					  cntSummaryObj.put("success-rate", firstScore);
					  cntSummaryObj.put("annotation-count", -1);//??
					  cntSummaryObj.put("like-count", -1);
					  cntSummaryObj.put("time-spent", -1);
					  cntSummaryObj.put("sub-activities", 3);
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
