package com.appsmith.server.helpers.ce;

import com.appsmith.server.helpers.GitCloudServicesUtils;
import com.appsmith.server.helpers.GitPrivateRepoHelper;
import com.appsmith.server.services.ApplicationService;
import com.appsmith.server.services.FeatureFlagService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Slf4j
public class GitPrivateRepoCEImplTest {

    @Autowired
    GitPrivateRepoHelper gitPrivateRepoHelper;

    @MockBean
    GitCloudServicesUtils gitCloudServicesUtils;

    @MockBean
    ApplicationService applicationService;

    /**
     * Mocking is required as the methods are feature flagged in EE codebase which will call the super class method in
     * case the feature is not supported
     * Refer {@link com.appsmith.server.aspect.FeatureFlaggedMethodInvokerAspect}
     */
    @MockBean
    FeatureFlagService featureFlagService;

    @BeforeEach
    void setup() {
        Mockito.when(gitCloudServicesUtils.getPrivateRepoLimitForOrg(anyString(), anyBoolean()))
                .thenReturn(Mono.just(3));
        Mockito.when(featureFlagService.check(any())).thenReturn(Mono.just(Boolean.FALSE));
    }

    @Test
    @WithUserDetails(value = "api_user")
    public void isRepoLimitReached_connectedAppCountIsLessThanLimit_Success() {

        Mockito.when(applicationService.getGitConnectedApplicationsCountWithPrivateRepoByWorkspaceId(anyString()))
                .thenReturn(Mono.just(1L));

        StepVerifier.create(gitPrivateRepoHelper.isRepoLimitReached("workspaceId", false))
                .assertNext(aBoolean -> assertEquals(false, aBoolean))
                .verifyComplete();
    }

    @Test
    @WithUserDetails(value = "api_user")
    public void isRepoLimitReached_connectedAppCountIsSameAsLimit_Success() {

        Mockito.when(applicationService.getGitConnectedApplicationsCountWithPrivateRepoByWorkspaceId(anyString()))
                .thenReturn(Mono.just(3L));

        StepVerifier.create(gitPrivateRepoHelper.isRepoLimitReached("workspaceId", true))
                .assertNext(aBoolean -> assertEquals(true, aBoolean))
                .verifyComplete();
    }

    // This test is to check if the limit is reached when the count of connected apps is more than the limit
    // This happens when public visible git repo is synced with application and then the visibility is changed
    @Test
    @WithUserDetails(value = "api_user")
    public void isRepoLimitReached_connectedAppCountIsMoreThanLimit_Success() {

        Mockito.when(applicationService.getGitConnectedApplicationsCountWithPrivateRepoByWorkspaceId(anyString()))
                .thenReturn(Mono.just(4L));

        StepVerifier.create(gitPrivateRepoHelper.isRepoLimitReached("workspaceId", false))
                .assertNext(aBoolean -> assertEquals(true, aBoolean))
                .verifyComplete();
    }
}
