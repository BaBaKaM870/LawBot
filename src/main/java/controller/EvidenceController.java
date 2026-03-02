package controller;

import model.Evidence;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.TrialService;

import java.util.List;

@RestController
@RequestMapping("/api/evidence")
public class EvidenceController {

    private final TrialService trialService;

    public EvidenceController(TrialService trialService) {
        this.trialService = trialService;
    }

    /** Retourne la liste des preuves disponibles pour une affaire. */
    @GetMapping("/case/{caseId}")
    public ResponseEntity<List<Evidence>> getEvidenceList(@PathVariable Long caseId) {
        return ResponseEntity.ok(trialService.getEvidencesForCase(caseId));
    }

    /** Présente une preuve au jury et retourne si elle a été favorable. */
    @PostMapping("/present")
    public ResponseEntity<String> presentEvidence(
            @RequestParam Long trialId,
            @RequestParam Long evidenceId) {
        boolean impactful = trialService.presentEvidenceToJury(trialId, evidenceId);
        return impactful
            ? ResponseEntity.ok("Le jury semble convaincu par cette preuve.")
            : ResponseEntity.ok("La preuve n'a pas eu l'effet escompté.");
    }
}
