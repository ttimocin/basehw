-- migration: 20260410001_atomic_counters.sql
-- Description: Implement database triggers for atomic counter updates
-- This replaces manual client-side counter updates for better consistency and security.

-- 1. Gönderi Sayısı (post_count) Tetikleyicisi
CREATE OR REPLACE FUNCTION handle_post_stats() 
RETURNS TRIGGER AS $$
BEGIN
  IF (TG_OP = 'INSERT') THEN
    UPDATE public.profiles SET post_count = post_count + 1 WHERE firebase_uid = NEW.author_uid;
  ELSIF (TG_OP = 'DELETE') THEN
    UPDATE public.profiles SET post_count = GREATEST(0, post_count - 1) WHERE firebase_uid = OLD.author_uid;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS on_post_added_or_deleted ON public.community_posts;
CREATE TRIGGER on_post_added_or_deleted
AFTER INSERT OR DELETE ON public.community_posts
FOR EACH ROW EXECUTE FUNCTION handle_post_stats();


-- 2. Takipçi Sayıları (follower_count & following_count) Tetikleyicisi
CREATE OR REPLACE FUNCTION handle_follow_stats() 
RETURNS TRIGGER AS $$
BEGIN
  IF (TG_OP = 'INSERT') THEN
    UPDATE public.profiles SET following_count = following_count + 1 WHERE firebase_uid = NEW.follower_uid;
    UPDATE public.profiles SET follower_count = follower_count + 1 WHERE firebase_uid = NEW.followed_uid;
  ELSIF (TG_OP = 'DELETE') THEN
    UPDATE public.profiles SET following_count = GREATEST(0, following_count - 1) WHERE firebase_uid = OLD.follower_uid;
    UPDATE public.profiles SET follower_count = GREATEST(0, follower_count - 1) WHERE firebase_uid = OLD.followed_uid;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS on_follow_added_or_deleted ON public.follows;
CREATE TRIGGER on_follow_added_or_deleted
AFTER INSERT OR DELETE ON public.follows
FOR EACH ROW EXECUTE FUNCTION handle_follow_stats();


-- 3. Yorum Sayısı (comment_count) Tetikleyicisi
CREATE OR REPLACE FUNCTION handle_comment_count() 
RETURNS TRIGGER AS $$
BEGIN
  IF (TG_OP = 'INSERT') THEN
    UPDATE public.community_posts SET comment_count = comment_count + 1 WHERE id = NEW.post_id;
  ELSIF (TG_OP = 'DELETE') THEN
    UPDATE public.community_posts SET comment_count = GREATEST(0, comment_count - 1) WHERE id = OLD.post_id;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS on_comment_added_or_deleted ON public.community_comments;
CREATE TRIGGER on_comment_added_or_deleted
AFTER INSERT OR DELETE ON public.community_comments
FOR EACH ROW EXECUTE FUNCTION handle_comment_count();


-- 4. Beğeni Sayısı (like_count) Tetikleyicisi
CREATE OR REPLACE FUNCTION handle_like_count() 
RETURNS TRIGGER AS $$
BEGIN
  IF (TG_OP = 'INSERT') THEN
    UPDATE public.community_posts SET like_count = like_count + 1 WHERE id = NEW.post_id;
  ELSIF (TG_OP = 'DELETE') THEN
    UPDATE public.community_posts SET like_count = GREATEST(0, like_count - 1) WHERE id = OLD.post_id;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS on_like_added_or_deleted ON public.community_likes;
CREATE TRIGGER on_like_added_or_deleted
AFTER INSERT OR DELETE ON public.community_likes
FOR EACH ROW EXECUTE FUNCTION handle_like_count();
