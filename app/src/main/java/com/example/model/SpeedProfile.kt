package com.example.model

enum class SpeedProfilePreset(
    val title: String,
    val speedLimitKmh: Int,
    val subtitle: String,
    val legalStatus: String,
    val isBoost: Boolean = false
) {
    PROFILE_15(
        title = "15 km/h",
        speedLimitKmh = 15,
        subtitle = "Mode Éco Urbain",
        legalStatus = "Conforme toutes zones piétonnes tolérées"
    ),
    PROFILE_20(
        title = "20 km/h",
        speedLimitKmh = 20,
        subtitle = "Norme Allemande / eKFV",
        legalStatus = "Réglementaire Allemagne & Suisse"
    ),
    PROFILE_25(
        title = "25 km/h",
        speedLimitKmh = 25,
        subtitle = "Limite Légale France / UE",
        legalStatus = "Conforme Décret EDPm (25 km/h max)"
    ),
    PROFILE_BOOST(
        title = "Boost (>25)",
        speedLimitKmh = 35,
        subtitle = "Pleine Puissance Moteur 1000W+",
        legalStatus = "Usage Voie Privée Uniquement (Firmware dépendant)",
        isBoost = true
    )
}
