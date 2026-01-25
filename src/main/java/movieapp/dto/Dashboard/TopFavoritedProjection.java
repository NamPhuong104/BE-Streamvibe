package movieapp.dto.Dashboard;

public interface TopFavoritedProjection {
    String getMovieSlug();

    String getMovieName();

    String getOriginName();

    String getPosterUrl();

    String getThumbUrl();

    Long getFavoriteCount();
}
