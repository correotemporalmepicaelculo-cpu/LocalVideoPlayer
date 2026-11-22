package com.lucas.videoplayer.domain.model

data class Subtitle(
    val path: String,
    val name: String,
    val language: String, // "es", "en", etc.
    val format: SubtitleFormat,
    val isExternal: Boolean, // true = archivo .srt/.vtt separado
    val trackIndex: Int // índice de pista si es interno
) {
    enum class SubtitleFormat {
        SRT, VTT, ASS, SSA, SUB, IDX, UNKNOWN
    }

    companion object {
        fun fromPath(path: String): Subtitle {
            val name = java.io.File(path).nameWithoutExtension
            val ext = java.io.File(path).extension.lowercase()
            val format = when (ext) {
                "srt" -> SubtitleFormat.SRT
                "vtt" -> SubtitleFormat.VTT
                "ass" -> SubtitleFormat.ASS
                "ssa" -> SubtitleFormat.SSA
                "sub" -> SubtitleFormat.SUB
                "idx" -> SubtitleFormat.IDX
                else -> SubtitleFormat.UNKNOWN
            }
            // Intentar detectar idioma del nombre (ej: video.es.srt)
            val langParts = name.split(".").filter { it.length == 2 || it.length == 3 }
            val language = langParts.lastOrNull() ?: "und"
            return Subtitle(
                path = path,
                name = name,
                language = language,
                format = format,
                isExternal = true,
                trackIndex = -1
            )
        }
    }
}