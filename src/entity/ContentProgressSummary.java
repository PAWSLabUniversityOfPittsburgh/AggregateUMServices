package entity;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ContentProgressSummary {
	
	@JsonProperty("content-id")
	private String contentId;
	
	private double progress;
	
	@JsonProperty("time-spent")
	private long timeSpent;
	
	@JsonProperty("success-rate")
	private double successRate;
	
	@JsonProperty("like-count")
	private int likeCount;
	
	@JsonProperty("annotation-count")
	private int annotationCount;
	
	private double attempts;
	
	@JsonProperty("attempts-seq")
	private String attemptsSequence;
	
	@JsonProperty("sub-activities")
	private int subActivities;	
	
	public ContentProgressSummary() {
		this.attemptsSequence = "";
	}

	public String getContentId() {
		return contentId;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	public double getProgress() {
		return progress;
	}

	public void setProgress(double progress) {
		this.progress = progress;
	}

	public long getTimeSpent() {
		return timeSpent;
	}

	public void setTimeSpent(long timeSpent) {
		this.timeSpent = timeSpent;
	}

	public double getSuccessRate() {
		return successRate;
	}

	public void setSuccessRate(double successRate) {
		this.successRate = successRate;
	}

	public int getLikeCount() {
		return likeCount;
	}

	public void setLikeCount(int likeCount) {
		this.likeCount = likeCount;
	}

	public int getAnnotationCount() {
		return annotationCount;
	}

	public void setAnnotationCount(int annotationCount) {
		this.annotationCount = annotationCount;
	}

	public double getAttempts() {
		return attempts;
	}

	public void setAttempts(double attempts) {
		this.attempts = attempts;
	}

	public String getAttemptsSequence() {
		return attemptsSequence;
	}

	public void setAttemptsSequence(String attemptsSequence) {
		this.attemptsSequence = attemptsSequence;
	}

	public int getSubActivities() {
		return subActivities;
	}

	public void setSubActivities(int subActivities) {
		this.subActivities = subActivities;
	}

}
