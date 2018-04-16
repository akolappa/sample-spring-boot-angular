package com.sr;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.sr.util.ExcelDataCache;
import com.sr.util.ExcelWorkBookUtil;

@Component
public class ProcessDefaultExcelFile {

	@Autowired
	ExcelDataCache excelDataCache;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@PostConstruct
	public void init() {
		try {
			excelDataCache.putExcelData("default",
					ExcelWorkBookUtil.read(
							ExcelWorkBookUtil.class.getClassLoader().getResourceAsStream("files/D3_2017_Programming_Challenge_Datasetcc75320.xlsx"),
							jdbcTemplate, "default", null));

		} catch (Exception e) {
			System.out.println("Exception occured while reading file-->" + e.getMessage());
			System.exit(1);
		}
	}
}
