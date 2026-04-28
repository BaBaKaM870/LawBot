package dto;

public record JuryMemberInfoDTO(
    int    index,
    String name,
    String profile,
    double convictionLevel
) {}
