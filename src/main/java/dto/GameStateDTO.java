package dto;

import java.util.List;

public record GameStateDTO(
    Long trialId,
    String phase,
    int phaseIndex,
    String caseTitle,
    String crimeType,
    String suspectName,
    String caseDescription,
    List<WitnessInfoDTO> witnesses,
    List<EvidenceInfoDTO> evidences,
    double juryConviction,
    int successfulActions,
    int totalActions,
    List<String> events
) {}
