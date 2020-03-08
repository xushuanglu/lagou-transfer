package com.lagou.edu.factory;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.lagou.edu.annotation.ExtAutowired;
import com.lagou.edu.annotation.ExtComponent;
import com.lagou.edu.annotation.ExtRepository;
import com.lagou.edu.annotation.ExtService;
import com.lagou.edu.annotation.ExtTransactional;

/**
 * @author 应癫
 *
 * 工厂类，生产对象（使用反射技术）
 */
public class BeanFactory {


    static List<String> classNames = new ArrayList<>();    // 缓存扫描到的class全限定类名
    static List<String> alreaydependencyInjection = new ArrayList<>(); 

    /**
     * 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
     * 任务二：对外提供获取实例对象的接口（根据id获取）
     */
    private static Map<String,Object> map = new HashMap<>();  // 存储对象


    static {
        // 任务一：扫描注解，通过反射实例化对象）
        // 加载xml
        InputStream resourceAsStream = BeanFactory.class.getClassLoader().getResourceAsStream("beans.xml");
        // 解析xml
        SAXReader saxReader = new SAXReader();
        try {
            Document document = saxReader.read(resourceAsStream);
            Element rootElement = document.getRootElement();
            Element scanElement = (Element) rootElement.selectSingleNode("//component-scan");
            String scanPackage = scanElement.attributeValue("base-package");

            // 扫描注解
            scanAnnotation(scanPackage);
            
            // 实例化
            executeInstantiate();
            
            // 维护依赖注入关系
            executeAutoWired();
            
            // 维护事务
            executeTransactional();


        } catch (DocumentException e) {
            e.printStackTrace();
        }

    }

    /**
     * 扫描指定包下的注解
     */
    private static void scanAnnotation(String scanPackage) {
        String scanPackagePath = Thread.currentThread().getContextClassLoader().getResource("").getPath() + scanPackage.replaceAll("\\.", "/");
        File pack = new File(scanPackagePath);

        File[] files = pack.listFiles();

        for(File file: files) {
            if(file.isDirectory()) { // 子package
                // 递归
            	scanAnnotation(scanPackage + "." + file.getName());  
            }else if(file.getName().endsWith(".class")) {
                String className = scanPackage + "." + file.getName().replaceAll(".class", "");
                classNames.add(className);
            }

        }
    }


    /**
     * 首字母小写方法
     */
    private static String lowerFirst(String str) {
        char[] chars = str.toCharArray();
        if('A' <= chars[0] && chars[0] <= 'Z') {
            chars[0] += 32;
        }
        return String.valueOf(chars);
    }


    /**
     * 通过反射实例化对象，此时暂不维护依赖注入关系
     */
    private static void executeInstantiate() {
        if (classNames.size() == 0) return;

        try {

            for (int i = 0; i < classNames.size(); i++) {
                String className = classNames.get(i);

                // 反射
                Class<?> classz = Class.forName(className);
                if (classz.isAnnotationPresent(ExtService.class)
                        || classz.isAnnotationPresent(ExtRepository.class)
                        || classz.isAnnotationPresent(ExtComponent.class)) {

                    //获取注解value值
                    String beanName = null;

                    if (classz.isAnnotationPresent(ExtService.class)) {
                        beanName = classz.getAnnotation(ExtService.class).value();
                    } else if (classz.isAnnotationPresent(ExtRepository.class)) {
                        beanName = classz.getAnnotation(ExtRepository.class).value();
                    } else if (classz.isAnnotationPresent(ExtComponent.class)) {
                        beanName = classz.getAnnotation(ExtComponent.class).value();
                    }

                    // 如果指定了id，就以指定的为准
                    Object o = classz.newInstance();
                    if ("".equals(beanName.trim())) {
                        beanName = lowerFirst(classz.getSimpleName());
                    }
                    
                    map.put(beanName,o);

                    Class<?>[] interfaces = classz.getInterfaces();
                    if(interfaces != null && interfaces.length > 0) {
                        for (int j = 0; j < interfaces.length; j++) {
                            Class<?> anInterface = interfaces[j];
                            // 以接口的全限定类名作为id放入
                            map.put(anInterface.getName(), classz.newInstance());
                        }
                    }

                } else {
                    continue;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /*
     * 实现依赖注入
     */
    private static void executeAutoWired(){
        if(map.isEmpty()) {return;}

        // 遍历ioc中所有对象，查看对象中的字段，是否有@ExtAutowired注解，如果有需要维护依赖注入关系
        for(Map.Entry<String,Object> entry: map.entrySet()) {
            try {
                doObjectDependancy(entry.getValue());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * A 可能依赖于 B ，B 可能依赖于 C
     */
    private static void doObjectDependancy(Object object) throws IllegalAccessException {
        Field[] declaredFields = object.getClass().getDeclaredFields();

        if(declaredFields == null || declaredFields.length ==0) {
            return;
        }
        // 遍历判断处理
        for (int i = 0; i < declaredFields.length; i++) {

            Field declaredField = declaredFields[i];

            if (!declaredField.isAnnotationPresent(ExtAutowired.class)) {
                continue;
            }

            // 判断当前字段是否处理过，如果已经处理过则continue，避免嵌套处理死循环
            if(alreaydependencyInjection.contains(object.getClass().getName()  + "." + declaredField.getName())){
                continue;
            }


            Object dependObject = null;
            dependObject = map.get(declaredField.getType().getName());  //  先按照声明的是接口去获取，如果获取不到再按照首字母小写

            if(dependObject == null) {
                dependObject = map.get(lowerFirst(declaredField.getType().getSimpleName()));
            }

            alreaydependencyInjection.add(object.getClass().getName() + "." + declaredField.getName());

            doObjectDependancy(dependObject);

            declaredField.setAccessible(true);
            declaredField.set(object,dependObject);

        }
    }


    private static void executeTransactional() {
        ProxyFactory proxyFactory = (ProxyFactory) map.get("proxyFactory");
        for(Map.Entry<String,Object> entry: map.entrySet()) {
            String beanName = entry.getKey();
            Object o = entry.getValue();
            Class<?> classz = entry.getValue().getClass();
            if(classz.isAnnotationPresent(ExtTransactional.class)) {
                Class<?>[] interfaces = classz.getInterfaces();
                if(interfaces != null && interfaces.length > 0) {
                    // 使用jdk动态代理
                    map.put(beanName,proxyFactory.getJdkProxy(o));
                }else{
                    // 使用cglib动态代理
                    map.put(beanName,proxyFactory.getCglibProxy(o));
                }
            }
        }
    }


    // 任务二：对外提供获取实例对象的接口（根据id获取）
    public static  Object getBean(String id) {
        return map.get(id);
    }

}
