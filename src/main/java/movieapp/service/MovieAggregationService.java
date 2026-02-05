package movieapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.dto.MovieDetail.AggregatedMovieDetailResponse;
import movieapp.dto.MovieDetail.OphimActorData;
import movieapp.dto.MovieDetail.UserMovieDataDTO;
import movieapp.dto.MovieDetail.UserMovieDataProjection;
import movieapp.dto.OphimResponse.OphimMovieDetailResponse;
import movieapp.entity.User;
import movieapp.repository.UserMovieDataRepository;
import movieapp.util.Util;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service tổng hợp movie detail từ nhiều nguồn:
 * - OPhim API: movie data, actor data (parallel HTTP calls)
 * - Local DB: user data via Stored Procedure (1 query)
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class MovieAggregationService {
    private final OPhimClientService ophimClient;
    private final UserMovieDataRepository userMovieDataRepository;
    private final Util util;

    /**
     * Lấy aggregated movie detail
     * <p>
     * Flow:
     * 1. Parallel: OPhim movie + OPhim actors + DB procedure
     * 2. Combine results
     * 3. Return response
     *
     * @param slug Movie slug
     * @param user Current user (null nếu guest)
     * @return Aggregated response
     */
    public AggregatedMovieDetailResponse getAggregatedMovieDetail(String slug, User user) {
        try {
            CompletableFuture<OphimMovieDetailResponse> movieFuture =
                    CompletableFuture.supplyAsync(() -> fetchMovieData(slug));
            CompletableFuture<OphimActorData> actorFuture =
                    CompletableFuture.supplyAsync(() -> fetchActorData(slug));
            CompletableFuture<UserMovieDataDTO> userDataFuture =
                    CompletableFuture.supplyAsync(() -> fetchUserData(slug, user));

            CompletableFuture.allOf(movieFuture, actorFuture, userDataFuture)
                    .get(10, TimeUnit.SECONDS);

            // Convert movie data
            AggregatedMovieDetailResponse.OphimMovieDetail movieConvert =
                    new AggregatedMovieDetailResponse.OphimMovieDetail();
            movieConvert.setItem(movieFuture.get().getData().getItem());
            movieConvert.setSeoOnPage(movieFuture.get().getData().getSeoOnPage());

            OphimActorData actorData = actorFuture.get();
            UserMovieDataDTO userData = userDataFuture.get();

            // Pass util để format time
            return AggregatedMovieDetailResponse.build(
                    movieConvert,
                    actorData,
                    userData,
                    user != null,
                    util  // ✅ Truyền util
            );
        } catch (Exception e) {
            log.error("❌ Aggregation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch movie detail: " + slug, e);
        }
    }

    /**
     * Fetch movie từ OPhim
     */
    private OphimMovieDetailResponse fetchMovieData(String slug) {
        try {
            OphimMovieDetailResponse response = ophimClient.getMovieDetail(slug);
            return response;
        } catch (Exception e) {
            log.error("❌ OPhim movie failed: {}", e.getMessage());

            return null;
        }
    }

    /**
     * Fetch actors từ OPhim
     */
    private OphimActorData fetchActorData(String slug) {

        try {
            OphimActorData response = ophimClient.getMovieActors(slug);
            return response;
        } catch (Exception e) {
            log.warn("⚠️ OPhim actors failed (optional): {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fetch user data từ Stored Procedure
     * <p>
     * GUEST: Return default ngay, KHÔNG query DB
     * AUTHENTICATED: Call stored procedure (1 query = 1 connection)
     */
    private UserMovieDataDTO fetchUserData(String slug, User user) {
        // ========================================
        // GUEST → Return default immediately
        // ========================================
        if (user == null)
            return UserMovieDataDTO.guest();

        // ========================================
        // AUTHENTICATED → Call Stored Procedure
        // ========================================
        try {
            Optional<UserMovieDataProjection> projection = userMovieDataRepository.getUserMovieData(user.getId(), slug);

            return projection.map(UserMovieDataDTO::fromProjection)
                    .orElse(UserMovieDataDTO.guest());
        } catch (Exception e) {
            log.error("❌ Stored Procedure failed: {}", e.getMessage());
            return UserMovieDataDTO.guest();
        }
    }

}
