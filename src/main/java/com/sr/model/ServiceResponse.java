package com.sr.model;

public class ServiceResponse<T> {
	
	public ServiceResponse() {
		
	}
	
	public ServiceResponse(T t) {
		this.content = t;
	}
	
	private T content;
	
	public T getContent() {
		return content;
	}

	public void setContent(T data) {
		this.content = data;
	}

}
