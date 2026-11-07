package com.mottu.ops.application.port.out;

import com.mottu.ops.domain.enums.DamageVerdict;

import java.math.BigDecimal;
import java.util.List;

public interface FeedbackPolisherPort {
    String polishFeedback(DamageVerdict finalVerdict, List<String> partNames, BigDecimal totalEstimatedBrl, String mechanicNotes);
}
