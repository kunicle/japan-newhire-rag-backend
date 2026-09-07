package com.teamproject.japan_newhire_rag_backend.domain.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.controller.dto.MyProfileResponse;
import com.teamproject.japan_newhire_rag_backend.domain.auth.service.internal.MyProfileQueryService;

@RestController
@RequestMapping("/api/me")
public class MyProfileController {

    private final CurrentUserProvider currentUserProvider;
    private final MyProfileQueryService myProfileQueryService;

    public MyProfileController(
            CurrentUserProvider currentUserProvider,
            MyProfileQueryService myProfileQueryService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.myProfileQueryService = myProfileQueryService;
    }

    @GetMapping
    public MyProfileResponse getMyProfile() {
        CurrentUserContext currentUser = currentUserProvider.getCurrentUser();
        return myProfileQueryService.getMyProfile(currentUser);
    }
}
