package com.agentic.ai.spring_ai_service.service;

import com.agentic.ai.spring_ai_service.audit.model.KnowledgeDocument;
import com.agentic.ai.spring_ai_service.audit.repository.KnowledgeDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeCatalogServiceTests {

    @Mock
    private KnowledgeDocumentRepository repository;

    private KnowledgeCatalogService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeCatalogService(repository);
    }

    @Test
    void returnsMappedPaginatedDocuments() {
        KnowledgeDocument document = document("doc-1", "PAM-PRIV-BRK-058", "POLICY");
        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(document)));

        var response = service.getDocuments(0, 25, "", "");

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().id()).isEqualTo("doc-1");
        assertThat(response.items().getFirst().metadata()).containsEntry("controlId", "PAM-PRIV-BRK-058");
    }

    @Test
    void appliesSourceTypeAndTitleFiltersTogether() {
        KnowledgeDocument document = document("doc-1", "PAM-PRIV-BRK-058", "POLICY");
        when(repository.findBySourceTypeIgnoreCaseAndTitleContainingIgnoreCase(
                org.mockito.ArgumentMatchers.eq("POLICY"),
                org.mockito.ArgumentMatchers.eq("PAM-PRIV-BRK"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(document)));

        var response = service.getDocuments(0, 25, " PAM-PRIV-BRK ", " POLICY ");

        assertThat(response.items()).extracting(item -> item.title())
                .containsExactly("PAM-PRIV-BRK-058");
        verify(repository).findBySourceTypeIgnoreCaseAndTitleContainingIgnoreCase(
                org.mockito.ArgumentMatchers.eq("POLICY"),
                org.mockito.ArgumentMatchers.eq("PAM-PRIV-BRK"),
                any(Pageable.class)
        );
    }

    private KnowledgeDocument document(String id, String title, String sourceType) {
        KnowledgeDocument document = new KnowledgeDocument(
                title,
                sourceType,
                "Policy content",
                List.of("policy"),
                Map.of("controlId", title)
        );
        ReflectionTestUtils.setField(document, "id", id);
        document.setCreatedAt(LocalDateTime.parse("2026-06-13T12:00:00"));
        return document;
    }
}
