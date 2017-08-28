
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;


public class CMDBInterface extends dbInterface {

	public CMDBInterface(String connurl, String user, String pass) {
		super(connurl, user, pass);
	}

	// Read the list of concepts which are "active" (would be shown)
	public HashMap<Integer,Concept> getAllConcepts(String domain) {
		HashMap<Integer,Concept> res = new HashMap<Integer,Concept>();
		try {

			stmt = conn.createStatement();
			String query = "SELECT C.id,C.concept_name, C.display_name, GROUP_CONCAT(CC.content_name SEPARATOR '~') as content_list,C.cm_topic, C.canonic_topic" 
						+ " FROM cm_concept C, agg_content_concept CC" 
						+ " WHERE C.concept_name = CC.concept_name and C.active=1 AND CC.active=1 and C.domain='"+domain+"'" 
						+ " GROUP BY CC.concept_name";
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				int id = rs.getInt("id");
				Concept c = new Concept(id,rs.getString("concept_name"),rs.getString("display_name"),rs.getString("cm_topic"),rs.getString("canonic_topic"),rs.getString("content_list"));				
				res.put(id, c);
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

	// Read the list of concepts pairs which are "active" (would be shown). It needs the hashmap with the concepts (which should be already read) 
	// to create the objects ConceptPair properly.
	public HashMap<Integer,ConceptPair> getAllConceptPairs(HashMap<Integer,Concept> concepts, String domain) {
		HashMap<Integer,ConceptPair> res = new HashMap<Integer,ConceptPair>();
		try {

			stmt = conn.createStatement();
			String query = "SELECT CP.id,CP.concept1,CP.concept2,CP.concept1Id,CP.concept2Id,CP.idf,CP.type,"
					+ "GROUP_CONCAT(CCP.content_name SEPARATOR '~') as content_list "
					+ " FROM cm_conceptpair CP left join cm_content_conceptpair CCP on CCP.conceptpair_id = CP.id AND CCP.active=1 "
					+ " WHERE CP.active = 1 AND CP.domain='"+domain+"'"
					+ " GROUP BY CCP.conceptpair_id;";
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				int id = rs.getInt("id");
				Concept c1 = concepts.get(rs.getInt("concept1Id"));
				Concept c2 = concepts.get(rs.getInt("concept2Id"));
				if (c1 != null && c2 != null){
					
					ConceptPair cp = new ConceptPair(id,c1,c2,rs.getDouble("idf"),rs.getString("type"));
					if(rs.getString("content_list") != null){
						cp.setContentNamesList(rs.getString("content_list"));
					}else
						cp.setContentNamesList("");
					
					res.put(id, cp);
				}
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
	public HashMap<String, Object[]> getUserParsonsActivity(String usr, String domain) {
		HashMap<String, Object[]> res = new HashMap<String, Object[]>();
		try {

			stmt = conn.createStatement();
			String query = "select AC.activity, count(UA.activityid) as attempts, max(result) as maxscore, "
					+ "group_concat(result order by UA.datentime asc separator ',') as scoresconcat, "
					+ "sum(result)/count(UA.activityid) as succrate "
					+ "from ent_user_activity UA, ent_activity AC "
					+ "WHERE UA.appid=38 AND UA.activityid = AC.activityid "
					+ "AND userid = (select userid from um2.ent_user where login='"+usr+"') "
					+ "group by UA.activityid order by activity asc";

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
					//	            			parsonsData[0] = new Integer(0);
					//	            			parsonsData[1] = new Double(0);
					//	            			parsonsData[2] = new Double(0);
					//	            			parsonsData[3] = "";
					res.put(parsonsId, parsonsData);
				}
				parsonsData[0] =  new Integer(rs.getInt("attempts")); 
				parsonsData[2] = rs.getDouble("maxscore"); 
				parsonsData[3] = rs.getDouble("succrate"); 
				parsonsData[4] = rs.getString("scoresconcat"); 
				String[] scores = rs.getString("scoresconcat").split(",");
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
			//	            if (noactivity)
			//	                return null;
			//	            else
			//	                return res;
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
			String query = "select QN.content_name as activity, count(UA.activityid) as nattempts,  sum(UA.Result) as nsuccess "
					+ " from um2.ent_user_activity UA, um2.sql_question_names QN where UA.appid=23 and "
					+ " UA.userid = (select userid from um2.ent_user where login='" + usr + "') and "
					+ " QN.activityid=UA.activityid and UA.Result != -1  "
					+ " group by UA.activityid; ";

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
			//	            if (noactivity)
			//	                return null;
			//	            else
			//	                return res;
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


	public HashMap<String, String[]> getUserQuizJetActivity(String usr) {
		HashMap<String, String[]> res = new HashMap<String, String[]>();
		try {

			stmt = conn.createStatement();
			String query = "select AC.activity as activity, count(UA.activityid) as nattempts,  sum(UA.Result) as nsuccess from um2.ent_user_activity UA, um2.ent_activity AC where UA.appid=25 and UA.userid = (select userid from um2.ent_user where login='"
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
			//	            if (noactivity)
			//	                return null;
			//	            else
			//	                return res;
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
						//	                        if(contentList.get(content_name)[5].equals("webex")){
						//	                        	System.out.println("  "+concepts[j]+"  " + concept[0] + " " + concept[1] + " " + concept[2]);
						//	                        	
						//	                        }
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


}

