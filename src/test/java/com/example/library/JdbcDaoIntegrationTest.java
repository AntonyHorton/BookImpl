package com.example.library;

import com.example.library.config.DataSourceConfig;
import com.example.library.config.TransactionalDataSource;
import com.example.library.dao.jdbc.JdbcAuthorDao;
import com.example.library.dao.jdbc.JdbcBookDao;
import com.example.library.dao.jdbc.JdbcLoanDao;
import com.example.library.dao.jdbc.JdbcReaderDao;
import com.example.library.exception.DaoException;
import com.example.library.model.Author;
import com.example.library.model.Book;
import com.example.library.model.BookLoan;
import com.example.library.model.Reader;
import com.example.library.service.LibraryService;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

/** Интеграционные тесты на H2 in-memory: DAO, пул, транзакции. */
class JdbcDaoIntegrationTest {
    private HikariDataSource pool;
    private TransactionalDataSource ds;
    private JdbcBookDao bookDao;
    private JdbcAuthorDao authorDao;
    private JdbcReaderDao readerDao;
    private JdbcLoanDao loanDao;

    @BeforeEach
    void setUp() throws Exception {
        String url = "jdbc:h2:mem:lib" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
        pool = (HikariDataSource) DataSourceConfig.createDataSource(url, "sa", "", 5);
        try (Connection c = pool.getConnection(); Statement st = c.createStatement()) {
            String script = new String(getClass().getResourceAsStream("/schema.sql").readAllBytes(), StandardCharsets.UTF_8);
            for (String sql : script.split(";")) {
                String cleaned = sql.lines().filter(l -> !l.trim().startsWith("--")).reduce("", (a, b) -> a + b + "\n").trim();
                if (!cleaned.isEmpty()) st.execute(cleaned);
            }
        }
        ds = new TransactionalDataSource(pool);
        bookDao = new JdbcBookDao(ds);
        authorDao = new JdbcAuthorDao(ds);
        readerDao = new JdbcReaderDao(ds);
        loanDao = new JdbcLoanDao(ds);
    }

    @AfterEach
    void tearDown() { pool.close(); }

    @Test
    void bookCrudAndSearches() {
        Author a = authorDao.save(new Author("Isaac Asimov"));
        Book b = bookDao.save(new Book("Foundation", a.getId(), 1951));
        assertTrue(b.getId() > 0);

        assertEquals("Foundation", bookDao.findById(b.getId()).orElseThrow().getTitle());
        assertEquals(1, bookDao.findByTitlePattern("found").size());
        assertEquals(1, bookDao.findByYearRange(1950, 1955).size());
        assertTrue(bookDao.findByYearRange(1960, 1970).isEmpty());
        assertEquals(1, bookDao.findByAuthorId(a.getId()).size());

        b.setTitle("Foundation and Empire");
        assertTrue(bookDao.update(b));
        assertTrue(bookDao.delete(b.getId()));
        assertTrue(bookDao.findById(b.getId()).isEmpty());
    }

    @Test
    void edgeCases_nonExistentIds() {
        assertTrue(bookDao.findById(999).isEmpty());
        assertFalse(bookDao.delete(999));
        assertFalse(bookDao.update(new Book(999, "x", 1, 2000)));
        assertTrue(loanDao.findActiveLoanByBookId(999).isEmpty());
        assertTrue(readerDao.findByEmail("nobody@").isEmpty());
    }

    @Test
    void fkViolation_isTranslatedToDaoException() {
        assertThrows(DaoException.class, () -> bookDao.save(new Book("Orphan", 12345, 2000)));
    }

    @Test
    void loanLifecycle_activeAndReturned() {
        Author a = authorDao.save(new Author("A"));
        Book b = bookDao.save(new Book("T", a.getId(), 2000));
        Reader r = readerDao.save(new Reader("R", "r@example.com"));

        BookLoan loan = loanDao.save(new BookLoan(b.getId(), r.getId(), LocalDate.now()));
        assertTrue(loanDao.findActiveLoanByBookId(b.getId()).isPresent());
        assertEquals(1, loanDao.findActiveLoans().size());

        loan.setReturnDate(LocalDate.now());
        assertTrue(loanDao.update(loan));
        assertTrue(loanDao.findActiveLoans().isEmpty());
        assertEquals(1, loanDao.findByReaderId(r.getId()).size());
    }

    @Test
    void transaction_rollsBackOnFailure() {
        assertThrows(IllegalStateException.class, () -> ds.execute(() -> {
            authorDao.save(new Author("Temp"));
            throw new IllegalStateException("boom");
        }));
        assertEquals(0, authorDao.count());
    }

    @Test
    void transaction_commitsOnSuccess_andReturnsConnectionToPool() {
        ds.execute(() -> authorDao.save(new Author("Kept")));
        assertEquals(1, authorDao.count());
        assertEquals(0, pool.getHikariPoolMXBean().getActiveConnections());
    }

    @Test
    void service_borrowAndReturnOnRealDb() {
        LibraryService service = new LibraryService(bookDao, authorDao, readerDao, loanDao, ds);
        Author a = authorDao.save(new Author("A"));
        Book b = bookDao.save(new Book("T", a.getId(), 2000));
        Reader r = readerDao.save(new Reader("R", "r@example.com"));

        assertTrue(service.borrowBookWithValidation(b.getId(), r.getId()));
        assertFalse(service.borrowBookWithValidation(b.getId(), r.getId()));
        assertEquals(1, service.getLibraryStatistics().getActiveLoans());

        int loanId = loanDao.findActiveLoanByBookId(b.getId()).orElseThrow().getId();
        assertTrue(service.returnBookWithValidation(loanId));
        assertEquals(1, service.getLibraryStatistics().getAvailableBooks());
    }

    @Test
    void pool_handlesConcurrentLoad_withoutLeaks() throws Exception {
        authorDao.save(new Author("A"));
        ExecutorService executor = Executors.newFixedThreadPool(32);   // потоков больше, чем соединений (5)
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            futures.add(executor.submit(() -> authorDao.findAll().size()));
        }
        for (Future<Integer> f : futures) {
            assertEquals(1, f.get());
        }
        executor.shutdown();
        assertEquals(0, pool.getHikariPoolMXBean().getActiveConnections(), "утечка соединений");
        assertEquals(0, pool.getHikariPoolMXBean().getThreadsAwaitingConnection());
    }
}
