import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;


public class ProgressOutput {
	
	@JsonProperty("user-id")
	private String userId;
	
	@JsonProperty("group-id")
	private String groudId;
	
	@JsonProperty("date-from")
	private String dateFrom;
	
	private List<ContentProgressSummary> contentSummaryList;
	
	public ProgressOutput(String userId, String groudId, String dateFrom) {
		this.userId = userId;
		this.groudId = groudId;
		this.dateFrom = dateFrom;
		this.contentSummaryList = new ArrayList<ContentProgressSummary>();
	}
	
	public void addContentProgress(ContentProgressSummary progressSummary) {
		contentSummaryList.add(progressSummary);
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getGroudId() {
		return groudId;
	}

	public void setGroudId(String groudId) {
		this.groudId = groudId;
	}

	public String getDateFrom() {
		return dateFrom;
	}

	public void setDateFrom(String dateFrom) {
		this.dateFrom = dateFrom;
	}

	@JsonProperty("content-list")
	public ContentProgressSummary[] getContentSummary() {
		return (ContentProgressSummary[]) contentSummaryList.toArray();
	}

}
