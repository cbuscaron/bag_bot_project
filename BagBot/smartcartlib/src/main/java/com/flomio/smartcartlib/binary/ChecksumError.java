package com.flomio.smartcartlib.binary;

public class ChecksumError extends IllegalArgumentException {
    public ChecksumError(String s) {
        super(s);
    }
}
