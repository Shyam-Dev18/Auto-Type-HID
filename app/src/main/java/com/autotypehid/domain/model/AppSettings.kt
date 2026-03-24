package com.autotypehid.domain.model

data class AppSettings(
    val profile: String = "NORMAL",
    val speed: Float = 1.0f,
    val typoProbability: Float = 0.18f
)
