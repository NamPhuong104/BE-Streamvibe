package movieapp.dto.MovieDetail;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import movieapp.dto.MetaAndHead.SeoOnPage;
import movieapp.util.Util;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response tổng hợp cho movie detail API
 * Bao gồm: OPhim data + User data (từ Stored Procedure)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedMovieDetailResponse {
    // ========== OPHIM DATA ==========
    private OphimMovieDetail movieData;
    private OphimActorData actorData;

    // ========== USER DATA ==========
    @JsonProperty("isFavorite")
    private boolean isFavorite;
    private Long favoriteId;

    // Watch Progress (ĐẦY ĐỦ như IWatchProgress)
    private WatchProgressDTO watchProgress;

    // Playlists (Array để FE render dropdown)
    private List<PlaylistDTO> playlists;
    private Long playlistCheckedId;

    // ========== META ==========
    private boolean isAuthenticated;
    private long fetchedAt;

    // ========== NESTED DTOs ==========

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OphimMovieDetail {
        private SeoOnPage seoOnPage;
        private movieapp.dto.OphimResponse.OphimMovieDetail item;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WatchProgressDTO {
        private Long id;
        private String movieSlug;
        private String movieName;
        private String posterUrl;
        private String thumbUrl;
        private String episodeSlug;
        private String episodeName;
        private String serverName;
        private Long currentTime;
        private String currentTimeFormatted;  // "12:34"
        private Long duration;
        private String durationFormatted;     // "45:00"
        private Double progressPercent;
        private Boolean completed;
        private LocalDateTime lastWatchedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaylistDTO {
        private Long id;
        private String name;
        private Integer movieCount;
        private Boolean hasMovie;  // true nếu movie này đã có trong playlist
    }

    // ========== BUILD METHOD ==========

    public static AggregatedMovieDetailResponse build(
            OphimMovieDetail movieData,
            OphimActorData actorData,
            UserMovieDataDTO userData,
            boolean isAuthenticated,
            Util util) {

        return AggregatedMovieDetailResponse.builder()
                .movieData(movieData)
                .actorData(actorData)
                .isFavorite(userData.isFavorite())
                .favoriteId(userData.getFavoriteId())
                .watchProgress(buildWatchProgress(userData, util))
                .playlistCheckedId(userData.getCheckedPlaylistId())
                .playlists(buildPlaylists(userData.getPlaylists()))
                .isAuthenticated(isAuthenticated)
                .fetchedAt(System.currentTimeMillis())
                .build();
    }

    private static WatchProgressDTO buildWatchProgress(UserMovieDataDTO userData, Util util) {
        if (!userData.hasWatchProgress()) return null;

        return WatchProgressDTO.builder()
                .id(userData.getWatchHistoryId())
                .movieSlug(userData.getMovieSlug())
                .movieName(userData.getMovieName())
                .posterUrl(userData.getPosterUrl())
                .thumbUrl(userData.getThumbUrl())
                .episodeSlug(userData.getEpisodeSlug())
                .episodeName(userData.getEpisodeName())
                .serverName(userData.getServerName())
                .currentTime(userData.getCurrentTime())
                .currentTimeFormatted(util.formatTime(userData.getCurrentTime()))
                .duration(userData.getDuration())
                .durationFormatted(util.formatTime(userData.getDuration()))
                .progressPercent(userData.getProgressPercent())
                .completed(userData.getCompleted())
                .lastWatchedAt(userData.getLastWatchedAt())
                .build();
    }

    private static List<PlaylistDTO> buildPlaylists(List<UserMovieDataDTO.PlaylistItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        return items.stream()
                .map(item -> PlaylistDTO.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .movieCount(item.getMovieCount())
                        .hasMovie(item.getHasMovie())
                        .build())
                .collect(Collectors.toList());
    }
}
