package com.stumbleupon.classifier;

public class EvaluationException extends Exception {
	public EvaluationException(String message){
		super(message);
	}

	public EvaluationException(String message,Throwable throwable){
		super(message,throwable);
	}
}
