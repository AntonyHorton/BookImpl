package com.example.library.dao.jdbc;

import com.example.library.dao.LoanDao;
import com.example.library.model.BookLoan;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class JdbcLoanDao extends AbstractJdbcDao implements LoanDao {
    private static final String SELECT = "SELECT id, book_id, reader_id, loan_date, return_date FROM loans";
    private static final RowMapper<BookLoan> MAPPER = rs -> new BookLoan(
            rs.getInt("id"), rs.getInt("book_id"), rs.getInt("reader_id"),
            rs.getObject("loan_date", LocalDate.class),
            rs.getObject("return_date", LocalDate.class));

    public JdbcLoanDao(DataSource dataSource) { super(dataSource); }

    @Override
    public Optional<BookLoan> findById(int id) {
        return queryOne(SELECT + " WHERE id = ?", ps -> ps.setInt(1, id), MAPPER);
    }

    @Override
    public List<BookLoan> findAll() {
        return queryList(SELECT + " ORDER BY id", StatementBinder.NONE, MAPPER);
    }

    @Override
    public BookLoan save(BookLoan loan) {
        int id = executeInsert(
                "INSERT INTO loans (book_id, reader_id, loan_date, return_date) VALUES (?, ?, ?, ?)", ps -> {
                    ps.setInt(1, loan.getBookId());
                    ps.setInt(2, loan.getReaderId());
                    ps.setObject(3, loan.getLoanDate());
                    setNullableDate(ps, 4, loan.getReturnDate());
                });
        loan.setId(id);
        return loan;
    }

    @Override
    public boolean update(BookLoan loan) {
        return executeUpdate("UPDATE loans SET return_date = ? WHERE id = ?", ps -> {
            setNullableDate(ps, 1, loan.getReturnDate());
            ps.setInt(2, loan.getId());
        }) > 0;
    }

    @Override
    public boolean delete(int id) {
        return executeUpdate("DELETE FROM loans WHERE id = ?", ps -> ps.setInt(1, id)) > 0;
    }

    @Override
    public List<BookLoan> findByBookId(int bookId) {
        return queryList(SELECT + " WHERE book_id = ? ORDER BY loan_date DESC, id DESC",
                ps -> ps.setInt(1, bookId), MAPPER);
    }

    @Override
    public List<BookLoan> findByReaderId(int readerId) {
        return queryList(SELECT + " WHERE reader_id = ? ORDER BY loan_date DESC, id DESC",
                ps -> ps.setInt(1, readerId), MAPPER);
    }

    @Override
    public List<BookLoan> findActiveLoans() {
        return queryList(SELECT + " WHERE return_date IS NULL ORDER BY id", StatementBinder.NONE, MAPPER);
    }

    @Override
    public Optional<BookLoan> findActiveLoanByBookId(int bookId) {
        return queryOne(SELECT + " WHERE book_id = ? AND return_date IS NULL",
                ps -> ps.setInt(1, bookId), MAPPER);
    }

    private static void setNullableDate(PreparedStatement ps, int index, LocalDate date) throws SQLException {
        if (date == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setObject(index, date);
        }
    }
}
