package com.mottu.ops.infrastructure.adapter.in.web;

import com.mottu.ops.application.exception.InvalidCredentialsException;
import com.mottu.ops.application.exception.ResourceNotFoundException;
import com.mottu.ops.application.service.BillingAndRentalService;
import com.mottu.ops.infrastructure.adapter.in.web.dto.billing.AddRiderDeliveriesRequestDto;
import com.mottu.ops.infrastructure.adapter.in.web.dto.billing.RiderRentalSummaryResponseDto;
import com.mottu.ops.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import com.mottu.ops.infrastructure.adapter.out.persistence.repository.SpringDataUserRepository;
import com.mottu.ops.infrastructure.adapter.out.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rider/rental")
@PreAuthorize("hasRole('RIDER')")
public class RiderRentalController {

    private final BillingAndRentalService billingAndRentalService;
    private final SpringDataUserRepository userRepository;

    public RiderRentalController(
            BillingAndRentalService billingAndRentalService,
            SpringDataUserRepository userRepository
    ) {
        this.billingAndRentalService = billingAndRentalService;
        this.userRepository = userRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<RiderRentalSummaryResponseDto> getSummary(Authentication authentication) {
        UUID riderId = extractUserId(authentication);
        return ResponseEntity.ok(billingAndRentalService.getRiderRentalSummary(riderId));
    }

    @PostMapping("/pay-pix")
    public ResponseEntity<RiderRentalSummaryResponseDto> payPix(Authentication authentication) {
        UUID riderId = extractUserId(authentication);
        return ResponseEntity.ok(billingAndRentalService.payRiderPix(riderId));
    }

    @PostMapping("/deliveries")
    public ResponseEntity<RiderRentalSummaryResponseDto> addDeliveries(
            @Valid @RequestBody AddRiderDeliveriesRequestDto request,
            Authentication authentication
    ) {
        UUID riderId = extractUserId(authentication);
        return ResponseEntity.ok(billingAndRentalService.addDailyDeliveries(riderId, request));
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication == null) {
            throw new InvalidCredentialsException("Usuário não autenticado");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUserPrincipal userPrincipal) {
            return userPrincipal.userId();
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            return userRepository.findByEmail(authentication.getName())
                    .map(UserJpaEntity::getId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + authentication.getName()));
        }
    }
}

