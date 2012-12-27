package com.lenovo.tv;

import org.springframework.beans.factory.annotation.Value;


public class Constants_bak {

	@Value("${shiyun.item.url}")
	private String itemUrl;
	
	@Value("${shiyun.vendor.url}")
	private String vendorUrl;
	
	@Value("${proxy.server}")
	private String proxyServer;
	
	@Value("${proxy.port}")
	private String proxyPort;

	
	public String getItemUrl() {
		return itemUrl;
	}

	public String getVendorUrl() {
		return vendorUrl;
	}

	public String getProxyServer() {
		return proxyServer;
	}

	public String getProxyPort() {
		return proxyPort;
	}
	
}	