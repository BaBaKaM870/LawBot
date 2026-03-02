package com.example.simulateur.controller;

import com.example.simulateur.service.TrialService;
import com.example.simulateur.service.VerdictService;
import com.example.simulateur.dto.VerdictDTO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trial")
public class TrialController {

    private final TrialService trialService;
    private final VerdictService verdictService;

    public TrialController(TrialService trialService, VerdictService verdictService) {
        this.trialService = trialService;
        this.verdictService = verdictService;
    }

    // 🎬 Démarrer un nouveau procès
    @PostMapping("/start")
    public String startTrial() {
        trialService.startNewTrial();
        return "Procès démarré avec succès.";
    }

    // ⚖️ Obtenir le verdict
    @GetMapping("/verdict")
    public VerdictDTO getVerdict() {
        return verdictService.calculateVerdict();
    }
}
