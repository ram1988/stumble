package com.stumbleupon.classifier;

public class EvalResult {
	private String classLabel;
	private double AUC;
	private double precision;
	private double recall;
	private double fmeasure;
	
	
	public double getPrecision() {
		return precision;
	}
	public void setPrecision(double precision) {
		this.precision = precision;
	}
	public double getRecall() {
		return recall;
	}
	public void setRecall(double recall) {
		this.recall = recall;
	}
	public double getFmeasure() {
		return fmeasure;
	}
	public void setFmeasure(double fmeasure) {
		this.fmeasure = fmeasure;
	}
	
	public String getClassLabel() {
		return classLabel;
	}
	public void setClassLabel(String classLabel) {
		this.classLabel = classLabel;
	}
	public double getAUC() {
		return AUC;
	}
	public void setAUC(double aUC) {
		AUC = aUC;
	}
	
}
