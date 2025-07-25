package com.ttg.certificate_upload.beans;

import java.util.List;

/**
 * Title: CertCaptureTaxCodeArrayBean
 * 
 * Description: This class holds data for a cert capture certificate search results.
 * 
 * Copyright: Copyright (c) 2021
 * 
 * Company: Tax Technology Group
 *
 * @author Cara Brauner
 * 
 * $Log: CertCaptureTaxCodeArrayBean.java,v $
 * Revision 1.1.1.1  2021/10/20 22:24:42  carab
 * Imported sources
 *
 *
 *
 */
public class CertCaptureTaxCodeArrayBean {
    private Integer current_page;
    private List<CertCaptureTaxCodeBean> data;
    private String first_page_url;
    private Integer from;
    private Integer last_page;
    private String last_page_url;
    private String path;
    private Integer per_page;
    private String prev_page_url;
    private Integer to;
    private Integer total;

    public Integer getCurrent_page() {
        return current_page;
    }

    public void setCurrent_page(Integer current_page) {
        this.current_page = current_page;
    }

    public List<CertCaptureTaxCodeBean> getData() {
        return data;
    }

    public void setData(List<CertCaptureTaxCodeBean> data) {
        this.data = data;
    }

    public String getFirst_page_url() {
        return first_page_url;
    }

    public void setFirst_page_url(String first_page_url) {
        this.first_page_url = first_page_url;
    }

    public Integer getFrom() {
        return from;
    }

    public void setFrom(Integer from) {
        this.from = from;
    }

    public Integer getLast_page() {
        return last_page;
    }

    public void setLast_page(Integer last_page) {
        this.last_page = last_page;
    }

    public String getLast_page_url() {
        return last_page_url;
    }

    public void setLast_page_url(String last_page_url) {
        this.last_page_url = last_page_url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getPer_page() {
        return per_page;
    }

    public void setPer_page(Integer per_page) {
        this.per_page = per_page;
    }

    public String getPrev_page_url() {
        return prev_page_url;
    }

    public void setPrev_page_url(String prev_page_url) {
        this.prev_page_url = prev_page_url;
    }

    public Integer getTo() {
        return to;
    }

    public void setTo(Integer to) {
        this.to = to;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }
    
    
}
