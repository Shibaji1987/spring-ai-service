package com.agentic.ai.spring_ai_service.audit.tools;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum InvestigationToolCatalog {

    USER_ACTIVITY(
            "getUserActivitySummary",
            "Summarize the actor's historical successes, failures, actions, and targets."),
    FAILED_LOGINS(
            "getFailedLoginCount",
            "Count failed authentication events for the actor."),
    RECENT_EVENTS(
            "getRecentEvents",
            "Return the actor's most recent audit events in chronological context."),
    IDENTITY_RISK(
            "getIdentityRiskProfile",
            "Inspect identity status, role, privilege, MFA, break-glass, and employment metadata."),
    AUTHENTICATION_RISK(
            "getAuthenticationRisk",
            "Assess authentication method, failures, MFA, credential age, and unusual login signals."),
    BEHAVIORAL_BASELINE(
            "getBehavioralBaseline",
            "Compare the event with the actor's historical actions, targets, times, and locations."),
    RELATED_SEQUENCE(
            "getRelatedEventSequence",
            "Build a bounded event timeline around the actor and target."),
    ASSET_RISK(
            "getAssetRiskProfile",
            "Inspect target criticality, environment, ownership, and data classification."),
    NETWORK_RISK(
            "getNetworkRiskContext",
            "Inspect source IP, location, VPN, proxy, TOR, reputation, and impossible-travel signals."),
    SESSION_RISK(
            "getSessionRiskContext",
            "Inspect session, device, token, authentication, and concurrent-session metadata."),
    AUTHORIZATION_CONTEXT(
            "getAuthorizationContext",
            "Inspect approval, ticket, maintenance-window, entitlement, and separation-of-duties evidence."),
    DATA_EXPOSURE(
            "getDataExposureContext",
            "Inspect export volume, record count, destination, sensitivity, and encryption signals."),
    APPLICABLE_POLICIES(
            "searchApplicablePolicies",
            "Search the policy knowledge base for evidence applicable to this event."),
    CONTROL_COVERAGE(
            "getControlCoverage",
            "Inspect preventive and detective control signals recorded on the event."),
    THREAT_INDICATORS(
            "getThreatIndicatorContext",
            "Inspect recorded threat-intelligence signals for IPs, domains, hashes, and user agents.");

    private static final Map<String, InvestigationToolCatalog> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(InvestigationToolCatalog::toolName, Function.identity()));

    private final String toolName;
    private final String description;

    InvestigationToolCatalog(String toolName, String description) {
        this.toolName = toolName;
        this.description = description;
    }

    public String toolName() {
        return toolName;
    }

    public String description() {
        return description;
    }

    public static boolean contains(String toolName) {
        return BY_NAME.containsKey(toolName);
    }

    public static Set<String> names() {
        return BY_NAME.keySet();
    }

    public static String promptCatalog() {
        return Arrays.stream(values())
                .map(tool -> "- %s: %s".formatted(tool.toolName, tool.description))
                .collect(Collectors.joining("\n"));
    }
}
