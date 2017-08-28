import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class GetContentConcepts
 */
@WebServlet("/GetContentConcepts")
public class GetContentConcepts extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetContentConcepts() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
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

        String domain = request.getParameter("domain"); // group id
        String output = "";
        if (domain == null || domain.length() < 1) {
            error = true;
            errorMsg = "domain identifier not provided or invalid";
        } else {

            um2_db = new um2DBInterface(cm.um2_dbstring, cm.um2_dbuser,cm.um2_dbpass);
            um2_db.openConnection();

            ArrayList<String[]> contentList = um2_db.getContentConcepts(domain);

            if (contentList == null) {
                error = true;
                errorMsg = "no content/concepts found";
            } else {

                output = "{ domain:\"" + domain + "\", content:[  \n";
                for (String[] content : contentList) {
                    output += "  {content_name: \"" + content[0]
                            + "\", concepts:\"" + content[1] + "\"},\n";
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

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        doGet(request,response);
    }

}
