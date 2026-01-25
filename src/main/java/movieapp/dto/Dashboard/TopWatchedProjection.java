package movieapp.dto.Dashboard;

public interface TopWatchedProjection {
    String getMovieSlug();

    String getMovieName();

    String getOriginName();

    String getPosterUrl();

    String getThumbUrl();

    Long getViewCount();

    Long getTotalWatchTime();
}
