-- =====================================================
-- SCHEMA INITIALIZATION SCRIPT
-- Chạy theo đúng thứ tự dependency
-- =====================================================

-- 1. Tạo bảng ROLES trước (không phụ thuộc bảng nào)
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    priority INTEGER DEFAULT 100,,
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

-- 7. Tạo bảng OPTIMIZED_IMAGES (không phụ thuộc)
CREATE TABLE IF NOT EXISTS optimized_images (
    id BIGSERIAL PRIMARY KEY,
    original_url VARCHAR(500) NOT NULL UNIQUE,
    cloudinary_url VARCHAR(500) NOT NULL,
    image_type VARCHAR(50) NOT NULL,
    cloudinary_public_id VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT uk_slug_image_type UNIQUE (slug, image_type)
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

-- Optimized images indexes
CREATE INDEX IF NOT EXISTS idx_optimized_image_slug ON optimized_images(slug);