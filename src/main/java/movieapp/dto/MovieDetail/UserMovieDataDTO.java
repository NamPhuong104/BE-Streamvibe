package movieapp.dto.MovieDetail;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMovieDataDTO {
    // ========== Favorite ==========
    private boolean isFavorite;
    private Long favoriteId;
    private LocalDateTime favoriteCreatedAt;

    // ========== Watch Progress (ĐẦY ĐỦ) ==========
    private Long watchHistoryId;
    private String movieSlug;
    private String movieName;
    private String originName;
    private String posterUrl;
    private String thumbUrl;
    private String episodeSlug;
    private String episodeName;
    private String serverName;
    private Long currentTime;
    private Long duration;
    private Double progressPercent;
    private Boolean completed;
    private LocalDateTime lastWatchedAt;

    // ========== Playlists ==========
    private Integer totalPlaylists;
    private Long checkedPlaylistId;
    private List<PlaylistItemDTO> playlists;

    // ========== Playlist Item ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaylistItemDTO {
        private Long id;
        private String name;
        private Integer movieCount;
        private Boolean hasMovie;
        private LocalDateTime createdAt;
    }

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public static UserMovieDataDTO fromProjection(UserMovieDataProjection p) {
        if (p == null) return guest();

        return UserMovieDataDTO.builder()
                // Favorite
                .isFavorite(Boolean.TRUE.equals(p.getIsFavorite()))
                .favoriteId(p.getFavoriteId())
                .favoriteCreatedAt(p.getFavoriteCreatedAt())

                // Watch Progress (ĐẦY ĐỦ)
                .watchHistoryId(p.getWatchHistoryId())
                .movieSlug(p.getMovieSlug())
                .movieName(p.getMovieName())
                .originName(p.getOriginName())
                .posterUrl(p.getPosterUrl())
                .thumbUrl(p.getThumbUrl())
                .episodeSlug(p.getEpisodeSlug())
                .episodeName(p.getEpisodeName())
                .serverName(p.getServerName())
                .currentTime(p.getWatchTime())
                .duration(p.getDuration())
                .progressPercent(p.getProgressPercent())
                .completed(p.getCompleted())
                .lastWatchedAt(p.getLastWatchedAt())

                // Playlists
                .totalPlaylists(p.getTotalPlaylists() != null ? p.getTotalPlaylists() : 0)
                .checkedPlaylistId(p.getCheckedPlaylistId())
                .playlists(parsePlaylistsJson(p.getPlaylistsJson()))
                .build();
    }

    private static List<PlaylistItemDTO> parsePlaylistsJson(String json) {
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<PlaylistItemDTO>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static UserMovieDataDTO guest() {
        return UserMovieDataDTO.builder()
                .isFavorite(false)
                .totalPlaylists(0)
                .playlists(Collections.emptyList())
                .build();
    }

    public boolean hasWatchProgress() {
        return watchHistoryId != null;
    }
}
