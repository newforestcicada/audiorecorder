package info.newforestcicada.audiorecorder.plugin;

public class LongTermResult {
	private boolean found;
	private float confidence;

	public LongTermResult(boolean found, float confidence) {
		this.setFound(found);
		this.setConfidence(confidence);
	}

	public boolean isFound() {
		return found;
	}

	public void setFound(boolean found) {
		this.found = found;
	}

	public float getConfidence() {
		return confidence;
	}

	public void setConfidence(float confidence) {
		this.confidence = confidence;
	}
	
	
}
