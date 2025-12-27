package com.hyperchip.common.util;

import org.springframework.core.io.InputStreamResource;
import java.io.IOException;
import java.io.InputStream;

/**
 * Helper class to wrap a MultipartFile's InputStream
 * so it can be sent via RestTemplate as multipart/form-data
 */
public class MultipartInputStreamFileResource extends InputStreamResource {

    private final String filename;

    public MultipartInputStreamFileResource(InputStream inputStream, String filename) {
        super(inputStream);
        this.filename = filename;
    }

    @Override
    public String getFilename() {
        return this.filename;
    }

    @Override
    public long contentLength() throws IOException {
        return -1; // we do not know the content length in advance
    }
}
