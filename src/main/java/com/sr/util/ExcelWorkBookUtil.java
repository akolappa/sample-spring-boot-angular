package com.sr.util;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sr.model.ExcelDataBean;
import com.sr.model.Variable;

public class ExcelWorkBookUtil {

	public static ExcelDataBean read(InputStream in, JdbcTemplate jdbcTemplate, String tableName, Observer observer) throws Exception {
		if(null == observer) {
			observer = new Observer();
		}
		XSSFWorkbook workbook = null;
		ExcelDataBean excelDataBean = new ExcelDataBean();
		try {
			workbook = new XSSFWorkbook(in);
			XSSFSheet sheet = workbook.getSheetAt(0);
			XSSFRow headerRow = sheet.getRow(0);

			// Throw exception if header row is not present
			if (null == headerRow) {
				throw new Exception("Invalid excel. Contains 0 rows");
			}

			// Get the headings in the header row
			observer.setMessage("Reading Headings");
			observer.setPercentage(10);
			Iterator<Cell> cellIterator = headerRow.cellIterator();
			excelDataBean.setVariables(new ArrayList<Variable>());
			while (cellIterator.hasNext()) {
				Cell cell = cellIterator.next();
				if (!cell.getCellTypeEnum().equals(CellType.STRING) || cell.getStringCellValue().isEmpty()) {
					throw new Exception("Invalid header row. Contains columns without heading or invalid heading type");
				}
				excelDataBean.getVariables().add(new Variable(cell.getStringCellValue()));
			}

			// Get the type and time format if present for each variable type
			observer.setMessage("Getting Variable Types of columns");
			observer.setPercentage(25);
			correctAllVariableTypes(excelDataBean, sheet);

			// Create database table
			observer.setMessage("Setting space in Memory");
			observer.setPercentage(40);
			String columns = "";
			String columnNames = "";
			for (int i = 0; i < excelDataBean.getVariables().size(); i++) {
				Variable variable = excelDataBean.getVariables().get(i);
				String dataType = null;
				if ("Categorical".equals(variable.getType())) {
					dataType = "VARCHAR(45)";
				} else if ("Time".equals(variable.getType())) {
					dataType = "DATE";
				} else {
					dataType = "DOUBLE";
				}
				if (i <= excelDataBean.getVariables().size() - 2) {
					columnNames = columnNames + "\"" + variable.getName() + "\",";
					columns = columns + "\"" + variable.getName() + "\" " + dataType + ",";
				} else {
					columnNames = columnNames + "\"" + variable.getName() + "\"";
					columns = columns + "\"" + variable.getName() + "\" " + dataType;
				}
			}

			String createTable = "create table " + tableName + "(" + columns + ")";
			String dropTable = "drop table if exists " + tableName;
			if (null != jdbcTemplate) {
				jdbcTemplate.execute(dropTable);
				jdbcTemplate.execute(createTable);
			}
			
			// Inserting data
			observer.setMessage("Saving data in Memory");
			observer.setPercentage(60);
			String insertStatement = "insert into " + tableName + " (" + columnNames + ") values (";

			// Initialize the right attributes for the variables
			for (int j = 0; j < excelDataBean.getVariables().size(); j++) {
				if (excelDataBean.getVariables().get(j).getType().equals("Numeric") || excelDataBean.getVariables().get(j).getType().equals("Time")) {
					excelDataBean.getVariables().get(j).setRange(new ArrayList<Long>());
				} else {
					excelDataBean.getVariables().get(j).setValues(new HashSet<String>());
				}
			}

			int rowCount = 0;
			Iterator<Row> rowIterator = sheet.rowIterator();
			rowIterator.next();
			while (rowIterator.hasNext()) {
				rowCount++;
				Row row = rowIterator.next();
				Map<String, Object> rowData = new HashMap<>();
				for (int i = 0; i < excelDataBean.getVariables().size(); i++) {

					// Get the cell and variable
					Cell cell = row.getCell(i);
					Variable variable = excelDataBean.getVariables().get(i);

					// Invalid cell
					if (cell.getCellTypeEnum().equals(CellType.BLANK) || cell.getCellTypeEnum().equals(CellType._NONE)) {
						continue;
					}

					// Correct the variable type for the data
					if ("Categorical".equals(variable.getType()) && cell.getCellTypeEnum().equals(CellType.NUMERIC)) {
						addCateogricalValueForVariable(variable, new Double(cell.getNumericCellValue()).toString(), rowData);
					} else if ("Time".equals(variable.getType()) && cell.getCellTypeEnum().equals(CellType.NUMERIC)) {
						try {
							addTimeValueForVariable(variable, cell, rowData);
						} catch (Exception e) {
							throw new Exception("Cell without matching format at row and column --> [" + rowCount + "] [" + i + "]" + "-->" + cell);
						}
					} else if ("Numeric".equals(variable.getType()) && cell.getCellTypeEnum().equals(CellType.NUMERIC)) {
						try {
							addIntegerValueForVariable(variable, cell, rowData);
						} catch (Exception e) {
							throw new Exception("Cell without matching format at row and column --> [" + rowCount + "] [" + i + "]" + "-->" + cell, e);
						}
					} else if ("Categorical".equals(variable.getType()) && cell.getCellTypeEnum().equals(CellType.STRING)) {
						addCateogricalValueForVariable(variable, cell.getStringCellValue(), rowData);
					} else if ("Numeric".equals(variable.getType()) && cell.getCellTypeEnum().equals(CellType.STRING)) {
						try {
							addIntegerValueForVariable(variable, Double.parseDouble(cell.getStringCellValue()), rowData);
						} catch (Exception e) {
							throw new Exception("Cell without matching format at row and column --> [" + rowCount + "] [" + i + "]" + "-->" + cell, e);
						}
					} else if ("Time".equals(variable.getType()) && cell.getCellTypeEnum().equals(CellType.STRING)) {
						throw new Exception("Cell without matching format at row and column --> [" + rowCount + "] [" + i + "]" + "-->" + cell);
					}

				}

				String insertValues = "";
				for (int i = 0; i < excelDataBean.getVariables().size(); i++) {
					Variable variable = excelDataBean.getVariables().get(i);
					String insert = null;
					if (variable.getType().equals("Categorical")) {
						insert = "\'" + rowData.get(variable.getName()) + "\'";
					} else if ("Time".equals(variable.getType())) {
						insert = "TO_DATE('" + new SimpleDateFormat("MM-dd-yyyy").format((Date) rowData.get(variable.getName())) + "\', \'MM-dd-yyyy')";
					} else {
						insert = rowData.get(variable.getName()).toString();
					}

					if (i <= excelDataBean.getVariables().size() - 2) {
						insertValues = insertValues + insert + ",";
					} else {
						insertValues = insertValues + insert;
					}
				}

				if (null != jdbcTemplate)
					jdbcTemplate.execute(insertStatement + insertValues + ")");
			}
			excelDataBean.setSize(rowCount);

			// Inserting data
			observer.setMessage("Completed saving data in Memory");
			observer.setPercentage(90);
			return excelDataBean;
		} finally {
			if (null != workbook)
				workbook.close();
		}
	}

	private static void addTimeValueForVariable(Variable variable, Cell cell, Map<String, Object> rowData) throws Exception {
		Date date = cell.getDateCellValue();
		// First entry
		if (variable.getRange().size() == 0) {
			variable.getRange().add(date.getTime());
			variable.getRange().add(date.getTime());
		} else {

			// Check if the value is less than min value
			if (variable.getRange().get(0) > date.getTime()) {
				variable.getRange().set(0, date.getTime());
			}

			// Check if value is greater than max value
			if (variable.getRange().get(1) < date.getTime()) {
				variable.getRange().set(1, date.getTime());
			}
		}
		rowData.put(variable.getName(), date);
	}

	private static void addIntegerValueForVariable(Variable variable, Cell cell, Map<String, Object> rowData) {

		cell.getDateCellValue();
		double numericCellValue = cell.getNumericCellValue();
		addIntegerValueForVariable(variable, numericCellValue, rowData);
	}

	private static void addIntegerValueForVariable(Variable variable, double numericCellValue, Map<String, Object> rowData) {

		// First entry
		if (variable.getRange().size() == 0) {
			variable.getRange().add(new Double(Math.floor(numericCellValue)).longValue());
			variable.getRange().add(new Double(Math.ceil(numericCellValue)).longValue());
		} else {

			// Check if the value is less than min value
			if (variable.getRange().get(0) > new Double(Math.floor(numericCellValue)).longValue()) {
				variable.getRange().set(0, new Double(Math.floor(numericCellValue)).longValue());
			}

			// Check if value is greater than max value
			if (variable.getRange().get(1) < new Double(Math.ceil(numericCellValue)).longValue()) {
				variable.getRange().set(1, new Double(Math.ceil(numericCellValue)).longValue());
			}
		}
		rowData.put(variable.getName(), numericCellValue);
	}

	private static void addCateogricalValueForVariable(Variable variable, String value, Map<String, Object> rowData) {
		variable.getValues().add(value);
		rowData.put(variable.getName(), value);
	}

	private static void correctAllVariableTypes(ExcelDataBean excelDataBean, XSSFSheet sheet) throws Exception {
		int lastRow = sheet.getLastRowNum();
		if (0 == lastRow) {
			throw new Exception("No data rows available.");
		}
		for (int j = 0; j < excelDataBean.getVariables().size(); j++) {

			// If the variable type is already known
			if (null != excelDataBean.getVariables().get(j).getType()) {
				continue;
			}

			// Prepare the column types by the looking 2nd 3rd and 4th row
			for (int i = 1; i < lastRow && i < 4; i++) {
				Row row = sheet.getRow(i);
				Cell cell = row.getCell(j);
				Variable variable = excelDataBean.getVariables().get(j);
				correctVariableType(cell, variable);
			}

			// Also check the last row as well
			Row row = sheet.getRow(lastRow);
			Cell cell = row.getCell(j);
			Variable variable = excelDataBean.getVariables().get(j);
			correctVariableType(cell, variable);
		}
	}

	private static void correctVariableType(Cell cell, Variable variable) {
		// Invalid cell
		if (cell.getCellTypeEnum().equals(CellType.BLANK) || cell.getCellTypeEnum().equals(CellType._NONE)) {
			return;
		}

		// If the variable type is categorical, then do nothing go ahead
		if ("Categorical".equals(variable.getType()) || cell.getCellTypeEnum().equals(CellType.STRING)) {
			variable.setType("Categorical");
			return;
		} else if ("Time".equals(variable.getType()) && cell.getCellTypeEnum().equals(CellType.NUMERIC) && !isTimeFormat(cell)) {
			variable.setType("Categorical");
			return;
		} else if ("Time".equals(variable.getType()) && cell.getCellTypeEnum().equals(CellType.NUMERIC) && isTimeFormat(cell)) {
			return;
		} else if ("Numeric".equals(variable.getType()) && cell.getCellTypeEnum().equals(CellType.NUMERIC) && !isTimeFormat(cell)) {
			return;
		} else if ("Numeric".equals(variable.getType()) && cell.getCellTypeEnum().equals(CellType.NUMERIC) && isTimeFormat(cell)) {
			variable.setType("Categorical");
			return;
		} else if (null == variable.getType() && cell.getCellTypeEnum().equals(CellType.NUMERIC) && !isTimeFormat(cell)) {
			variable.setType("Numeric");
			return;
		} else if (null == variable.getType() && isTimeFormat(cell)) {
			variable.setType("Time");
			return;
		} else {
			variable.setType("Categorical");
		}
	}

	private static boolean isTimeFormat(Cell cell) {
		try {
			new SimpleDateFormat("dd-MMM-yyyy").parse(cell.toString());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static void main(String[] args) throws Exception {
		ExcelDataBean excelDataBean = ExcelWorkBookUtil
				.read(ExcelWorkBookUtil.class.getClassLoader().getResourceAsStream("files/D3_2017_Programming_Challenge_Datasetcc75320.xlsx"), null, "default", null);
		System.out.println(excelDataBean);
	}
}
