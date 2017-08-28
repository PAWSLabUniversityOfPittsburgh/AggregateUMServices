import java.io.InputStream;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;


public class ConfigManager {
	public String um2_dbstring;
	public String um2_dbuser;
	public String um2_dbpass;
	public boolean verbose;
	
	private static String config_string = "./WEB-INF/config.xml";
	
	public ConfigManager(HttpServlet servlet){
		try{
			ServletContext context = servlet.getServletContext();
			//System.out.println(context.getContextPath());
			InputStream input = context.getResourceAsStream(config_string);
			if (input != null){
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(input);
				doc.getDocumentElement().normalize();

				um2_dbstring = doc.getElementsByTagName("um2_dbstring").item(0).getTextContent();
				um2_dbuser = doc.getElementsByTagName("um2_dbuser").item(0).getTextContent();
				um2_dbpass = doc.getElementsByTagName("um2_dbpass").item(0).getTextContent();
				verbose = doc.getElementsByTagName("verbose").item(0).getTextContent().equalsIgnoreCase("true");
			}else{
				System.out.println("config not found!");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
