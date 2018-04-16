package com.sr.model;

import java.util.List;

public class ExcelDataVO {

	private int maxRows;
	private List<Variable> variables;
	public int getMaxRows() {
		return maxRows;
	}
	public void setMaxRows(int maxRows) {
		this.maxRows = maxRows;
	}
	public List<Variable> getVariables() {
		return variables;
	}
	public void setVariables(List<Variable> variables) {
		this.variables = variables;
	}
	
}
