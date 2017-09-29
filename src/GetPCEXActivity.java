

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
 * Servlet implementation class GetPCEXActivity
 * @author roya
 */
@WebServlet("/GetPCEXActivity")
public class GetPCEXActivity extends HttpServlet {
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
	public GetPCEXActivity() {
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
			if (GetPCEXActivity.set_activities == null)
				set_activities = this.getActivitiesInPCEXSet(); //fill the static variable the first time
			if (GetPCEXActivity.set_challenges == null)
				set_challenges = this.getChallengesInPCEXSet(); //fill the static variable the first time

			HashMap<String, String[]> examples_activity = this.getUserExamplesActivity(usr, dateFrom); // activity report for the user u in ALL pcEx examples with appid XX
			HashMap<String, String[]> challenges_activity = this.getUserChallengesActivity(usr, dateFrom); // get activity report for the user u in ALL pcEx challenges with appid XX

			/*---------------------------------------------------------
			 * Iterate through the sets and compute progress for each
			 * --------------------------------------------------------- */	
			//define the variables that we need to compute the progress for each set
			double ex_progress, set_progress, challenge_progress, total_challenge,
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
						String nsuccess = "";
						String attempts = "";
						String successRate = "";
						String attemptSeq = "";
						String ex_clicked = "";
						
						/*---------------------------------------------------------
						 * Compute progress for the i-th content (set)
						 * --------------------------------------------------------- */	
						//reset the progress variables before we start computing the progress for each set
						ex_progress = 0;
						challenge_progress = 0;
						total_challenge = 0; 
						set_progress = 0;
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
									
									//additional info per each challenge
									attempts += challenge_nattempts+";";
									nsuccess += challenge_nsuccess + ";";
									attemptSeq += challenges_activity.get(a)[3]+";";
									if (challenge_nattempts > 0) {
										successRate += challenge_nsuccess/challenge_nattempts +";";
									}else
										successRate += ";";
									
								} catch (Exception e) { 
									challenge_progress += 0;
									attempts += ";";
									nsuccess += ";";
									attemptSeq += ";";
									successRate += ";";
								}
							} else { // student does not have any attempt on this activity
								if (set_challenges.get(content).contains(a)){ //if this is a challenge, increments total challenges by 1
									
									total_challenge++;
								
									//add additional info
									attempts += "NA;";
									nsuccess += "NA;";
									attemptSeq += "NA;";
									successRate += "NA;";
								}
								  
							}
						}

						//computation for baseline and experimental group
						if (grp.contains("CONTROL") | grp.contains("G0"))
						{
							set_progress = (example_viewed + challenges_viewed)/set_activities.get(content).size();
							if (set_progress > 1.0)
							{
								System.out.println("Error in computing the pcex progrss - (Control group)" 
										  + " progress was greater than 1, so it got updated to 1. Set: " + content);
								set_progress = 1.0;
							}
						} else {
							//compute the progress
							if ( challenge_progress == 0 & challenges_viewed > 0 ) { //check if no challenge solved
								set_progress = 0.4 * ex_progress + 0.1 * (challenges_viewed/total_challenge) ;
							} else if ( challenge_progress < total_challenge ) { //check if not all challenges are solved
								set_progress = ( 0.4 * ex_progress ) + ( 0.6 * ( (double) challenge_progress / total_challenge ) );
							} else if ( challenge_progress == total_challenge ) { //check if all challenges are solved
								set_progress = 1.0;
							} else {  //check if number of solved challenges is greater than total challenges (ERROR)
								System.out.println("Error in computing the pcex progrss - number of solved challenges" 
												  + " greater than total challenge. Set: " + content);
								set_progress = 1.0;
							}
						}
						

						progress = set_progress; //the final progress for the set (content)
						
						cntSummaryObj.put("content-id", content);
						cntSummaryObj.put("progress", progress); //set progress
						cntSummaryObj.put("attempts", attempts);//attempts per challenge separated by ;
						cntSummaryObj.put("success-rate", successRate); //successrate per challenge separated by ;
						cntSummaryObj.put("annotation-count", ex_clicked); //clicked lines in the example separated by ,
						cntSummaryObj.put("like-count", -1);
						cntSummaryObj.put("time-spent", -1);
						cntSummaryObj.put("sub-activities", nsuccess); //nsuccess per challege separated by ;
						cntSummaryObj.put("attempts-seq", attemptSeq); //attemptSeq per challenge separated by ;

						outputCntListArray.add(cntSummaryObj);
					}
				}else{ // in this case, content items names are not provided, and it gets all of what is collected from the database
					
					for ( Map.Entry<String, ArrayList<String>> entry : set_activities.entrySet() )
					{
						String content = entry.getKey();  //pcEx set (content) name
						JSONObject cntSummaryObj = new JSONObject();
						
						double progress = 0;
						String nsuccess = "";
						String attempts = "";
						String successRate = "";
						String attemptSeq = "";
						String ex_clicked = "";
						
						/*---------------------------------------------------------
						 * Compute progress for the i-th content (set)
						 * --------------------------------------------------------- */	
						//reset the progress variables before we start computing the progress for each set
						ex_progress = 0;
						challenge_progress = 0;
						total_challenge = 0; 
						set_progress = 0;
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
									
									//additional info per each challenge
									attempts += challenge_nattempts+";";
									nsuccess += challenge_nsuccess + ";";
									attemptSeq += challenges_activity.get(a)[3]+";";
									if (challenge_nattempts > 0) {
										successRate += challenge_nsuccess/challenge_nattempts +";";
									}else
										successRate += ";";
									
								} catch (Exception e) { 
									challenge_progress += 0;
									attempts += ";";
									nsuccess += ";";
									attemptSeq += ";";
									successRate += ";";
								}
							} else { // student does not have any attempt on this activity
								if (set_challenges.get(content).contains(a)){ //if this is a challenge, increments total challenges by 1
									
									total_challenge++;
								
									//add additional info
									attempts += "NA;";
									nsuccess += "NA;";
									attemptSeq += "NA;";
									successRate += "NA;";
								}
								  
							}
						}

						//computation for baseline and experimental group
						if (grp.contains("CONTROL") | grp.contains("G0"))
						{
							set_progress = (example_viewed + challenges_viewed)/set_activities.get(content).size();
							if (set_progress > 1.0)
							{
								System.out.println("Error in computing the pcex progrss - (Control group)" 
										  + " progress was greater than 1, so it got updated to 1. Set: " + content);
								set_progress = 1.0;
							}
						} else {
							//compute the progress
							if ( challenge_progress == 0 & challenges_viewed > 0 ) { //check if no challenge solved
								set_progress = 0.4 * ex_progress + 0.1 * (challenges_viewed/total_challenge) ;
							} else if ( challenge_progress < total_challenge ) { //check if not all challenges are solved
								set_progress = ( 0.4 * ex_progress ) + ( 0.6 * ( (double) challenge_progress / total_challenge ) );
							} else if ( challenge_progress == total_challenge ) { //check if all challenges are solved
								set_progress = 1.0;
							} else {  //check if number of solved challenges is greater than total challenges (ERROR)
								System.out.println("Error in computing the pcex progrss - number of solved challenges" 
												  + " greater than total challenge. Set: " + content);
								set_progress = 1.0;
							}
						}
						

						progress = set_progress; //the final progress for the set (content)
						
						cntSummaryObj.put("content-id", content);
						cntSummaryObj.put("progress", progress); //set progress
						cntSummaryObj.put("attempts", attempts);//attempts per challenge separated by ;
						cntSummaryObj.put("success-rate", successRate); //successrate per challenge separated by ;
						cntSummaryObj.put("annotation-count", ex_clicked); //clicked lines in the example separated by ,
						cntSummaryObj.put("like-count", -1);
						cntSummaryObj.put("time-spent", -1);
						cntSummaryObj.put("sub-activities", nsuccess); //nsuccess per challege separated by ;
						cntSummaryObj.put("attempts-seq", attemptSeq); //attemptSeq per challenge separated by ;

						
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
