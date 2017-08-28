

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
 * Servlet implementation class GetConceptMapStructure
 */
@WebServlet("/GetConceptMapStructure")
public class GetConceptMapStructure extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private boolean verbose;
    private ConfigManager cm;
    private CMDBInterface db;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetConceptMapStructure() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		
		response.setContentType("application/json");
        response.setHeader("Access-Control-Allow-Origin", "*");
        
        cm = new ConfigManager(this);
        //System.out.println(cm.dbString+" "+cm.dbUser+" "+cm.dbPass);
        db = new CMDBInterface(cm.um2_dbstring,cm.um2_dbuser,cm.um2_dbpass);
        verbose = cm.verbose;
        
        String domain = request.getParameter("domain"); // java, py, sql
        String format = request.getParameter("format"); // json, plain
        if(domain == null) domain="java";
        if(format == null) format="json";
        	db.openConnection();
        	HashMap<Integer,Concept> concepts = db.getAllConcepts(domain);
        	HashMap<Integer,ConceptPair> pairs = db.getAllConceptPairs(concepts, domain);
        //	ConceptPair.checkPairs(concepts, pairs);
        db.closeConnection();
        
        PrintWriter out = response.getWriter();
        if(format.equalsIgnoreCase("json")){
            String output = "{ \"nodes\":"+conceptsToJson(concepts);
            output += ",\"links\":"+pairsToJson(pairs);
            output += "," + getTopicMap();
            output += "}";
            out.write(output);
        }
        
        out.close();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

	
	public String conceptsToJson(HashMap<Integer,Concept> concepts){
		String r = "[ ";
		for (Map.Entry<Integer, Concept> concept : concepts.entrySet()) {
			r += concept.getValue().toJsonString() + ",";
		}
		r = r.substring(0,r.length()-1);
		r += "]";
		return r;
	}
	
	public String pairsToJson(HashMap<Integer,ConceptPair> pairs){
		String r = "[ ";
		for (Map.Entry<Integer, ConceptPair> pair : pairs.entrySet()) {
			r += pair.getValue().toJsonString() + ",";
		}
		r = r.substring(0,r.length()-1);
		r += "]";
		return r;
	}
	
	public String getTopicMap(){
		return  "\"topic-map\" : ["
				+ "{\"t\":\"Arithmetic_Operations\",\"ct\":\"Arithmetic_Operations\"},"
				+ "{\"t\":\"ArrayList\",\"ct\":\"ArrayList\"},"
				+ "{\"t\":\"Arrays\",\"ct\":\"Arrays\"},"
				+ "{\"t\":\"Boolean_Expressions\",\"ct\":\"Boolean_Expressions\"},"
				+ "{\"t\":\"Classes\",\"ct\":\"Classes\"},"
				+ "{\"t\":\"Constants\",\"ct\":\"Constants\"},"
				+ "{\"t\":\"Decisions\",\"ct\":\"Decisions\"},"
				+ "{\"t\":\"Inheritance\",\"ct\":\"Inheritance\"},"
				+ "{\"t\":\"Loops\",\"ct\":\"Loops_do_while\"},"
				+ "{\"t\":\"Loops\",\"ct\":\"Loops_for\"},"
				+ "{\"t\":\"Loops\",\"ct\":\"Loops_while\"},"
				+ "{\"t\":\"Loops\",\"ct\":\"Nested_Loops\"},"
				+ "{\"t\":\"Objects\",\"ct\":\"Objects\"},"
				+ "{\"t\":\"Primitive_Data_Types\",\"ct\":\"Primitive_Data_Types\"},"
				+ "{\"t\":\"Strings\",\"ct\":\"Strings\"},"
				+ "{\"t\":\"Arrays\",\"ct\":\"Two-dimensional_Arrays\"},"
				+ "{\"t\":\"Variables\",\"ct\":\"Variables\"},"
				+ "{\"t\":\"Switch\",\"ct\":\"Switch\"},"
				+ "{\"t\":\"Interfaces\",\"ct\":\"Interfaces\"}]";
	}
}
