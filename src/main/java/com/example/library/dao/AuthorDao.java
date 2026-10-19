package com.example.library.dao;

import com.example.library.model.Author;
import java.util.List;
import java.util.Optional;

public interface AuthorDao {
    Optional<Author> findById(int id);
    List<Author> findAll();
    Author save(Author author);
    boolean update(Author author);
    boolean delete(int id);
    List<Author> findByName(String namePattern);
    long count();
}
