package com.lenovo.tv;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {
    
    static final Logger sLogger = LoggerFactory.getLogger(Constants.class);
    
    private static String sShiyunItemUrl;
    private static String sShiyunVendorUrl;
    private static String proxyServer;
    private static int proxyPort;
    
    static {
        InputStream mStream = null;
        try {
            mStream = new FileInputStream(new File("conf/common.properties"));
            Properties prop = new Properties();
            prop.load(mStream);
            sShiyunItemUrl = prop.getProperty("shiyun.item.url", "http://cord.tvxio.com/api/item/");
            sShiyunVendorUrl = prop.getProperty("shiyun.vendor.url", "http://cord.tvxio.com/api/vendor/lenovo/items/?format=xml");
            proxyServer = prop.getProperty("proxy.server");
            proxyPort = Integer.parseInt(prop.getProperty("proxy.port"));
        } catch (IOException e) {
            sLogger.error("读取配置文件失败！", e);
        } finally {
            if (mStream != null) {
                try {
                    mStream.close();
                } catch (IOException e) {
                }
            }
        }
    }
    
    /**
     * 获取视云视频详情URL
     */
    public static final String shiyunItemUrl() {
        return sShiyunItemUrl;
    }

    /**
     * 获取视云数据全量接口
     */
    public static final String shiyunVendorUrl() {
        return sShiyunVendorUrl;
    }
    
    public static final String proxyServer() {
    	return proxyServer;
    }
    public static final int proxyPort() {
    	return proxyPort;
    }
}
