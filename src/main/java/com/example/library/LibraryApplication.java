package com.example.library;

import com.example.library.config.DataSourceConfig;
import com.example.library.config.TransactionalDataSource;
import com.example.library.dao.AuthorDao;
import com.example.library.dao.BookDao;
import com.example.library.dao.LoanDao;
import com.example.library.dao.ReaderDao;
import com.example.library.dao.jdbc.JdbcAuthorDao;
import com.example.library.dao.jdbc.JdbcBookDao;
import com.example.library.dao.jdbc.JdbcLoanDao;
import com.example.library.dao.jdbc.JdbcReaderDao;
import com.example.library.dto.BookSearchCriteria;
import com.example.library.exception.DaoException;
import com.example.library.exception.LibraryException;
import com.example.library.model.Author;
import com.example.library.model.Book;
import com.example.library.model.Reader;
import com.example.library.service.LibraryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class LibraryApplication implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(LibraryApplication.class);

    private final DataSource pool;
    private final BookDao bookDao;
    private final AuthorDao authorDao;
    private final ReaderDao readerDao;
    private final LibraryService libraryService;

    public LibraryApplication() {
        this.pool = DataSourceConfig.createDataSource();
        // Один DataSource на все DAO; обёртка добавляет транзакции для сервиса
        TransactionalDataSource dataSource = new TransactionalDataSource(pool);

        this.bookDao = new JdbcBookDao(dataSource);
        this.authorDao = new JdbcAuthorDao(dataSource);
        this.readerDao = new JdbcReaderDao(dataSource);
        LoanDao loanDao = new JdbcLoanDao(dataSource);

        this.libraryService = new LibraryService(bookDao, authorDao, readerDao, loanDao, dataSource);
    }

    public void run() {
        initSchema();

        // Демо-данные (в реальном приложении этим занимались бы отдельные сервисные методы)
        Author tolkien = authorDao.save(new Author("J.R.R. Tolkien"));
        Author orwell = authorDao.save(new Author("George Orwell"));
        Book hobbit = bookDao.save(new Book("The Hobbit", tolkien.getId(), 1937));
        bookDao.save(new Book("1984", orwell.getId(), 1949));
        bookDao.save(new Book("Animal Farm", orwell.getId(), 1945));
        Reader reader = readerDao.save(new Reader("Ivan Petrov", "ivan@example.com"));

        System.out.println("Выдача:           " + libraryService.borrowBookWithValidation(hobbit.getId(), reader.getId()));
        System.out.println("Повторная выдача: " + libraryService.borrowBookWithValidation(hobbit.getId(), reader.getId()));
        System.out.println("Статистика:       " + libraryService.getLibraryStatistics());
        System.out.println("Поиск 'orwell':   " + libraryService.searchBooks(null, "orwell", null));

        BookSearchCriteria available = new BookSearchCriteria(null, null, 1900, 1950, true);
        System.out.println("Доступные до 1950: " + libraryService.searchBooks(available));
    }

    private void initSchema() {
        try (InputStream in = getClass().getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("schema.sql не найден в classpath");
            }
            String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection con = pool.getConnection(); Statement st = con.createStatement()) {
                for (String sql : script.split(";")) {
                    String cleaned = sql.lines().filter(l -> !l.trim().startsWith("--"))
                            .reduce("", (a, b) -> a + b + "\n").trim();
                    if (!cleaned.isEmpty()) {
                        st.execute(cleaned);
                    }
                }
            }
        } catch (IOException | SQLException e) {
            throw new DaoException("Не удалось инициализировать схему БД", e);
        }
    }

    @Override
    public void close() {
        if (pool instanceof AutoCloseable) {
            try { ((AutoCloseable) pool).close(); } catch (Exception e) { log.warn("Ошибка закрытия пула", e); }
        }
    }

    public static void main(String[] args) {
        try (LibraryApplication app = new LibraryApplication()) {
            app.run();
        } catch (LibraryException e) {
            System.err.println("Ошибка: " + e.getMessage());     // бизнес-сообщение безопасно показывать
        } catch (RuntimeException e) {
            log.error("Необработанная ошибка", e);               // детали - только в лог
            System.err.println("Произошла внутренняя ошибка. Подробности в журнале.");
        }
    }
}
