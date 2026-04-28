package dto;

public record WitnessInfoDTO(
    int    index,
    String name,
    String profession,
    int    credibility,
    double stressLevel,
    String initialStatement,
    String personality
) {}
