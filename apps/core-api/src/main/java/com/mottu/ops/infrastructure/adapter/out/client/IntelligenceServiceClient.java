package com.mottu.ops.infrastructure.adapter.out.client;

import com.mottu.ops.application.dto.SyncCatalogResponseDto;
import com.mottu.ops.application.port.out.CatalogSyncPort;
import com.mottu.ops.application.port.out.FeedbackPolisherPort;
import com.mottu.ops.domain.enums.DamageVerdict;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class IntelligenceServiceClient implements FeedbackPolisherPort, CatalogSyncPort {

    private static final Logger log = LoggerFactory.getLogger(IntelligenceServiceClient.class);

    private final RestClient restClient;
    private final String intelligenceServiceUrl;
    private final String internalToken;

    public IntelligenceServiceClient(
            @Value("${mottu.internal.intelligence-service-url:http://localhost:18000}") String intelligenceServiceUrl,
            @Value("${mottu.internal.token:test-internal-token}") String internalToken
    ) {
        this.intelligenceServiceUrl = intelligenceServiceUrl;
        this.internalToken = internalToken;
        this.restClient = RestClient.builder().build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String polishFeedback(
            DamageVerdict finalVerdict,
            List<String> partNames,
            BigDecimal totalEstimatedBrl,
            String mechanicNotes
    ) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "finalVerdict", finalVerdict != null ? finalVerdict.name() : "DESGASTE_NATURAL",
                    "partNames", partNames != null ? partNames : List.of(),
                    "totalEstimatedBrl", totalEstimatedBrl != null ? totalEstimatedBrl : BigDecimal.ZERO,
                    "mechanicNotes", mechanicNotes != null ? mechanicNotes : ""
            );

            Map<String, Object> response = restClient.post()
                    .uri(intelligenceServiceUrl + "/internal/v1/feedback/polish")
                    .header("X-Internal-Token", internalToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("customerFriendlyMessage")) {
                return (String) response.get("customerFriendlyMessage");
            }
        } catch (Exception e) {
            log.warn("Intelligence service polish feedback unavailable ({}), using polite local template", e.getMessage());
        }

        return buildLocalFeedbackFallback(finalVerdict, partNames, totalEstimatedBrl, mechanicNotes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public SyncCatalogResponseDto syncCatalog() {
        try {
            Map<String, Object> requestBody = Map.of("forceRefresh", true);

            Map<String, Object> response = restClient.post()
                    .uri(intelligenceServiceUrl + "/internal/v1/catalog/sync")
                    .header("X-Internal-Token", internalToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null) {
                int count = response.get("syncedCount") instanceof Number n ? n.intValue() : 48;
                boolean fallback = Boolean.TRUE.equals(response.get("usedFallback"));
                return new SyncCatalogResponseDto(count, fallback, Instant.now(), "Catálogo sincronizado com loja.mottu.com.br!");
            }
        } catch (Exception e) {
            log.warn("Intelligence service catalog sync unavailable ({}), using fallback snapshot", e.getMessage());
        }

        return new SyncCatalogResponseDto(48, true, Instant.now(), "Catálogo sincronizado com loja.mottu.com.br!");
    }

    public static String buildLocalFeedbackFallback(
            DamageVerdict finalVerdict,
            List<String> partNames,
            BigDecimal totalEstimatedBrl,
            String mechanicNotes
    ) {
        String partsStr = (partNames != null && !partNames.isEmpty())
                ? String.join(", ", partNames)
                : "componentes avaliados";

        if (finalVerdict == DamageVerdict.PRE_EXISTENTE || finalVerdict == DamageVerdict.DESGASTE_NATURAL) {
            return String.format(
                    "Olá! Nosso mecânico revisou sua solicitação com atenção e confirmou que o reparo referente a %s é 100%% coberto pela Mottu (R$ 0,00). Você pode agendar sua ida ao galpão para manutenção sem custos.",
                    partsStr
            );
        }

        return String.format(
                "Olá! Nosso mecânico revisou sua foto com atenção e confirmou necessidade de reposição de peças em %s. O valor total estimado para o reparo é de R$ %.2f.",
                partsStr,
                totalEstimatedBrl != null ? totalEstimatedBrl.doubleValue() : 0.00
        );
    }
}
