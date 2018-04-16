package com.sr.util;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sr.model.ExcelDataBean;

@Component
public class ExcelDataCache {

	private Map<String, ExcelDataBean> excelDataCache;
	
	public void putExcelData(String name, ExcelDataBean excelDataBean) {
		if(excelDataCache == null) {
			excelDataCache = new HashMap<>();
		}
		
		excelDataCache.put(name, excelDataBean);
	}
	
	
	public ExcelDataBean getExcelData(String name) {
		if(excelDataCache == null) {
			return null;
		} else {
			return excelDataCache.get(name);
		}
	}
	
	
}
