package com.smartdoc.exception;

import java.util.List;

public class UnsupportedFileTypeException extends RuntimeException {
    public UnsupportedFileTypeException(String received, List<String> allowed) {
        super("File type '" + received + "' is not supported. Allowed types: " + allowed);
    }
}
