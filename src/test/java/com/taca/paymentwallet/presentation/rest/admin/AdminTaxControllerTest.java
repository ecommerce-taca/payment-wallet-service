package com.taca.paymentwallet.presentation.rest.admin;

import com.taca.paymentwallet.application.command.CreateAdminTaxConfigCommand;
import com.taca.paymentwallet.application.exception.ForbiddenException;
import com.taca.paymentwallet.application.exception.MfaRequiredException;
import com.taca.paymentwallet.application.exception.UnauthenticatedException;
import com.taca.paymentwallet.application.port.in.CreateAdminTaxConfigUseCase;
import com.taca.paymentwallet.application.port.in.GetAdminTaxConfigsUseCase;
import com.taca.paymentwallet.application.result.CreateAdminTaxConfigResult;
import com.taca.paymentwallet.application.result.GetAdminTaxConfigResult;
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

class AdminTaxControllerTest {

    private GetAdminTaxConfigsUseCase getAdminTaxConfigsUseCase;
    private FinanceOpsPolicy financeOpsPolicy;
    private MockMvc mockMvc;
    private CreateAdminTaxConfigUseCase createAdminTaxConfigUseCase;

    @BeforeEach
    void setUp() {
        getAdminTaxConfigsUseCase = mock(GetAdminTaxConfigsUseCase.class);

        createAdminTaxConfigUseCase = mock(CreateAdminTaxConfigUseCase.class);

        financeOpsPolicy = mock(FinanceOpsPolicy.class);

        AdminTaxController controller = new AdminTaxController(
                getAdminTaxConfigsUseCase,
                createAdminTaxConfigUseCase,
                financeOpsPolicy
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalRestExceptionHandler()
                )
                .build();
    }

    @Test
    void shouldReturnTaxConfigs() throws Exception {
        UUID id = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        when(getAdminTaxConfigsUseCase.execute()).thenReturn(
                List.of(
                        new GetAdminTaxConfigResult(
                                id,
                                "PLATFORM",
                                null,
                                100,
                                Instant.parse("2026-10-01T00:00:00Z"),
                                "Platform tax",
                                createdBy,
                                Instant.parse("2026-09-30T00:00:00Z")
                        )
                )
        );

        mockMvc.perform(
                        get("/api/v1/admin/taxes")
                                .header("X-Request-ID", "req-admin-taxes-001")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(id.toString()))
                .andExpect(jsonPath("$.data[0].scope").value("PLATFORM"))
                .andExpect(jsonPath("$.data[0].rate_bps").value(100))
                .andExpect(jsonPath("$.data[0].effective_from")
                        .value("2026-10-01T00:00:00Z"))
                .andExpect(jsonPath("$.data[0].note")
                        .value("Platform tax"))
                .andExpect(jsonPath("$.data[0].created_by")
                        .value(createdBy.toString()))
                .andExpect(jsonPath("$.data[0].created_at")
                        .value("2026-09-30T00:00:00Z"))
                .andExpect(jsonPath("$.meta.request_id")
                        .value("req-admin-taxes-001"));

        verify(financeOpsPolicy).requireFinanceOps();
        verify(getAdminTaxConfigsUseCase).execute();
    }

    @Test
    void shouldReturnEmptyList() throws Exception {
        when(getAdminTaxConfigsUseCase.execute()).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/admin/taxes")
                                .header("X-Request-ID", "req-admin-taxes-empty")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(financeOpsPolicy).requireFinanceOps();
        verify(getAdminTaxConfigsUseCase).execute();
    }

    @Test
    void shouldRequireRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/admin/taxes"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("MISSING_REQUIRED_HEADER"));

        verifyNoInteractions(financeOpsPolicy);
        verifyNoInteractions(getAdminTaxConfigsUseCase);
    }

    @Test
    void shouldRejectAnonymousActor() throws Exception {
        doThrow(new UnauthenticatedException())
                .when(financeOpsPolicy)
                .requireFinanceOps();

        mockMvc.perform(
                        get("/api/v1/admin/taxes")
                                .header("X-Request-ID", "req-admin-taxes-anonymous")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code")
                        .value("PAYMENT_UNAUTHENTICATED"));

        verifyNoInteractions(getAdminTaxConfigsUseCase);
    }

    @Test
    void shouldRejectNonFinanceOpsActor() throws Exception {
        doThrow(new ForbiddenException())
                .when(financeOpsPolicy)
                .requireFinanceOps();

        mockMvc.perform(
                        get("/api/v1/admin/taxes")
                                .header("X-Request-ID", "req-admin-taxes-forbidden")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code")
                        .value("PAYMENT_FORBIDDEN"));

        verifyNoInteractions(getAdminTaxConfigsUseCase);
    }

    @Test
    void shouldAuthorizeBeforeReadingTaxConfigs() throws Exception {
        when(getAdminTaxConfigsUseCase.execute()).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/admin/taxes")
                                .header("X-Request-ID", "req-admin-taxes-order")
                )
                .andExpect(status().isOk());

        var inOrder = inOrder(
                financeOpsPolicy,
                getAdminTaxConfigsUseCase
        );

        inOrder.verify(financeOpsPolicy).requireFinanceOps();
        inOrder.verify(getAdminTaxConfigsUseCase).execute();
    }

    @Test
    void shouldCreatePlatformTaxConfigVersion()
            throws Exception {

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

        when(createAdminTaxConfigUseCase.execute(any()))
                .thenReturn(
                        new CreateAdminTaxConfigResult(
                                configId,
                                "PLATFORM",
                                null,
                                100,
                                Instant.parse("2026-11-01T00:00:00Z"),
                                "Platform tax November 2026",
                                actorUserId,
                                Instant.parse("2026-10-23T12:30:00Z")
                        )
                );

        mockMvc.perform(
                        put("/api/v1/admin/taxes")
                                .header(
                                        "X-Request-ID",
                                        "req-admin-tax-update-001"
                                )
                                .contentType("application/json")
                                .content("""
                                    {
                                      "scope": "PLATFORM",
                                      "category_id": null,
                                      "rate_bps": 100,
                                      "effective_from": "2026-11-01T00:00:00Z",
                                      "note": "Platform tax November 2026"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(configId.toString()))
                .andExpect(jsonPath("$.data.scope").value("PLATFORM"))
                .andExpect(jsonPath("$.data.rate_bps").value(100))
                .andExpect(jsonPath("$.data.effective_from").value("2026-11-01T00:00:00Z"))
                .andExpect(jsonPath("$.data.note").value("Platform tax November 2026"))
                .andExpect(jsonPath("$.data.created_by").value(actorUserId.toString()))
                .andExpect(jsonPath("$.data.created_at").value("2026-10-23T12:30:00Z"))
                .andExpect(jsonPath("$.meta.request_id").value("req-admin-tax-update-001"));
    }

    @Test
    void shouldCreateCategoryTaxConfigVersion()
            throws Exception {

        UUID actorUserId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        ActorContext actor = new ActorContext(
                actorUserId,
                Set.of(SecurityRoles.FINANCE_OPS),
                Set.of(),
                Set.of()
        );

        when(financeOpsPolicy.requireFinanceOpsWithStepUp())
                .thenReturn(actor);

        when(createAdminTaxConfigUseCase.execute(any()))
                .thenReturn(
                        new CreateAdminTaxConfigResult(
                                UUID.randomUUID(),
                                "CATEGORY",
                                categoryId,
                                150,
                                Instant.parse("2026-11-01T00:00:00Z"),
                                "Category tax",
                                actorUserId,
                                Instant.parse("2026-10-23T12:30:00Z")
                        )
                );

        mockMvc.perform(
                        put("/api/v1/admin/taxes")
                                .header(
                                        "X-Request-ID",
                                        "req-admin-tax-category"
                                )
                                .contentType("application/json")
                                .content("""
                                    {
                                      "scope": "CATEGORY",
                                      "category_id": "%s",
                                      "rate_bps": 150,
                                      "effective_from": "2026-11-01T00:00:00Z",
                                      "note": "Category tax"
                                    }
                                    """.formatted(categoryId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scope").value("CATEGORY"))
                .andExpect(jsonPath("$.data.category_id").value(categoryId.toString()))
                .andExpect(jsonPath("$.data.rate_bps").value(150));
    }

    @Test
    void shouldUseAuthenticatedActorAsTaxCreatedBy()
            throws Exception {

        UUID actorUserId = UUID.randomUUID();

        ActorContext actor = new ActorContext(
                actorUserId,
                Set.of(SecurityRoles.FINANCE_OPS),
                Set.of(),
                Set.of()
        );

        when(financeOpsPolicy.requireFinanceOpsWithStepUp())
                .thenReturn(actor);

        when(createAdminTaxConfigUseCase.execute(any()))
                .thenReturn(
                        new CreateAdminTaxConfigResult(
                                UUID.randomUUID(),
                                "PLATFORM",
                                null,
                                100,
                                Instant.parse("2026-11-01T00:00:00Z"),
                                null,
                                actorUserId,
                                Instant.parse("2026-10-23T12:30:00Z")
                        )
                );

        mockMvc.perform(
                        put("/api/v1/admin/taxes")
                                .header(
                                        "X-Request-ID",
                                        "req-admin-tax-actor"
                                )
                                .contentType("application/json")
                                .content("""
                                    {
                                      "scope": "PLATFORM",
                                      "rate_bps": 100,
                                      "effective_from": "2026-11-01T00:00:00Z"
                                    }
                                    """)
                )
                .andExpect(status().isOk());

        ArgumentCaptor<CreateAdminTaxConfigCommand> captor =
                ArgumentCaptor.forClass(
                        CreateAdminTaxConfigCommand.class
                );

        verify(createAdminTaxConfigUseCase).execute(captor.capture());

        assertThat(captor.getValue().actorUserId()).isEqualTo(actorUserId);

        assertThat(captor.getValue().scope()).isEqualTo("PLATFORM");

        assertThat(captor.getValue().rateBps()).isEqualTo(100);
    }

    @Test
    void shouldRequireStepUpForTaxMutation()
            throws Exception {

        doThrow(new MfaRequiredException())
                .when(financeOpsPolicy)
                .requireFinanceOpsWithStepUp();

        mockMvc.perform(
                        put("/api/v1/admin/taxes")
                                .header(
                                        "X-Request-ID",
                                        "req-admin-tax-mfa"
                                )
                                .contentType("application/json")
                                .content("""
                                    {
                                      "scope": "PLATFORM",
                                      "rate_bps": 100,
                                      "effective_from": "2026-11-01T00:00:00Z"
                                    }
                                    """)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code")
                        .value("AUTH_MFA_REQUIRED"));

        verifyNoInteractions(
                createAdminTaxConfigUseCase
        );
    }

    @Test
    void shouldRejectNonFinanceOpsTaxMutation()
            throws Exception {

        doThrow(new ForbiddenException())
                .when(financeOpsPolicy)
                .requireFinanceOpsWithStepUp();

        mockMvc.perform(
                        put("/api/v1/admin/taxes")
                                .header(
                                        "X-Request-ID",
                                        "req-admin-tax-forbidden"
                                )
                                .contentType("application/json")
                                .content("""
                                    {
                                      "scope": "PLATFORM",
                                      "rate_bps": 100,
                                      "effective_from": "2026-11-01T00:00:00Z"
                                    }
                                    """)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code")
                        .value("PAYMENT_FORBIDDEN"));

        verifyNoInteractions(
                createAdminTaxConfigUseCase
        );
    }

    @Test
    void shouldRequireRequestIdForTaxMutation()
            throws Exception {

        mockMvc.perform(
                        put("/api/v1/admin/taxes")
                                .contentType("application/json")
                                .content("""
                                    {
                                      "scope": "PLATFORM",
                                      "rate_bps": 100,
                                      "effective_from": "2026-11-01T00:00:00Z"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("MISSING_REQUIRED_HEADER"));

        verifyNoInteractions(financeOpsPolicy);
        verifyNoInteractions(
                createAdminTaxConfigUseCase
        );
    }

    @Test
    void shouldRejectCategoryIdForPlatformTaxScope()
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

        mockMvc.perform(
                        put("/api/v1/admin/taxes")
                                .header(
                                        "X-Request-ID",
                                        "req-admin-tax-invalid"
                                )
                                .contentType("application/json")
                                .content("""
                                    {
                                      "scope": "PLATFORM",
                                      "category_id": "10000000-0000-0000-0000-000000000001",
                                      "rate_bps": 100,
                                      "effective_from": "2026-11-01T00:00:00Z"
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code")
                        .value("INVALID_REQUEST"));

        verifyNoInteractions(
                createAdminTaxConfigUseCase
        );
    }

    @Test
    void shouldAuthorizeWithStepUpBeforeCreatingTaxConfig()
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

        when(createAdminTaxConfigUseCase.execute(any()))
                .thenReturn(
                        new CreateAdminTaxConfigResult(
                                UUID.randomUUID(),
                                "PLATFORM",
                                null,
                                100,
                                Instant.parse(
                                        "2026-11-01T00:00:00Z"
                                ),
                                null,
                                actorUserId,
                                Instant.parse(
                                        "2026-10-23T12:30:00Z"
                                )
                        )
                );

        mockMvc.perform(
                        put("/api/v1/admin/taxes")
                                .header(
                                        "X-Request-ID",
                                        "req-admin-tax-order"
                                )
                                .contentType("application/json")
                                .content("""
                                    {
                                      "scope": "PLATFORM",
                                      "rate_bps": 100,
                                      "effective_from": "2026-11-01T00:00:00Z"
                                    }
                                    """)
                )
                .andExpect(status().isOk());

        var inOrder = inOrder(
                financeOpsPolicy,
                createAdminTaxConfigUseCase
        );

        inOrder.verify(financeOpsPolicy)
                .requireFinanceOpsWithStepUp();

        inOrder.verify(createAdminTaxConfigUseCase)
                .execute(any());
    }
}