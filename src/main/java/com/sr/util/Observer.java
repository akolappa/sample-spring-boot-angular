package com.sr.util;

public class Observer {

	private String message;
	private int percentage;
	private String status;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		System.out.println(message);
		this.message = message;
	}

	public int getPercentage() {
		return percentage;
	}

	public void setPercentage(int percentage) {
		this.percentage = percentage;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
