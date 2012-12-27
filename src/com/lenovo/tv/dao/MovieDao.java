package com.lenovo.tv.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.lenovo.tv.schedule.Shiyun;

@Repository
public class MovieDao {

    static final String sTableName = "RAW_SHIYUN";

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRED)
    public void insertShiyun(Shiyun data) {
    	String sql = "INSERT IGNORE INTO " + sTableName + " (PK, PARENT_PK, MAIN_URL, PLAY_URL, SOURCE, NAME, TITLE, FOCUS, TYPE, TAG, " +
    			"ACTOR, DIRECTOR, PRESENTER, GUEST, SESSION, EPISODE, EPISODE_TOTAL, DURATION, GRADE, AREA, AIR_DATE, IS_COMPLEX, SOURCE_QUALITY," +
    			"DESCRIPTION, ADLET_URL, POSTER_URL, THUMB_URL, CREATE_TIME) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())";
    	jdbcTemplate.update(sql, new Object[] {data.pk, data.parentPk, data.mainUrl, data.playUrl, "Shiyun", data.name, data.title,  data.focus,
    			data.type, data.tag, data.actor, data.director, data.presenter, data.guest, data.session, data.episode, data.episodeTotal, 
    			data.dutation, data.grade, data.area, data.airDate, data.isComplex, data.sourceQuality, data.description, data.adletUrl, data.posterUrl, data.thumbUrl});
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void updateSubItems(String pk) {
    	String sql = "UPDATE " + sTableName + " SET IS_COMPLEX = 1 WHERE PARENT_PK = ?";
    	jdbcTemplate.update(sql, new Object[] {pk});
    }
}
