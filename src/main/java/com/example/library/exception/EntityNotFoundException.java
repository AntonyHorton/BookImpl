package com.example.library.exception;

public class EntityNotFoundException extends LibraryException {
    public EntityNotFoundException(String entity, int id) { super(entity + " с id=" + id + " не найден(а)"); }
}
