

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class GetConceptKnowledgeList
 */
@WebServlet("/GetConceptKnowledgeList")
public class GetConceptKnowledgeList extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private boolean verbose;
    private ConfigManager cm;
    private CMDBInterface db;
	/* 
	 * TODO 
	 * make it receive or query the concepts
	 * */
	private static String theConcepts = "AbstractMethodDefinition,AddAssignmentExpression,AddExpression,AndExpression,ArrayCreationStatement,ArrayDataType,ArrayElement,ArrayInitializationStatement,ArrayInitializer,ArrayLength,ArrayVariable,BooleanDataType,BreakStatement,CaseClause,CharDataType,ClassConstantInitializationStatement,ClassField,Constant,ConstantInitializationStatement,ConstantInvocation,ConstructorCall,ConstructorDefinition,DefaultClause,DivideExpression,DoStatement,DoubleDataType,EqualExpression,ExplicitTypeCasting,ExtendsSpecification,FALSE,FinalFieldSpecifier,FloatDataType,ForEachStatement,ForStatement,GenericObjectCreationStatement,GreaterEqualExpression,GreaterExpression,IfElseIfStatement,IfElseStatement,IfStatement,ImplementsSpecification,ImportStatement,InheritanceBasedPolymorphism,InstanceField,InstanceFieldInitializationStatement,InstanceFieldInvocation,IntDataType,InterfaceBasedPolymorphism,InterfaceClassConversion,InterfaceDefinition,LessEqualExpression,LessExpression,MethodImplementation,MethodInheritance,MethodOverriding,ModulusExpression,MultiDimensionalArrayDataType,MultiplyExpression,NestedForLoops,NotEqualExpression,NotExpression,ObjectCreationStatement,ObjectEquality,ObjectMethodInvocation,OrExpression,OverridingEquals,OverridingToString,PolymorphicObjectCreationStatement,PostDecrementExpression,PostIncrementExpression,PreDecrementExpression,PreIncrementExpression,PrivateFieldSpecifier,PublicConstructorSpecifier,PublicFieldSpecifier,PublicInterfaceSpecifier,ReturnStatement,SimpleAssignmentExpression,SimpleVariable,StaticFieldSpecifier,StringAddition,StringConstructorCall,StringCreationStatement,StringDataType,StringInitializationStatement,StringLiteral,StringLiteralMethodInvocation,StringVariable,SubtractExpression,SuperReference,SuperclassConstructorCall,SuperclassMethodCall,SuperclassSubclassConversion,SwitchStatement,TRUE,ThisReference,WhileStatement,java.lang.Double.parseDouble,java.lang.Integer.parseInt,java.lang.Math.abs,java.lang.Math.ceil,java.lang.Math.pow,java.lang.Math.round,java.lang.Math.sqrt,java.lang.Object,java.lang.String.charAt,java.lang.String.equals,java.lang.String.equalsIgnoreCase,java.lang.String.length,java.lang.String.replace,java.lang.String.substring,java.lang.System.out.print,java.lang.System.out.println,java.util.ArrayList,java.util.ArrayList.add,java.util.ArrayList.get,java.util.ArrayList.remove,java.util.ArrayList.set,java.util.ArrayList.size,null";
	private String conceptLevelsServiceURL = "http://pawscomp2.sis.pitt.edu/cbum/ReportManager";
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetConceptKnowledgeList() {
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
        
        
        String usr = request.getParameter("usr");
		String grp = request.getParameter("grp");
		String domain = request.getParameter("domain");
		String format = request.getParameter("format");
		if(domain == null) domain="java";
        if(format == null) format="json";
        
        HashMap<String, Double> conceptsK = null;
        
        if(domain.equalsIgnoreCase("java")){
        		URL url = new URL(conceptLevelsServiceURL
    				+ "?typ=con&dir=out&frm=xml&app=25&dom=java_ontology"
    				+ "&usr=" + URLEncoder.encode(usr, "UTF-8") + "&grp="
    				+ URLEncoder.encode(grp, "UTF-8"));
        		String output = "";
        		conceptsK = GetUserKnowledge.processUserKnowledgeReport(url);
        }
		

        db.openConnection();
    		HashMap<Integer,Concept> concepts = db.getAllConcepts(domain);
        	HashMap<Integer,ConceptPair> pairs = db.getAllConceptPairs(concepts, domain);
    		db.closeConnection();

		String output = "";
		
		//String[] conceptArray = theConcepts.split(",");
		DecimalFormat df = new DecimalFormat("#.##");
		
		if(format.equalsIgnoreCase("json")){
			output += "{ \"nodes\":[";
		}
		
		for (Map.Entry<Integer, Concept> entry : concepts.entrySet()) {
			double v = 0.0;
			double a = 0.0;
			Concept c = entry.getValue();
			if(conceptsK != null){
				try{ 
					v = conceptsK.get(c.getIdName());
					if(v > 0) a = 1.0;
				}catch(Exception e){
					v = -1;
					a = 0.0;
				}
			}				
			if(format.equalsIgnoreCase("json")){
				output += "{"+"\"id\":"+c.getId()+",\"uSR\": "+(0.0)+", \"uATT\": "+a+
							", \"uK\":"+df.format(v)+",\"uP\": "+df.format(v)+
							",\"gSR\":"+(0.0)+",\"gATT\":"+(0.0)+
							", \"gK\":"+(0.0)+",\"gP\":"+(0.0)+"},";
			}
		}
		output = output.substring(0,output.length()-1);
		output += "]";
		output += ",\"links\":[";
		for (Map.Entry<Integer, ConceptPair> pair : pairs.entrySet()) {
			ConceptPair cp = pair.getValue();
			if(format.equalsIgnoreCase("json")){	
				output += "{\"id\":"+cp.getId()+",\"uSR\":1.0,\"uATT\":1.0,\"uK\":1.0,\"uP\":1.0,\"gSR\":0.0,\"gK\":0.0,\"gP\":0.0,\"gATT\":0},";
			}
		}
		output = output.substring(0,output.length()-1);
		output += "]";
		output += "}";
		PrintWriter out = response.getWriter();
		out.print(output);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
