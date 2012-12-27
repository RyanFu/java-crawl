package com.lenovo.tv.schedule;

import java.io.IOException;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.xmlpull.v1.XmlPullParserException;

public class ShiyunMain {

    private static ApplicationContext sContext;

    @SuppressWarnings("unchecked")
    public static <T> T getBean(String beanName) {
        return (T) sContext.getBean(beanName);
    }

    public static void main(String[] args) throws IOException, XmlPullParserException {
        sContext = new ClassPathXmlApplicationContext("applicationContext.xml");
//        ContentIncrementJob job = new ContentIncrementJob();
//        job.execute();
//        job.detail("126726");
    }

}