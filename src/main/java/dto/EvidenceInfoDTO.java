package dto;

public record EvidenceInfoDTO(
    int index,
    String description,
    double weight,
    boolean authentic,
    boolean contested
) {}
