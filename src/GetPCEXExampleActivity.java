

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
@WebServlet("/GetPCEXExampleActivity")
public class GetPCEXExampleActivity extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static boolean verbose = false;
       
	/*---------------------------------------------------------
	 * The set-examples relationship data structures (STATIC)
	 * --------------------------------------------------------- */			
	private static HashMap<String,String> set_examples = null;

    /**
     * 
     * @see HttpServlet#HttpServlet()
     */
    public GetPCEXExampleActivity() {
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
			JSONArray outputCntListArray = new JSONArray();
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			
			
			/*---------------------------------------------------------
			 * Getting Data that we need for computation of student progress
			 * --------------------------------------------------------- */			
			if (GetPCEXExampleActivity.set_examples == null)
				set_examples = this.getExamplesInPCEXSet(); //fill the static variable the first time

			
			JSONArray provider_cntListArray = (JSONArray)jsonObject.get("content-list-by-provider");
			if(verbose) System.out.println(provider_cntListArray);
			HashMap<String, String[]> examples_activity = this.getUserExamplesActivity(usr, dateFrom);
			
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
					  //System.out.println(value);
					  contentList.add(content);
					  
					  //double[] cntSummary = getContentSummary(usr, domain, provider_id, content);
					  
					  //Note: content is in fact the set, we need to get progress on the example in the set
					  String[] currentExampleAct = examples_activity.get(set_examples.get(content));
					  JSONObject cntSummaryObj = new JSONObject();

					  if(currentExampleAct == null){
						  if(verbose) System.out.println("currentExampleAct is null");
					  }
					  double progress = 0;
					  double nDistAct = 0;
					  double totalLines = 0;
					  double attempts = 0;
					  if(currentExampleAct != null){
						  if(verbose) System.out.println(Arrays.toString(currentExampleAct));
						  try{
							  attempts = Double.parseDouble(currentExampleAct[1]);
							  nDistAct = Double.parseDouble(currentExampleAct[2]);
							  totalLines = Double.parseDouble(currentExampleAct[3]);
							  progress = nDistAct/totalLines;

						  }catch(Exception e){}
						  
					  }
					  
					  cntSummaryObj.put("content-id", content);
					  cntSummaryObj.put("progress", progress);
					  cntSummaryObj.put("attempts", attempts);
					  cntSummaryObj.put("success-rate", -1);
					  cntSummaryObj.put("annotation-count", -1);//??
					  cntSummaryObj.put("like-count", -1);
					  cntSummaryObj.put("time-spent", -1);
					  cntSummaryObj.put("sub-activities", totalLines);
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
			e.printStackTrace();
		}
		
		
	}//end of doPost


	/**
	 * The method to get PCEX examples that student has attempted 
	 */
	public HashMap<String, String[]> getUserExamplesActivity(String usr, String dateFrom) {
    	um2DBInterface um2_db;
		ConfigManager cm = new ConfigManager(this);
		Boolean error;
		String errorMsg;
		HashMap<String,String[]> examplesActivity = new HashMap<String, String[]>();
		if(usr==null || usr.length()<3){
			error = true;
			errorMsg = "user identifier not provided or invalid";
		}else{
			um2_db = new um2DBInterface(cm.um2_dbstring,cm.um2_dbuser,cm.um2_dbpass);
			um2_db.openConnection();
			examplesActivity = um2_db.getUserPCEXExamplesActivity(usr, dateFrom);
			um2_db.closeConnection();
		}
    	return examplesActivity;
    }
    
    /**
	 * The method to get mappings between sets and examples
	 */
	private HashMap<String, String> getExamplesInPCEXSet() {
		um2DBInterface um2_db;
		ConfigManager cm = new ConfigManager(this);
		HashMap<String,String> setExamples = new HashMap<String, String>();
		um2_db = new um2DBInterface(cm.um2_dbstring,cm.um2_dbuser,cm.um2_dbpass);
		um2_db.openConnection();
		setExamples = um2_db.getExamplesInPCEXSets();
		um2_db.closeConnection();
    	return setExamples;
	}


}
