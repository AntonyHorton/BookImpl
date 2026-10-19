package com.example.library.dao;

import com.example.library.model.Reader;
import java.util.List;
import java.util.Optional;

public interface ReaderDao {
    Optional<Reader> findById(int id);
    List<Reader> findAll();
    Reader save(Reader reader);
    boolean update(Reader reader);
    boolean delete(int id);
    List<Reader> findByEmail(String emailPattern);
    long count();
}
