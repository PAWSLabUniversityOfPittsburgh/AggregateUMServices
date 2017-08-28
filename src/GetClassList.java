
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class GetClassList
 */
@WebServlet("/GetClassList")
public class GetClassList extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private String secureKey = "AHD91JSKC72";

    public GetClassList() {
        super();
    }

    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        boolean error = false;

        String errorMsg = "";
        um2DBInterface um2_db;
        ConfigManager cm = new ConfigManager(this); // this object gets the
                                                    // database connections
                                                    // values

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String grp = request.getParameter("grp"); // group id
        String secureKey = request.getParameter("key"); // key for including
                                                        // email and names

        String output = "";
        if (grp == null || grp.length() < 3) {
            error = true;
            errorMsg = "group identifier not provided or invalid";
        } else {

            um2_db = new um2DBInterface(cm.um2_dbstring, cm.um2_dbuser,
                    cm.um2_dbpass);
            um2_db.openConnection();

            ArrayList<String[]> classList = um2_db.getClassList(grp);

            if (classList == null) {
                error = true;
                errorMsg = "group has no members or does not exist";
            } else {

                output = "{ group:\"" + grp + "\", learners:[  \n";
                if (secureKey == null || !secureKey.equals(this.secureKey)) {
                    for (String[] student : classList) {
                        output += "  {learnerId: \""
                                + student[0]
                                + "\", name:\"undefined\", email:\"undefined\"},\n";
                    }
                } else {
                    for (String[] student : classList) {
                        output += "  {learnerId: \"" + student[0]
                                + "\", name:\"" + student[1] + "\", email:\""
                                + student[2] + "\"},\n";
                    }
                }

                output = output.substring(0, output.length() - 2);
                output += "\n]}";
            }

            um2_db.closeConnection();
        }
        if (error) {
            out.print("{ error: 1, errorMsg:\"" + errorMsg + "\"}");
        } else {
            out.print(output);
        }

    }

    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
