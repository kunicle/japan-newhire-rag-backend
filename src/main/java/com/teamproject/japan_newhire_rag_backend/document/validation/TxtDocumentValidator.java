package com.teamproject.japan_newhire_rag_backend.document.validation;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public class TxtDocumentValidator {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    public void validate(String fileName, byte[] content) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new InvalidTxtDocumentException("파일명이 비어 있습니다.");
        }

        if (!fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".txt")) {
            throw new InvalidTxtDocumentException("TXT 파일만 업로드할 수 있습니다.");
        }

        if (content == null || content.length == 0) {
            throw new InvalidTxtDocumentException("파일 내용이 비어 있습니다.");
        }

        if (content.length > MAX_FILE_SIZE_BYTES) {
            throw new InvalidTxtDocumentException("파일 크기가 10MB를 초과했습니다.");
        }

        String decodedContent = decodeUtf8(content);
        if (decodedContent.trim().isEmpty()) {
            throw new InvalidTxtDocumentException("파일에 실제 내용이 없습니다.");
        }
    }

    private String decodeUtf8(byte[] content) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new InvalidTxtDocumentException("파일 인코딩이 UTF-8이 아닙니다.");
        }
    }
}
