package com.sr.util;

public class GroupTreatmentVO {
	
	public GroupTreatmentVO () {
		
	}
	
	public GroupTreatmentVO(String groupDetails, int treatmentNumber, int count) {
		this.groupDetails = groupDetails;
		this.treatmentNumber = treatmentNumber;
		this.count = count;
	}

	public String getGroupDetails() {
		return groupDetails;
	}
	public void setGroupDetails(String groupDetails) {
		this.groupDetails = groupDetails;
	}
	public int getTreatmentNumber() {
		return treatmentNumber;
	}
	public void setTreatmentNumber(int treatmentNumber) {
		this.treatmentNumber = treatmentNumber;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	private String groupDetails;
	private int treatmentNumber;
	private int count;
	
}
