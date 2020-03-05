import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class um2DBInterface extends dbInterface {
    public um2DBInterface(String connurl, String user, String pass) {
        super(connurl, user, pass);
    }

    // returns the user information given the username
    public String[] getUsrInfo(String usr) {
        try {
            String[] res = null;
            stmt = conn.createStatement();
            String query = "select U.name, U.email from ent_user U where U.login = '"
                    + usr + "';";
            System.out.println(query);
            rs = stmt.executeQuery(query);
            while (rs.next()) {
                res = new String[2];
                res[0] = "";
                res[1] = "";
                res[0] = rs.getString("name").trim();
                res[1] = rs.getString("email").trim();
                System.out.println(res[0]);
                System.out.println(res[1]);
            }
            return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            return null;
        } finally {
            this.releaseStatement(stmt, rs);
        }

    }

    // returns the activity of the user in content of type example
    // TODO animated examples support @@@@
    public HashMap<String, String[]> getUserExamplesActivity(String usr) {
    	HashMap<String, String[]> res = new HashMap<String, String[]>();
    	try {
            
            stmt = conn.createStatement();
            /*
            String query = "select A.activity, "
                    + "AA.parentactivityid, count(UA.activityid) as nactions,  "
                    + "count(distinct(UA.activityid)) as distinctactions, "
                    + "(select count(AA2.childactivityid) from rel_activity_activity AA2 where AA2.parentactivityid = AA.parentactivityid and AA2.childactivityid not in (select activityid from ent_activity where active = 0)) as totallines "
                    + "from ent_user_activity UA, rel_activity_activity AA, ent_activity A "
                    + " where (UA.appid=3 OR UA.appid=35) and UA.userid = (select userid from ent_user where login='"+ usr+ "') "
                    + " and AA.parentactivityid=A.activityid and AA.childactivityid=UA.activityid "
                    + "group by AA.parentactivityid "
                    + "order by AA.parentactivityid;";
            */
            
            String query = "select A.activity,AA.parentactivityid, count(UA.activityid) as nactions,  "
            		+ "count(distinct(UA.activityid)) as distinctactions,  "
            		+ "(select count(AA2.childactivityid) from rel_activity_activity AA2 where AA2.parentactivityid = AA.parentactivityid  "
            		+ "and AA2.childactivityid not in (select activityid from ent_activity where active = 0)) as totallines,  "
            		+ "group_concat(distinct A2.activity separator ',') as clicked   "
            		+ "from ent_user_activity UA, rel_activity_activity AA, ent_activity A  "
            		+ " , ent_activity A2 "
            		+ "            where (UA.appid=3 OR UA.appid=35) and UA.userid = (select userid from ent_user where login='"+ usr+ "')  "
                    + "            and AA.parentactivityid=A.activityid and AA.childactivityid=UA.activityid  "
                    + "                                                    and A2.activityid = UA.activityid "
                    + "           group by AA.parentactivityid  "
                    + "            order by AA.parentactivityid;"; 
            
            // System.out.println(query);
            rs = stmt.executeQuery(query);
            // System.out.println(query);

            boolean noactivity = true;
            while (rs.next()) {
                noactivity = false;
                String[] act = new String[5];
                act[0] = rs.getString("activity");
                act[1] = rs.getString("nactions");
                act[2] = rs.getString("distinctactions");
                act[3] = rs.getString("totallines");
                act[4] = rs.getString("clicked");
                res.put(act[0], act);
                // System.out.println(act[0]+" actions: "+act[2]+", "+act[3]+"/"+act[4]);
            }
            this.releaseStatement(stmt, rs);
            
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            
        } finally {
            this.releaseStatement(stmt, rs);
        }
    	return res;

    }

    public HashMap<String, String[]> getUserQuestionsActivity(String usr) {
    	HashMap<String, String[]> res = new HashMap<String, String[]>();
        try {
            stmt = conn.createStatement();
            String query = "(select if(UA.appid=25,AC.activity,mid(AC.URI,INSTR(AC.URI,'#')+1)) as activity, count(UA.activityid) as nattempts,  sum(UA.Result) as nsuccess from um2.ent_user_activity UA, um2.ent_activity AC where (UA.appid=25 OR UA.appid=2) and UA.userid = (select userid from um2.ent_user where login='"
                    + usr
                    + "') and AC.activityid=UA.activityid and UA.Result != -1 group by UA.activityid) \n";
            query += " UNION ALL \n";
            query += "(select QN.content_name as activity, count(UA.activityid) as nattempts,  sum(UA.Result) as nsuccess "
                    + " from um2.ent_user_activity UA, um2.sql_question_names QN where UA.appid=23 and "
                    + " UA.userid = (select userid from um2.ent_user where login='" + usr + "') and "
                    + " QN.activityid=UA.activityid and UA.Result != -1  "
                    + " group by UA.activityid); ";

            // System.out.println(query);
            rs = stmt.executeQuery(query);
            boolean noactivity = true;
            while (rs.next()) {
                noactivity = false;
                String[] act = new String[3];
                act[0] = rs.getString("activity");
                act[1] = rs.getString("nattempts");
                act[2] = rs.getString("nsuccess");
                if (act[0].length() > 0)
                    res.put(act[0], act);
            }
            this.releaseStatement(stmt, rs);
            
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            
        } finally {
            this.releaseStatement(stmt, rs);
        }
        return res;
    }
    
    
    // get activity of salt content containing an Object array with the following columns:
    // [0] Integer : description views
    // [1] Integer : example views
    // [2] Integer : test views
    // [3] Integer : test answers count
    // [4] Double  : first score 
    // [5] Double  : maximum score 
    // [6] String  : test answers (comma-separated)
    public HashMap<String, Object[]> getUserSaltActivity(String usr, String domain) {
    	HashMap<String, Object[]> res = new HashMap<String, Object[]>();
    	try {
            
            stmt = conn.createStatement();
            String query = "select AC.activity, count(UA.activityid) as views, max(result) as maxscore, group_concat(result order by UA.datentime asc separator ',') as scoresconcat "
            		+ "from ent_user_activity UA, ent_activity AC "
            		+ "WHERE UA.appid=37 and AC.appid=37 and UA.activityid = AC.activityid "
            		+ "and userid = (select userid from um2.ent_user where login='"+usr+"') "
            		+ "group by UA.activityid order by activity asc";

            //System.out.println(query);
            rs = stmt.executeQuery(query);
            boolean noactivity = true;
            
            String lessletId;
            while (rs.next()) {
            	String subact = rs.getString("activity"); // first part is the lesslet id
            	String[] parts = subact.split("_");
            	if(parts != null && parts.length == 2){
            		lessletId = parts[0];
            		subact = parts[1];
            		
            		Object[] lessletData = res.get(lessletId);
            		if(lessletData == null){
            			lessletData = new Object[7];
            			lessletData[0] = new Integer(0);
            			lessletData[1] = new Integer(0);
            			lessletData[2] = new Integer(0);
            			lessletData[3] = new Integer(0);
            			lessletData[4] = new Double(0);
            			lessletData[5] = new Double(0);
            			lessletData[6] = "";
            			res.put(lessletId, lessletData);
            		}
            		
            		if(subact.equalsIgnoreCase("description")){
            			lessletData[0] = new Integer(rs.getInt("views")); 
            		}else if(subact.equalsIgnoreCase("example")){
            			lessletData[1] = new Integer(rs.getInt("views")); 
            		}else if(subact.equalsIgnoreCase("test")){
            			lessletData[2] = new Integer(rs.getInt("views")); 
            			lessletData[3] = new Integer(rs.getInt("views")); // @@@@ 
            			String[] scores = rs.getString("scoresconcat").split(",");
            			if(scores != null && scores.length>0){
            				lessletData[4] = Double.parseDouble(scores[0]);
            				lessletData[5] = rs.getDouble("maxscore");
            				lessletData[6] = rs.getString("scoresconcat");
            			}
            			
            		}
            		
            		
            	}
            	
                noactivity = false;
                
            }
            this.releaseStatement(stmt, rs);
                
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            
        } finally {
            this.releaseStatement(stmt, rs);
        }
    	return res;
    }
    
    
 // get activity of salt content containing an Object array with the following columns:
    // [0] Integer : attempts
    // [1] Double  : first score 
    // [2] Double  : maximum score 
    // [3] Double  : succrate
    // [4] String  : attempts results (comma-separated)
    // date in format YYYY-MM-DD HH:MM:SS or null
    public HashMap<String, Object[]> getUserParsonsActivity(String usr, String domain, String dateFrom) {
    	HashMap<String, Object[]> res = new HashMap<String, Object[]>();
    	try {
            
            stmt = conn.createStatement();
            String query = "select AC.activity, count(UA.activityid) as attempts, max(result) as maxscore, "
            		//+ "group_concat(result order by UA.datentime asc separator ',') as scoresconcat, "
            		+ "GROUP_CONCAT(cast(UA.Result as char) order by UA.datentime asc separator ',') as attemptSeq, "
            		+ "sum(result)/count(UA.activityid) as succrate "
            		+ "from ent_user_activity UA, ent_activity AC "
            		+ "WHERE UA.appid=38 AND UA.activityid = AC.activityid "
            		+ "AND userid = (select userid from um2.ent_user where login='"+usr+"') ";
            if(dateFrom != null && dateFrom.length() > 0){
            	query += "AND UA.datentime > '"+dateFrom+"' ";
            }		
            query +=  "group by UA.activityid order by activity asc";

            //System.out.println(query);
            rs = stmt.executeQuery(query);
            boolean noactivity = true;
            
            String parsonsId;
            while (rs.next()) {
            	parsonsId = rs.getString("activity"); // first part is the content id
            	//String[] parts = subact.split("_");
            	//if(parts != null && parts.length == 2){
            		//parsonsId = parts[0];
            		//subact = parts[1];
            		
            		Object[] parsonsData = res.get(parsonsId);
            		if(parsonsData == null){
            			parsonsData = new Object[5];
//            			parsonsData[0] = new Integer(0);
//            			parsonsData[1] = new Double(0);
//            			parsonsData[2] = new Double(0);
//            			parsonsData[3] = "";
            			res.put(parsonsId, parsonsData);
            		}
            		parsonsData[0] =  new Integer(rs.getInt("attempts")); 
            		parsonsData[2] = rs.getDouble("maxscore"); 
            		parsonsData[3] = rs.getDouble("succrate"); 
            		parsonsData[4] = rs.getString("attemptSeq"); 
        			String[] scores = rs.getString("attemptSeq").split(",");
        			if(scores != null && scores.length>0){
        				parsonsData[1] = Double.parseDouble(scores[0]);
        			}
            			
            	//}
            	
                noactivity = false;
                
            }
            this.releaseStatement(stmt, rs);
            
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            
        } finally {
            this.releaseStatement(stmt, rs);
        }
    	return res;
    }
    
    public HashMap<String, String[]> getUserQuizPackActivity(String usr) {
    	HashMap<String, String[]> res = new HashMap<String, String[]>();
    	try {
            
            stmt = conn.createStatement();
            String query = "select mid(AC.URI,INSTR(AC.URI,'#')+1) as activity, count(UA.activityid) as nattempts,  sum(UA.Result) as nsuccess from um2.ent_user_activity UA, um2.ent_activity AC "
            		+ " WHERE  UA.appid=2 and UA.userid = (select userid from um2.ent_user where login='"
                    + usr + "') and AC.activityid=UA.activityid and UA.Result != -1 group by UA.activityid;";

            // System.out.println(query);
            rs = stmt.executeQuery(query);
            boolean noactivity = true;
            while (rs.next()) {
                noactivity = false;
                String[] act = new String[3];
                act[0] = rs.getString("activity");
                act[1] = rs.getString("nattempts");
                act[2] = rs.getString("nsuccess");
                if (act[0].length() > 0)
                    res.put(act[0], act);
            }
            this.releaseStatement(stmt, rs);
//            if (noactivity)
//                return null;
//            else
//                return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            //return null;
        } finally {
            this.releaseStatement(stmt, rs);
        }
        return res;
    }

    public HashMap<String, String[]> getUserSQLKnotActivity(String usr) {
    	HashMap<String, String[]> res = new HashMap<String, String[]>();
    	try {
            
            stmt = conn.createStatement();
            //Old query which does not take into account the success rate on the k-th last attempts
//            String query = "select QN.content_name as activity, count(UA.activityid) as nattempts,  sum(UA.Result) as nsuccess "
//                    + " from um2.ent_user_activity UA, um2.sql_question_names QN where UA.appid=23 and "
//                    + " UA.userid = (select userid from um2.ent_user where login='" + usr + "') and "
//                    + " QN.activityid=UA.activityid and UA.Result != -1  "
//                    + " group by UA.activityid; ";
            int kLastResults = 15;//@Jordan pending make it parameterizable
            String query = "SELECT * " + 
            		"FROM (SELECT" + 
            		"    QN.content_name AS activity," + 
            		"    UA.activityid," + 
            		"    COUNT(UA.activityid) AS nattempts," + 
            		"    SUM(UA.Result) AS nsuccess " + 
            		"FROM" + 
            		"    um2.ent_user_activity UA," + 
            		"    um2.sql_question_names QN " + 
            		"WHERE" + 
            		"    UA.appid = 23" + 
            		"        AND UA.userid = (SELECT " + 
            		"            userid" + 
            		"        FROM" + 
            		"            um2.ent_user" + 
            		"        WHERE" + 
            		"            login = '"+usr+"')" + 
            		"        AND QN.activityid = UA.activityid" + 
            		"        AND UA.Result != - 1 " + 
            		"GROUP BY UA.activityid) HA " + 
            		"LEFT JOIN (SELECT " + 
            		"        LastResults.activityid," + 
            		"            COUNT(LastResults.activityid) AS lastk_nattempts," + 
            		"            SUM(LastResults.Result) AS lastk_nsuccess" + 
            		"    FROM" + 
            		"        (SELECT " + 
            		"			*" + 
            		"		FROM" + 
            		"			um2.ent_user_activity " + 
            		"		WHERE" + 
            		"			userid = (SELECT " + 
            		"					userid " + 
            		"				FROM" + 
            		"					um2.ent_user " + 
            		"				WHERE" + 
            		"					login = '"+usr+"')" + 
            		"				AND appid = 23 " + 
            		"				AND Result != - 1 " + 
            		"		ORDER BY DateNTime DESC " + 
            		"		LIMIT "+kLastResults+") AS LastResults " + 
            		"    GROUP BY LastResults.activityid) LA " + 
            		"ON HA.activityid = LA.activityid;";
            System.out.println(query);
            rs = stmt.executeQuery(query);
            boolean noactivity = true;
            while (rs.next()) {
                noactivity = false;
                String[] act = new String[5];
                act[0] = rs.getString("activity");
                act[1] = rs.getString("nattempts");
                act[2] = rs.getString("nsuccess");
                act[3] = rs.getString("lastk_nattempts");
                if(act[3]==null) act[3] = "-1";
                act[4] = rs.getString("lastk_nsuccess");
                if(act[4]==null) act[4] = "-1";
                if (act[0].length() > 0)
                    res.put(act[0], act);
            }
            this.releaseStatement(stmt, rs);
//            if (noactivity)
//                return null;
//            else
//                return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            //return null;
        } finally {
            this.releaseStatement(stmt, rs);
        }
    	return res;
    }

    public HashMap<String, String[]> getUserQuizJetActivity(String usr){
    	return getUserQuizJetActivity(usr, null);
    }
    
    // date in format YYYY-MM-DD HH:MM:SS
    public HashMap<String, String[]> getUserQuizJetActivity(String usr, String dateFrom) {
    	HashMap<String, String[]> res = new HashMap<String, String[]>();
    	try {
            
            stmt = conn.createStatement();
            /*String query = "SELECT AC.activity as activity, count(UA.activityid) as nattempts, sum(UA.Result) as nsuccess, "
            		+ "GROUP_CONCAT(cast(UA.Result as char) order by UA.datentime asc separator ',') as attemptSeq "
            		+ "FROM um2.ent_user_activity UA, um2.ent_activity AC "
            		+ "WHERE UA.appid=25 AND UA.userid = (select userid from um2.ent_user where login='"+ usr + "') "
            		+ "AND AC.activityid=UA.activityid AND UA.Result != -1 ";*/
            
            int kLastResults = 15;//@Jordan pending make it parameterizable
            String query = "SELECT * " + 
            		"FROM (SELECT" + 
            		"    QN.content_name AS activity," + 
            		"    UA.activityid," + 
            		"    COUNT(UA.activityid) AS nattempts," + 
            		"    SUM(UA.Result) AS nsuccess " + 
            		"FROM" + 
            		"    um2.ent_user_activity UA," + 
            		"    um2.sql_question_names QN " + 
            		"WHERE" + 
            		"    UA.appid = 25" + 
            		"        AND UA.userid = (SELECT " + 
            		"            userid" + 
            		"        FROM" + 
            		"            um2.ent_user" + 
            		"        WHERE" + 
            		"            login = '"+usr+"')" + 
            		"        AND QN.activityid = UA.activityid" + 
            		"        AND UA.Result != - 1 " + 
            		"GROUP BY UA.activityid) HA " + 
            		"LEFT JOIN (SELECT " + 
            		"        LastResults.activityid," + 
            		"            COUNT(LastResults.activityid) AS lastk_nattempts," + 
            		"            SUM(LastResults.Result) AS lastk_nsuccess" + 
            		"    FROM" + 
            		"        (SELECT " + 
            		"			*" + 
            		"		FROM" + 
            		"			um2.ent_user_activity " + 
            		"		WHERE" + 
            		"			userid = (SELECT " + 
            		"					userid " + 
            		"				FROM" + 
            		"					um2.ent_user " + 
            		"				WHERE" + 
            		"					login = '"+usr+"')" + 
            		"				AND appid = 23 " + 
            		"				AND Result != - 1 " + 
            		"		ORDER BY DateNTime DESC " + 
            		"		LIMIT "+kLastResults+") AS LastResults " + 
            		"    GROUP BY LastResults.activityid) LA " + 
            		"ON HA.activityid = LA.activityid;";
            
            if(dateFrom != null && dateFrom.length() > 0){
            	query += "AND UA.datentime > '"+dateFrom+"' ";
            }
            query += "GROUP BY UA.activityid;";

            // group_concat(CC.concept_name , ',', cast(CC.weight as char) , ',' , cast(CC.direction as char) order by CC.weight separator ';') as concepts
            
            
            //System.out.println(query);
            rs = stmt.executeQuery(query);
            boolean noactivity = true;
            while (rs.next()) {
                noactivity = false;
                /*String[] act = new String[4];
                act[0] = rs.getString("activity");
                act[1] = rs.getString("nattempts");
                act[2] = rs.getString("nsuccess");
                act[3] = rs.getString("attemptSeq");
                if (act[0].length() > 0)
                    res.put(act[0], act);*/
                String[] act = new String[5];
                act[0] = rs.getString("activity");
                act[1] = rs.getString("nattempts");
                act[2] = rs.getString("nsuccess");
                act[3] = rs.getString("lastk_nattempts");
                if(act[3]==null) act[3] = "-1";
                act[4] = rs.getString("lastk_nsuccess");
                if(act[4]==null) act[4] = "-1";
                if (act[0].length() > 0)
                    res.put(act[0], act);
            }
            this.releaseStatement(stmt, rs);
//            if (noactivity)
//                return null;
//            else
//                return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            
        } finally {
            this.releaseStatement(stmt, rs);
        }
    	return res;
    }

    // this is a temporal implementation. The logic of which application are for each domain should not be in this way. 
    // Instead, services for each activity has to be able to report activity in "extended" format 
    public ArrayList<String[]> getUserActivity(String usr, String domain, String dateFrom) {
    	String appIds = "25";
    	if(domain.equalsIgnoreCase("py") || domain.equalsIgnoreCase("python")) appIds = "41,38";
    	if(domain.equalsIgnoreCase("sql")) appIds = "23";

    	
    	ArrayList<String[]> res = new ArrayList<String[]>();
    	try {
            
            stmt = conn.createStatement();
            String query = "SELECT AC.activity as activity, UA.Result "
            		+ "FROM um2.ent_user_activity UA, um2.ent_activity AC "
            		+ "WHERE UA.appid in ("+appIds+") AND UA.userid = (select userid from um2.ent_user where login='"+ usr + "') "
            		+ "AND AC.activityid=UA.activityid AND UA.Result != -1 " 
            		+ "AND UA.datentime > '" + dateFrom + "' ORDER BY UA.datentime asc;";

            //System.out.println(query);
            rs = stmt.executeQuery(query);
            boolean noactivity = true;
            while (rs.next()) {
                noactivity = false;
                String[] act = new String[2];
                act[0] = rs.getString("activity");
                act[1] = rs.getString("Result");
                res.add(act);
            }
            this.releaseStatement(stmt, rs);
//            if (noactivity)
//                return null;
//            else
//                return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            
        } finally {
            this.releaseStatement(stmt, rs);
        }
    	return res;
    }
    
    
    
    public HashMap<String, String[]> getUserQuizPetActivity(String usr){
    	return getUserQuizPetActivity(usr, null);
    }

    public HashMap<String, String[]> getUserMChQActivity(String usr, String dateFrom) {
    	HashMap<String, String[]> res = new HashMap<String, String[]>();
    	try {
            
            stmt = conn.createStatement();
            String query = "SELECT AC.activity as activity, count(UA.activityid) as nattempts, sum(UA.Result) as nsuccess, "
            		+ "GROUP_CONCAT(cast(UA.Result as char) order by UA.datentime asc separator ',') as attemptSeq "
            		+ "FROM um2.ent_user_activity UA, um2.ent_activity AC "
            		+ "WHERE UA.appid=42 AND UA.userid = (select userid from um2.ent_user where login='"+ usr + "') "
            		+ "AND AC.activityid=UA.activityid AND UA.Result != -1 ";
            if(dateFrom != null && dateFrom.length() > 0){
            	query += "AND UA.datentime > '"+dateFrom+"' ";
            }
            query += "GROUP BY UA.activityid;";

            // group_concat(CC.concept_name , ',', cast(CC.weight as char) , ',' , cast(CC.direction as char) order by CC.weight separator ';') as concepts
            
            
            //System.out.println(query);
            rs = stmt.executeQuery(query);
            boolean noactivity = true;
            while (rs.next()) {
                noactivity = false;
                String[] act = new String[4];
                act[0] = rs.getString("activity");
                act[1] = rs.getString("nattempts");
                act[2] = rs.getString("nsuccess");
                act[3] = rs.getString("attemptSeq");
                if (act[0].length() > 0)
                    res.put(act[0], act);
            }
            this.releaseStatement(stmt, rs);
//            if (noactivity)
//                return null;
//            else
//                return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            
        } finally {
            this.releaseStatement(stmt, rs);
        }
    	return res;
    }
    
    public HashMap<String, String[]> getUserQuizPetActivity(String usr, String dateFrom) {
    	HashMap<String, String[]> res = new HashMap<String, String[]>();
    	try {
            stmt = conn.createStatement();
            String query = "SELECT AC.activity as activity, count(UA.activityid) as nattempts, sum(UA.Result) as nsuccess, "
            		+ "GROUP_CONCAT(cast(UA.Result as char) order by UA.datentime asc separator ',') as attemptSeq "
            		+ "FROM um2.ent_user_activity UA, um2.ent_activity AC "
            		+ "WHERE UA.appid=41 AND UA.userid = (select userid from um2.ent_user where login='"+ usr + "') "
            		+ "AND AC.activityid=UA.activityid AND UA.Result != -1 ";
            if(dateFrom != null && dateFrom.length()>0){
            	query += "AND UA.datentime > '"+dateFrom+"' ";
            }
            query +=  "GROUP BY UA.activityid;";

            // System.out.println(query);
            rs = stmt.executeQuery(query);
            boolean noactivity = true;
            while (rs.next()) {
                noactivity = false;
                String[] act = new String[4];
                act[0] = rs.getString("activity");
                act[1] = rs.getString("nattempts");
                act[2] = rs.getString("nsuccess");
                act[3] = rs.getString("attemptSeq");
                if (act[0].length() > 0)
                    res.put(act[0], act);
            }
            this.releaseStatement(stmt, rs);
//            if (noactivity)
//                return null;
//            else
//                return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            
        } finally {
            this.releaseStatement(stmt, rs);
        }
    	return res;
    }

    
    
    public ArrayList<String[]> getClassList(String grp) {

        try {
            ArrayList<String[]> res = new ArrayList<String[]>();
            stmt = conn.createStatement();
            String query = "select U.userid, U.login, U.name, U.email "
                    + "from ent_user U, rel_user_user UU "
                    + "where UU.groupid = (select userid from ent_user where login='"
                    + grp + "' and isgroup=1) " + "and U.userid=UU.userid";
            // System.out.println(query);
            rs = stmt.executeQuery(query);
            int i = 0;
            while (rs.next()) {
                String[] act = new String[3];
                act[0] = rs.getString("login");
                act[1] = rs.getString("name").trim();
                act[2] = rs.getString("email").trim();
                res.add(act);
                // System.out.println(act[0]+" "+act[1]+" "+act[2]+" "+act[3]);
                i++;
            }
            this.releaseStatement(stmt, rs);
            return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            return null;
        } finally {
            this.releaseStatement(stmt, rs);
        }
    }

    // get a list with the content and for each content item, all the concepts in an array list with  
    public ArrayList<String[]> getContentConcepts(String domain) {
        try {
            //HashMap<String, ArrayList<String[]>> res = new HashMap<String, ArrayList<String[]>>();
            ArrayList<String[]> res = new ArrayList<String[]>();
            stmt = conn.createStatement();
            String query = "SELECT CC.content_name, "
                    + " group_concat(CC.concept_name , ',', cast(CONVERT(CC.weight,DECIMAL(10,3)) as char ), ',' , cast(CC.direction as char) order by CC.weight separator ';') as concepts "
                    + " FROM agg_content_concept CC  "
                    + " WHERE CC.domain = '" + domain + "'"
                    + " group by CC.content_name order by CC.content_name;";
            rs = stmt.executeQuery(query);
            //System.out.println(query);
            //String content_name = "";
            //ArrayList<String[]> c_c = null;
            while (rs.next()) {
                String[] data = new String[2];
                data[0] = rs.getString("content_name");
                data[1] = rs.getString("concepts");
                
                res.add(data);
            }
            this.releaseStatement(stmt, rs);
            return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            this.releaseStatement(stmt, rs);
            return null;
        }
    }
    
    public HashMap<String, ArrayList<String[]>> getContentConcepts(String domain, ArrayList<String> contentList) {
    		HashMap<String, ArrayList<String[]>> res = null;
    		try {
            //HashMap<String, ArrayList<String[]>> res = new HashMap<String, ArrayList<String[]>>();
    			String contents = " ";
    			for(String c : contentList)  contents += "'"+c+"'"+",";
    			contents = contents.substring(0,contents.length()-1);
        	
            stmt = conn.createStatement();
            String query = "SELECT CC.content_name, "
                    + " group_concat(CC.concept_name , ',', cast(CONVERT(CC.weight,DECIMAL(10,3)) as char ), ',' , cast(CC.direction as char) order by CC.weight separator ';') as concepts "
                    + " FROM agg_content_concept CC  "
                    + " WHERE CC.domain = '" + domain + "'" + (contents.length() == 0 ? "" : " and CC.content_name in ("+contents+")")
                    + " group by CC.content_name order by CC.content_name;";
            //System.out.println(query);
            
            rs = stmt.executeQuery(query);
            //String content_name = "";
            //ArrayList<String[]> c_c = null;
            res = new HashMap<String, ArrayList<String[]>>();
            
            while (rs.next()) {
                
                String contentName = rs.getString("content_name");
                String conceptStrList = rs.getString("concepts");
         
                ArrayList<String[]> conceptList;
                if (conceptStrList == null
                        || conceptStrList.equalsIgnoreCase("[null]")
                        || conceptStrList.length() == 0) {
                    conceptList = null;
                } else {
                    conceptList = new ArrayList<String[]>();
                    String[] concepts = conceptStrList.split(";");
                    for (int j = 0; j < concepts.length; j++) {
                        String[] concept = concepts[j].split(",");
                        //if(concept.length<3) System.out.println("  "+j+" "+content_name+"  "+concept[0]+"  "+concept.length);
                        
                        //System.out.println(concept[0] + " " + concept[1] + " " + concept[2]);
                        
                        conceptList.add(concept); // concept_name, weight, direction
//                        if(contentList.get(content_name)[5].equals("webex")){
//                        	System.out.println("  "+concepts[j]+"  " + concept[0] + " " + concept[1] + " " + concept[2]);
//                        	
//                        }
                    }
                }
                res.put(contentName, conceptList);

            }
            this.releaseStatement(stmt, rs);
            return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            this.releaseStatement(stmt, rs);
            return null;
        }
    }
    
    public HashMap<String, String[]> getUserPCRSActivity(String usr, String dateFrom) {
    	HashMap<String, String[]> res = new HashMap<String, String[]>();
    	try {
            
            stmt = conn.createStatement();
            String query = "SELECT AC.activity as activity, count(UA.activityid) as nattempts, sum(result) as nsuccess, "
            		+ "GROUP_CONCAT(cast(UA.Result as char) order by UA.datentime asc separator ',') as attemptSeq "
            		+ "FROM um2.ent_user_activity UA, um2.ent_activity AC "
            		+ "WHERE UA.appid=44 AND UA.userid = (select userid from um2.ent_user where login='"+ usr + "') "
            		+ "AND AC.activityid=UA.activityid AND UA.Result != -1 ";
            if(dateFrom != null && dateFrom.length() > 0){
            	query += "AND UA.datentime > '"+dateFrom+"' ";
            }
            query += "GROUP BY UA.activityid;";

            // group_concat(CC.concept_name , ',', cast(CC.weight as char) , ',' , cast(CC.direction as char) order by CC.weight separator ';') as concepts
            
            
            //System.out.println(query);
            rs = stmt.executeQuery(query);
            boolean noactivity = true;
            while (rs.next()) {
                noactivity = false;
                String[] act = new String[4];
                act[0] = rs.getString("activity");
                act[1] = rs.getString("nattempts");
                act[2] = rs.getString("nsuccess");
                act[3] = rs.getString("attemptSeq");
                if (act[0].length() > 0)
                    res.put(act[0], act);
            }
            this.releaseStatement(stmt, rs);
//            if (noactivity)
//                return null;
//            else
//                return res;
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            
        } finally {
            this.releaseStatement(stmt, rs);
        }
    	return res;
    }
    
    /**
	 * This method returns the user activities in PCEX challenges 
	 * @author roya
	 */
	public HashMap<String, String[]> getUserPCEXChallengesActivity(String usr, String dateFrom) {

		HashMap<String, String[]> res = new HashMap<String, String[]>();
		try {

			stmt = conn.createStatement();
			String query = "SELECT AC.activity as activity, count(UA.activityid) as nattempts, sum(result) as nsuccess, "
					+ "GROUP_CONCAT(cast(UA.Result as char) order by UA.datentime asc separator ',') as attemptSeq "
					+ "FROM um2.ent_user_activity UA, um2.ent_activity AC "
					+ "WHERE UA.appid=47 AND UA.userid = (select userid from um2.ent_user where login='" + usr + "') "
					+ "AND AC.activityid=UA.activityid AND UA.Result != -1 ";
			if (dateFrom != null && dateFrom.length() > 0) {
				query += "AND UA.datentime > '" + dateFrom + "' ";
			}
			query += "GROUP BY UA.activityid;";

			// group_concat(CC.concept_name , ',', cast(CC.weight as char) , ','
			// , cast(CC.direction as char) order by CC.weight separator ';') as
			// concepts

			// System.out.println(query);
			rs = stmt.executeQuery(query);
			boolean noactivity = true;
			while (rs.next()) {
				noactivity = false;
				String[] act = new String[4];
				act[0] = rs.getString("activity");
				act[1] = rs.getString("nattempts");
				act[2] = rs.getString("nsuccess");
				act[3] = rs.getString("attemptSeq");
				if (act[0].length() > 0)
					res.put(act[0], act);
			}
			this.releaseStatement(stmt, rs);
			// if (noactivity)
			// return null;
			// else
			// return res;
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());

		} finally {
			this.releaseStatement(stmt, rs);
		}
		return res;
	}

	/**
	 * This method returns the user activities in PCEX examples 
	 * @author roya
	 */
	public HashMap<String, String[]> getUserPCEXExamplesActivity(String usr, String dateFrom) {

		HashMap<String, String[]> res = new HashMap<String, String[]>();
		try {

			stmt = conn.createStatement();
			/*
			 * String query = "select A.activity, " +
			 * "AA.parentactivityid, count(UA.activityid) as nactions,  " +
			 * "count(distinct(UA.activityid)) as distinctactions, " +
			 * "(select count(AA2.childactivityid) from rel_activity_activity AA2 where AA2.parentactivityid = AA.parentactivityid and AA2.childactivityid not in (select activityid from ent_activity where active = 0)) as totallines "
			 * +
			 * "from ent_user_activity UA, rel_activity_activity AA, ent_activity A "
			 * +
			 * " where (UA.appid=3 OR UA.appid=35) and UA.userid = (select userid from ent_user where login='"
			 * + usr+ "') " +
			 * " and AA.parentactivityid=A.activityid and AA.childactivityid=UA.activityid "
			 * + "group by AA.parentactivityid " +
			 * "order by AA.parentactivityid;";
			 */

			String query = "select A.activity,AA.parentactivityid, count(UA.activityid) as nactions,  "
					+ "count(distinct(UA.activityid)) as distinctactions,  "
					+ "(select count(AA2.childactivityid) from rel_activity_activity AA2 where AA2.parentactivityid = AA.parentactivityid  "
					+ "and AA2.childactivityid not in (select activityid from ent_activity where active = 0)) as totallines,  "
					+ "group_concat(distinct A2.activity separator ',') as clicked   "
					+ "from ent_user_activity UA, rel_activity_activity AA, ent_activity A  " + " , ent_activity A2 "
					+ "where (UA.appid=46) and UA.Result = -1 and UA.userid = (select userid from ent_user where login='"
					+ usr + "')  "
					+ "and AA.parentactivityid=A.activityid and AA.childactivityid=UA.activityid  "
					+ "and A2.activityid = UA.activityid ";
			if (dateFrom != null && dateFrom.length() > 0) {
				query += "AND UA.datentime > '" + dateFrom + "' ";
			}
			query += "group by AA.parentactivityid  " + " order by AA.parentactivityid;";

			//System.out.println(query);
			rs = stmt.executeQuery(query);
			// System.out.println(query);

			boolean noactivity = true;
			while (rs.next()) {
				noactivity = false;
				String[] act = new String[5];
				act[0] = rs.getString("activity");
				act[1] = rs.getString("nactions");
				act[2] = rs.getString("distinctactions");
				act[3] = rs.getString("totallines");
				act[4] = rs.getString("clicked");
				res.put(act[0], act);
				// System.out.println(act[0]+" actions: "+act[2]+",
				// "+act[3]+"/"+act[4]);
			}
			this.releaseStatement(stmt, rs);

		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());

		} finally {
			this.releaseStatement(stmt, rs);
		}
		return res;
	}

	/**
	 * This method returns the mappings between activities and PCEX sets
	 * @author roya
	 */
	public HashMap<String, ArrayList<String>> getActivitiesInPCEXSet() {
		try {
			HashMap<String, ArrayList<String>> res = new HashMap<String, ArrayList<String>>();
			stmt = conn.createStatement();
			String query = "SELECT A1.activity as set_name, A2.activity as act_name" 
					+ " FROM um2.ent_activity A1, um2.ent_activity A2, um2.rel_pcex_set_component AA1"
					+ " where A1.AppID = 45 and (A2.AppID = 46 or A2.AppID = 47)"
					+ " and AA1.ParentActivityID = A1.ActivityID and AA1.ChildActivityID = A2.ActivityID;";
			rs = stmt.executeQuery(query);
			// System.out.println(query);
			String set, act;
			while (rs.next()) {
				set = rs.getString("set_name");
				act = rs.getString("act_name");
				if ( res.containsKey(set) ) {
					res.get(set).add(act);
				} else {
					ArrayList<String> list = new ArrayList<String>();
					list.add(act);
					res.put(set, list);
				}	
			}
			this.releaseStatement(stmt, rs);
			return res;
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			this.releaseStatement(stmt, rs);
			return null;
		}
	}

	/**
	 * This method returns the mappings between PCEX sets and PCEX challegnes
	 * @author roya
	 */
	public HashMap<String, ArrayList<String>> getChallengesInPCEXSets() {
		try {
			HashMap<String, ArrayList<String>> res = new HashMap<String, ArrayList<String>>();
			stmt = conn.createStatement();
			String query = "SELECT A1.activity as set_name, A2.activity as act_name" 
					+ " FROM um2.ent_activity A1, um2.ent_activity A2, um2.rel_pcex_set_component AA1"
					+ " where A1.AppID = 45 and A2.AppID = 47"
					+ " and AA1.ParentActivityID = A1.ActivityID and AA1.ChildActivityID = A2.ActivityID;";
			rs = stmt.executeQuery(query);
			// System.out.println(query);
			String set, act;
			while (rs.next()) {
				set = rs.getString("set_name");
				act = rs.getString("act_name");
				if ( res.containsKey(set) ) {
					res.get(set).add(act);
				} else {
					ArrayList<String> list = new ArrayList<String>();
					list.add(act);
					res.put(set, list);
				}	
			}
			this.releaseStatement(stmt, rs);
			return res;
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			this.releaseStatement(stmt, rs);
			return null;
		}
	}
	
	
	/**
	 * This method returns the mappings between PCEX sets and PCEX examples.
	 * Each set is mapped to one example
	 * @author roya
	 */
	public HashMap<String, String> getExamplesInPCEXSets() {
		try {
			HashMap<String, String> res = new HashMap<String, String>();
			stmt = conn.createStatement();
			String query = "SELECT A1.activity as set_name, A2.activity as act_name" 
					+ " FROM um2.ent_activity A1, um2.ent_activity A2, um2.rel_pcex_set_component AA1"
					+ " where A1.AppID = 45 and A2.AppID = 46 and A2.description = 'PCEX Example' "
					+ " and AA1.ParentActivityID = A1.ActivityID and AA1.ChildActivityID = A2.ActivityID;";
			rs = stmt.executeQuery(query);
			// System.out.println(query);
			String set, act;
			while (rs.next()) {
				set = rs.getString("set_name");
				act = rs.getString("act_name");
				res.put(set,act); 
			}
			this.releaseStatement(stmt, rs);
			return res;
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			this.releaseStatement(stmt, rs);
			return null;
		}
	}
	/**
	 * This method returns SQLTUTORActivity map of the user. 
	 * @author cskamil
	 */
	public HashMap<String, String[]> getUserSQLTUTORActivity(String user, String dateFrom) {
    	HashMap<String, String[]> res = new HashMap<String, String[]>();
    	try {
            
            stmt = conn.createStatement();
            
            int kLastResults = 15;//@Jordan pending make it parameterizable
            
            String query = "SELECT " + 
							    "AC.activity AS activity," +
							    "COUNT(UA.activityid) AS nattempts," +
							    "MAX(result) AS progress," +
							    "GROUP_CONCAT(CAST(UA.Result AS CHAR) " +
							        "ORDER BY UA.datentime ASC " +
							        "SEPARATOR ',') AS attemptSeq " +
							"FROM " +
							    "um2.ent_user_activity UA INNER JOIN " +
							    "um2.ent_activity AC on UA.ActivityID = AC.ActivityID INNER JOIN " +
							    "um2.ent_user usr on UA.UserID = usr.UserID " +
							"WHERE " +
							    "UA.appid = 19 " +
									"AND usr.Login = '"+ user + "' " +
							        "AND UA.Result != - 1 ";
            
            
            if(dateFrom != null && dateFrom.length() > 0){
            	query += "AND UA.datentime > '"+dateFrom+"' ";
            }
            query += "GROUP BY UA.activityid;";
            
            query = "SELECT " + 
            		"    * " + 
            		"FROM " + 
            		"    (" +
            		query + 
            		 ") AS HA "+
			        "LEFT JOIN "+
				    "(SELECT "+
				        "LastResults.activityid, "+
				            "COUNT(LastResults.activityid) AS lastk_nattempts, "+
				            "SUM(LastResults.Result) AS lastk_nsuccess "+
				    "FROM "+
				        "(SELECT "+
				        "*"+
				    "FROM"+
				        "um2.ent_user_activity "+
				    "WHERE "+
				        "userid = (SELECT "+
				                "userid "+
				            "FROM "+
				                "um2.ent_user "+
				            "WHERE "+
				                "login = '"+user+"') "+
				            "AND appid = 19 "+
				            "AND Result != - 1 "+
				    "ORDER BY DateNTime DESC "+
				    "LIMIT "+kLastResults+") AS LastResults "+
				    "GROUP BY LastResults.activityid) LA "+
				"ON HA.activityid = LA.activityid;";

//            while (rs.next()) {
//                String[] act = new String[4];
//                act[0] = rs.getString("activity");
//                act[1] = rs.getString("nattempts");
//                act[2] = rs.getString("progress");
//                act[3] = rs.getString("attemptSeq");
//                if (act[0].length() > 0)
//                    res.put(act[0], act);
//            }
//            this.releaseStatement(stmt, rs);
            
            System.out.println(query);
            rs = stmt.executeQuery(query);
            
            boolean noactivity = true;
            while (rs.next()) {
                noactivity = false;
                String[] act = new String[6];
                act[0] = rs.getString("activity");
                act[1] = rs.getString("nattempts");
                act[2] = rs.getString("progress");
                act[3] = rs.getString("attemptSeq");
                act[4] = rs.getString("nsuccess");
                act[5] = rs.getString("lastk_nattempts");
                if(act[5]==null) act[5] = "-1";
                act[6] = rs.getString("lastk_nsuccess");
                if(act[6]==null) act[6] = "-1";
                if (act[0].length() > 0)
                    res.put(act[0], act);
            }
            this.releaseStatement(stmt, rs);
            
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
            
        } finally {
            this.releaseStatement(stmt, rs);
        }
    	return res;
    }
}
