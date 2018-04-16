package com.sr.model;

import java.util.List;

public class StratumInput {

	private List<Variable> variables;
	private int rows;
	private int treatmentCount;
	private String tableName;
	private int excelMaxRows;

	public List<Variable> getVariables() {
		return variables;
	}

	public void setVariables(List<Variable> variables) {
		this.variables = variables;
	}

	public int getRows() {
		return rows;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public int getTreatmentCount() {
		return treatmentCount;
	}

	public void setTreatmentCount(int treatmentCount) {
		this.treatmentCount = treatmentCount;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public int getExcelMaxRows() {
		return excelMaxRows;
	}

	public void setExcelMaxRows(int excelMaxRows) {
		this.excelMaxRows = excelMaxRows;
	}
	
}
