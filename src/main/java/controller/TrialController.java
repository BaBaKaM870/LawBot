package controller;

import dto.VerdictDTO;
import model.Trial;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.TrialService;

@RestController
@RequestMapping("/api/trial")
public class TrialController {

    private final TrialService trialService;

    public TrialController(TrialService trialService) {
        this.trialService = trialService;
    }

    /** Démarre un nouveau procès avec une affaire aléatoire. */
    @PostMapping("/start")
    public ResponseEntity<Trial> startNewTrial() {
        Trial newTrial = trialService.createNewTrial();
        return ResponseEntity.ok(newTrial);
    }

    /** Passe à la phase suivante du procès. */
    @PostMapping("/{id}/next-phase")
    public ResponseEntity<String> advancePhase(@PathVariable Long id) {
        trialService.advanceTrialPhase(id);
        return ResponseEntity.ok("Phase suivante activée");
    }

    /** Calcule et retourne le verdict final. */
    @GetMapping("/{id}/verdict")
    public ResponseEntity<VerdictDTO> getFinalVerdict(@PathVariable Long id) {
        return ResponseEntity.ok(trialService.calculateVerdict(id));
    }
}
