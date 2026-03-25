-- =====================================================
-- SCHEMA INITIALIZATION SCRIPT
-- Chạy theo đúng thứ tự dependency
-- =====================================================

-- 1. Tạo bảng ROLES trước (không phụ thuộc bảng nào)
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    priority INTEGER DEFAULT 100,
    is_system_role BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP
);

-- 2. Tạo bảng USERS (phụ thuộc roles)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    username VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255),
    avatar_url VARCHAR(500),
    provider VARCHAR(50) DEFAULT 'LOCAL',
    provider_id VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    is_email_verified BOOLEAN DEFAULT FALSE,
    role_id BIGINT NOT NULL,
    refresh_token TEXT,
    reset_password_token VARCHAR(255),
    reset_password_expiry TIMESTAMP,
    verify_email_token VARCHAR(255),
    verify_email_expiry TIMESTAMP,
    pending_email VARCHAR(255),
    change_email_token VARCHAR(255),
    change_email_expiry TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- 3. Tạo bảng FAVORITES (phụ thuộc users)
CREATE TABLE IF NOT EXISTS favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    movie_slug VARCHAR(255) NOT NULL,
    movie_name VARCHAR(255),
    origin_name VARCHAR(255),
    poster_url VARCHAR(500),
    thumb_url VARCHAR(500),
    lang VARCHAR(50),
    quality VARCHAR(50),
    episode_current VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_favorites_user_movie UNIQUE (user_id, movie_slug)
);

-- 4. Tạo bảng PLAYLISTS (phụ thuộc users)
CREATE TABLE IF NOT EXISTS playlists (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    movie_count INTEGER DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_playlists_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 5. Tạo bảng PLAYLIST_MOVIES (phụ thuộc playlists)
CREATE TABLE IF NOT EXISTS playlist_movies (
    id BIGSERIAL PRIMARY KEY,
    playlist_id BIGINT NOT NULL,
    movie_slug VARCHAR(255) NOT NULL,
    movie_name VARCHAR(255),
    origin_name VARCHAR(255),
    poster_url VARCHAR(500),
    thumb_url VARCHAR(500),
    quality VARCHAR(50),
    lang VARCHAR(50),
    episode_current VARCHAR(100),
    added_at TIMESTAMP,
    created_at TIMESTAMP,
    CONSTRAINT fk_playlist_movies_playlist FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    CONSTRAINT uk_playlist_movie UNIQUE (playlist_id, movie_slug)
);

-- 6. Tạo bảng WATCH_HISTORY (phụ thuộc users)
CREATE TABLE IF NOT EXISTS watch_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    movie_slug VARCHAR(255),
    movie_name VARCHAR(255),
    origin_name VARCHAR(255),
    movie_type VARCHAR(50),
    episode_slug VARCHAR(255),
    episode_name VARCHAR(255),
    server_name VARCHAR(100),
    poster_url VARCHAR(500),
    thumb_url VARCHAR(500),
    watch_time BIGINT NOT NULL DEFAULT 0,
    duration BIGINT NOT NULL DEFAULT 0,
    progress_percent DOUBLE PRECISION,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    last_watched_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_watch_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_movie_episode UNIQUE (user_id, movie_slug, episode_slug)
);

-- 7. Tạo bảng BLOCKED_KEYWORDS (không phụ thuộc)
CREATE TABLE IF NOT EXISTS blocked_keywords (
    id BIGSERIAL PRIMARY KEY,
    keyword VARCHAR(100) NOT NULL UNIQUE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 8. Tạo bảng NOTIFICATION (không phụ thuộc)
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(20) NOT NULL,
    target VARCHAR(20) NOT NULL DEFAULT 'ALL',
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    image_url VARCHAR(500),
    action_url VARCHAR(500),
    action_text VARCHAR(100),
    start_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_at TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    pushed BOOLEAN DEFAULT FALSE,
    created_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- INDEXES
-- =====================================================

-- Users indexes
CREATE INDEX IF NOT EXISTS idx_user_refresh_token ON users(refresh_token);
CREATE INDEX IF NOT EXISTS idx_user_reset_password_token ON users(reset_password_token);
CREATE INDEX IF NOT EXISTS idx_user_verify_email_token ON users(verify_email_token);
CREATE INDEX IF NOT EXISTS idx_user_change_email_token ON users(change_email_token);
CREATE INDEX IF NOT EXISTS idx_user_role_id ON users(role_id);
CREATE INDEX IF NOT EXISTS idx_blocked_keyword_active ON blocked_keywords(is_active);

-- Favorites indexes
CREATE INDEX IF NOT EXISTS idx_favorite_user_created ON favorites(user_id, created_at DESC);

-- Playlists indexes
CREATE INDEX IF NOT EXISTS idx_playlist_user_created ON playlists(user_id, created_at DESC);

-- Playlist movies indexes
CREATE INDEX IF NOT EXISTS idx_playlist_movie_playlist_added ON playlist_movies(playlist_id, added_at DESC);
CREATE INDEX IF NOT EXISTS idx_playlist_movie_slug ON playlist_movies(movie_slug);

-- Watch history indexes
CREATE INDEX IF NOT EXISTS idx_watch_history_user_watched ON watch_history(user_id, last_watched_at DESC);
CREATE INDEX IF NOT EXISTS idx_watch_history_user_movie ON watch_history(user_id, movie_slug, last_watched_at DESC);
CREATE INDEX IF NOT EXISTS idx_watch_history_user_completed ON watch_history(user_id, completed, last_watched_at DESC);

-- Create function
CREATE OR REPLACE FUNCTION get_user_movie_data(p_user_id BIGINT, p_movie_slug VARCHAR(255))
    RETURNS TABLE(
                     is_favorite BOOLEAN,
                     favorite_id BIGINT,
                     favorite_created_at TIMESTAMP,
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
                     total_playlist_ids INTEGER,
                     playlists_json TEXT,
                     checked_playlist_id BIGINT
                 )
    LANGUAGE plpgsql
AS $$
DECLARE
    v_is_favorite BOOLEAN := FALSE;
    v_favorite_id BIGINT;
    v_favorite_created_at TIMESTAMP;
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
    v_total_playlists INTEGER := 0;
    v_playlists_json TEXT := '[]';
    v_checked_playlist_id BIGINT;
BEGIN
    SELECT TRUE, f.id, f.created_at
    INTO v_is_favorite, v_favorite_id, v_favorite_created_at
    FROM favorites f
    WHERE f.user_id = p_user_id AND f.movie_slug = p_movie_slug
    LIMIT 1;

    IF v_is_favorite IS NULL THEN v_is_favorite := FALSE;
    END IF;

    SELECT wh.id, wh.movie_slug, wh.movie_name, wh.origin_name,
           wh.poster_url, wh.thumb_url, wh.episode_slug, wh.episode_name,
           wh.server_name, wh.watch_time, wh.duration, wh.progress_percent,
           wh.completed, wh.last_watched_at
    INTO v_watch_history_id, v_movie_slug, v_movie_name, v_origin_name,
        v_poster_url, v_thumb_url, v_episode_slug, v_episode_name,
        v_server_name, v_watch_time, v_duration, v_progress_percent,
        v_completed, v_last_watched_at
    FROM watch_history wh
    WHERE wh.user_id = p_user_id AND wh.movie_slug = p_movie_slug
    ORDER BY wh.last_watched_at DESC
    LIMIT 1;

    SELECT COUNT(*) INTO v_total_playlists
    FROM playlists p WHERE p.user_id = p_user_id;

    SELECT pm.playlist_id INTO v_checked_playlist_id
    FROM playlist_movies pm
    INNER JOIN playlists p ON pm.playlist_id = p.id
    WHERE p.user_id = p_user_id AND pm.movie_slug = p_movie_slug
    LIMIT 1;

    SELECT COALESCE(
        json_agg(
            json_build_object(
                'id', p.id,
                'name', p.name,
                'movieCount', COALESCE(p.movie_count, 0),
                'hasMovie', CASE WHEN pm.id IS NOT NULL THEN true ELSE false END,
                'createdAt', p.created_at
            ) ORDER BY p.created_at DESC
        )::TEXT, '[]'
    )
    INTO v_playlists_json
    FROM playlists p
    LEFT JOIN playlist_movies pm ON p.id = pm.playlist_id AND pm.movie_slug = p_movie_slug
    WHERE p.user_id = p_user_id;

    RETURN QUERY SELECT
        v_is_favorite, v_favorite_id, v_favorite_created_at,
        v_watch_history_id, v_movie_slug, v_movie_name, v_origin_name,
        v_poster_url, v_thumb_url, v_episode_slug, v_episode_name,
        v_server_name, v_watch_time, v_duration, v_progress_percent,
        v_completed, v_last_watched_at,
        v_total_playlists, v_playlists_json, v_checked_playlist_id;
END;
$$;

GRANT EXECUTE ON FUNCTION get_user_movie_data(BIGINT, VARCHAR) TO PUBLIC;