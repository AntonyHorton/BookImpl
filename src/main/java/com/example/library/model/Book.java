package com.example.library.model;

public class Book {
    private int id;
    private String title;
    private int authorId;
    private int publicationYear;

    public Book(String title, int authorId, int publicationYear) { this(0, title, authorId, publicationYear); }
    public Book(int id, String title, int authorId, int publicationYear) {
        this.id = id; this.title = title; this.authorId = authorId; this.publicationYear = publicationYear;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }
    public int getPublicationYear() { return publicationYear; }
    public void setPublicationYear(int publicationYear) { this.publicationYear = publicationYear; }

    @Override public String toString() {
        return "Book{id=" + id + ", title='" + title + "', authorId=" + authorId + ", year=" + publicationYear + "}";
    }
}
