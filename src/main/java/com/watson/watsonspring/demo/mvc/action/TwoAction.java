package com.watson.watsonspring.demo.mvc.action;

import com.watson.watsonspring.demo.service.IDemoService;
import com.watson.watsonspring.mvcframework.annotation.WAutowired;
import com.watson.watsonspring.mvcframework.annotation.WService;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TwoAction {

    @WAutowired
	private IDemoService demoService;

	public void edit(HttpServletRequest req,HttpServletResponse resp,
					 String name){
		String result = demoService.get(name);
		try {
			resp.getWriter().write(result);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
