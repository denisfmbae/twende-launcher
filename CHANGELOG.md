package co.nedlink.twende.data.media

import android.service.notification.NotificationListenerService

/**
 * A deliberately empty listener.
 *
 * Twende does not read, store or forward a single notification. This service
 * exists for one reason: Android gates `MediaSessionManager.getActiveSessions()`
 * behind notification-listener access, and that is the only sanctioned way for a
 * third-party app to read what a background media app is playing (track, artist,
 * album art). The alternative permission, MEDIA_CONTENT_CONTROL, is
 * signature|privileged — reserved for apps baked into the system image.
 *
 * If you never grant notification access, the mini player still works: the
 * transport buttons fall back to AudioManager media-key events, which need no
 * permission whatsoever. You simply lose the track title.
 *
 * On exported="true": the service is guarded by BIND_NOTIFICATION_LISTENER_SERVICE,
 * a signature-level permission that only the OS holds. The system server has to be
 * able to bind it, and nothing else can. That's the opposite of the CarLink receiver,
 * which had no permission guard and so was locked down instead.
 */
class TwendeNotificationListener : NotificationListenerService()
