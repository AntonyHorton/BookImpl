package com.example.library.dao;

import com.example.library.model.BookLoan;
import java.util.List;
import java.util.Optional;

public interface LoanDao {
    Optional<BookLoan> findById(int id);
    List<BookLoan> findAll();
    BookLoan save(BookLoan loan);
    boolean update(BookLoan loan);
    boolean delete(int id);
    List<BookLoan> findByBookId(int bookId);
    List<BookLoan> findByReaderId(int readerId);
    List<BookLoan> findActiveLoans();
    Optional<BookLoan> findActiveLoanByBookId(int bookId);
}
