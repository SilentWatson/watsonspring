package com.watson.watsonspring.demo.mvc.action;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.watson.watsonspring.demo.service.IDemoService;
import com.watson.watsonspring.mvcframework.annotation.WAutowired;
import com.watson.watsonspring.mvcframework.annotation.WController;
import com.watson.watsonspring.mvcframework.annotation.WRequestMapping;
import com.watson.watsonspring.mvcframework.annotation.WRequestParam;

@WController
@WRequestMapping("/demo")
public class DemoAction {

  	@WAutowired
	private IDemoService demoService;

	@WRequestMapping("/query")
	public void query(HttpServletRequest req, HttpServletResponse resp,
					  @WRequestParam("name") String name){
		String result = demoService.get(name);
//		String result = "My name is " + name;
		try {
			resp.getWriter().write(result);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@WRequestMapping("/add")
	public void add(HttpServletRequest req, HttpServletResponse resp,
					@WRequestParam("a") Integer a, @WRequestParam("b") Integer b){
		try {
			resp.getWriter().write(a + "+" + b + "=" + (a + b));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@WRequestMapping("/remove")
	public void remove(HttpServletRequest req,HttpServletResponse resp,
					   @WRequestParam("id") Integer id){
	}

}
