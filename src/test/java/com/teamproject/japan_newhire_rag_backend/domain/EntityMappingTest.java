package com.teamproject.japan_newhire_rag_backend.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.teamproject.japan_newhire_rag_backend.domain.chat.entity.ChatMessage;
import com.teamproject.japan_newhire_rag_backend.domain.chat.entity.ChatSession;
import com.teamproject.japan_newhire_rag_backend.domain.chat.entity.MessageRole;
import com.teamproject.japan_newhire_rag_backend.domain.chat.repository.ChatMessageRepository;
import com.teamproject.japan_newhire_rag_backend.domain.chat.repository.ChatSessionRepository;
import com.teamproject.japan_newhire_rag_backend.domain.regulation.entity.RegulationChunk;
import com.teamproject.japan_newhire_rag_backend.domain.regulation.entity.RegulationDocument;
import com.teamproject.japan_newhire_rag_backend.domain.regulation.repository.RegulationChunkRepository;
import com.teamproject.japan_newhire_rag_backend.domain.regulation.repository.RegulationDocumentRepository;

@DataJpaTest
class EntityMappingTest {

    @Autowired
    private RegulationDocumentRepository documentRepository;

    @Autowired
    private RegulationChunkRepository chunkRepository;

    @Autowired
    private ChatSessionRepository sessionRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @Test
    void regulationDocumentAndChunkCanBeSaved() {
        RegulationDocument document = RegulationDocument.create(
                "취업 규칙",
                "employment-rules.pdf"
        );

        RegulationDocument savedDocument =
                documentRepository.save(document);

        RegulationChunk chunk = RegulationChunk.create(
                savedDocument,
                0,
                "신입사원은 입사 첫날 인사 교육에 참석해야 합니다.",
                1,
                20
        );

        RegulationChunk savedChunk = chunkRepository.save(chunk);

        assertThat(savedDocument.getId()).isNotNull();
        assertThat(savedChunk.getId()).isNotNull();
        assertThat(savedChunk.getDocument().getId())
                .isEqualTo(savedDocument.getId());
    }

    @Test
    void chatSessionAndMessageCanBeSaved() {
        ChatSession session = ChatSession.create("휴가 규정 문의");

        ChatSession savedSession = sessionRepository.save(session);

        ChatMessage message = ChatMessage.create(
                savedSession,
                MessageRole.USER,
                "연차는 입사 후 언제부터 사용할 수 있나요?"
        );

        ChatMessage savedMessage = messageRepository.save(message);

        assertThat(savedSession.getId()).isNotNull();
        assertThat(savedSession.getSessionKey()).isNotBlank();
        assertThat(savedMessage.getId()).isNotNull();
        assertThat(savedMessage.getRole()).isEqualTo(MessageRole.USER);
    }
}
