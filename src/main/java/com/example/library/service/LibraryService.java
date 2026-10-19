package com.example.library.service;

import com.example.library.dao.AuthorDao;
import com.example.library.dao.BookDao;
import com.example.library.dao.LoanDao;
import com.example.library.dao.ReaderDao;
import com.example.library.dto.BookSearchCriteria;
import com.example.library.dto.LibraryStatistics;
import com.example.library.exception.EntityNotFoundException;
import com.example.library.model.Author;
import com.example.library.model.Book;
import com.example.library.model.BookLoan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Бизнес-логика библиотеки. Зависит только от интерфейсов DAO и TransactionRunner
 */
public class LibraryService {
    private static final Logger log = LoggerFactory.getLogger(LibraryService.class);
    /** Максимум книг на руках у одного читателя. */
    static final int MAX_ACTIVE_LOANS_PER_READER = 5;

    private final BookDao bookDao;
    private final AuthorDao authorDao;
    private final ReaderDao readerDao;
    private final LoanDao loanDao;
    private final TransactionRunner tx;

    public LibraryService(BookDao bookDao, AuthorDao authorDao, ReaderDao readerDao, LoanDao loanDao) {
        this(bookDao, authorDao, readerDao, loanDao, TransactionRunner.NONE);
    }

    public LibraryService(BookDao bookDao, AuthorDao authorDao, ReaderDao readerDao,
                          LoanDao loanDao, TransactionRunner tx) {
        this.bookDao = Objects.requireNonNull(bookDao);
        this.authorDao = Objects.requireNonNull(authorDao);
        this.readerDao = Objects.requireNonNull(readerDao);
        this.loanDao = Objects.requireNonNull(loanDao);
        this.tx = Objects.requireNonNull(tx);
    }

    /**
     * Выдача книги. Проверка и запись выполняются в одной транзакции.
     */
    public boolean borrowBookWithValidation(int bookId, int readerId) {
        return tx.execute(() -> {
            bookDao.findById(bookId).orElseThrow(() -> new EntityNotFoundException("Книга", bookId));
            readerDao.findById(readerId).orElseThrow(() -> new EntityNotFoundException("Читатель", readerId));

            if (loanDao.findActiveLoanByBookId(bookId).isPresent()) {
                log.info("Отказ в выдаче: книга {} уже на руках", bookId);
                return false;
            }
            long readerActive = loanDao.findByReaderId(readerId).stream().filter(BookLoan::isActive).count();
            if (readerActive >= MAX_ACTIVE_LOANS_PER_READER) {
                log.info("Отказ в выдаче: у читателя {} уже {} книг", readerId, readerActive);
                return false;
            }
            loanDao.save(new BookLoan(bookId, readerId, LocalDate.now()));
            log.info("Книга {} выдана читателю {}", bookId, readerId);
            return true;
        });
    }

    /**
     * Возврат книги.
     */
    public boolean returnBookWithValidation(int loanId) {
        return tx.execute(() -> {
            BookLoan loan = loanDao.findById(loanId).orElseThrow(() -> new EntityNotFoundException("Выдача", loanId));
            if (!loan.isActive()) {
                log.info("Выдача {} уже закрыта {}", loanId, loan.getReturnDate());
                return false;
            }
            loan.setReturnDate(LocalDate.now());
            return loanDao.update(loan);
        });
    }

    public List<Book> searchBooks(String title, String author, Integer year) {
        return searchBooks(new BookSearchCriteria(title, author, year, year, null));
    }

    public List<Book> searchBooks(BookSearchCriteria c) {
        boolean hasTitle = hasText(c.getTitle());
        boolean hasAuthor = hasText(c.getAuthor());
        boolean hasYears = c.getStartYear() != null || c.getEndYear() != null;
        int from = c.getStartYear() == null ? Integer.MIN_VALUE : c.getStartYear();
        int to = c.getEndYear() == null ? Integer.MAX_VALUE : c.getEndYear();

        Set<Integer> authorIds = null;
        if (hasAuthor) {
            authorIds = authorDao.findByName(c.getAuthor()).stream().map(Author::getId).collect(Collectors.toSet());
            if (authorIds.isEmpty()) {
                return List.of();
            }
        }


        List<Book> candidates;
        if (hasTitle) {
            candidates = bookDao.findByTitlePattern(c.getTitle());
        } else if (hasYears) {
            candidates = bookDao.findByYearRange(Math.max(from, 0), Math.min(to, 9999));
        } else if (authorIds != null && authorIds.size() == 1) {
            candidates = bookDao.findByAuthorId(authorIds.iterator().next());
        } else {
            candidates = bookDao.findAll();
        }

        Set<Integer> loanedIds = Boolean.TRUE.equals(c.getAvailableOnly())
                ? loanDao.findActiveLoans().stream().map(BookLoan::getBookId).collect(Collectors.toSet())
                : Set.of();
        final Set<Integer> allowedAuthors = authorIds;

        return candidates.stream()
                .filter(b -> b.getPublicationYear() >= from && b.getPublicationYear() <= to)
                .filter(b -> allowedAuthors == null || allowedAuthors.contains(b.getAuthorId()))
                .filter(b -> !loanedIds.contains(b.getId()))
                .collect(Collectors.toList());
    }

    public LibraryStatistics getLibraryStatistics() {
        long totalBooks = bookDao.count();
        long activeLoans = loanDao.findActiveLoans().size();
        // На руках не больше одного экземпляра каждой книги, поэтому доступно = всего - на руках
        return new LibraryStatistics(totalBooks, authorDao.count(), readerDao.count(),
                activeLoans, Math.max(0, totalBooks - activeLoans));
    }

    private static boolean hasText(String s) { return s != null && !s.isBlank(); }
}
