package com.example.library.service;

import java.util.function.Supplier;


public interface TransactionRunner {
    <T> T execute(Supplier<T> work);

    TransactionRunner NONE = new TransactionRunner() {
        @Override public <T> T execute(Supplier<T> work) { return work.get(); }
    };
}
