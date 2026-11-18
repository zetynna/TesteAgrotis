package com.agrotis.backend.controller;

import com.agrotis.backend.dto.IntegrationRequest;
import com.agrotis.backend.dto.SyncBatchResponse;
import com.agrotis.backend.service.IntegrationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/integration")
public class IntegrationController {

    private final IntegrationService integrationService;

    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @PostMapping({"/sync", "/v1/sync"})
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SyncBatchResponse sync(@Valid @RequestBody IntegrationRequest request) {
        return integrationService.start(request);
    }

    @GetMapping("/batches")
    public List<SyncBatchResponse> batches() {
        return integrationService.listBatches();
    }

    @GetMapping("/dlq")
    public List<SyncBatchResponse> dlq() {
        return integrationService.listDlq();
    }

    @PostMapping("/batches/{id}/reprocess")
    public SyncBatchResponse reprocess(@PathVariable Long id) {
        return integrationService.reprocess(id);
    }
}
