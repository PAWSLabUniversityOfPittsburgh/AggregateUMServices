
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Servlet implementation class GetUserKnowledge
 */
@WebServlet("/GetUserKnowledge")
public class GetUserKnowledge extends HttpServlet {

	private static boolean verbose = true;
	private boolean contrainOutcomeLevel = true; // knowledge levels will be
													// computed only on outcome
													// concepts

	private static final long serialVersionUID = 1L;

	private String conceptLevelsServiceURL = "http://pawscomp2.sis.pitt.edu/cbum/ReportManager";

	private HashMap<String, ArrayList<String[]>> kcByContent; // for each
																// content there
																// is an array
																// list of kc
																// (concepts)
																// with id,
																// weight
																// (double) and
																// direction
																// (prerequisite/outcome)
	private HashMap<String, Double> kcSummary;
	private ArrayList<String[]> contentKnowledge;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public GetUserKnowledge() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
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
		try {
			/**
			 * {
					"user­-id" : "dguerra", 
					"group­-id" : "test", 
					"domain" : "java", 
					"content­-list-­by­provider" : 
					[
						{ 	"provider­-id" : "QJ", 
							"content­-list" : ["jVariables1","jVariables2"] }
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
				System.out.println("JSON content:"+ jsonObject.get("content-list-by-provider").toString());
			}
			
			
			JSONArray provider_cntListArray = (JSONArray)jsonObject.get("content-list-by-provider");
			
			//if(verbose) System.out.println(content_list_by_provider.toString());
			//For each object in this JSONArray
			Iterator ir = provider_cntListArray.iterator();
			while (ir.hasNext()) {
				JSONObject each_content_list_by_provider = (JSONObject)ir.next();
				//System.out.println(each_content_list_by_provider.toString());
				String provider_id = (String)each_content_list_by_provider.get("provider-id");
				JSONArray cntListArray = (JSONArray)each_content_list_by_provider.get("content-list");
				if (verbose) {
					System.out.println(provider_id);
					System.out.println(cntListArray.toString());					
				}
				//Because there is no key in this array, we handle it in this way
				//http://stackoverflow.com/questions/23393312/parse-simple-json-array-without-key
				for (int i = 0; i < cntListArray.size(); i++) {
					  String value =  (String) cntListArray.get(i);
					  //System.out.println(value);
					  contentList.add(value);
					}
				//System.out.println(contentListArray);
			}
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		

		

		// ----------------------------------------
		//String usr = "adl01";
		//String grp = "ADL";
		//String domain = "java";
		
		// ArrayList<String> contentList = new
		// ArrayList<String>(Arrays.asList(contentarray1));
//		ArrayList<String> contentList = new ArrayList<String>(
//				Arrays.asList(shortcontent));
		// contentList.addAll(Arrays.asList(contentarray2));
		// contentList.addAll(Arrays.asList(shortcontent));

		// contentList = new ArrayList();
		// ----------------------------------------

		um2DBInterface um2_db;
		ConfigManager cm = new ConfigManager(this);

		response.setContentType("application/json");
		PrintWriter out = response.getWriter();

		um2_db = new um2DBInterface(cm.um2_dbstring, cm.um2_dbuser,
				cm.um2_dbpass);
		um2_db.openConnection();

		// 1. GET THE MAPPING OF CONCEPT-CONTENT
		if (verbose)
			System.out.println("getting concept-content mapping");
		kcByContent = um2_db.getContentConcepts(domain, contentList);

		um2_db.closeConnection();

		// 2. GET KNOWLEDGE OF THE USER IN CONCEPTS
		// if(verbose) System.out.println("getting concept knowledge");

		kcSummary = this.getConceptLevels(usr, domain, grp);

		// 3. COMPUTE KNOWLEDGE FOR EACH CONTENT GIVING KNOWLEDGE IN CONCEPTS

		contentKnowledge = new ArrayList<String[]>();

		for (String content_name : contentList) {
			ArrayList<String[]> c_concepts = kcByContent.get(content_name);
			double sum_weights = 0.0;
			double user_concept_k = 0.0;
			double user_content_k = 0.0;
			// System.out.println(topic[0]); //
			if (c_concepts != null && kcSummary != null) {
				for (int j = 0; j < c_concepts.size(); j++) {
					String[] _concept = c_concepts.get(j);
					// if (_concept.length<2)
					// System.out.println(_concept[0]+" : "+_concept.length);

					String direction = _concept[2]; // outcome or prerequisite
					// only compute levels in outcome concepts (how much already
					// know that is new in the content?)
					if (direction.equalsIgnoreCase("outcome")
							|| !contrainOutcomeLevel) {
						String concept = _concept[0];
						double weight = Double.parseDouble(_concept[1]);
						if (kcSummary.get(concept) == null)
							user_concept_k = -1.0;
						else {
							user_concept_k = kcSummary.get(concept);
							sum_weights += weight;
							user_content_k += user_concept_k * weight;
						}

					}
				}

			}
			if (sum_weights == 0.0)
				user_content_k = 0.0;
			else
				user_content_k = user_content_k / sum_weights;
			if (verbose)
				System.out.println(content_name + " " + user_content_k);
			contentKnowledge.add(new String[] { content_name,
					"" + user_content_k });

		}

		// 4. GENERATE THE JSON OUTPUT
		String output = "{\n";
		output += "    \"user-id\" : \"" + usr + "\",\n";
		output += "    \"group-id\" : \"" + grp + "\",\n";
		output += "    \"domain\" : \"" + domain + "\",\n";
		output += "    \"content-list\" : [ \n";
		for (String[] c_data : contentKnowledge) {
			output += "        {\"content-id\":\"" + c_data[0]
					+ "\",\"knowledge\":" + c_data[1] + "},\n";
		}
		output = output.substring(0, output.length() - 2);
		output += "\n    ],\n";

		output += "    \"concepts\" : [ \n";
		for (Map.Entry<String, Double> concepts : kcSummary.entrySet()) {
			output += "        {\"concept-id\":\"" + concepts.getKey()
					+ "\",\"concept-name\":\"" + concepts.getKey()
					+ "\",\"knowledge\":" + concepts.getValue() + "},\n";
		}
		output = output.substring(0, output.length() - 2);
		output += "\n    ]\n";

		output += "}";
		out.write(output);

	}

	public HashMap<String, Double> getConceptLevels(String usr, String domain,
			String grp) {
		HashMap<String, Double> user_concept_knowledge_levels = new HashMap<String, Double>();
		try {
			URL url = null;
			if (domain.equalsIgnoreCase("java")) {
				url = new URL(conceptLevelsServiceURL
						+ "?typ=con&dir=out&frm=xml&app=25&dom=java_ontology"
						+ "&usr=" + URLEncoder.encode(usr, "UTF-8") + "&grp="
						+ URLEncoder.encode(grp, "UTF-8"));

			}

			if (domain.equalsIgnoreCase("sql")) {
				url = new URL(conceptLevelsServiceURL
						+ "?typ=con&dir=out&frm=xml&app=23&dom=sql_ontology"
						+ "&usr=" + URLEncoder.encode(usr, "UTF-8") + "&grp="
						+ URLEncoder.encode(grp, "UTF-8"));

			}
			// TODO @@@@
			if (domain.equalsIgnoreCase("c")) {
				url = new URL(conceptLevelsServiceURL
						+ "?typ=con&dir=out&frm=xml&app=23&dom=c_programming"
						+ "&usr=" + URLEncoder.encode(usr, "UTF-8") + "&grp="
						+ URLEncoder.encode(grp, "UTF-8"));

			}
			if (url != null)
				user_concept_knowledge_levels = processUserKnowledgeReport(url);
			// System.out.println(url.toString());
		} catch (Exception e) {
			user_concept_knowledge_levels = null;
			System.out.println("UM: Error in reporting UM for user " + usr);
			// e.printStackTrace();
		}
		return user_concept_knowledge_levels;

	}

	public static HashMap<String, Double> processUserKnowledgeReport(URL url) {

		HashMap<String, Double> userKnowledgeMap = new HashMap<String, Double>();
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(url.openStream());
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("concept");

			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;
					NodeList cogLevels = eElement
							.getElementsByTagName("cog_level");
					for (int i = 0; i < cogLevels.getLength(); i++) {
						Node cogLevelNode = cogLevels.item(i);
						if (cogLevelNode.getNodeType() == Node.ELEMENT_NODE) {
							Element cogLevel = (Element) cogLevelNode;
							if (getTagValue("name", cogLevel).trim().equals(
									"application")) {

								double level = 0.0;
								level = Double.parseDouble(getTagValue("value",
										cogLevel).trim());
								if(verbose) System.out.println(getTagValue("name",eElement)+" K="+level);
								userKnowledgeMap.put(
										getTagValue("name", eElement), level);
							}
						}
					}
				}
			}

		} catch (Exception e) {
			// e.printStackTrace();
			System.out.println("UM: Error in reporting UM. URL = " + url);
			return null;
		}
		return userKnowledgeMap;
	}

	private static String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0)
				.getChildNodes();
		Node nValue = (Node) nlList.item(0);
		return nValue.getNodeValue();
	}

}
