package com.consola.launcher.upload

import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.io.InputStream
import java.net.URLDecoder

sealed interface UploadReply {
    data object Accepted : UploadReply
    data class Rejected(val message: String) : UploadReply
}

/** Lo que el servidor necesita del resto de la app. El servidor solo habla HTTP: no sabe de videos ni de ajustes. */
interface UploadHandler {
    fun receive(originalName: String, length: Long, body: InputStream): UploadReply
    fun statusJson(): String
    fun videosJson(): String
    fun delete(name: String): Boolean

    // Apps a tu gusto y mensaje del protector: el celular como control de la personalización.
    fun appsJson(): String
    fun rename(packageName: String, name: String): Boolean
    fun saveCover(packageName: String, length: Long, body: InputStream): UploadReply
    fun resetCover(packageName: String): Boolean
    fun messageJson(): String
    fun setMessage(text: String): Boolean

    // Widgets que necesitan escribir algo: la ciudad del clima y la cuenta regresiva.
    fun widgetsJson(): String
    /** Devuelve el JSON de respuesta: la ciudad encontrada, o por qué no. Vacío = automática. */
    fun setCity(name: String): String
    fun setCountdown(title: String, date: String): Boolean
}

/**
 * Servidor HTTP que vive SOLO mientras Ajustes → Fondos está abierto.
 * Todas las rutas cuelgan de un código aleatorio ([token]): sin él, un dispositivo de la red
 * no puede subir ni borrar nada aunque encuentre el puerto.
 */
class UploadServer(
    port: Int,
    private val token: String,
    private val page: String,
    private val handler: UploadHandler,
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val base = "/$token"
        val path = session.uri.trimEnd('/')
        val method = session.method
        return when {
            path == base && method == Method.GET -> newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", page)
            path == "$base/status" && method == Method.GET -> json(handler.statusJson())
            path == "$base/videos" && method == Method.GET -> json(handler.videosJson())
            path == "$base/upload" && method == Method.POST -> upload(session)
            path == "$base/delete" && method == Method.POST -> delete(session)
            path == "$base/apps" && method == Method.GET -> json(handler.appsJson())
            path == "$base/rename" && method == Method.POST ->
                ok(handler.rename(session.param("package"), session.param("name")))
            path == "$base/cover" && method == Method.POST -> cover(session)
            path == "$base/cover/reset" && method == Method.POST -> ok(handler.resetCover(session.param("package")))
            path == "$base/message" && method == Method.GET -> json(handler.messageJson())
            path == "$base/message" && method == Method.POST -> ok(handler.setMessage(session.param("text")))
            path == "$base/widgets" && method == Method.GET -> json(handler.widgetsJson())
            path == "$base/city" && method == Method.POST -> json(handler.setCity(session.param("name")))
            path == "$base/countdown" && method == Method.POST ->
                ok(handler.setCountdown(session.param("title"), session.param("date")))
            // Sin el código correcto ni siquiera confirmamos que acá hay algo.
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "No encontrado")
        }
    }

    // El video viaja como el cuerpo entero del POST (no como formulario multipart): lo copiamos a disco
    // a medida que llega, sin cargarlo en memoria. Con 1,8 GB de RAM, un video de 300 MB en memoria no entra.
    private fun upload(session: IHTTPSession): Response {
        val length = session.headers["content-length"]?.toLongOrNull() ?: -1L
        val name = session.headers["x-file-name"]?.let { URLDecoder.decode(it, "UTF-8") }.orEmpty()
        return when (val reply = handler.receive(name, length, session.inputStream)) {
            UploadReply.Accepted -> json(JSONObject().put("ok", true).toString())
            is UploadReply.Rejected -> {
                val body = JSONObject().put("ok", false).put("message", reply.message).toString()
                newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", body).apply {
                    // No leímos el cuerpo: cerramos la conexión para no interpretar el video como el próximo pedido.
                    closeConnection(true)
                }
            }
        }
    }

    // La portada viaja como el cuerpo entero del POST, igual que los videos.
    private fun cover(session: IHTTPSession): Response {
        val length = session.headers["content-length"]?.toLongOrNull() ?: -1L
        return when (val reply = handler.saveCover(session.param("package"), length, session.inputStream)) {
            UploadReply.Accepted -> json(JSONObject().put("ok", true).toString())
            is UploadReply.Rejected -> {
                val body = JSONObject().put("ok", false).put("message", reply.message).toString()
                newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", body).apply { closeConnection(true) }
            }
        }
    }

    private fun IHTTPSession.param(name: String): String = parameters[name]?.firstOrNull().orEmpty()

    private fun ok(success: Boolean): Response = json(JSONObject().put("ok", success).toString())

    private fun delete(session: IHTTPSession): Response {
        val name = session.parameters["name"]?.firstOrNull().orEmpty()
        return json(JSONObject().put("ok", handler.delete(name)).toString())
    }

    private fun json(body: String): Response =
        newFixedLengthResponse(Response.Status.OK, "application/json", body)
}
