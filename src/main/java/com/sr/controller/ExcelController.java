package com.sr.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.sr.model.ExcelDataBean;
import com.sr.model.ExcelDataVO;
import com.sr.model.ServiceResponse;
import com.sr.model.StratumInput;
import com.sr.model.Variable;
import com.sr.util.ExcelDataCache;
import com.sr.util.ExcelWorkBookUtil;
import com.sr.util.GroupTreatmentVO;
import com.sr.util.Observer;
import com.sr.util.ObserverCache;

@Controller
public class ExcelController {

	@Autowired
	private ExcelDataCache excelDataCache;

	@Autowired
	private ObserverCache observerCache;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@PostMapping("excel/upload")
	public @ResponseBody ResponseEntity<?> uploadFile(Model model, final MultipartFile file, HttpServletRequest request) throws IOException {

		try {
			final InputStream in = file.getInputStream();
			final Observer observer = new Observer();
			System.out.println("remote-->" + request.getRemoteAddr());
			observerCache.putObserver(request.getRemoteAddr(), observer);
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						observer.setStatus("In Progress");
						observer.setPercentage(0);
						observer.setMessage("Reading Excel");
						excelDataCache.putExcelData("additional", ExcelWorkBookUtil.read(in, jdbcTemplate, "additional", observer));
						observer.setStatus("Success");
						observer.setPercentage(100);
						observer.setMessage("Completed");
					} catch (Exception e) {
						observer.setStatus("Failed");
						observer.setMessage(e.getMessage());
						e.printStackTrace(System.out);
					} finally {
						try {
							in.close();
						} catch (IOException e) {
						}
					}
				}

			}).start();

			return ResponseEntity.ok(new ServiceResponse<String>("success"));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Error");
		}

	}

	@RequestMapping(value = "excel/progress", method = RequestMethod.POST)
	public @ResponseBody ServiceResponse<Observer> getUploadProgress(HttpServletRequest request) {
		System.out.println("remote-->" + request.getRemoteAddr());
		return new ServiceResponse<Observer>(observerCache.getExcelData(request.getRemoteAddr()));
	}

	@RequestMapping(value = "excel/maxrows", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
	public @ResponseBody ServiceResponse<?> getMaxRows(@RequestBody StratumInput stratumInput) {

		String selectQuery = "select count(*) from ";
		String wherePart = " where ";
		String whereQuery = "";
		for (int i = 0; i < stratumInput.getVariables().size(); i++) {
			Variable variable = stratumInput.getVariables().get(i);
			if (variable.getType().equals("Categorical")) {
				if (!variable.getSelectedValues().isEmpty()) {
					wherePart = wherePart + "\"" + variable.getName() + "\" in (";
					for (int j = 0; j < variable.getSelectedValues().size(); j++) {
						wherePart = wherePart + "'" + variable.getSelectedValues().get(j) + "',";
					}
					wherePart = removeTrailingComma(wherePart) + ") and ";
				}
			}
		}

		if (!" where ".equals(wherePart)) {
			whereQuery = wherePart;
			whereQuery = removeTrailingAnd(whereQuery);
		}
		selectQuery = selectQuery + stratumInput.getTableName() + whereQuery + " ";
		selectQuery = removeTrailingComma(selectQuery);

		System.out.println(selectQuery);
		int maxRows = jdbcTemplate.queryForObject(selectQuery, Integer.class);

		return new ServiceResponse<Integer>(maxRows);

	}

	@RequestMapping(value = "excel/stratum", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
	public @ResponseBody ServiceResponse<?> getStratum(@RequestBody StratumInput stratumInput) {

		String selectQuery = "select ";
		String selectPart = "";
		String groupBy = " group by ";
		String wherePart = " where ";
		String whereQuery = "";
		String orderQuery = "";
		String orderPart = " order by ";
		for (int i = 0; i < stratumInput.getVariables().size(); i++) {
			Variable variable = stratumInput.getVariables().get(i);
			if (variable.getType().equals("Categorical")) {
				groupBy = groupBy + "\"" + variable.getName() + "\",";
				selectPart = selectPart + "\"" + variable.getName() + "\",";
				orderPart = orderPart + "\""  + variable.getName() + "\","; 
				if (!variable.getSelectedValues().isEmpty()) {
					wherePart = wherePart + "\"" + variable.getName() + "\" in (";
					for (int j = 0; j < variable.getSelectedValues().size(); j++) {
						wherePart = wherePart + "'" + variable.getSelectedValues().get(j) + "',";
					}
					wherePart = removeTrailingComma(wherePart) + ") and ";
				}
			} else if (variable.getType().equals("Time")) {
				groupBy = groupBy + "\"" + variable.getName() + "\" > TO_DATE('01-01-" + variable.getValue() + "', 'MM-dd-yyyy')  " + ",";
				orderPart = orderPart + "\"" + variable.getName() + "\" > TO_DATE('01-01-" + variable.getValue() + "', 'MM-dd-yyyy')  " + ",";
				selectPart = selectPart + "\"" + variable.getName() + "\" > TO_DATE('01-01-" + variable.getValue() + "', 'MM-dd-yyyy') as \""
						+ variable.getName() + " after " + variable.getValue() + "\",";
			} else {
				groupBy = groupBy + "\"" + variable.getName() + "\" > " + variable.getValue()  + ",";
				orderPart = orderPart + "\"" + variable.getName() + "\" > " + variable.getValue()  + ",";
				selectPart = selectPart + "\"" + variable.getName() + "\" > " + variable.getValue() + " as \"" + variable.getName() + " > "
						+ variable.getValue() + "\",";
			}
		}

		if(!" order by ".equals(orderPart)) {
			orderQuery = orderPart;
			orderQuery = removeTrailingComma(orderQuery);
		}
		
		if (!" where ".equals(wherePart)) {
			whereQuery = wherePart;
			whereQuery = removeTrailingAnd(whereQuery);
		}
		selectQuery = selectQuery + selectPart + " count(*) from " + stratumInput.getTableName() + whereQuery + " " + groupBy;
		selectQuery = removeTrailingComma(selectQuery) + orderQuery;

		System.out.println(selectQuery);

		List<Map<String, Object>> results = jdbcTemplate.queryForList(selectQuery);
		Iterator<Map<String, Object>> resultsIterator = results.iterator();
		int appendOneFieldLocation = 0;
		double error = 0;
		List<List<GroupTreatmentVO>> groupTreatmentList = new ArrayList<>();
		while (resultsIterator.hasNext()) {
			List<GroupTreatmentVO> treatmentList = new ArrayList<>();
			groupTreatmentList.add(treatmentList);
			Map<String, Object> row = resultsIterator.next();
			String rowData = "";
			int count = 0;
			for (Map.Entry<String, Object> entry : row.entrySet()) {
				if (entry.getKey().equals("COUNT(*)")) {
					count = ((Long) entry.getValue()).intValue();
				} else {
					rowData = rowData + entry.getKey() + ":<b>" + entry.getValue() + "</b>,";
				}
			}
			rowData = removeTrailingComma(rowData);
			double groupCount = ((double) count * stratumInput.getRows()) / stratumInput.getExcelMaxRows();
			double currentError = groupCount - Math.floor(groupCount);
			int groupRemainder = ((int) Math.floor(groupCount)) % stratumInput.getTreatmentCount();
			error = error + currentError;
			if (error >= 1.0) {
				groupRemainder++;
				error = error - 1;
			}

			// For each treatment
			for (int i = 1; i <= stratumInput.getTreatmentCount(); i++) {
				int finalCount = (int) Math.round(((count * stratumInput.getRows()) / (stratumInput.getExcelMaxRows() * stratumInput.getTreatmentCount())));
				treatmentList.add(new GroupTreatmentVO(rowData, i, finalCount));
			}

			// Adjust the remaining remainder
			while (groupRemainder > 0) {
				for (int i = 1; i <= stratumInput.getTreatmentCount(); i++) {
					if (appendOneFieldLocation == i - 1) {
						int adjustedCount = treatmentList.get(i - 1).getCount() + 1;
						treatmentList.get(i - 1).setCount(adjustedCount);
						appendOneFieldLocation++;
						if (appendOneFieldLocation >= stratumInput.getTreatmentCount()) {
							appendOneFieldLocation = 0;
						}
						groupRemainder = groupRemainder - 1;
						if (groupRemainder == 0) {
							break;
						}
					}
				}
			}
		}

		if (error > 0.5) {
			int countAdjustError = groupTreatmentList.get(groupTreatmentList.size() - 1).get(appendOneFieldLocation).getCount() + 1;
			groupTreatmentList.get(groupTreatmentList.size() - 1).get(appendOneFieldLocation).setCount(countAdjustError);
		}

		return new ServiceResponse<List<List<GroupTreatmentVO>>>(groupTreatmentList);
	}

	private String removeTrailingComma(String selectQuery) {
		if (selectQuery.charAt(selectQuery.length() - 1) == ',') {
			selectQuery = selectQuery.substring(0, selectQuery.length() - 1);
		}
		return selectQuery;
	}

	private String removeTrailingAnd(String selectQuery) {
		if (selectQuery.lastIndexOf(" and ") != -1) {
			selectQuery = selectQuery.substring(0, selectQuery.lastIndexOf(" and "));
		}
		return selectQuery;
	}

	@RequestMapping(value = "excel/{excelName}", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody ServiceResponse<?> searchItemsForRegion(@PathVariable(value = "excelName") String excelName) {

		System.out.println("get data for excel: " + excelName);

		// Get the ExcelDataBean
		ExcelDataBean excelDataBean = excelDataCache.getExcelData(excelName);
		if (null == excelDataBean) {
			ServiceResponse<String> serviceResponse = new ServiceResponse<String>();
			serviceResponse.setContent("Cannot find excel sheet");
			return serviceResponse;
		}

		ExcelDataVO excelDataVO = convertExcelBeanToVO(excelDataBean);

		ServiceResponse<ExcelDataVO> serviceResponse = new ServiceResponse<ExcelDataVO>();
		serviceResponse.setContent(excelDataVO);
		return serviceResponse;
	}

	private ExcelDataVO convertExcelBeanToVO(ExcelDataBean excelDataBean) {
		// Convert the ExcelDataBean to VO
		ExcelDataVO excelDataVO = new ExcelDataVO();
		excelDataVO.setVariables(new ArrayList<Variable>());
		excelDataVO.setMaxRows(excelDataBean.getSize());
		for (int i = 0; i < excelDataBean.getVariables().size(); i++) {
			Variable beanVariable = excelDataBean.getVariables().get(i);
			Variable variable = new Variable(beanVariable.getName());
			variable.setType(beanVariable.getType());
			if ("Categorical".equals(variable.getType())) {
				variable.setValues(beanVariable.getValues());
			} else if ("Time".equals(variable.getType())) {
				List<Long> beanVariableRange = beanVariable.getRange();
				List<Long> range = new ArrayList<>();
				variable.setRange(range);
				Date startDate = new Date(beanVariableRange.get(0));
				Date endDate = new Date(beanVariableRange.get(1));

				Calendar start = Calendar.getInstance();
				start.setTime(startDate);

				Calendar end = Calendar.getInstance();
				end.setTime(endDate);

				// if the year is different set the max and min as years
				if (start.get(Calendar.YEAR) != end.get(Calendar.YEAR)) {
					range.add(new Long(start.get(Calendar.YEAR)));
					range.add(new Long(end.get(Calendar.YEAR)));
					variable.setRangeType("Year");
				} else if (start.get(Calendar.MONTH) != end.get(Calendar.MONTH)) {
					range.add(new Long(start.get(Calendar.MONTH)));
					range.add(new Long(end.get(Calendar.MONTH)));
					variable.setRangeType("Month");
				} else {
					range.add(new Long(start.get(Calendar.DAY_OF_MONTH)));
					range.add(new Long(end.get(Calendar.DAY_OF_MONTH)));
					variable.setRangeType("Day of Month");
				}

			} else {
				variable.setRange(beanVariable.getRange());
			}
			excelDataVO.getVariables().add(variable);
		}
		return excelDataVO;
	}

}
