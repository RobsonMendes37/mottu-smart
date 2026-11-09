package com.mottu.ops.infrastructure.adapter.in.web;

import com.mottu.ops.application.service.BillingAndRentalService;
import com.mottu.ops.infrastructure.adapter.in.web.dto.billing.BillingContractSummaryDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/manager/billing")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerBillingController {

    private final BillingAndRentalService billingAndRentalService;

    public ManagerBillingController(BillingAndRentalService billingAndRentalService) {
        this.billingAndRentalService = billingAndRentalService;
    }

    @GetMapping("/contracts")
    public ResponseEntity<List<BillingContractSummaryDto>> listContracts() {
        return ResponseEntity.ok(billingAndRentalService.listBillingContracts());
    }

    @PostMapping("/contracts/{contractCode}/confirm-pix")
    public ResponseEntity<BillingContractSummaryDto> confirmPix(@PathVariable String contractCode) {
        return ResponseEntity.ok(billingAndRentalService.confirmPixPayment(contractCode));
    }
}

