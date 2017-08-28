import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/GetUserInfo")
public class GetUserInfo extends HttpServlet {
	
	private String secureKey = "AHD91JSKC72";
	
	private static final long serialVersionUID = 1L;

    public GetUserInfo() {
        super();
    }


	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		boolean error = false;
		
		String errorMsg = "";
		um2DBInterface um2_db;
		ConfigManager cm = new ConfigManager(this); // this object gets the database connections values

		response.setContentType("application/json");
		PrintWriter out = response.getWriter();

		String usr = request.getParameter("usr"); // user name
		String secureKey = request.getParameter("key"); // key for including email and names
		String output = "";
		if(usr==null || usr.length()<3){
			error = true;
			errorMsg = "username not provided or invalid";
		}else{
		
			um2_db = new um2DBInterface(cm.um2_dbstring,cm.um2_dbuser,cm.um2_dbpass);
			um2_db.openConnection();
		
			String[] userData = um2_db.getUsrInfo(usr);
			if(userData==null || userData.length<1){
				error = true;
				errorMsg = "username does not exist in user model engine";
			}else{
				if(secureKey == null || !secureKey.equals(this.secureKey)) output = "{ learnerId:\""+usr+"\", learnerName:\"undefined\", learnerEmail:\"undefined\"}";
				else output = "{ learnerId:\""+usr+"\", learnerName:\""+userData[0]+"\", learnerEmail:\""+userData[1]+"\"}";
			}
			
			um2_db.closeConnection();
		}
		if (error){
			out.print("{ error: 1, errorMsg:\""+errorMsg+"\"}");
		}else{
			out.print(output);
		}

	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request,response);
	}

}
