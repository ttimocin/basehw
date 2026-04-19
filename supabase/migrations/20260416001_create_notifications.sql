-- migration: 20260416001_create_notifications.sql
-- Description: Create notifications table and triggers for social actions

-- 1. Create the notifications table
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    recipient_uid TEXT NOT NULL,
    sender_uid TEXT,
    type VARCHAR(50) NOT NULL, -- e.g., 'FOLLOW'
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 2. Create indices for faster reads
CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON public.notifications (recipient_uid);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON public.notifications (created_at DESC);

-- 3. Enable RLS
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

-- 4. RLS Policies
DROP POLICY IF EXISTS notifications_select_own ON public.notifications;
CREATE POLICY notifications_select_own
ON public.notifications
FOR SELECT
TO public
USING (recipient_uid = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, ''));

DROP POLICY IF EXISTS notifications_update_own ON public.notifications;
CREATE POLICY notifications_update_own
ON public.notifications
FOR UPDATE
TO public
USING (recipient_uid = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, ''))
WITH CHECK (recipient_uid = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, ''));

DROP POLICY IF EXISTS notifications_delete_own ON public.notifications;
CREATE POLICY notifications_delete_own
ON public.notifications
FOR DELETE
TO public
USING (recipient_uid = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, ''));

-- Allow insert by triggers (and internally), or from the app if needed
DROP POLICY IF EXISTS notifications_insert_auth ON public.notifications;
CREATE POLICY notifications_insert_auth
ON public.notifications
FOR INSERT
TO public
WITH CHECK (true); -- Trigger will typically handle this, but it's safe since RLS SELECT blocks access to others

-- 5. Trigger for 'FOLLOW' notifications
CREATE OR REPLACE FUNCTION handle_new_follow_notification()
RETURNS TRIGGER AS $$
BEGIN
  -- Insert a notification for the user who was followed
  -- Type: 'FOLLOW'
  INSERT INTO public.notifications (recipient_uid, sender_uid, type, message)
  VALUES (
      NEW.followed_uid, 
      NEW.follower_uid, 
      'FOLLOW', 
      'seni takip etmeye başladı.'
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS on_follow_notification ON public.follows;
CREATE TRIGGER on_follow_notification
AFTER INSERT ON public.follows
FOR EACH ROW EXECUTE FUNCTION handle_new_follow_notification();
