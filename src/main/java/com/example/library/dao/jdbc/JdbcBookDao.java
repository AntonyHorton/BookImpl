package com.example.library.dao.jdbc;

import com.example.library.dao.BookDao;
import com.example.library.model.Book;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

public class JdbcBookDao extends AbstractJdbcDao implements BookDao {
    private static final String SELECT = "SELECT id, title, author_id, publication_year FROM books";
    private static final RowMapper<Book> MAPPER = rs -> new Book(
            rs.getInt("id"), rs.getString("title"), rs.getInt("author_id"), rs.getInt("publication_year"));

    public JdbcBookDao(DataSource dataSource) { super(dataSource); }

    @Override
    public Optional<Book> findById(int id) {
        return queryOne(SELECT + " WHERE id = ?", ps -> ps.setInt(1, id), MAPPER);
    }

    @Override
    public List<Book> findAll() {
        return queryList(SELECT + " ORDER BY id", StatementBinder.NONE, MAPPER);
    }

    @Override
    public Book save(Book book) {
        int id = executeInsert(
                "INSERT INTO books (title, author_id, publication_year) VALUES (?, ?, ?)", ps -> {
                    ps.setString(1, book.getTitle());
                    ps.setInt(2, book.getAuthorId());
                    ps.setInt(3, book.getPublicationYear());
                });
        book.setId(id);
        return book;
    }

    @Override
    public boolean update(Book book) {
        return executeUpdate(
                "UPDATE books SET title = ?, author_id = ?, publication_year = ? WHERE id = ?", ps -> {
                    ps.setString(1, book.getTitle());
                    ps.setInt(2, book.getAuthorId());
                    ps.setInt(3, book.getPublicationYear());
                    ps.setInt(4, book.getId());
                }) > 0;
    }

    @Override
    public boolean delete(int id) {
        return executeUpdate("DELETE FROM books WHERE id = ?", ps -> ps.setInt(1, id)) > 0;
    }

    @Override
    public List<Book> findByAuthorId(int authorId) {
        return queryList(SELECT + " WHERE author_id = ? ORDER BY id", ps -> ps.setInt(1, authorId), MAPPER);
    }

    @Override
    public List<Book> findByTitlePattern(String titlePattern) {
        return queryList(SELECT + " WHERE LOWER(title) LIKE LOWER(?) ORDER BY title",
                ps -> ps.setString(1, likePattern(titlePattern)), MAPPER);
    }

    @Override
    public List<Book> findByYearRange(int startYear, int endYear) {
        return queryList(SELECT + " WHERE publication_year BETWEEN ? AND ? ORDER BY publication_year, id", ps -> {
            ps.setInt(1, startYear);
            ps.setInt(2, endYear);
        }, MAPPER);
    }

    @Override
    public long count() { return countRows("books"); }
}
