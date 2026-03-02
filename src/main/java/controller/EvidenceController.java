package com.example.simulateur.controller;

import com.example.simulateur.model.Evidence;
import com.example.simulateur.service.TrialService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evidence")
public class EvidenceController {

    private final TrialService trialService;

    public EvidenceController(TrialService trialService) {
        this.trialService = trialService;
    }

    // 📂 Voir toutes les preuves
    @GetMapping
    public List<Evidence> getAllEvidence() {
        return trialService.getAllEvidence();
    }

    // 🛑 Contester une preuve
    @PostMapping("/challenge/{id}")
    public String challengeEvidence(@PathVariable Long id) {
        trialService.challengeEvidence(id);
        return "Preuve contestée.";
    }
}
