package com.example.library.dto;

public class BookSearchCriteria {
    private String title;
    private String author;
    private Integer startYear;
    private Integer endYear;
    private Boolean availableOnly;

    public BookSearchCriteria() { }

    public BookSearchCriteria(String title, String author, Integer startYear, Integer endYear, Boolean availableOnly) {
        this.title = title;
        this.author = author;
        this.startYear = startYear;
        this.endYear = endYear;
        this.availableOnly = availableOnly;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public Integer getStartYear() { return startYear; }
    public void setStartYear(Integer startYear) { this.startYear = startYear; }
    public Integer getEndYear() { return endYear; }
    public void setEndYear(Integer endYear) { this.endYear = endYear; }
    public Boolean getAvailableOnly() { return availableOnly; }
    public void setAvailableOnly(Boolean availableOnly) { this.availableOnly = availableOnly; }
}
