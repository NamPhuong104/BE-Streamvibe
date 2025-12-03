package movieapp.dto.WatchHistory;

import lombok.*;

import java.time.LocalDateTime;

@Data
public class WatchHistoryRes {
    private Long id;
    private ResUserDTO user;

    private String movieSlug;
    private String movieName;
    private String movieType;

    private String episodeSlug;
    private String episodeName;
    private String serverName;

    private Long currentTime;
    private Long duration;
    private Double progressPercent;
    private Boolean completed;

    private String currentTimeFormatted;
    private String durationFormatted;

    private LocalDateTime lastWatchedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class ResUserDTO {
        private Long id;
        private String email;
    }
}
