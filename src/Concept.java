import java.util.ArrayList;



public class Concept {
	private int id;
	private String idName;
	private String displayName;
	private String cmTopic;
	private String canonicTopic;
	private String contentNamesList;
//	private double levelK;
//	private double succRate;
//	private int userAttempts;
//	private int groupAttempts;
	
	
	//public ArrayList<ConceptPair> conceptPairs;
	
	public Concept(int id, String idName, String displayName, String cmTopic, String canonicTopic, String content) {
		super();
		this.id = id;
		this.idName = idName;
		this.displayName = displayName;
		this.cmTopic = cmTopic;
		this.canonicTopic = canonicTopic;
//		this.levelK = 0;
//		this.succRate = 0;
//		this.userAttempts=0;
//		this.groupAttempts=0;
		this.contentNamesList = content;
		//conceptPairs = new ArrayList<ConceptPair>();
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getIdName() {
		return idName;
	}
	public void setIdName(String idName) {
		this.idName = idName;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
//	public double getLevelK() {
//		return levelK;
//	}
//	public void setLevelK(double levelK) {
//		this.levelK = levelK;
//	}
//	public double getSuccRate() {
//		return succRate;
//	}
//	public void setSuccRate(double succRate) {
//		this.succRate = succRate;
//	}
//	public int getUserAttempts() {
//		return userAttempts;
//	}
//	public void setUserAttempts(int userAttempts) {
//		this.userAttempts = userAttempts;
//	}
//	public int getGroupAttempts() {
//		return groupAttempts;
//	}
//	public void setGroupAttempts(int groupAttempts) {
//		this.groupAttempts = groupAttempts;
//	}
	
	
	public String getCmTopic() {
		return cmTopic;
	}
	public void setCmTopic(String cmTopic) {
		this.cmTopic = cmTopic;
	}
	public String getCanonicTopic() {
		return canonicTopic;
	}
	public void setCanonicTopic(String canonicTopic) {
		this.canonicTopic = canonicTopic;
	}
	public String getContentNamesList() {
		return contentNamesList;
	}
	public void setContentNamesList(String contentNamesList) {
		this.contentNamesList = contentNamesList;
	}
	
	public String toJsonString(){
		return "{"+"\"id\": "+getId()+",\"n\": \""+getIdName()+"\", \"dn\": \""+getDisplayName()+"\", \"cnt\": \""+getContentNamesList()+"\", \"t\": \""+getCmTopic()+"\" ,\"ct\": \""+getCanonicTopic()+"\"}";
	}
}
