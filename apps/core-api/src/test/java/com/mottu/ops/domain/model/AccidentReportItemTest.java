package com.mottu.ops.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccidentReportItemTest {

    @Test
    @DisplayName("Deve criar item gerado pela IA com timestamp do vídeo")
    void shouldCreateAiItem() {
        UUID partId = UUID.randomUUID();
        AccidentReportItem item = AccidentReportItem.createAiItem(
                partId, "Manete", Money.of(25.00), 1, 6
        );

        assertThat(item.getId()).isNotNull();
        assertThat(item.getCatalogPartId()).isEqualTo(partId);
        assertThat(item.getPartName()).isEqualTo("Manete");
        assertThat(item.getUnitPrice()).isEqualTo(Money.of(25.00));
        assertThat(item.getQuantity()).isEqualTo(1);
        assertThat(item.getVideoTimestampSeconds()).isEqualTo(6);
        assertThat(item.getAddedBy()).isEqualTo("AI");
        assertThat(item.isConfirmedByMechanic()).isTrue();
    }

    @Test
    @DisplayName("Deve criar item adicionado pelo mecânico")
    void shouldCreateMechanicItem() {
        UUID partId = UUID.randomUUID();
        AccidentReportItem item = AccidentReportItem.createMechanicItem(
                partId, "Retrovisor", Money.of(42.50), 2
        );

        assertThat(item.getId()).isNotNull();
        assertThat(item.getCatalogPartId()).isEqualTo(partId);
        assertThat(item.getPartName()).isEqualTo("Retrovisor");
        assertThat(item.getUnitPrice()).isEqualTo(Money.of(42.50));
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getVideoTimestampSeconds()).isNull();
        assertThat(item.getAddedBy()).isEqualTo("MECHANIC");
        assertThat(item.isConfirmedByMechanic()).isTrue();
    }

    @Test
    @DisplayName("Deve permitir desmarcar confirmação pelo mecânico")
    void shouldToggleConfirmation() {
        AccidentReportItem item = AccidentReportItem.createAiItem(
                UUID.randomUUID(), "Manete", Money.of(25.00), 1, 6
        );
        item.setConfirmedByMechanic(false);
        assertThat(item.isConfirmedByMechanic()).isFalse();
    }

    @Test
    @DisplayName("Deve reconstituir item do banco de dados")
    void shouldReconstituteItem() {
        UUID id = UUID.randomUUID();
        UUID partId = UUID.randomUUID();
        AccidentReportItem item = AccidentReportItem.reconstitute(
                id, partId, "Carenagem", Money.of(120.00), 1, 10, true, "AI"
        );
        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getCatalogPartId()).isEqualTo(partId);
        assertThat(item.getPartName()).isEqualTo("Carenagem");
    }

    @Test
    @DisplayName("Validação de campos obrigatórios")
    void shouldValidateFields() {
        assertThatThrownBy(() -> AccidentReportItem.createAiItem(null, "Item", Money.of(10.00), 1, 0))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("catalogPartId cannot be null");

        assertThatThrownBy(() -> AccidentReportItem.createAiItem(UUID.randomUUID(), "", Money.of(10.00), 1, 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> AccidentReportItem.createAiItem(UUID.randomUUID(), "Item", null, 1, 0))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("unitPrice cannot be null");

        assertThatThrownBy(() -> AccidentReportItem.createAiItem(UUID.randomUUID(), "Item", Money.of(10.00), 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

