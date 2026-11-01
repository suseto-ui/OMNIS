package com.example.ui.octagon

import androidx.compose.ui.graphics.Color
import com.example.data.OmnisRecord

data class DimensionDefinition(
    val key: String,
    val name: String,
    val desc: String,
    val color: Color,
    val getter: (OmnisRecord) -> Float
)

val OMNIS_8D_DIMENSIONS = listOf(
    DimensionDefinition("Sys", "Systémové inž.", "Modularita a kybernetika", Color(0xFF60A5FA)) { it.valSys },
    DimensionDefinition("Econ", "Ekonomie", "Nákladová efektivita", Color(0xFFFBBF24)) { it.valEcon },
    DimensionDefinition("Psych", "Kognice & Etika", "Důvěra operátora", Color(0xFFC084FC)) { it.valPsych },
    DimensionDefinition("Eco", "Ekologie", "Regenerativní biosféra", Color(0xFF34D399)) { it.valEco },
    DimensionDefinition("Law", "Právo & Soulad", "Legislativa a normy", Color(0xFFFB7185)) { it.valLaw },
    DimensionDefinition("Sec", "Zero-Trust", "Bezpečnost a rizika", Color(0xFFEF4444)) { it.valSec },
    DimensionDefinition("Phys", "Termodynamika", "Fyzikální limity", Color(0xFFFB923C)) { it.valPhys },
    DimensionDefinition("Soc", "Sociální dopad", "Kulturní dynamika", Color(0xFFF472B6)) { it.valSoc }
)
