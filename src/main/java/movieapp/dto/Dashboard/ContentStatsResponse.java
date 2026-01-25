package movieapp.dto.Dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentStatsResponse {
    private long totalFavorites;
    private long totalPlaylists;
    private long totalPlaylistMovies;
    private long totalWatchTimeSeconds;
    private String totalWatchTimeFormatted;
    private long completedWatches;
    private double avgCompletionRate;
    private long uniqueMoviesWatched;
    private long uniqueMoviesFavorited;
}
