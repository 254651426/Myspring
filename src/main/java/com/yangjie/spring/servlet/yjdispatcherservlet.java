package com.yangjie.spring.servlet;

import com.yangjie.spring.annoation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

//手写spring了 看了tom老师的视频 自己练习
public class yjdispatcherservlet extends HttpServlet {

    //存蓄配置文件里面扫描出来的类
    List<String> className = new ArrayList<String>();

    //ioc容器
    Map<String, Object> iocmap = new HashMap<String, Object>();

    //handlerMapping容器
    Map<String, Method> handlerMapping = new HashMap<String, Method>();
    private Properties properties = new Properties();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String url = req.getRequestURI(); //获取路径
        String contextPath = req.getContextPath(); //获取项目名称
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/"); //去掉项目名称获取后面的路径

        if (!handlerMapping.containsKey(url)) {
            resp.getWriter().write("404");
            return;
        }

        Map<String,String[]> params = req.getParameterMap();

        Method method = this.handlerMapping.get(url);

        //获取形参列表
        Class<?>[] parameterTypes = method.getParameterTypes();
        //实参列表
        Object[] parameter = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class parameterType = parameterTypes[i];
            if (parameterType == HttpServletResponse.class) {
                parameter[i] = resp;
            } else if (parameterType == HttpServletRequest.class) {
                parameter[i] = req;
            } else if (parameterType == String.class) {
                Annotation[][] pa = method.getParameterAnnotations();
                for (int j = 0; j < pa.length; j++) {
                    for (Annotation a : pa[i]) {
                        if (a instanceof YJRequestParam) {
                            String paramName = ((YJRequestParam) a).value();
                            if (!"".equals(paramName.trim())) {
                                String value = Arrays.toString(params.get(paramName))
                                        .replaceAll("\\[|\\]", "")
                                        .replaceAll("\\s+", ",");
                                parameter[i] = value;
                            }
                        }
                    }
                }
            }
        }


        //暂时硬编码
        String beanName = YJtoLowerCase(method.getDeclaringClass().getSimpleName());
        //赋值实参列表
        try {
            method.invoke(iocmap.get(beanName), parameter);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        resp.getWriter().write("hello");
    }

    //配置文件一开始是从ServletConfig 里面取的
    @Override
    public void init(ServletConfig config) throws ServletException {
        //扫描配置文件 读取web.xml里面的标签
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //读取配置文件相关类信息
        doScanner(properties.getProperty("package"));
        //初始化IOC容器 将刚刚从配置文件里面扫描到的类进行实例化
        doInstance();
        //DI 依赖注入
        doAutowired();
        //5、初始化HandlerMapping
        doInitHandlerMapping();
    }


    private void doInitHandlerMapping() {
        if (iocmap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : iocmap.entrySet()) {
            //如果不是controller 就直接跳出
            if (!entry.getValue().getClass().isAnnotationPresent(YJController.class)) {
                continue;
            }

            //先获取controller的url
            String baseurl = "";
            if (entry.getValue().getClass().isAnnotationPresent(YJRequestMapping.class)) {
                baseurl = entry.getValue().getClass().getAnnotation(YJRequestMapping.class).value();
            }

            //获取方法上面的url
            for (Method method : entry.getValue().getClass().getMethods()) {
                if (!method.isAnnotationPresent(YJRequestMapping.class)) {
                    continue;
                }
                String url = ("/" + baseurl + method.getAnnotation(YJRequestMapping.class).value()).replaceAll("/+", "/");
                handlerMapping.put(url, method);
                System.out.println("Mapped : " + url + "," + method);
            }


        }

    }

    private void doAutowired() {
        if (iocmap.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : iocmap.entrySet()) {
            //取出实体类里面所有的字段 private public 所有类型都取出来
            for (Field field : entry.getValue().getClass().getDeclaredFields()) {
                if (!field.isAnnotationPresent(YJAutowired.class)) {
                    continue;
                }

                //得到beanname 如果没有指定beanname 就用默认类型注入
                String beanname = field.getAnnotation(YJAutowired.class).value();
                if (beanname.equals("")) {
                    beanname = field.getType().getName();
                }
                //暴力访问
                field.setAccessible(true);

                try {
                    field.set(entry.getValue(), iocmap.get(beanname));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }

        }

    }

    private void doInstance() {
        if (className.isEmpty()) {
            return;
        }

        //这个位置。tom老师存入集合的时候用的getSimpleName 判断的时候用的getName 有问题。
        //次问题想明白了。getName 是针对接口的。
        //遍历classname
        for (String classname : className) {
            try {
                Class<?> clazz = Class.forName(classname);

                //判断带了注解的类才实例化
                if (clazz.isAnnotationPresent(YJController.class)) {
                    //得到beanname  首字母小写
                    String beanname = YJtoLowerCase(clazz.getSimpleName());
                    iocmap.put(beanname, clazz.newInstance());
                } else if (clazz.isAnnotationPresent(YJService.class)) {
                    //如果@Service没有给名字就用默认的首字母小写用于beanname
                    YJService yjService = clazz.getAnnotation(YJService.class);
                    //一个接口有多个实现类 就自己取一个名字区别 自己理解如果这个注解自己起了名字就用起的名字
                    String beanname = yjService.value();
                    if (beanname.equals("")) {
                        beanname = YJtoLowerCase(clazz.getSimpleName());
                    }

                    iocmap.put(beanname, clazz.newInstance());

                    //如果有多个实现类就报错
                    for (Class<?> c : clazz.getInterfaces()) {
                        if (iocmap.containsKey(c.getName())) {
                            throw new Exception("the" + c.getName() + "is exists");
                        }
                        //一定要把接口装进去。。而不是放beanname。。。。
                        iocmap.put(c.getName(), clazz.newInstance());
                    }
                } else {
                    continue;//启动的不实例化
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    private String YJtoLowerCase(String simpleName) {
        char[] c = simpleName.toCharArray();
        c[0] += 32;
        return String.valueOf(c);
    }

    private void doScanner(String Package) {
        URL url = this.getClass().getClassLoader().getResource("/" + Package.replaceAll("\\.", "/"));
        File file = new File(url.getFile());

        for (File f : file.listFiles()) {

            //如果是文件夹继续递归
            if (f.isDirectory()) {
                doScanner(Package + "." + f.getName());
            } else {
                if (!f.getName().endsWith(".class")) {
                    continue;
                }
                //.class 替换掉
                String classname = Package + "." + f.getName().replaceAll(".class", "");
                className.add(classname);
            }
        }

    }


    private void doLoadConfig(String contextConfigLocation) {
        //从项目根目录找到该位置文件并转化成properties文件
        InputStream input = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


}
