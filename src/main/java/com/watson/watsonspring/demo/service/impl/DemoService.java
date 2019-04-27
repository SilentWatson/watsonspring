package com.watson.watsonspring.demo.service.impl;

import com.watson.watsonspring.demo.service.IDemoService;
import com.watson.watsonspring.mvcframework.annotation.WService;

/**
 * 核心业务逻辑
 */
@WService
public class DemoService implements IDemoService {

	public String get(String name) {
		return "My name is " + name;
	}

}
