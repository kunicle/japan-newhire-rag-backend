package com.teamproject.japan_newhire_rag_backend.document.access.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessEvaluator;
import com.teamproject.japan_newhire_rag_backend.document.access.DocumentAccessRule;
import com.teamproject.japan_newhire_rag_backend.document.access.UserAccessContext;
import com.teamproject.japan_newhire_rag_backend.document.access.adapter.AccessRuleAssembler;
import com.teamproject.japan_newhire_rag_backend.document.access.adapter.CurrentUserAccessContextAssembler;
import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessDepartment;
import com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRole;
import com.teamproject.japan_newhire_rag_backend.document.access.repository.DocumentAccessDepartmentRepository;
import com.teamproject.japan_newhire_rag_backend.document.access.repository.DocumentAccessRoleRepository;
import com.teamproject.japan_newhire_rag_backend.document.access.repository.DocumentAccessRuleRepository;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.AccessReferenceQueryService;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;

@Service
@Transactional(readOnly = true)
public class DocumentAccessService {

    private final CurrentUserProvider currentUserProvider;
    private final DocumentAccessRuleRepository documentAccessRuleRepository;
    private final DocumentAccessRoleRepository documentAccessRoleRepository;
    private final DocumentAccessDepartmentRepository documentAccessDepartmentRepository;
    private final CurrentUserAccessContextAssembler currentUserAccessContextAssembler;
    private final AccessRuleAssembler accessRuleAssembler;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    public DocumentAccessService(
            CurrentUserProvider currentUserProvider,
            DocumentAccessRuleRepository documentAccessRuleRepository,
            DocumentAccessRoleRepository documentAccessRoleRepository,
            DocumentAccessDepartmentRepository documentAccessDepartmentRepository,
            AccessReferenceQueryService accessReferenceQueryService) {
        this.currentUserProvider = currentUserProvider;
        this.documentAccessRuleRepository = documentAccessRuleRepository;
        this.documentAccessRoleRepository = documentAccessRoleRepository;
        this.documentAccessDepartmentRepository = documentAccessDepartmentRepository;
        this.currentUserAccessContextAssembler =
                new CurrentUserAccessContextAssembler(accessReferenceQueryService);
        this.accessRuleAssembler = new AccessRuleAssembler(accessReferenceQueryService);
        this.documentAccessEvaluator = new DocumentAccessEvaluator();
    }

    public boolean canAccess(Long documentVersionId) {
        if (documentVersionId == null) {
            throw new IllegalArgumentException("documentVersionId는 null일 수 없습니다.");
        }

        Optional<com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule>
                ruleEntityOptional = documentAccessRuleRepository
                        .findByDocumentVersion_DocumentVersionId(documentVersionId);
        if (ruleEntityOptional.isEmpty()) {
            return false;
        }

        com.teamproject.japan_newhire_rag_backend.document.access.entity.DocumentAccessRule ruleEntity =
                ruleEntityOptional.get();
        Long documentAccessRuleId = ruleEntity.getDocumentAccessRuleId();
        List<DocumentAccessRole> roleRows = documentAccessRoleRepository
                .findByDocumentAccessRule_DocumentAccessRuleId(documentAccessRuleId);
        List<DocumentAccessDepartment> departmentRows = documentAccessDepartmentRepository
                .findByDocumentAccessRule_DocumentAccessRuleId(documentAccessRuleId);
        DocumentAccessRule pureRule = accessRuleAssembler.assemble(ruleEntity, roleRows, departmentRows);

        CurrentUserContext context = currentUserProvider.getCurrentUser();
        UserAccessContext userAccessContext = currentUserAccessContextAssembler.assemble(context);

        return documentAccessEvaluator.canAccess(userAccessContext, pureRule);
    }
}
