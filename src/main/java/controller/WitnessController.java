package controller;

import dto.QuestionDTO;
import dto.WitnessResponseDTO;
import model.Contradiction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.WitnessService;

@RestController
@RequestMapping("/api/witness")
public class WitnessController {

    private final WitnessService witnessService;

    public WitnessController(WitnessService witnessService) {
        this.witnessService = witnessService;
    }

    /**
     * Le joueur pose une question à un témoin.
     * Le trialId est inclus dans le body QuestionDTO.
     */
    @PostMapping("/{witnessId}/ask")
    public ResponseEntity<WitnessResponseDTO> askQuestion(
            @PathVariable Long witnessId,
            @RequestBody QuestionDTO question) {

        WitnessResponseDTO response = witnessService.processQuestion(witnessId, question);
        return ResponseEntity.ok(response);
    }

    /**
     * Confronte un témoin avec une preuve pour détecter une contradiction.
     * Requiert le trialId pour retrouver le contexte du procès.
     */
    @PostMapping("/{witnessId}/confront")
    public ResponseEntity<Contradiction> confrontWitness(
            @PathVariable Long witnessId,
            @RequestParam Long trialId,
            @RequestParam Long evidenceId) {

        Contradiction result = witnessService.checkContradiction(trialId, witnessId, evidenceId);
        return ResponseEntity.ok(result);
    }
}
