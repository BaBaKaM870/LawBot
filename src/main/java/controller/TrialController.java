@RestController
@RequestMapping("/api/trial")
public class TrialController {

    private final TrialService trialService;

    public TrialController(TrialService trialService) {
        this.trialService = trialService;
    }

    @PostMapping("/start")
    public ResponseEntity<Trial> startNewTrial() {
        Trial newTrial = trialService.createNewTrial();
        return ResponseEntity.ok(newTrial);
    }

    @PostMapping("/{id}/next-phase")
    public ResponseEntity<String> advancePhase(@PathVariable Long id) {
        trialService.advanceTrialPhase(id);
        return ResponseEntity.ok("Phase suivante activée");
    }

    @GetMapping("/{id}/verdict")
    public ResponseEntity<VerdictDTO> getFinalVerdict(@PathVariable Long id) {
        return ResponseEntity.ok(trialService.calculateVerdict(id));
    }
}
