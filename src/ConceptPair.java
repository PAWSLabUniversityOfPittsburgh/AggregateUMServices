import java.util.HashMap;
import java.util.Map;


// edge or link
public class ConceptPair {
	private int id;
	public Concept concept1;
	public Concept concept2;
	private double idf;
	private String type;
	private String contentNamesList;
	public ConceptPair(int id, Concept concept1, Concept concept2, double idf, String type) {
		super();
		this.id = id;
		this.concept1 = concept1;
		this.concept2 = concept2;
		this.idf = idf;
		this.type = type;
		this.contentNamesList = "";
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public double getIdf() {
		return idf;
	}

	public void setIdf(double idf) {
		this.idf = idf;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getContentNamesList() {
		return contentNamesList;
	}
	public void setContentNamesList(String contentNamesList) {
		this.contentNamesList = contentNamesList;
	}
	
	public String toJsonString(){
		int nContent = 0;
		// TODO
		if(getContentNamesList() == null || getContentNamesList().length() == 0) 
			this.setContentNamesList("JavaTutorial_4_2_9");
		
		String[] s = getContentNamesList().split("~");
		nContent = s.length;
		String content = getContentNamesList();
		
		return "{"+"\"id\": "+getId()+",\"s\": "+concept1.getId()+", \"t\": "+concept2.getId()+", \"idf\": "+getIdf()+",\"ncnt\": "+nContent+",\"cnt\": \""+content+"\"}";
	}
	
//	public static void checkPairs(HashMap<Integer,Concept> concepts, HashMap<Integer,ConceptPair> pairs){
//		for (Map.Entry<Integer, ConceptPair> pair : pairs.entrySet()) {
//			ConceptPair cp = pair.getValue();
//			if(!concepts.containsKey(cp.concept1.getId()) || !concepts.containsKey(cp.concept2.getId())){
//				pairs.remove(pair);
//			}
//		}
//	}
	
}
