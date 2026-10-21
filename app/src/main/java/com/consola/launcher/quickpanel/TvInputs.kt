package com.consola.launcher.quickpanel

enum class InputKind { Hdmi, Composite, Tuner, Other }

/** Una entrada de la tele (HDMI 1, AV, antena...). [port] ordena los HDMI aunque les hayas puesto nombre. */
data class InputEntry(
    val id: String,
    val label: String,
    val kind: InputKind,
    val connected: Boolean,
    val port: Int? = null,
)

/** El orden en que se muestran: como vienen en el control y en la tele. Puro: se testea sin Android. */
object TvInputs {
    fun ordered(inputs: List<InputEntry>): List<InputEntry> =
        inputs.sortedWith(compareBy<InputEntry>({ it.kind.ordinal }, { it.port ?: Int.MAX_VALUE }, { it.label }))
}
