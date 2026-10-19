package com.example.library;

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
import com.example.library.model.Reader;
import com.example.library.service.LibraryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {
    @Mock BookDao bookDao;
    @Mock AuthorDao authorDao;
    @Mock ReaderDao readerDao;
    @Mock LoanDao loanDao;
    LibraryService service;

    @BeforeEach
    void setUp() { service = new LibraryService(bookDao, authorDao, readerDao, loanDao); }

    private void bookAndReaderExist() {
        when(bookDao.findById(1)).thenReturn(Optional.of(new Book(1, "B", 1, 2000)));
        when(readerDao.findById(2)).thenReturn(Optional.of(new Reader(2, "R", "r@x.com")));
    }

    @Test
    void borrow_success_savesLoan() {
        bookAndReaderExist();
        when(loanDao.findActiveLoanByBookId(1)).thenReturn(Optional.empty());
        when(loanDao.findByReaderId(2)).thenReturn(List.of());

        assertTrue(service.borrowBookWithValidation(1, 2));
        verify(loanDao).save(any(BookLoan.class));
    }

    @Test
    void borrow_alreadyLoaned_returnsFalse() {
        bookAndReaderExist();
        when(loanDao.findActiveLoanByBookId(1))
                .thenReturn(Optional.of(new BookLoan(9, 1, 3, LocalDate.now(), null)));

        assertFalse(service.borrowBookWithValidation(1, 2));
        verify(loanDao, never()).save(any());
    }

    @Test
    void borrow_readerLimitReached_returnsFalse() {
        bookAndReaderExist();
        when(loanDao.findActiveLoanByBookId(1)).thenReturn(Optional.empty());
        when(loanDao.findByReaderId(2)).thenReturn(List.of(
                active(1), active(2), active(3), active(4), active(5)));

        assertFalse(service.borrowBookWithValidation(1, 2));
        verify(loanDao, never()).save(any());
    }

    @Test
    void borrow_unknownBook_throws() {
        when(bookDao.findById(1)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.borrowBookWithValidation(1, 2));
        verifyNoInteractions(loanDao);
    }

    @Test
    void return_success_setsReturnDate() {
        BookLoan loan = new BookLoan(5, 1, 2, LocalDate.now().minusDays(3), null);
        when(loanDao.findById(5)).thenReturn(Optional.of(loan));
        when(loanDao.update(loan)).thenReturn(true);

        assertTrue(service.returnBookWithValidation(5));
        assertEquals(LocalDate.now(), loan.getReturnDate());
    }

    @Test
    void return_alreadyReturned_returnsFalse() {
        BookLoan loan = new BookLoan(5, 1, 2, LocalDate.now().minusDays(3), LocalDate.now().minusDays(1));
        when(loanDao.findById(5)).thenReturn(Optional.of(loan));

        assertFalse(service.returnBookWithValidation(5));
        verify(loanDao, never()).update(any());
    }

    @Test
    void return_unknownLoan_throws() {
        when(loanDao.findById(5)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.returnBookWithValidation(5));
    }

    @Test
    void search_byAuthorAndYear_combinesCriteria() {
        when(authorDao.findByName("orwell")).thenReturn(List.of(new Author(7, "George Orwell")));
        when(bookDao.findByYearRange(1940, 1950)).thenReturn(List.of(
                new Book(1, "1984", 7, 1949), new Book(2, "Other", 8, 1945)));

        List<Book> result = service.searchBooks(new BookSearchCriteria(null, "orwell", 1940, 1950, null));

        assertEquals(1, result.size());
        assertEquals("1984", result.get(0).getTitle());
    }

    @Test
    void search_availableOnly_excludesLoanedBooks() {
        when(bookDao.findAll()).thenReturn(List.of(new Book(1, "A", 1, 2000), new Book(2, "B", 1, 2001)));
        when(loanDao.findActiveLoans()).thenReturn(List.of(active(1)));

        List<Book> result = service.searchBooks(new BookSearchCriteria(null, null, null, null, true));

        assertEquals(List.of(2), result.stream().map(Book::getId).toList());
    }

    @Test
    void search_unknownAuthor_returnsEmptyWithoutScanningBooks() {
        when(authorDao.findByName("nobody")).thenReturn(List.of());
        assertTrue(service.searchBooks(null, "nobody", null).isEmpty());
        verifyNoInteractions(bookDao);
    }

    @Test
    void statistics_aggregatesCounts() {
        when(bookDao.count()).thenReturn(10L);
        when(authorDao.count()).thenReturn(4L);
        when(readerDao.count()).thenReturn(6L);
        when(loanDao.findActiveLoans()).thenReturn(List.of(active(1), active(2)));

        LibraryStatistics s = service.getLibraryStatistics();

        assertEquals(10, s.getTotalBooks());
        assertEquals(2, s.getActiveLoans());
        assertEquals(8, s.getAvailableBooks());
    }

    private static BookLoan active(int bookId) { return new BookLoan(bookId, 2, LocalDate.now()); }
}
