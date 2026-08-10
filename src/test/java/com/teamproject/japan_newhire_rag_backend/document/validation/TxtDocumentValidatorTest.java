package com.teamproject.japan_newhire_rag_backend.document.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class TxtDocumentValidatorTest {

    private final TxtDocumentValidator validator = new TxtDocumentValidator();
    private final byte[] validContent = "사내 규정 내용".getBytes(StandardCharsets.UTF_8);

    @Test
    void rejectsNullFileName() {
        assertThrows(InvalidTxtDocumentException.class,
                () -> validator.validate(null, validContent));
    }

    @Test
    void rejectsBlankFileName() {
        assertThrows(InvalidTxtDocumentException.class,
                () -> validator.validate("", validContent));
        assertThrows(InvalidTxtDocumentException.class,
                () -> validator.validate("   ", validContent));
    }

    @Test
    void rejectsNonTxtExtension() {
        assertThrows(InvalidTxtDocumentException.class,
                () -> validator.validate("규정.pdf", validContent));
    }

    @Test
    void acceptsUppercaseTxtExtension() {
        assertDoesNotThrow(() -> validator.validate("규정.TXT", validContent));
    }

    @Test
    void rejectsNullContent() {
        assertThrows(InvalidTxtDocumentException.class,
                () -> validator.validate("규정.txt", null));
    }

    @Test
    void rejectsEmptyContent() {
        assertThrows(InvalidTxtDocumentException.class,
                () -> validator.validate("규정.txt", new byte[0]));
    }

    @Test
    void rejectsContentLargerThanTenMegabytes() {
        byte[] oversizedContent = new byte[10 * 1024 * 1024 + 1];

        assertThrows(InvalidTxtDocumentException.class,
                () -> validator.validate("규정.txt", oversizedContent));
    }

    @Test
    void rejectsWhitespaceOnlyContent() {
        byte[] whitespaceContent = " \n\t ".getBytes(StandardCharsets.UTF_8);

        assertThrows(InvalidTxtDocumentException.class,
                () -> validator.validate("규정.txt", whitespaceContent));
    }

    @Test
    void rejectsMalformedUtf8Content() {
        byte[] malformedUtf8 = {(byte) 0xC3, (byte) 0x28};

        assertThrows(InvalidTxtDocumentException.class,
                () -> validator.validate("규정.txt", malformedUtf8));
    }

    @Test
    void acceptsValidTxtDocument() {
        byte[] content = "신입사원을 위한 사내 규정 안내입니다.".getBytes(StandardCharsets.UTF_8);

        assertDoesNotThrow(() -> validator.validate("규정안내.txt", content));
    }
}
