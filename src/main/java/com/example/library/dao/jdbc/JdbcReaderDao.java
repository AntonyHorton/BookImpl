package com.example.library.dao.jdbc;

import com.example.library.dao.ReaderDao;
import com.example.library.model.Reader;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

public class JdbcReaderDao extends AbstractJdbcDao implements ReaderDao {
    private static final String COLUMNS = "id, name, email";
    private static final RowMapper<Reader> MAPPER =
            rs -> new Reader(rs.getInt("id"), rs.getString("name"), rs.getString("email"));

    public JdbcReaderDao(DataSource dataSource) { super(dataSource); }

    @Override
    public Optional<Reader> findById(int id) {
        return queryOne("SELECT " + COLUMNS + " FROM readers WHERE id = ?", ps -> ps.setInt(1, id), MAPPER);
    }

    @Override
    public List<Reader> findAll() {
        return queryList("SELECT " + COLUMNS + " FROM readers ORDER BY id", StatementBinder.NONE, MAPPER);
    }

    @Override
    public Reader save(Reader reader) {
        int id = executeInsert("INSERT INTO readers (name, email) VALUES (?, ?)", ps -> {
            ps.setString(1, reader.getName());
            ps.setString(2, reader.getEmail());
        });
        reader.setId(id);
        return reader;
    }

    @Override
    public boolean update(Reader reader) {
        return executeUpdate("UPDATE readers SET name = ?, email = ? WHERE id = ?", ps -> {
            ps.setString(1, reader.getName());
            ps.setString(2, reader.getEmail());
            ps.setInt(3, reader.getId());
        }) > 0;
    }

    @Override
    public boolean delete(int id) {
        return executeUpdate("DELETE FROM readers WHERE id = ?", ps -> ps.setInt(1, id)) > 0;
    }

    @Override
    public List<Reader> findByEmail(String emailPattern) {
        return queryList("SELECT " + COLUMNS + " FROM readers WHERE LOWER(email) LIKE LOWER(?) ORDER BY id",
                ps -> ps.setString(1, likePattern(emailPattern)), MAPPER);
    }

    @Override
    public long count() { return countRows("readers"); }
}
