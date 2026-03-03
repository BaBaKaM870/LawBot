package dto;

public record ActionResultDTO(
    boolean success,
    String message,
    boolean contradictionDetected,
    String witnessResponse,
    GameStateDTO gameState
) {}
