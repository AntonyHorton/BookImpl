package com.example.library.model;

import java.time.LocalDate;

public class BookLoan {
    private int id;
    private int bookId;
    private int readerId;
    private LocalDate loanDate;
    private LocalDate returnDate; // null = книга ещё на руках

    public BookLoan(int bookId, int readerId, LocalDate loanDate) { this(0, bookId, readerId, loanDate, null); }
    public BookLoan(int id, int bookId, int readerId, LocalDate loanDate, LocalDate returnDate) {
        this.id = id; this.bookId = bookId; this.readerId = readerId;
        this.loanDate = loanDate; this.returnDate = returnDate;
    }

    public boolean isActive() { return returnDate == null; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getBookId() { return bookId; }
    public int getReaderId() { return readerId; }
    public LocalDate getLoanDate() { return loanDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    @Override public String toString() {
        return "BookLoan{id=" + id + ", bookId=" + bookId + ", readerId=" + readerId
                + ", loanDate=" + loanDate + ", returnDate=" + returnDate + "}";
    }
}
