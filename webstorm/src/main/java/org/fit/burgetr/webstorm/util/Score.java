package org.fit.burgetr.webstorm.util;

public class Score {
	
	private Float score;
	private int docId;
	
	public Score(float score,int docId){
		this.setScore(score);
		this.setDocId(docId);
	}

	public Float getScore() {
		return score;
	}

	public void setScore(Float score) {
		this.score = score;
	}

	public int getDocId() {
		return docId;
	}

	public void setDocId(int docId) {
		this.docId = docId;
	}

}
