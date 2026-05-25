package com.example.util

object Translation {
    private val enMap = mapOf(
        "app_title" to "SatFinder Standard",
        "search_hint" to "Search satellites...",
        "gps_status_active" to "GPS Active",
        "gps_status_manual" to "Manual Coordinates",
        "satellite" to "Satellite",
        "longitude" to "Longitude",
        "azimuth" to "Azimuth",
        "elevation" to "Elevation",
        "polarization" to "LNB Polarization",
        "user_pos" to "User Location",
        "set_manual_pos" to "Set Position Manually",
        "tab_list" to "List",
        "tab_pointer" to "Pointer",
        "tab_settings" to "Settings",
        "btn_add_manual" to "Add Manual",
        "dialog_add_title" to "Add Custom Satellite",
        "field_name" to "Name",
        "field_longitude" to "Longitude (-West, +East)",
        "btn_cancel" to "Cancel",
        "btn_add" to "Add",
        "header_offsets" to "Manual Offsets",
        "header_lang" to "Languages",
        "phone_header" to "Device Sensors in Real Time",
        "alignment_success" to "Aligned!",
        "alignment_hunting" to "Aligning device...",
        "instr_pointing" to "Move the phone so that the blue/green dot is on the yellow cross center (0,0).",
        "instr_pointing_sub" to "Blue dot: phone direction. Yellow: safe satellite center.",
        "delete_tooltip" to "Delete Custom Satellite",
        "compass_north" to "N",
        "compass_south" to "S",
        "compass_east" to "E",
        "compass_west" to "W",
        "location_perm_request" to "Please authorize GPS access to track real-time alignment.",
        "location_perm_btn" to "Authorize GPS"
    )

    private val frMap = mapOf(
        "app_title" to "SatFinder Standard",
        "search_hint" to "Rechercher...",
        "gps_status_active" to "A-GPS Actif",
        "gps_status_manual" to "Coordonnées manuelles",
        "satellite" to "Satellite",
        "longitude" to "Longitude",
        "azimuth" to "Azimut",
        "elevation" to "Élévation",
        "polarization" to "Polarisation LNB",
        "user_pos" to "Position Utilisateur",
        "set_manual_pos" to "Modifier position",
        "tab_list" to "Satellites",
        "tab_pointer" to "Pointage",
        "tab_settings" to "Paramètres",
        "btn_add_manual" to "Ajout Manuel",
        "dialog_add_title" to "Nouveau Satellite",
        "field_name" to "Nom",
        "field_longitude" to "Longitude (négatif Ouest / positif Est)",
        "btn_cancel" to "Annuler",
        "btn_add" to "Ajouter",
        "header_offsets" to "Calibration Manuelle Capteurs",
        "header_lang" to "Sélection Langue",
        "phone_header" to "Capteurs Temps Réel",
        "alignment_success" to "Ajusté !",
        "alignment_hunting" to "Recherche satellite...",
        "instr_pointing" to "Inclinez et tournez le téléphone pour aligner le point bleu sur le point jaune fixe (0,0).",
        "instr_pointing_sub" to "Position : bleu (téléphone). Jaune : satellite visé.",
        "delete_tooltip" to "Supprimer le satellite",
        "compass_north" to "N",
        "compass_south" to "S",
        "compass_east" to "E",
        "compass_west" to "O",
        "location_perm_request" to "Veuillez autoriser l'accès GPS pour calculer l'azimut et l'élévation locale.",
        "location_perm_btn" to "Autoriser GPS"
    )

    fun getString(key: String, language: String): String {
        val map = if (language == "fr") frMap else enMap
        return map[key] ?: enMap[key] ?: key
    }
}
