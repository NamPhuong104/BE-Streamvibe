package movieapp.dto.MovieDetail;


import java.time.LocalDateTime;

/**
 * Interface Projection để map kết quả từ Stored Procedure get_user_movie_data
 * <p>
 * QUAN TRỌNG: Tên method phải khớp với tên column trong procedure result!
 * Column: is_favorite → getIsFavorite() hoặc getIs_favorite()
 * <p>
 * Spring Data JPA sẽ tự động map dựa trên naming convention
 */

public interface UserMovieDataProjection {
    // ========== Favorite ==========
    Boolean getIsFavorite();

    Long getFavoriteId();

    LocalDateTime getFavoriteCreatedAt();

    // ========== Watch Progress (ĐẦY ĐỦ) ==========
    Long getWatchHistoryId();

    String getMovieSlug();

    String getMovieName();

    String getOriginName();

    String getPosterUrl();

    String getThumbUrl();

    String getEpisodeSlug();

    String getEpisodeName();

    String getServerName();

    Long getWatchTime();

    Long getDuration();

    Double getProgressPercent();

    Boolean getCompleted();

    LocalDateTime getLastWatchedAt();

    // ========== Playlists ==========
    Integer getTotalPlaylists();

    Long getCheckedPlaylistId();

    String getPlaylistsJson();
}
