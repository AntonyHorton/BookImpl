package com.example.library.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
interface StatementBinder {
    StatementBinder NONE = ps -> { };
    void bind(PreparedStatement ps) throws SQLException;
}
