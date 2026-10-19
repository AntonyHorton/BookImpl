package com.example.library.dao.jdbc;

import com.example.library.exception.DaoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


abstract class AbstractJdbcDao {
    private static final Logger log = LoggerFactory.getLogger(AbstractJdbcDao.class);

    protected final DataSource dataSource;

    protected AbstractJdbcDao(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    protected <T> List<T> queryList(String sql, StatementBinder binder, RowMapper<T> mapper) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw translate(sql, e);
        }
    }

    protected <T> Optional<T> queryOne(String sql, StatementBinder binder, RowMapper<T> mapper) {
        List<T> list = queryList(sql, binder, mapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** возвращает число затронутых строк. */
    protected int executeUpdate(String sql, StatementBinder binder) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw translate(sql, e);
        }
    }

    /** возвращает сгенерированный id  */
    protected int executeInsert(String sql, StatementBinder binder) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(ps);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new DaoException("Запись создана, но БД не вернула идентификатор", null);
            }
        } catch (SQLException e) {
            throw translate(sql, e);
        }
    }

    /** Имя таблицы - только внутренняя константа DAO, не ввод. */
    protected long countRows(String table) {
        return queryOne("SELECT COUNT(*) FROM " + table, StatementBinder.NONE, rs -> rs.getLong(1)).orElse(0L);
    }


    protected static String likePattern(String pattern) {
        String p = pattern == null ? "" : pattern.trim();
        return p.contains("%") ? p : "%" + p + "%";
    }

    private DaoException translate(String sql, SQLException e) {
        // Детали (SQL, SQLState, код) - в лог; наружу - короткое сообщение
        log.error("SQL error [state={}, code={}] in query: {}", e.getSQLState(), e.getErrorCode(), sql, e);
        return new DaoException("Ошибка доступа к данным (SQLState=" + e.getSQLState() + ")", e);
    }
}
