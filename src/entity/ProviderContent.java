import com.fasterxml.jackson.annotation.JsonProperty;


public class ProviderContent {
	
	@JsonProperty("provider-id")
	private String providerId;
	
	@JsonProperty("content-list")
	private String[] contentList;

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public String[] getContentList() {
		return contentList;
	}

	public void setContentList(String[] contentList) {
		this.contentList = contentList;
	}

}
