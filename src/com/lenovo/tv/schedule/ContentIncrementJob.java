package com.lenovo.tv.schedule;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParserException;

import com.lenovo.tv.Constants;
import com.lenovo.tv.dao.MovieDao;
import com.mysql.jdbc.StringUtils;

/**
 * 视云内容增量更新。处理流程是先把视云的数据内容先落地，然后解析视云XML格式数据文件，再入库新纪录。
 */
public class ContentIncrementJob {

    static final Logger sLogger = Logger.getLogger(ContentIncrementJob.class);
    static final File sTempFile = new File("tmp.xml");
    static long counter = 0;
//    static MovieDao movieDao = ShiyunMain.getBean("movieDao");
    @Resource
    MovieDao movieDao;
    
    static DefaultHttpClient httpclient;
    
    static {
//    	HttpParams params = new BasicHttpParams();
//    	ConnRouteParams.setDefaultProxy(params, new HttpHost(Constants.proxyServer(), Constants.proxyPort()));
//    	HttpConnectionParams.setSoTimeout(params, 5 * 60 * 1000);
//		HttpConnectionParams.setConnectionTimeout(params, 5 * 60 * 1000);
    	httpclient = new DefaultHttpClient();
    }
    
    public void execute() {
        counter = 0;
        long begin = System.currentTimeMillis();
        sLogger.info("开始执行视云数据增量更新！");
        download();
        long download = System.currentTimeMillis();
        sLogger.info("获取视云数据完成！耗时:" + (download - begin));
        if (!sTempFile.exists()) {
            sLogger.info("初始化视云数据文件失败！");
            return;
        }
        
        FileReader reader = null;
        
        try {
            reader = new FileReader(sTempFile);
            MXParser parser = new MXParser();
            parser.setInput(reader);
            int eventType = parser.getEventType();
            while(eventType != MXParser.END_DOCUMENT) {
                if (eventType == MXParser.START_TAG) {
                    String name = parser.getName();
                    if (name.equalsIgnoreCase("list-item")) {
                        Item item = parseItem(parser);//解析视云数据，并返回一个自定义的Item对象
                        
                        Shiyun data = detail(item.id);
                        if (data != null) {
                        	if (data.title==null && data.name!=null) {
                        		data.title = data.name;
                            } 
                        	if (StringUtils.isNullOrEmpty(data.pk)) {
                        		data.pk = item.id;
                        	}
                        	if (data.episodeTotal > 0) {
                        		Shiyun subItem = data;
                        		data.title = data.name;
                        		data.pk = data.parentPk;
                        		data.mainUrl = "http://cord.tvxio.com/api/item/" + data.parentPk + "/?format=xml";
                        		data.episode = 0;
                        		movieDao.insertShiyun(subItem);
                        		if (data.isComplex == 1) {
                        			movieDao.updateSubItems(data.pk);
                        		}
                        	}
                        	 //保存到数据库
//                        	sLogger.info(String.format("视云：entity = %1$s", data.toString()));
                            movieDao.insertShiyun(data);
                            counter++;
                        }
                    }
                }
                eventType = parser.next();
            }
        } catch (FileNotFoundException e) {
            sLogger.error("未找到视云数据文件！", e); 
        } catch (XmlPullParserException e) {
            sLogger.error("视云数据文件格式错误！", e);
        } catch (IOException e) {
            sLogger.error("视云数据文件解析错误！", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        long end = System.currentTimeMillis();
        sLogger.info("视云数据增量更新完成！耗时:" + (end - begin) + ", 新增数据：" + counter + " 条");
    }
    
    /**
     * 视云数据临时落地
     */
    static void download() {
        if (sTempFile.exists()) {
            sTempFile.delete();
        }
        BufferedInputStream input = null;
        FileOutputStream output = null;
        try {
//            URL url = new URL(Constants.shiyunVendorUrl());
//            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection(new Proxy(Type.HTTP, new InetSocketAddress("10.99.20.30", 8080)));
//            httpURLConnection.setDoInput(true);
//            httpURLConnection.setRequestMethod("GET");
//            httpURLConnection.setRequestProperty("Content-type", "text/xml");
//            httpURLConnection.connect();
            
            HttpGet httpGet = new HttpGet(Constants.shiyunVendorUrl());
            HttpResponse response = httpclient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            InputStream stream = entity.getContent();
            
            input = new BufferedInputStream(stream);
            output = new FileOutputStream(sTempFile);
            byte[] buff = new byte[8192];
            int len = -1;
            while ((len = input.read(buff)) != -1) {
                output.write(buff, 0, len);
            }
//            httpURLConnection.disconnect();
            
        } catch (IOException e) {
            sLogger.error("下载视云数据文件错误！", e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                }
            }
        }
    }
    
    Item parseItem(MXParser parser) throws XmlPullParserException, IOException {
        Item item = new Item();
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == MXParser.START_TAG) {
                String name = parser.getName();
                if (name.equalsIgnoreCase("pk")) {
                    item.id = parserText(parser).trim();
                } else if (name.equalsIgnoreCase("content_model")) {
                    item.model = parserText(parser).trim();
                } else if (name.equalsIgnoreCase("title")) {
                    item.title = parserText(parser).trim();
                }
            } else if (eventType == MXParser.END_TAG) {
                String name = parser.getName();
                if (name.equalsIgnoreCase("list-item")) {
                    done = true;
                }
            }
        }
        
        return item;
    }
    
    String parserText(MXParser parser) throws XmlPullParserException, IOException {
        String text = "";
        boolean done = false;
        while (!done) {
            int type = parser.next();
            if (type == MXParser.TEXT) {
                text = parser.getText();
                done = true;
            }
        }
        return text;
    }
    
    
    /**
     * 获取视云内容信息
     * 
     * @param id
     *            视云内容id
     * @return
     * @throws IOException
     * @throws XmlPullParserException 
     */
    Shiyun detail(final String id) throws IOException, XmlPullParserException {
        Shiyun entity = new Shiyun();
        final String url = shiyunUrlBuilder(id);
        entity.parentPk = id;
        entity.mainUrl = url;
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpGet);
        Integer status = response.getStatusLine().getStatusCode();
        if (status != HttpStatus.SC_OK) {
        	sLogger.info("connection exception,url : " + url);
//            throw new RuntimeException();
            return null;
        }
        String charset = "utf-8";
        HttpEntity tmpEntity = response.getEntity();
        String body = EntityUtils.toString(tmpEntity, charset);
        MXParser parser = new MXParser();
        parser.setInput(new StringReader(body));
        int eventType = parser.getEventType();
        while(eventType != MXParser.END_DOCUMENT) {
            if (eventType == MXParser.START_TAG) {
                String name = parser.getName().intern();
                int depth = parser.getDepth();
                if (name == "title" && depth == 2) {
                	parse(parser, entity, "name");
                } else if (name == "length" && depth == 3) {
                	parse(parser, entity, "length");
                } else if (name == "focus" && depth == 2) {
                	parse(parser, entity, "focus");
                } else if (name == "content_model" && depth == 2) {
                	parse(parser, entity, "type");
                } else if (name == "pk" && depth == 2) {
                	parse(parser, entity, "pk");
                } else if (name == "quality" && depth == 2) {
                	parse(parser, entity, "quality");
                } else if (name == "poster_url" && depth == 2) {
                	parse(parser, entity, "poster_url");
                } else if (name == "thumb_url" && depth == 2) {
                	parse(parser, entity, "thumb_url");
                } else if (name == "adlet_url" && depth == 2) {
                	parse(parser, entity, "adlet_url");
                } else if (name == "description" && depth == 2) {
                	parse(parser, entity, "description");
                } else if (name == "rating_average" && depth == 2) {
                	parse(parser, entity, "rating_average");
                } else if (name == "publish_date" && depth == 2) {
                	parse(parser, entity, "publish_date");
                } else if (name == "is_complex" && depth == 2) {
                	parse(parser, entity, "is_complex");
                } else if (name == "episode" && depth == 2) {
                	parse(parser, entity, "episode");
                } else if (name == "director" && depth == 3) {
                    parseDirector(parser, entity);
                } else if (name == "actor" && depth == 3) {
                    parseActor(parser, entity);
                } else if (name == "area" && depth == 3) {
                	parse2(parser, entity, "area");
                } else if (name== "emcee" && depth == 3) {
                	parsePresenter(parser, entity);
                } else if (name == "tags" && depth == 3) {
                	parse2(parser, entity, "tags");
                } else if (name == "genre" && depth == 3) {
                	parseGenre(parser, entity);
                } else if (name == "subitems" && depth == 2) {
                	parseSubitems(parser, entity);
                }
            }
            eventType = parser.next();
        }
        
        return entity;
    }
    
    /**
     * 解析非数组属性
     * @param parser
     * @param entity
     * @throws XmlPullParserException
     * @throws IOException
     */
    void parse(MXParser parser, Shiyun entity, String key) throws XmlPullParserException, IOException {
        String value = "";
        boolean done = false;
        while (!done) {
            int type = parser.next();
            if (type == MXParser.TEXT) {
                value = parser.getText();
                done = true;
            }
        }
//        sLogger.info("id is : " + entity.pk + ", attribute key is : " + key + ", value is :" + value);
        if (key == "title") {
        	entity.title = value;
        	Pattern p = Pattern.compile(".+第(\\d+)集");
        	Matcher m = p.matcher(value);
        	if (m.find()) {
        		int episode = Integer.parseInt(m.group(1));
        		entity.episode = episode;
        	}
        	
        } else if (key == "name") {
        	entity.name = value.trim();
        	Pattern p = Pattern.compile(".+第(\\w+)季");
        	Matcher m = p.matcher(value);
        	if (m.find()) {
        		String session = m.group(1);
        		entity.session = session;
        	}
        } else if (key == "focus") {
        	if (value.intern() != "False")
        		entity.focus = value;
        } else if (key == "type") {
        	entity.type = value;
        } else if (key == "length") {
        	try {
        		entity.dutation = Integer.parseInt(value);
        	} catch (Exception e) {
        		return ;
        	}
        } else if (key == "pk") {
        	entity.pk = value;
        } else if (key == "url") {
        	entity.mainUrl = value;
        } else if (key == "quality") {
        	entity.sourceQuality = Integer.parseInt(value);
        } else if (key == "poster_url") {
        	entity.posterUrl=  value;
        } else if (key == "thumb_url") {
        	entity.thumbUrl = value;
        } else if (key == "adlet_url") {
        	entity.adletUrl = value;
        } else if (key == "description") {
        	entity.description = value;
        } else if (key == "rating_average") {
        	float rate_f = Float.parseFloat(value);
        	int rate = (int) (rate_f*10);
        	entity.grade = rate;
        } else if (key == "publish_date") {
        	SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        	try {
				entity.airDate = sf.parse(value);
			} catch (ParseException e) {
				e.printStackTrace();
				return ;
			}
        } else if (key == "is_complex") {
        	if (value.intern() == "True") {
        		entity.isComplex = 1;
        	}
        } else if (key == "episode") {
        	try {
        		entity.episodeTotal = Integer.parseInt(value);
        	} catch (Exception e) {
        		e.printStackTrace();
        		return ;
        	}
        }
        
    }
    
    /**
     * 解析分集属性
     * @param parser
     * @param entity
     * @throws XmlPullParserException
     * @throws IOException
     */
    void parseSubitems(MXParser parser, Shiyun entity) throws XmlPullParserException, IOException {
    	
        boolean done = false;
        while (!done) {
            int type = parser.next();
            if (type == MXParser.START_TAG) {
                boolean iterateList = false;
                while (!iterateList) {
                	int sub_type = parser.next();
                	int depth = parser.getDepth();
                	if (sub_type == MXParser.START_TAG ) {
                		String sub_name = parser.getName().intern();
                		if (depth == 4 && sub_name == "title") {
                        	parse(parser, entity, "title");
                    	} else if (depth == 4 && sub_name == "url") {
                        	parse(parser, entity, "url");
                        } else if (depth == 4 && sub_name == "pk") {
                        	parse(parser, entity, "pk");
                        } else if (depth == 5 && sub_name == "length") {
                        	parse(parser, entity, "length");
                        } else if (depth == 4 && sub_name == "publish_date") {
                        	parse(parser, entity, "publish_date");
                        } else if (depth == 4 && sub_name == "is_complex") {
                        	parse(parser, entity, "is_complex");
                        } else if (depth == 4 && sub_name == "poster_url") {
                        	parse(parser, entity, "poster_url");
                        } else if (depth == 4 && sub_name == "thumb_url") {
                        	parse(parser, entity, "thumb_url");
                        } else if (depth == 4 && sub_name == "adlet_url") {
                        	parse(parser, entity, "adlet_url");
                        }
                	} else if (sub_type == MXParser.END_TAG) {
                		String sub_name = parser.getName().intern();
                		if (sub_name == "list-item") {
                			iterateList = true;
                		}
                	}
                }
//                sLogger.info(entity);
              //每个分集都作为一个实体保存
                sLogger.info(String.format("视云：entity = %1$s", entity.toString()));
                movieDao.insertShiyun(entity);
                counter ++ ;
            } else if (type == MXParser.END_TAG) {
                String name = parser.getName().intern();
                if (name == "subitems") {
                    done = true;
                }
            }
        }
    }
    
    /**
     * 解析导演
     * @param parser
     * @param entity
     * @throws XmlPullParserException
     * @throws IOException
     */
    void parseDirector(MXParser parser, Shiyun entity) throws XmlPullParserException, IOException {
        boolean done = false;
        StringBuilder directors = new StringBuilder();
        String id = null;
        while (!done) {
            int type = parser.next();
            if (type == MXParser.START_TAG) {
                int depth = parser.getDepth();
                String name = parser.getName().intern();
                if (depth == 5 && name == "list-item") {
                    String text = parser.nextText().trim();
                	if (text.matches("\\d+")){
                		id = text;
                	} 
                	if (!text.matches("\\d+") && id!=null) {
                    	directors.append(text).append("-").append(id).append("/");
                    }
                }
            } else if (type == MXParser.END_TAG) {
                String name = parser.getName().intern();
                if (name == "director") {
                    done = true;
                }
            }
        }
        if (directors != null){
	    	if (directors.length() > 1) {
	    		entity.director = directors.substring(0,directors.length() - 1);
	         }
        }
    }

    /**
     * 解析演员
     * @param parser
     * @param entity
     * @throws XmlPullParserException
     * @throws IOException
     */
    void parseActor(MXParser parser, Shiyun entity) throws XmlPullParserException, IOException {
        boolean done = false;
        StringBuilder actors = new StringBuilder();
        String id = null;
        while (!done) {
            int type = parser.next();
            if (type == MXParser.START_TAG) {
                int depth = parser.getDepth();
                String name = parser.getName().intern();
                if (depth == 5 && name == "list-item") {
                    String text = parser.nextText().trim();
                	if (text.matches("\\d+")){
                		id = text;
                	} 
                	if (!text.matches("\\d+") && id!=null) {
                    	actors.append(text).append("-").append(id).append("/");
                    }
                }
            } else if (type == MXParser.END_TAG) {
                String name = parser.getName().intern();
                if (name == "actor") {
                    done = true;
                }
            }
        }
        if (actors != null){
        	if (actors.length() > 1) {
	    		entity.actor = actors.substring(0,actors.length() - 1);
	         }
        }	
    }
    
    
    static String shiyunUrlBuilder(String id) {
        return String.format("%1$s%2$s/?format=xml", Constants.shiyunItemUrl(), id);
    }
    
    /**
     * 解析地区，标签，参数类型：数组
     * @param parser
     * @param entity
     * @param attr
     * @throws XmlPullParserException
     * @throws IOException
     */
    void parse2(MXParser parser, Shiyun entity, String attr) throws XmlPullParserException, IOException {
    	boolean done = false; 
    	 StringBuilder sb = new StringBuilder();
         while (!done) {
             int type = parser.next();
             if (type == MXParser.START_TAG) {
                 int depth = parser.getDepth();
                 String name = parser.getName().intern();
                 if (depth == 4 && name == "list-item") {
                     String text = parser.nextText().trim();
                     if (!text.matches("\\d+")) {
                    	 sb.append(text).append("/");
                     }
                 }
             } else if (type == MXParser.END_TAG) {
                 String name = parser.getName().intern();
                 if (name == attr) {
                     done = true;
                 }
             }
         }
		if (sb != null) {
			if (sb.length() > 1) {
				String value = sb.substring(0, sb.length() - 1);
				if (attr == "area") {
					entity.area = value;
				} else if (attr == "tags") {
					entity.tag = value;
				}
			}
		}
    }
    
    /**
     * 解析影视类型
     * @param parser
     * @param entity
     * @throws XmlPullParserException
     * @throws IOException
     */
    void parseGenre(MXParser parser, Shiyun entity) throws XmlPullParserException, IOException {
    	boolean done = false; 
    	 StringBuilder tags = new StringBuilder();
         while (!done) {
             int type = parser.next();
             if (type == MXParser.START_TAG) {
                 int depth = parser.getDepth();
                 String name = parser.getName().intern();
				if (depth == 5 && name == "list-item") {
					String text = parser.nextText().trim();
					if (!text.matches("\\d+")) {
						tags.append(text).append("/");
					}
				}
             } else if (type == MXParser.END_TAG) {
                 String name = parser.getName().intern();
                 if (name == "genre") {
                     done = true;
                 }
             }
         }
         if (tags != null && tags.length() > 1) {
        	 if (entity.tag == null) {
         		entity.tag = tags.substring(0,tags.length() - 1);
 	         }
         }
    }
    
    /**
     * 解析主持人
     * @param parser
     * @param entity
     * @throws XmlPullParserException
     * @throws IOException
     */
    void parsePresenter(MXParser parser, Shiyun entity) throws XmlPullParserException, IOException {
    	boolean done = false; 
    	 StringBuilder presenters = new StringBuilder();
    	 String id = null;
         while (!done) {
             int type = parser.next();
             if (type == MXParser.START_TAG) {
                 int depth = parser.getDepth();
                 String name = parser.getName().intern();
                 if (depth == 5 && name == "list-item") {
                     String text = parser.nextText().trim();
                 	if (text.matches("\\d+")){
                 		id = text;
                 	} 
                 	if (!text.matches("\\d+") && id!=null) {
                 		presenters.append(text).append("-").append(id).append("/");
                     }
                 }
             } else if (type == MXParser.END_TAG) {
                 String name = parser.getName().intern();
                 if (name == "emcee") {
                     done = true;
                 }
             }
         }
         if (presenters != null) {
        	 if (presenters.length() > 1) {
         		entity.presenter = presenters.substring(0,presenters.length() - 1);
 	         }
         }
    }
    
    static class Item {
        public String id;
        public String title;
        public String model;

        @Override
        public String toString() {
            return "Item [id=" + id + ", title=" + title + ", model=" + model + "]";
        }
    }
}
