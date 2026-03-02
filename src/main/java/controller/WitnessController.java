@RestController
@RequestMapping("/api/witness")
public class WitnessController {

    private final WitnessService witnessService;

    public WitnessController(WitnessService witnessService) {
        this.witnessService = witnessService;
    }

    @PostMapping("/{witnessId}/ask")
    public ResponseEntity<WitnessResponseDTO> askQuestion(
            @PathVariable Long witnessId, 
            @RequestBody QuestionDTO question) {
        
        WitnessResponseDTO response = witnessService.processQuestion(witnessId, question);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{witnessId}/confront")
    public ResponseEntity<Contradiction> confrontWitness(
            @PathVariable Long witnessId, 
            @RequestParam Long evidenceId) {
            
        Contradiction result = witnessService.checkContradiction(witnessId, evidenceId);
        return ResponseEntity.ok(result);
    }
}
