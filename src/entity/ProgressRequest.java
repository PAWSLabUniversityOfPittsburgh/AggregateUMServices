import com.fasterxml.jackson.annotation.JsonProperty;


public class ProgressRequest {
	
	@JsonProperty("user-id")
	private String userId;
	
	@JsonProperty("group-id")
	private String groudId;
	
	private String domain;
	
	@JsonProperty("date-from")
	private String dateFrom;
	
	@JsonProperty("content-list-by-provider")
	private ProviderContent[] providerContentList;

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

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getDateFrom() {
		return dateFrom;
	}

	public void setDateFrom(String dateFrom) {
		this.dateFrom = dateFrom;
	}

	public ProviderContent[] getProviderContentList() {
		return providerContentList;
	}

	public void setProviderContentList(ProviderContent[] providerContentList) {
		this.providerContentList = providerContentList;
	}

}
