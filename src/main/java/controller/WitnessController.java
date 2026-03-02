package com.example.simulateur.controller;

import com.example.simulateur.dto.QuestionDTO;
import com.example.simulateur.dto.WitnessResponseDTO;
import com.example.simulateur.service.WitnessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/witness")
public class WitnessController {

    private final WitnessService witnessService;

    public WitnessController(WitnessService witnessService) {
        this.witnessService = witnessService;
    }

    // ❓ Poser une question à un témoin
    @PostMapping("/question")
    public WitnessResponseDTO askQuestion(@RequestBody QuestionDTO questionDTO) {
        return witnessService.processQuestion(questionDTO);
    }
}
