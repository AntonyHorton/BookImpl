package com.example.library.dao;

import com.example.library.model.Book;
import java.util.List;
import java.util.Optional;

public interface BookDao {
    Optional<Book> findById(int id);
    List<Book> findAll();
    Book save(Book book);
    boolean update(Book book);
    boolean delete(int id);
    List<Book> findByAuthorId(int authorId);
    List<Book> findByTitlePattern(String titlePattern);
    List<Book> findByYearRange(int startYear, int endYear);
    long count();
}
