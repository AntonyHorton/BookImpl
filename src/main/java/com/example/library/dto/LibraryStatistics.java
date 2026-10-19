package com.example.library.dto;

public class LibraryStatistics {
    private final long totalBooks;
    private final long totalAuthors;
    private final long totalReaders;
    private final long activeLoans;
    private final long availableBooks;

    public LibraryStatistics(long totalBooks, long totalAuthors, long totalReaders,
                             long activeLoans, long availableBooks) {
        this.totalBooks = totalBooks;
        this.totalAuthors = totalAuthors;
        this.totalReaders = totalReaders;
        this.activeLoans = activeLoans;
        this.availableBooks = availableBooks;
    }

    public long getTotalBooks() { return totalBooks; }
    public long getTotalAuthors() { return totalAuthors; }
    public long getTotalReaders() { return totalReaders; }
    public long getActiveLoans() { return activeLoans; }
    public long getAvailableBooks() { return availableBooks; }

    @Override public String toString() {
        return "LibraryStatistics{books=" + totalBooks + ", authors=" + totalAuthors + ", readers=" + totalReaders
                + ", activeLoans=" + activeLoans + ", availableBooks=" + availableBooks + "}";
    }
}
