-- =====================================================
-- V1__create_user_movie_data_proc.sql
-- Stored Procedure lấy user movie data
-- =====================================================

-- Drop existing function
DROP FUNCTION IF EXISTS get_user_movie_data(BIGINT, VARCHAR);

-- Create function
CREATE OR REPLACE FUNCTION get_user_movie_data(p_user_id BIGINT, p_movie_slug VARCHAR(255))
    RETURNS TABLE(
-- ========== Favorite ==========
                     is_favorite BOOLEAN,
                     favorite_id BIGINT,
                     favorite_created_at TIMESTAMP,

-- ========== Watch Progress (latest by last_watched_at) ==========
                     watch_history_id BIGINT,
                     movie_slug VARCHAR(255),
                     movie_name VARCHAR(255),
                     origin_name VARCHAR(255),
                     poster_url TEXT,
                     thumb_url TEXT,
                     episode_slug VARCHAR(255),
                     episode_name VARCHAR(255),
                     server_name VARCHAR(100),
                     watch_time BIGINT,
                     duration BIGINT,
                     progress_percent DOUBLE PRECISION,
                     completed BOOLEAN,
                     last_watched_at TIMESTAMP,

-- ========== Playlists ==========
                     total_playlist_ids INTEGER,
                     playlists_json TEXT,
                     checked_playlist_id BIGINT
                 )
    LANGUAGE plpgsql
AS $$
DECLARE
    -- Favorite variables
    v_is_favorite BOOLEAN := FALSE;
    v_favorite_id BIGINT;
    v_favorite_created_at TIMESTAMP;

    -- Watch progress variables
    v_watch_history_id BIGINT;
    v_movie_slug VARCHAR(255);
    v_movie_name VARCHAR(255);
    v_origin_name VARCHAR(255);
    v_poster_url TEXT;
    v_thumb_url TEXT;
    v_episode_slug VARCHAR(255);
    v_episode_name VARCHAR(255);
    v_server_name VARCHAR(100);
    v_watch_time BIGINT;
    v_duration BIGINT;
    v_progress_percent DOUBLE PRECISION;
    v_completed BOOLEAN;
    v_last_watched_at TIMESTAMP;

    -- Playlist variables
    v_total_playlists INTEGER := 0;
    v_playlists_json TEXT := '[]';
    v_checked_playlist_id BIGINT;
BEGIN
    -- ========================================
    -- 1. Check Favorite
    -- Table: favorites
    -- Index: idx_favorite_user_created (user_id, created_at DESC)
    -- ========================================
    SELECT TRUE, f.id, f.created_at
    INTO v_is_favorite, v_favorite_id, v_favorite_created_at
    FROM favorites f
    WHERE f.user_id = p_user_id AND f.movie_slug = p_movie_slug
    LIMIT 1;

    -- Default if not found
    IF v_is_favorite IS NULL THEN v_is_favorite := FALSE;
    END IF;

    -- ========================================
    -- 2. Get Latest Watch Progress
    -- Table: watch_history
    -- Index: idx_watch_history_user_movie (user_id, movie_slug, last_watched_at DESC)
    -- ========================================
    SELECT
        wh.id,
        wh.movie_slug,
        wh.movie_name,
        wh.origin_name,
        wh.poster_url,
        wh.thumb_url,
        wh.episode_slug,
        wh.episode_name,
        wh.server_name,
        wh.watch_time,
        wh.duration,
        wh.progress_percent,
        wh.completed,
        wh.last_watched_at
    INTO v_watch_history_id,
        v_movie_slug,
        v_movie_name,
        v_origin_name,
        v_poster_url,
        v_thumb_url,
        v_episode_slug,
        v_episode_name,
        v_server_name,
        v_watch_time,
        v_duration,
        v_progress_percent,
        v_completed,
        v_last_watched_at
    FROM watch_history wh
    WHERE wh.user_id = p_user_id AND wh.movie_slug = p_movie_slug
    ORDER BY wh.Last_watched_at DESC
    LIMIT 1;


    -- ========================================
    -- 3. Count Total Playlists of User
    -- Table: playlists
    -- Index: idx_playlist_user_created (user_id, created_at DESC)
    -- ========================================
    SELECT COUNT(*)
    INTO v_total_playlists
    FROM playlists p
    WHERE p.user_id = p_user_id;

    -- ========================================
    -- 4. Get Playlist ID containing this movie
    -- Tables: playlist_movies JOIN playlists
    -- Index: idx_playlist_movie_slug (movie_slug)
    -- ========================================
    SELECT pm.playlist_id
    INTO v_checked_playlist_id
    FROM playlist_movies pm
    INNER JOIN playlists p ON pm.playlist_id = p.id
    WHERE p.user_id = p_user_id AND pm.movie_slug = p_movie_slug
    LIMIT 1;

    -- ========================================
    -- 5. Get Playlists Array với hasMovie flag
    -- ========================================
    SELECT COALESCE(
                   json_agg(
                           json_build_object(
                                   'id', p.id,
                                   'name', p.name,
                                   'movieCount', COALESCE(p.movie_count, 0),
                                   'hasMovie', CASE WHEN pm.id IS NOT NULL THEN true ELSE false END,
                                   'createdAt', p.created_at
                           ) ORDER BY p.created_at DESC
                   )::TEXT,
                   '[]'
           )
    INTO v_playlists_json
    FROM playlists p
             LEFT JOIN playlist_movies pm ON p.id = pm.playlist_id AND pm.movie_slug = p_movie_slug
    WHERE p.user_id = p_user_id;

    -- ========================================
    -- Return single row result
    -- ========================================
    RETURN QUERY SELECT
                     v_is_favorite,
                     v_favorite_id,
                     v_favorite_created_at,
                     v_watch_history_id,
                     v_movie_slug,
                     v_movie_name,
                     v_origin_name,
                     v_poster_url,
                     v_thumb_url,
                     v_episode_slug,
                     v_episode_name,
                     v_server_name,
                     v_watch_time,
                     v_duration,
                     v_progress_percent,
                     v_completed,
                     v_last_watched_at,
                     v_total_playlists,
                     v_playlists_json,
                     v_checked_playlist_id;

END;
$$;

-- Grant permissions
GRANT EXECUTE ON FUNCTION get_user_movie_data(BIGINT, VARCHAR) TO PUBLIC;