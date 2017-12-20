

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
 * Servlet implementation class GetPCEXChallengeActivity
 */
@WebServlet("/GetPCEXChallengeActivity")
public class GetPCEXChallengeActivity extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static boolean verbose = false;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public GetPCEXChallengeActivity() {
		super();
		// TODO Auto-generated constructor stub
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
		// TODO Auto-generated method stub
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
			 * {
					"user­-id" : "dguerra", 
					"group­-id" : "test", 
					"domain" : "java",
					"date-from" : "2015-03-23 16:32:21", // leave empty or do not include to get all activity
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
			HashMap<String, String[]> problems_activity = this.getUserChallengesActivity (usr, dateFrom);

			//if(verbose) System.out.println(content_list_by_provider.toString());
			//For each object in this JSONArray
			Iterator ir = provider_cntListArray.iterator();
			while (ir.hasNext()) {
				contentList.clear();
				JSONObject each_content_list_by_provider = (JSONObject)ir.next();
				if (verbose) System.out.println(each_content_list_by_provider.toString());
				String provider_id = (String)each_content_list_by_provider.get("provider-id");
				//each_content_list_by_provider.
				JSONArray cntListArray = (JSONArray)each_content_list_by_provider.get("content-list");
				if (verbose) {
					System.out.println(provider_id);
					System.out.println(cntListArray.toJSONString());					
				}

				// if content items names are provided, just get the information of them
				if(cntListArray.size()>0){
					//Because there is no key in this array, we handle it in this way
					//http://stackoverflow.com/questions/23393312/parse-simple-json-array-without-key
					for (int i = 0; i < cntListArray.size(); i++) {
						String content =  (String) cntListArray.get(i);
						//System.out.println(value);
						contentList.add(content);

						//double[] cntSummary = getContentSummary(usr, domain, provider_id, content);

						String[] currentQuestionAct = problems_activity.get(content);
						JSONObject cntSummaryObj = new JSONObject();

						if(currentQuestionAct == null){
							if(verbose) System.out.println("currentQuestionAct is null");
						}
						//double progress = 0;
						double progress = 0;
						double nsuccess = 0;
						double attempts = 0;
						double successRate = 0;
						String attemptSeq = "";
						
						if(currentQuestionAct != null){
							if(verbose) System.out.println(Arrays.toString(currentQuestionAct));
							try{
								attempts = Double.parseDouble(currentQuestionAct[1]);
								nsuccess = Double.parseDouble(currentQuestionAct[2]);
								attemptSeq = currentQuestionAct[3];
								if (attempts>0) {
									successRate = nsuccess/attempts;
								}	
								if (nsuccess > 0)
									progress = 1;
								
							}catch(Exception e){progress = 0;nsuccess=0;successRate=0;attemptSeq="";}

						}

						cntSummaryObj.put("content-id", content);
						cntSummaryObj.put("progress", progress);
						cntSummaryObj.put("attempts", attempts);
						cntSummaryObj.put("success-rate", successRate);
						cntSummaryObj.put("annotation-count", -1);//??
						cntSummaryObj.put("like-count", -1);
						cntSummaryObj.put("time-spent", -1);
						cntSummaryObj.put("sub-activities", -1);
						cntSummaryObj.put("attempts-seq", attemptSeq);

						outputCntListArray.add(cntSummaryObj);
					}
				}else{ // in this case, content items names are not provided, and it gets all of what is collected from the database
					for(Map.Entry<String,String[]> contentActivity : problems_activity.entrySet()){
						JSONObject cntSummaryObj = new JSONObject();
						String[] currentQuestionAct = contentActivity.getValue();
						String content = contentActivity.getKey();
						double progress = 0;
						double nsuccess = 0;
						double attempts = 0;
						double successRate = 0;
						String attemptSeq = "";
						if(currentQuestionAct != null){
							if(verbose) System.out.println(Arrays.toString(currentQuestionAct));
							try{
								attempts = Double.parseDouble(currentQuestionAct[1]);
								nsuccess = Double.parseDouble(currentQuestionAct[2]);
								attemptSeq = currentQuestionAct[3];
								if (attempts>0) {
									successRate = nsuccess/attempts;
								}	
								if (nsuccess > 0)
									progress = 1;  
								
							}catch(Exception e){progress = 0;nsuccess=0;successRate=0;attemptSeq="";}

						}

						cntSummaryObj.put("content-id", content);
						cntSummaryObj.put("progress", progress);
						cntSummaryObj.put("attemps", attempts);
						cntSummaryObj.put("success-rate", successRate);
						cntSummaryObj.put("annotation-count", -1);//??
						cntSummaryObj.put("like-count", -1);
						cntSummaryObj.put("time-spent", -1);
						cntSummaryObj.put("sub-activities", -1);
						cntSummaryObj.put("attempts-seq", attemptSeq);

						outputCntListArray.add(cntSummaryObj);
					}
					
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

	/**
	 * The method to get PCEX challenges that student has attempted 
	 */
	public HashMap<String, String[]> getUserChallengesActivity(String usr, String dateFrom) {
		HashMap<String, String[]> qActivity = new HashMap<String, String[]>();
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
			qActivity = um2_db.getUserPCEXChallengesActivity(usr, dateFrom);
			um2_db.closeConnection();
		}
		return qActivity;
	}
}
