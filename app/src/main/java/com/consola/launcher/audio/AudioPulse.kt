package com.consola.launcher.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Escucha lo que suena en la tele (Spotify, YouTube, lo que sea) y publica graves/medios/agudos ~20 veces
 * por segundo, para que el fondo "baile".
 *
 * Usa el Visualizer de Android sobre la sesión 0, que es la mezcla de todo el audio de la tele.
 * NO graba ni guarda nada: solo mide energía por frecuencia. Pide el permiso de micrófono porque
 * Android lo exige para analizar audio, aunque no se use el micrófono.
 */
class AudioPulse(context: Context) {

    enum class Status { Off, Running, NeedsPermission, Unavailable }

    private val appContext = context.applicationContext
    private val _levels = MutableStateFlow(SILENCE)
    val levels: StateFlow<Bands> = _levels.asStateFlow()
    private val _status = MutableStateFlow(Status.Off)
    val status: StateFlow<Status> = _status.asStateFlow()

    private var visualizer: Visualizer? = null
    // Inicio y protector pueden usarlo a la vez: se apaga recién cuando lo suelta el último.
    private var users = 0
    private var loggedSignal = false

    // Un control de ganancia por zona: graves, medios y agudos tienen volúmenes muy distintos entre sí.
    private val bassGain = AutoGain()
    private val midGain = AutoGain()
    private val trebleGain = AutoGain()

    // Diagnóstico: a los ~5 segundos dejamos en el registro el rango que alcanzaron los graves. Si fuera
    // 0,9–1,0 el fondo no "late" (está siempre prendido); con música real tiene que ir de cerca de 0 a 1.
    private var samples = 0
    private var bassMin = 1f
    private var bassMax = 0f

    fun hasPermission(): Boolean =
        appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    @Synchronized
    fun start() {
        users++
        if (visualizer != null) return
        if (!hasPermission()) {
            _status.value = Status.NeedsPermission
            return
        }
        try {
            val created = Visualizer(OUTPUT_MIX_SESSION)
            val range = Visualizer.getCaptureSizeRange()
            created.captureSize = CAPTURE_SIZE.coerceIn(range[0], range[1])
            created.setDataCaptureListener(listener, Visualizer.getMaxCaptureRate(), false, true)
            created.enabled = true
            visualizer = created
            _status.value = Status.Running
            Log.i(TAG, "Visualizer listo: captura ${created.captureSize}, ${created.samplingRate / 1000} Hz")
        } catch (e: RuntimeException) {
            // Algunas teles mandan el audio por un camino de hardware que no se puede analizar.
            Log.w(TAG, "Esta tele no deja analizar el audio", e)
            _status.value = Status.Unavailable
        }
    }

    @Synchronized
    fun stop() {
        users = (users - 1).coerceAtLeast(0)
        if (users > 0) return
        visualizer?.release()
        visualizer = null
        _levels.value = SILENCE
        if (_status.value == Status.Running) _status.value = Status.Off
    }

    private val listener = object : Visualizer.OnDataCaptureListener {
        override fun onWaveFormDataCapture(visualizer: Visualizer, waveform: ByteArray, samplingRate: Int) = Unit

        override fun onFftDataCapture(visualizer: Visualizer, fft: ByteArray, samplingRate: Int) {
            val raw = AudioLevels.rawBands(fft, samplingRate)
            val target = Bands(bassGain.normalize(raw.bass), midGain.normalize(raw.mid), trebleGain.normalize(raw.treble))
            recordRange(target.bass)
            val previous = _levels.value
            _levels.value = Bands(
                bass = AudioLevels.smooth(previous.bass, target.bass),
                mid = AudioLevels.smooth(previous.mid, target.mid),
                treble = AudioLevels.smooth(previous.treble, target.treble),
            )
            if (!loggedSignal && target.bass + target.mid + target.treble > 0f) {
                loggedSignal = true
                Log.i(TAG, "Recibiendo audio: graves=${target.bass}, medios=${target.mid}, agudos=${target.treble}")
            }
        }
    }

    private fun recordRange(bass: Float) {
        if (samples > DIAGNOSTIC_SAMPLES) return
        bassMin = minOf(bassMin, bass)
        bassMax = maxOf(bassMax, bass)
        if (++samples == DIAGNOSTIC_SAMPLES) Log.i(TAG, "Rango de graves en 5 s: %.2f – %.2f".format(bassMin, bassMax))
    }

    private companion object {
        const val TAG = "ConsolaAudio"
        const val DIAGNOSTIC_SAMPLES = 100
        const val OUTPUT_MIX_SESSION = 0
        const val CAPTURE_SIZE = 256
        val SILENCE = Bands(0f, 0f, 0f)
    }
}
