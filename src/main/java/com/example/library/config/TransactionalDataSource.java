package com.example.library.config;

import com.example.library.exception.DaoException;
import com.example.library.service.TransactionRunner;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.function.Supplier;
import java.util.logging.Logger;


public class TransactionalDataSource implements DataSource, TransactionRunner {
    private final DataSource delegate;
    private final ThreadLocal<Connection> bound = new ThreadLocal<>();

    public TransactionalDataSource(DataSource delegate) { this.delegate = delegate; }

    @Override
    public <T> T execute(Supplier<T> work) {
        if (bound.get() != null) {      // уже внутри транзакции - присоединяемся
            return work.get();
        }
        Connection real = null;
        try {
            real = delegate.getConnection();
            real.setAutoCommit(false);
            bound.set(real);
            T result = work.get();
            real.commit();
            return result;
        } catch (SQLException e) {
            rollbackQuietly(real);
            throw new DaoException("Ошибка управления транзакцией", e);
        } catch (RuntimeException e) {
            rollbackQuietly(real);
            throw e;
        } finally {
            bound.remove();
            if (real != null) {
                try {
                    real.setAutoCommit(true);   // вернуть пулу соединение в исходном состоянии
                    real.close();
                } catch (SQLException ignored) {
                    // соединение уже сломано; пул его отбросит
                }
            }
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection current = bound.get();
        if (current == null) {
            return delegate.getConnection();
        }
        // Прокси: close() не закрывает транзакционное соединение
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    try {
                        return method.invoke(current, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    private static void rollbackQuietly(Connection con) {
        if (con != null) {
            try { con.rollback(); } catch (SQLException ignored) { }
        }
    }

    @Override public Connection getConnection(String u, String p) throws SQLException { return delegate.getConnection(u, p); }
    @Override public PrintWriter getLogWriter() throws SQLException { return delegate.getLogWriter(); }
    @Override public void setLogWriter(PrintWriter out) throws SQLException { delegate.setLogWriter(out); }
    @Override public void setLoginTimeout(int seconds) throws SQLException { delegate.setLoginTimeout(seconds); }
    @Override public int getLoginTimeout() throws SQLException { return delegate.getLoginTimeout(); }
    @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { return delegate.getParentLogger(); }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException { return delegate.unwrap(iface); }
    @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return delegate.isWrapperFor(iface); }
}
