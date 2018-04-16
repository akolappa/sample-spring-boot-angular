package com.sr.util;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ObserverCache {

	private Map<String, Observer> observerCache;

	public void putObserver(String name, Observer observer) {
		if (observerCache == null) {
			observerCache = new HashMap<>();
		}
		observerCache.put(name, observer);
	}

	public Observer getExcelData(String name) {
		if (observerCache == null) {
			return null;
		} else {
			return observerCache.get(name);
		}
	}

}
