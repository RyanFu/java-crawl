package com.lenovo.tv.schedule;

import java.util.Date;

public class Shiyun {

	public String pk;
	public String parentPk;
	public String name;
	public String title;
	public String director;
	public String actor;
	public String adletUrl;
	public String mainUrl;
	public String playUrl;
	public String focus;
	public String type;
	public String presenter;
	public String guest;
	public int grade;
	public Date airDate;
	public String description;
	public String language;
	public String tag;
	public String session;
	public int episode;
	public int episodeTotal;
	public int dutation;
	public String area;
	public int sourceQuality;
	public String posterUrl;
	public String thumbUrl;
	public int isComplex;
	

	@Override
	public String toString() {
		return "{ pk: " + pk + ", title: " + title + ", director: " + director + ", isComplex: " + isComplex + "}";
	}
}
