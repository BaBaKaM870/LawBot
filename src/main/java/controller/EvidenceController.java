@RestController
@RequestMapping("/api/evidence")
public class EvidenceController {

    private final TrialService trialService; // Pour lier la preuve au procès en cours

    public EvidenceController(TrialService trialService) {
        this.trialService = trialService;
    }

    @GetMapping("/case/{caseId}")
    public ResponseEntity<List<Evidence>> getEvidenceList(@PathVariable Long caseId) {
        return ResponseEntity.ok(trialService.getEvidencesForCase(caseId));
    }

    @PostMapping("/present")
    public ResponseEntity<String> presentEvidence(@RequestParam Long trialId, @RequestParam Long evidenceId) {
        boolean impactful = trialService.presentEvidenceToJury(trialId, evidenceId);
        return impactful ? 
            ResponseEntity.ok("Le jury semble convaincu par cette preuve.") : 
            ResponseEntity.ok("La preuve n'a pas eu l'effet escompté.");
    }
}
