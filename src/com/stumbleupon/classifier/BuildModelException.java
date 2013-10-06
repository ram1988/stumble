package com.stumbleupon.classifier;

public class BuildModelException extends Exception {
	public BuildModelException(String message){
		super(message);
	}

	public BuildModelException(String message,Throwable throwable){
		super(message,throwable);
	}
	
}
