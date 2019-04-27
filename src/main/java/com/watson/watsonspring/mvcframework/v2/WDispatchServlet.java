package com.watson.watsonspring.mvcframework.v2;

import com.watson.watsonspring.mvcframework.annotation.WAutowired;
import com.watson.watsonspring.mvcframework.annotation.WController;
import com.watson.watsonspring.mvcframework.annotation.WRequestMapping;
import com.watson.watsonspring.mvcframework.annotation.WService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class WDispatchServlet extends HttpServlet {

    private Properties contextConfigure = new Properties();
    private List<String> classNames = new ArrayList<>();
    private Map<String, Object> ioc = new HashMap<>();
    private Map<String, Method> handlerMapping = new HashMap<>();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //6、调用，运行阶段
        try {
            doDispatch(req, resp);
        }catch (Exception e){
            e.printStackTrace();
            resp.getWriter().write("500 Exception, detail:"+Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     * 调用
     * @param req
     * @param resp
     */
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception{
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
        if(!handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found, by Watson");
            return;
        }
        Map<String, String[]> params = req.getParameterMap();
        Method method = this.handlerMapping.get(url);
        String beanName = firstToLowerCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName),new Object[]{req,resp,params.get("name")[0]});

    }
    //初始化阶段
    @Override
    public void init(ServletConfig config) throws ServletException {
        //1、加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //2、扫描相关类
        doScanner(contextConfigure.getProperty("scanPackage"));
        //3、初始化扫描到的类，将他们放入ioc容器中
        doInstance();
        //4、完成依赖注入
        doAutowired();
        //5、初始化HandlerMapping
        initHandlerMapping();
        System.out.println("Watson spring framework is inited.");
    }
    private void doLoadConfig(String contextConfigLocation) {
        InputStream is = null;
        is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfigure.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(is!=null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));
        File classPath = new File(url.getFile());
        for (File file: classPath.listFiles()) {
            if(file.isDirectory()){
                doScanner(scanPackage+"."+file.getName());
            } else{
                if(!file.getName().endsWith(".class")){
                    continue;
                }
                String className = (scanPackage+"."+file.getName()).replace(".class","");
                this.classNames.add(className);
            }
        }
    }
    private void doInstance() {
        if(classNames.isEmpty()){
            return;
        }
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                //什么样的类才需要初始化
                //加了注解的
                if(clazz.isAnnotationPresent(WController.class)){
                    Object instance = clazz.newInstance();
                    String beanName = firstToLowerCase(clazz.getSimpleName());
                    ioc.put(beanName, instance);
                }else if(clazz.isAnnotationPresent(WService.class)){
                    //1、将首字母小写
                    //2、自定义的beanName
                    WService service = clazz.getAnnotation(WService.class);
                    String beanName = service.value();
                    if(!"".equals(beanName.trim())){
                        beanName = firstToLowerCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                    //3根据类型自动赋值，投机取巧的方式
                    for(Class<?> i: clazz.getInterfaces()){
                        if(ioc.containsKey(i.getName())){
                            throw new Exception("The"+i.getName()+"exists!");
                        }
                        ioc.put(i.getName(), instance);
                    }
                }else{
                    continue;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void doAutowired() {
        if(ioc.isEmpty()){
            return;
        }
        for (Map.Entry<String, Object> entry: ioc.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field: fields ) {
                if(!field.isAnnotationPresent(WAutowired.class)){
                    continue;
                }
                WAutowired autowired = field.getAnnotation(WAutowired.class);
                String beanName = autowired.value().trim();
                if("".equals(beanName)){
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

    }
    private void initHandlerMapping() {
        if(ioc.isEmpty()){
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(WController.class)){continue;}
            String baseUrl = "";
            if(clazz.isAnnotationPresent(WRequestMapping.class)){
                WRequestMapping requestMapping = clazz.getAnnotation(WRequestMapping.class);
                baseUrl = requestMapping.value();
            }
            for (Method method : clazz.getMethods()) {
                if(!method.isAnnotationPresent(WRequestMapping.class)){continue;}
                WRequestMapping requestMapping = method.getAnnotation(WRequestMapping.class);
                String url = (baseUrl+"/"+requestMapping.value()).replaceAll("/+","/");
                handlerMapping.put(url, method);
                System.out.println("Mapped:"+url+","+method);
            }


        }

    }
    private String firstToLowerCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }





}
