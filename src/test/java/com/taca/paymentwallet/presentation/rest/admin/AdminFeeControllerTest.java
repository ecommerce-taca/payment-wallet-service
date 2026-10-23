package com.taca.paymentwallet.presentation.rest.admin;

import com.taca.paymentwallet.application.command.CreateAdminFeeConfigCommand;
import com.taca.paymentwallet.application.exception.ForbiddenException;
import com.taca.paymentwallet.application.exception.MfaRequiredException;
import com.taca.paymentwallet.application.exception.UnauthenticatedException;
import com.taca.paymentwallet.application.port.in.CreateAdminFeeConfigUseCase;
import com.taca.paymentwallet.application.port.in.GetAdminFeeConfigsUseCase;
import com.taca.paymentwallet.application.result.CreateAdminFeeConfigResult;
import com.taca.paymentwallet.application.result.GetAdminFeeConfigResult;
import com.taca.paymentwallet.application.security.ActorContext;
import com.taca.paymentwallet.application.security.FinanceOpsPolicy;
import com.taca.paymentwallet.application.security.SecurityRoles;
import com.taca.paymentwallet.presentation.rest.GlobalRestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminFeeControllerTest {

    private GetAdminFeeConfigsUseCase getAdminFeeConfigsUseCase;
    private FinanceOpsPolicy financeOpsPolicy;
    private CreateAdminFeeConfigUseCase createAdminFeeConfigUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        getAdminFeeConfigsUseCase = mock(GetAdminFeeConfigsUseCase.class);
        financeOpsPolicy = mock(FinanceOpsPolicy.class);
        createAdminFeeConfigUseCase = mock(CreateAdminFeeConfigUseCase.class);

        AdminFeeController controller = new AdminFeeController(
                getAdminFeeConfigsUseCase,
                createAdminFeeConfigUseCase,
                financeOpsPolicy
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalRestExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnFeeConfigs() throws Exception {
        UUID id = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        when(getAdminFeeConfigsUseCase.execute()).thenReturn(
                List.of(
                        new GetAdminFeeConfigResult(
                                id,
                                "PLATFORM",
                                null,
                                700,
                                Instant.parse("2026-10-01T00:00:00Z"),
                                "Platform commission",
                                createdBy,
                                Instant.parse("2026-09-30T00:00:00Z")
                        )
                )
        );

        mockMvc.perform(
                        get("/api/v1/admin/fees")
                                .header("X-Request-ID", "req-admin-fees-001")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(id.toString()))
                .andExpect(jsonPath("$.data[0].scope").value("PLATFORM"))
                .andExpect(jsonPath("$.data[0].rate_bps").value(700))
                .andExpect(jsonPath("$.data[0].effective_from")
                        .value("2026-10-01T00:00:00Z"))
                .andExpect(jsonPath("$.data[0].note")
                        .value("Platform commission"))
                .andExpect(jsonPath("$.data[0].created_by")
                        .value(createdBy.toString()))
                .andExpect(jsonPath("$.data[0].created_at")
                        .value("2026-09-30T00:00:00Z"))
                .andExpect(jsonPath("$.meta.request_id")
                        .value("req-admin-fees-001"));

        verify(financeOpsPolicy).requireFinanceOps();
        verify(getAdminFeeConfigsUseCase).execute();
    }

    @Test
    void shouldReturnEmptyList() throws Exception {
        when(getAdminFeeConfigsUseCase.execute()).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/admin/fees")
                                .header("X-Request-ID", "req-admin-fees-empty")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(financeOpsPolicy).requireFinanceOps();
        verify(getAdminFeeConfigsUseCase).execute();
    }

    @Test
    void shouldRequireRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/admin/fees"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("MISSING_REQUIRED_HEADER"));

        verifyNoInteractions(financeOpsPolicy);
        verifyNoInteractions(getAdminFeeConfigsUseCase);
    }

    @Test
    void shouldRejectAnonymousActor() throws Exception {
        doThrow(new UnauthenticatedException())
                .when(financeOpsPolicy)
                .requireFinanceOps();

        mockMvc.perform(
                        get("/api/v1/admin/fees")
                                .header("X-Request-ID", "req-admin-fees-anonymous")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code")
                        .value("PAYMENT_UNAUTHENTICATED"));

        verifyNoInteractions(getAdminFeeConfigsUseCase);
    }

    @Test
    void shouldRejectNonFinanceOpsActor() throws Exception {
        doThrow(new ForbiddenException())
                .when(financeOpsPolicy)
                .requireFinanceOps();

        mockMvc.perform(
                        get("/api/v1/admin/fees")
                                .header("X-Request-ID", "req-admin-fees-forbidden")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code")
                        .value("PAYMENT_FORBIDDEN"));

        verifyNoInteractions(getAdminFeeConfigsUseCase);
    }

    @Test
    void shouldAuthorizeBeforeReadingFeeConfigs() throws Exception {
        when(getAdminFeeConfigsUseCase.execute()).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/admin/fees")
                                .header("X-Request-ID", "req-admin-fees-order")
                )
                .andExpect(status().isOk());

        var inOrder = inOrder(
                financeOpsPolicy,
                getAdminFeeConfigsUseCase
        );

        inOrder.verify(financeOpsPolicy).requireFinanceOps();
        inOrder.verify(getAdminFeeConfigsUseCase).execute();
    }

    @Test
    void shouldCreatePlatformFeeConfigVersion() throws Exception {
        UUID actorUserId = UUID.randomUUID();
        UUID configId = UUID.randomUUID();

        ActorContext actor = new ActorContext(
                actorUserId,
                Set.of(SecurityRoles.FINANCE_OPS),
                Set.of(),
                Set.of()
        );

        when(financeOpsPolicy.requireFinanceOpsWithStepUp())
                .thenReturn(actor);

        when(createAdminFeeConfigUseCase.execute(any()))
                .thenReturn(
                        new CreateAdminFeeConfigResult(
                                configId,
                                "PLATFORM",
                                null,
                                500,
                                Instant.parse(
                                        "2026-11-01T00:00:00Z"
                                ),
                                "Platform commission November 2026",
                                actorUserId,
                                Instant.parse(
                                        "2026-10-23T11:30:00Z"
                                )
                        )
                );

        mockMvc.perform(
                        put("/api/v1/admin/fees")
                                .header(
                                        "X-Request-ID",
                                        "req-admin-fee-update-001"
                                )
                                .contentType("application/json")
                                .content("""
                                    {
                                      "scope": "PLATFORM",
                                      "category_id": null,
                                      "rate_bps": 500,
                                      "effective_from": "2026-11-01T00:00:00Z",
                                      "note": "Platform commission November 2026"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id")
                        .value(configId.toString()))
                .andExpect(jsonPath("$.data.scope")
                        .value("PLATFORM"))
                .andExpect(jsonPath("$.data.category_id")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.rate_bps")
                        .value(500))
                .andExpect(jsonPath("$.data.effective_from")
                        .value("2026-11-01T00:00:00Z"))
                .andExpect(jsonPath("$.data.note")
                        .value("Platform commission November 2026"))
                .andExpect(jsonPath("$.data.created_by")
                        .value(actorUserId.toString()))
                .andExpect(jsonPath("$.data.created_at")
                        .value("2026-10-23T11:30:00Z"))
                .andExpect(jsonPath("$.meta.request_id")
                        .value("req-admin-fee-update-001"));
    }

    @Test
    void shouldUseAuthenticatedActorAsCreatedBy() throws Exception {
        UUID actorUserId = UUID.randomUUID();

        ActorContext actor = new ActorContext(
                actorUserId,
                Set.of(SecurityRoles.FINANCE_OPS),
                Set.of(),
                Set.of()
        );

        when(financeOpsPolicy.requireFinanceOpsWithStepUp())
                .thenReturn(actor);

        when(createAdminFeeConfigUseCase.execute(any()))
                .thenReturn(
                        new CreateAdminFeeConfigResult(
                                UUID.randomUUID(),
                                "PLATFORM",
                                null,
                                500,
                                Instant.parse(
                                        "2026-11-01T00:00:00Z"
                                ),
                                null,
                                actorUserId,
                                Instant.parse(
                                        "2026-10-23T11:30:00Z"
                                )
                        )
                );

        mockMvc.perform(
                        put("/api/v1/admin/fees")
                                .header(
                                        "X-Request-ID",
                                        "req-admin-fee-actor"
                                )
                                .contentType("application/json")
                                .content("""
                                    {
                                      "scope": "PLATFORM",
                                      "rate_bps": 500,
                                      "effective_from": "2026-11-01T00:00:00Z"
                                    }
                                    """)
                )
                .andExpect(status().isOk());

        ArgumentCaptor<CreateAdminFeeConfigCommand> captor =
                ArgumentCaptor.forClass(
                        CreateAdminFeeConfigCommand.class
                );

        verify(createAdminFeeConfigUseCase).execute(captor.capture());

        assertThat(captor.getValue().actorUserId()).isEqualTo(actorUserId);

        assertThat(captor.getValue().scope()).isEqualTo("PLATFORM");

        assertThat(captor.getValue().rateBps()).isEqualTo(500);
    }

    @Test
    void shouldRequireStepUpForFeeMutation() throws Exception {
        doThrow(new MfaRequiredException())
                .when(financeOpsPolicy)
                .requireFinanceOpsWithStepUp();

        mockMvc.perform(
                        put("/api/v1/admin/fees")
                                .header(
                                        "X-Request-ID",
                                        "req-admin-fee-mfa"
                                )
                                .contentType("application/json")
                                .content("""
                                    {
                                      "scope": "PLATFORM",
                                      "rate_bps": 500,
                                      "effective_from": "2026-11-01T00:00:00Z"
                                    }
                                    """)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code")
                        .value("AUTH_MFA_REQUIRED"));

        verifyNoInteractions(createAdminFeeConfigUseCase);
    }

    @Test
    void shouldRejectNonFinanceOpsFeeMutation() throws Exception {
        doThrow(new ForbiddenException())
                .when(financeOpsPolicy)
                .requireFinanceOpsWithStepUp();

        mockMvc.perform(
                        put("/api/v1/admin/fees")
                                .header(
                                        "X-Request-ID",
                                        "req-admin-fee-forbidden"
                                )
                                .contentType("application/json")
                                .content("""
                                    {
                                      "scope": "PLATFORM",
                                      "rate_bps": 500,
                                      "effective_from": "2026-11-01T00:00:00Z"
                                    }
                                    """)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code")
                        .value("PAYMENT_FORBIDDEN"));

        verifyNoInteractions(createAdminFeeConfigUseCase);
    }

    @Test
    void shouldRequireRequestIdForFeeMutation() throws Exception {
        mockMvc.perform(
                        put("/api/v1/admin/fees")
                                .contentType("application/json")
                                .content("""
                                    {
                                      "scope": "PLATFORM",
                                      "rate_bps": 500,
                                      "effective_from": "2026-11-01T00:00:00Z"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("MISSING_REQUIRED_HEADER"));

        verifyNoInteractions(financeOpsPolicy);
        verifyNoInteractions(createAdminFeeConfigUseCase);
    }

    @Test
    void shouldRejectCategoryIdForPlatformScope() throws Exception {
        UUID actorUserId = UUID.randomUUID();

        when(financeOpsPolicy.requireFinanceOpsWithStepUp())
                .thenReturn(
                        new ActorContext(
                                actorUserId,
                                Set.of(SecurityRoles.FINANCE_OPS),
                                Set.of(),
                                Set.of()
                        )
                );

        mockMvc.perform(
                        put("/api/v1/admin/fees")
                                .header(
                                        "X-Request-ID",
                                        "req-admin-fee-invalid"
                                )
                                .contentType("application/json")
                                .content("""
                                    {
                                      "scope": "PLATFORM",
                                      "category_id": "10000000-0000-0000-0000-000000000001",
                                      "rate_bps": 500,
                                      "effective_from": "2026-11-01T00:00:00Z"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("INVALID_REQUEST"));

        verifyNoInteractions(createAdminFeeConfigUseCase);
    }

    @Test
    void shouldAuthorizeWithStepUpBeforeCreatingFeeConfig()
            throws Exception {

        UUID actorUserId = UUID.randomUUID();

        when(financeOpsPolicy.requireFinanceOpsWithStepUp())
                .thenReturn(
                        new ActorContext(
                                actorUserId,
                                Set.of(SecurityRoles.FINANCE_OPS),
                                Set.of(),
                                Set.of()
                        )
                );

        when(createAdminFeeConfigUseCase.execute(any()))
                .thenReturn(
                        new CreateAdminFeeConfigResult(
                                UUID.randomUUID(),
                                "PLATFORM",
                                null,
                                500,
                                Instant.parse(
                                        "2026-11-01T00:00:00Z"
                                ),
                                null,
                                actorUserId,
                                Instant.parse(
                                        "2026-10-23T11:30:00Z"
                                )
                        )
                );

        mockMvc.perform(
                        put("/api/v1/admin/fees")
                                .header(
                                        "X-Request-ID",
                                        "req-admin-fee-order"
                                )
                                .contentType("application/json")
                                .content("""
                                    {
                                      "scope": "PLATFORM",
                                      "rate_bps": 500,
                                      "effective_from": "2026-11-01T00:00:00Z"
                                    }
                                    """)
                )
                .andExpect(status().isOk());

        var inOrder = inOrder(
                financeOpsPolicy,
                createAdminFeeConfigUseCase
        );

        inOrder.verify(financeOpsPolicy)
                .requireFinanceOpsWithStepUp();

        inOrder.verify(createAdminFeeConfigUseCase)
                .execute(any());
    }
}