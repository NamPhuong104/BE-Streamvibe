package movieapp.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.dto.MovieDetail.AggregatedMovieDetailResponse;
import movieapp.entity.User;
import movieapp.service.MovieAggregationService;
import movieapp.service.UserService;
import movieapp.util.SecurityUtil;
import movieapp.util.annotation.ApiMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
@Slf4j
public class MovieController {
    private final MovieAggregationService movieAggregationService;
    private final UserService userService;

    @GetMapping("/{slug}/detail")
    @ApiMessage("Fetch movie detail")
    public AggregatedMovieDetailResponse getMovieDetail(@PathVariable String slug) {
        User currentUser = getCurrentUserOrNull();

        try {
            AggregatedMovieDetailResponse response = movieAggregationService.getAggregatedMovieDetail(slug, currentUser);

            if (response.getMovieData() == null || response.getMovieData() == null || response.getMovieData() == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy phim: " + slug);

            return response;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch movie detail"
            );
        }
    }

    private User getCurrentUserOrNull() {
        try {
            String email = SecurityUtil.getCurrentUserLogin().orElse(null);
            if (email == null || email.isEmpty()) return null;

            return userService.handleFindUserByEmailOrUsername(email);
        } catch (Exception e) {
            return null;
        }
    }
}
