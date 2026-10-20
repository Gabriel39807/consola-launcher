package com.consola.launcher.widgets

import android.service.notification.NotificationListenerService

/**
 * No hace nada con las notificaciones: existe porque Android solo deja leer "qué está sonando"
 * (MediaSessionManager.getActiveSessions) a las apps con acceso a notificaciones.
 * Ese acceso se da en Ajustes de la tele → Privacidad → Acceso a notificaciones, o por ADB con debloat.ps1.
 */
class MediaListener : NotificationListenerService()
