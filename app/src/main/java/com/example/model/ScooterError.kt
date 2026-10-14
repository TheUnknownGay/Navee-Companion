package com.example.model

data class ScooterError(
    val code: Int,
    val displayCode: String,
    val title: String,
    val description: String,
    val severity: ErrorSeverity,
    val recommendedAction: String
)

enum class ErrorSeverity {
    INFO,
    WARNING,
    CRITICAL
}

object ScooterErrorDirectory {
    val errorMap: Map<Int, ScooterError> = mapOf(
        0 to ScooterError(
            code = 0,
            displayCode = "OK",
            title = "Système Nominal",
            description = "Aucun défaut détecté par le contrôleur ESC Brightway.",
            severity = ErrorSeverity.INFO,
            recommendedAction = "Fonctionnement normal."
        ),
        1 to ScooterError(
            code = 1,
            displayCode = "E1",
            title = "Défaut Capteur Accélérateur",
            description = "Signal de la gâchette d'accélérateur hors plage de tension valide (0.8V - 4.2V).",
            severity = ErrorSeverity.CRITICAL,
            recommendedAction = "Vérifier le câble de l'accélérateur et le connecteur au niveau du guidon."
        ),
        2 to ScooterError(
            code = 2,
            displayCode = "E2",
            title = "Défaut Capteur Frein",
            description = "Capteur Hall du levier de frein bloqué ou déconnecté.",
            severity = ErrorSeverity.CRITICAL,
            recommendedAction = "Inspecter le capteur magnétique du levier de frein gauche et droit."
        ),
        3 to ScooterError(
            code = 3,
            displayCode = "E3",
            title = "Défaut Capteurs Hall Moteur",
            description = "Perte de signal sur une des trois phases du moteur brushless.",
            severity = ErrorSeverity.CRITICAL,
            recommendedAction = "Vérifier le câblage moteur reliant la roue motrice au contrôleur."
        ),
        4 to ScooterError(
            code = 4,
            displayCode = "E4",
            title = "Protection Surintensité (Overcurrent)",
            description = "Courant moteur supérieur au seuil maximal admissible (>30A crête).",
            severity = ErrorSeverity.WARNING,
            recommendedAction = "Laisser refroidir le moteur, éviter les pentes excessives en mode Boost."
        ),
        5 to ScooterError(
            code = 5,
            displayCode = "E5",
            title = "Erreur Communication UART / BLE",
            description = "Échec de synchronisation entre l'afficheur guidon et le contrôleur ESC (19200 baud).",
            severity = ErrorSeverity.CRITICAL,
            recommendedAction = "Vérifier le câble spiralé dans la potence de direction."
        ),
        6 to ScooterError(
            code = 6,
            displayCode = "E6",
            title = "Surchauffe Contrôleur ESC",
            description = "Température des MOSFETs du contrôleur supérieure à 80°C.",
            severity = ErrorSeverity.WARNING,
            recommendedAction = "Stationner la trottinette à l'ombre et attendre la baisse sous 50°C."
        ),
        7 to ScooterError(
            code = 7,
            displayCode = "E7",
            title = "Surtension ou Sous-tension Batterie",
            description = "Tension du pack en dehors de la plage de sécurité (39V - 55V).",
            severity = ErrorSeverity.CRITICAL,
            recommendedAction = "Recharger immédiatement la batterie ou vérifier le chargeur officiel."
        ),
        8 to ScooterError(
            code = 8,
            displayCode = "E8",
            title = "Surchauffe Moteur",
            description = "La sonde thermique interne du bobinage moteur dépasse le seuil critique (85°C).",
            severity = ErrorSeverity.WARNING,
            recommendedAction = "Réduire la vitesse et ménager le moteur."
        )
    )

    fun getError(code: Int): ScooterError {
        return errorMap[code] ?: ScooterError(
            code = code,
            displayCode = "E$code",
            title = "Code d'erreur Inconnu ($code)",
            description = "Code constructeur Brightway non documenté.",
            severity = ErrorSeverity.WARNING,
            recommendedAction = "Consulter le journal de diagnostic BLE et contacter le support NAVEE."
        )
    }
}
