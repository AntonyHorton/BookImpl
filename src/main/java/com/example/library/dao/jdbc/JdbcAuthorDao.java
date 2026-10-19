package com.example.library.dao.jdbc;

import com.example.library.dao.AuthorDao;
import com.example.library.model.Author;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

public class JdbcAuthorDao extends AbstractJdbcDao implements AuthorDao {
    private static final String COLUMNS = "id, name";
    private static final RowMapper<Author> MAPPER = rs -> new Author(rs.getInt("id"), rs.getString("name"));

    public JdbcAuthorDao(DataSource dataSource) { super(dataSource); }

    @Override
    public Optional<Author> findById(int id) {
        return queryOne("SELECT " + COLUMNS + " FROM authors WHERE id = ?", ps -> ps.setInt(1, id), MAPPER);
    }

    @Override
    public List<Author> findAll() {
        return queryList("SELECT " + COLUMNS + " FROM authors ORDER BY id", StatementBinder.NONE, MAPPER);
    }

    @Override
    public Author save(Author author) {
        int id = executeInsert("INSERT INTO authors (name) VALUES (?)", ps -> ps.setString(1, author.getName()));
        author.setId(id);
        return author;
    }

    @Override
    public boolean update(Author author) {
        return executeUpdate("UPDATE authors SET name = ? WHERE id = ?", ps -> {
            ps.setString(1, author.getName());
            ps.setInt(2, author.getId());
        }) > 0;
    }

    @Override
    public boolean delete(int id) {
        return executeUpdate("DELETE FROM authors WHERE id = ?", ps -> ps.setInt(1, id)) > 0;
    }

    @Override
    public List<Author> findByName(String namePattern) {
        return queryList("SELECT " + COLUMNS + " FROM authors WHERE LOWER(name) LIKE LOWER(?) ORDER BY name",
                ps -> ps.setString(1, likePattern(namePattern)), MAPPER);
    }

    @Override
    public long count() { return countRows("authors"); }
}
