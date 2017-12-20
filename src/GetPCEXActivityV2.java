

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
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
 * Servlet implementation class GetPCEXActivityV2
 * This version is to calculate the progress for the example. The example progress is 1 if 
 * the students solves one challenge in the set or clicks on all clickable lines in the example.
 * Here, we use the same approach for calculating the progress for all groups.
 * @author roya
 */
@WebServlet("/GetPCEXActivityV2")
public class GetPCEXActivityV2 extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static boolean verbose = false;
	
	/*---------------------------------------------------------
	 * The set-activity relationship data structures (STATIC)
	 * --------------------------------------------------------- */			
	private static HashMap<String, ArrayList<String>> set_activities = null;
	private static HashMap<String, ArrayList<String>> set_challenges = null;


	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public GetPCEXActivityV2() {
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
			
			/*---------------------------------------------------------
			 * Getting Data that we need for computation of student progress
			 * --------------------------------------------------------- */			
			if (GetPCEXActivityV2.set_activities == null)
				set_activities = this.getActivitiesInPCEXSet(); //fill the static variable the first time
			if (GetPCEXActivityV2.set_challenges == null)
				set_challenges = this.getChallengesInPCEXSet(); //fill the static variable the first time

			HashMap<String, String[]> examples_activity = this.getUserExamplesActivity(usr, dateFrom); // activity report for the user u in ALL pcEx examples with appid XX
			HashMap<String, String[]> challenges_activity = this.getUserChallengesActivity(usr, dateFrom); // get activity report for the user u in ALL pcEx challenges with appid XX

			/*---------------------------------------------------------
			 * Iterate through the sets and compute progress for each
			 * --------------------------------------------------------- */	
			//define the variables that we need to compute the progress for each set
			double ex_progress, challenge_progress, total_challenge,
					challenge_nsuccess, challenge_nattempts,challenges_viewed, example_viewed;
			
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
						
						JSONObject cntSummaryObj = new JSONObject();
						double progress = 0;
						double nsuccess = 0;
						double attempts = 0;
						double successRate = 0;
						String attemptSeq = "";
						String ex_clicked = "";
						
						/*---------------------------------------------------------
						 * Compute progress for the i-th content (set)
						 * --------------------------------------------------------- */	
						//reset the progress variables before we start computing the progress for each set
						ex_progress = 0;
						challenge_progress = 0;
						total_challenge = 0; 
						challenge_nsuccess = 0;
						challenge_nattempts = 0;
						challenges_viewed = 0;//for baseline progress calculation
						example_viewed = 0;//for baseline progress calculation
						for (String a : set_activities.get(content) ) //Iterate through the activities of the set 
						{
							if ( examples_activity.containsKey(a) ) //check if the activity is an example 
							{
								example_viewed++; 
								ex_clicked = examples_activity.get(a)[4];
								try{
									//index 2 is nDistAct; index 3 is totalLines; (See GetWEActivity.java)
									ex_progress = Double.parseDouble(examples_activity.get(a)[2]) / Double.parseDouble(examples_activity.get(a)[3]); 
									/*
									 * Note that the first element of attemptSeq is the completion ratio in the example.
									 */
									attemptSeq = ""+ ex_progress;
								} catch (Exception e) {
									ex_progress = 0; 
								}
							} else if ( challenges_activity.containsKey(a) ) { //check if the activity is a challenge
								challenges_viewed++;
								total_challenge++;
								
								try{
									//index 2 progress (max result); (See GetPCRSActivity.java)
									challenge_nattempts = Double.parseDouble(challenges_activity.get(a)[1]); 
									challenge_nsuccess = Double.parseDouble(challenges_activity.get(a)[2]);
									if (challenge_nsuccess > 0)
										challenge_progress += 1;
									
									//additional info obtained from challenge attempts
									attempts += challenge_nattempts;
									nsuccess += challenge_nsuccess;
									
									/* 
									 * in case a challenge is solved correctly, we only add 1 to show the success on the challenge in the sequence
									 * note that we don't use challenges_activity.get(a)[3] because although there is a successful attempt on the challenge, the last
									 * attempt on the challenge might be wrong. Aggregate class always gets the last attempt in the sequence and pass it to MG UI, so
									 * if there is a correct attempt on the challenge, we just add 1 to the sequence. If there is no correct attempt in a challenge,
									 * then, the attemptSeq will only have example completion ratio, and aggregate will pass that to MG UI. 
						             */
									if (challenge_nsuccess > 0) {
										attemptSeq += ",1" ;
									}
									
								} catch (Exception e) { 
									
								}
							} else { // student does not have any attempt on this activity
								if (set_challenges.get(content).contains(a)){ //if this is a challenge, increments total challenges by 1
									
									total_challenge++;
								}
								  
							}
						}
								
						progress = (challenge_progress > 0 ? 1 : ex_progress); //the final progress for the set (content)
						
						if (attempts > 0)
							successRate = nsuccess / attempts;
						
						//if no clicks on the example lines and  no challenges solved (i.e., attemptSeq = ""), then set the attemptSeq to 0
						if (attemptSeq.isEmpty())
							attemptSeq = "0";
						
						cntSummaryObj.put("content-id", content);
						cntSummaryObj.put("progress", progress); //set progress
						cntSummaryObj.put("attempts", attempts);//total attempts on challenges in this set (sum attempts over all challenges)
						cntSummaryObj.put("success-rate", successRate); //overal success rate in the set (sum success over all challenges / total attempts over all challenges)
						cntSummaryObj.put("annotation-count", ex_clicked); //clicked lines in the example separated by ,
						cntSummaryObj.put("like-count", -1);
						cntSummaryObj.put("time-spent", -1);
						cntSummaryObj.put("sub-activities", nsuccess); //total succuesses on challenges in this set (sum success over all challenges)
						cntSummaryObj.put("attempts-seq", attemptSeq); //attemptSeq in the set (comma-separated list of all attempts; it does not show which attempts are for which challenge though) 

						outputCntListArray.add(cntSummaryObj);
					}
				}else{ // in this case, content items names are not provided, and it gets all of what is collected from the database
					
					for ( Map.Entry<String, ArrayList<String>> entry : set_activities.entrySet() )
					{
						String content = entry.getKey();  //pcEx set (content) name
						JSONObject cntSummaryObj = new JSONObject();
						
						double progress = 0;
						double nsuccess = 0;
						double attempts = 0;
						double successRate = 0;
						String attemptSeq = "";
						String ex_clicked = "";
						
						/*---------------------------------------------------------
						 * Compute progress for the i-th content (set)
						 * --------------------------------------------------------- */	
						//reset the progress variables before we start computing the progress for each set
						ex_progress = 0;
						challenge_progress = 0;
						total_challenge = 0; 
						challenge_nsuccess = 0;
						challenge_nattempts = 0;
						challenges_viewed = 0;//for baseline progress calculation
						example_viewed = 0;//for baseline progress calculation
						for (String a : set_activities.get(content) ) //Iterate through the activities of the set 
						{
							if ( examples_activity.containsKey(a) ) //check if the activity is an example 
							{
								example_viewed++; 
								ex_clicked = examples_activity.get(a)[4];
								try{
									//index 2 is nDistAct; index 3 is totalLines; (See GetWEActivity.java)
									ex_progress = Double.parseDouble(examples_activity.get(a)[2]) / Double.parseDouble(examples_activity.get(a)[3]); 
									/*
									 * Note that the first element of attemptSeq is the completion ratio in the example.
									 */
									attemptSeq = ""+ ex_progress;
								} catch (Exception e) {
									ex_progress = 0; 
								}
							} else if ( challenges_activity.containsKey(a) ) { //check if the activity is a challenge
								challenges_viewed++;
								total_challenge++;
								
								try{
									//index 2 progress (max result); (See GetPCRSActivity.java)
									challenge_nattempts = Double.parseDouble(challenges_activity.get(a)[1]); 
									challenge_nsuccess = Double.parseDouble(challenges_activity.get(a)[2]);
									if (challenge_nsuccess > 0)
										challenge_progress += 1;
									
									//additional info obtained from challenge attempts
									attempts += challenge_nattempts;
									nsuccess += challenge_nsuccess;
									
									
									/* 
									 * in case a challenge is solved correctly, we only add 1 to show the success on the challenge in the sequence
									 * note that we don't use challenges_activity.get(a)[3] because although there is a successful attempt on the challenge, the last
									 * attempt on the challenge might be wrong. Aggregate class always gets the last attempt in the sequence and pass it to MG UI, so
									 * if there is a correct attempt on the challenge, we just add 1 to the sequence. If there is no correct attempt in a challenge,
									 * then, the attemptSeq will only have example completion ratio, and aggregate will pass that to MG UI. 
						             */
									if (challenge_nsuccess > 0) {
										attemptSeq += ",1" ;
									}
									
								} catch (Exception e) { 
								}
							} else { // student does not have any attempt on this activity
								if (set_challenges.get(content).contains(a)){ //if this is a challenge, increments total challenges by 1
									
									total_challenge++;
								}
								  
							}
						}

						progress = (challenge_progress > 0 ? 1 : ex_progress); //the final progress for the set (content)

						if (attempts > 0)
							successRate = nsuccess / attempts;
						
						//if no clicks on the example lines and  no challenges solved (i.e., attemptSeq = ""), then set the attemptSeq to 0
						if (attemptSeq.isEmpty())
							attemptSeq = "0";
						
						cntSummaryObj.put("content-id", content);
						cntSummaryObj.put("progress", progress); //set progress
						cntSummaryObj.put("attempts", attempts);//total attempts on challenges in this set (sum attempts over all challenges)
						cntSummaryObj.put("success-rate", successRate); //overal success rate in the set (sum success over all challenges / total attempts over all challenges)
						cntSummaryObj.put("annotation-count", ex_clicked); //clicked lines in the example separated by ,
						cntSummaryObj.put("like-count", -1);
						cntSummaryObj.put("time-spent", -1);
						cntSummaryObj.put("sub-activities", nsuccess); //total succuesses on challenges in this set (sum success over all challenges)
						cntSummaryObj.put("attempts-seq", attemptSeq); //attemptSeq in the set (comma-separated list of all attempts; it does not show which attempts are for which challenge though) 

						
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
	 * The method to get mappings between sets and activities
	 */
	public HashMap<String, ArrayList<String>> getActivitiesInPCEXSet() {
		um2DBInterface um2_db;
		ConfigManager cm = new ConfigManager(this);
		HashMap<String, ArrayList<String>> setActivities = new HashMap<String, ArrayList<String>>();
		um2_db = new um2DBInterface(cm.um2_dbstring,cm.um2_dbuser,cm.um2_dbpass);
		um2_db.openConnection();
		setActivities = um2_db.getActivitiesInPCEXSet();
		um2_db.closeConnection();
    	return setActivities;
	}
	
	
	/**
	 * The method to get mappings between challeges and sets
	 */
	private HashMap<String, ArrayList<String>> getChallengesInPCEXSet() {
		um2DBInterface um2_db;
		ConfigManager cm = new ConfigManager(this);
		HashMap<String, ArrayList<String>> setChallenges = new HashMap<String, ArrayList<String>>();
		um2_db = new um2DBInterface(cm.um2_dbstring,cm.um2_dbuser,cm.um2_dbpass);
		um2_db.openConnection();
		setChallenges = um2_db.getChallengesInPCEXSets();
		um2_db.closeConnection();
    	return setChallenges;
	}
}
