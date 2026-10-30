package com.example.accessibility

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex

/**
 * OmnisAccessibilityHelper:
 * Poskytuje sémantické modifikátory pro splnění WAI-ARIA standardů a evropské normy o přístupnosti.
 * Zajišťuje podporu pro odečítače obrazovky (TalkBack), bezmyšovou klávesovou/D-pad navigaci a živé zóny.
 */
object OmnisAccessibilityHelper {

    /**
     * Nastaví sémantiku tlačítka s explicitním popisem a rolí.
     */
    fun Modifier.accessibleButton(
        description: String,
        traversalIdx: Float = 0f
    ): Modifier = this.semantics {
        role = Role.Button
        contentDescription = description
        traversalIndex = traversalIdx
    }

    /**
     * Nastaví sémantiku textového pole pro zadávání.
     */
    fun Modifier.accessibleInputField(
        description: String,
        traversalIdx: Float = 0f
    ): Modifier = this.semantics {
        contentDescription = description
        traversalIndex = traversalIdx
    }

    /**
     * Nastaví sémantický nadpis pro snadnou navigaci odečítačem po sekcích.
     */
    fun Modifier.accessibleHeading(
        title: String,
        traversalIdx: Float = 0f
    ): Modifier = this.semantics {
        heading()
        contentDescription = title
        traversalIndex = traversalIdx
    }

    /**
     * Nastaví živou zónu (WAI-ARIA aria-live) pro okamžité hlášení změn stavu (např. chybové hlášky, odpovědi bota).
     */
    fun Modifier.accessibleLiveRegion(
        announcement: String
    ): Modifier = this.semantics {
        liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
        contentDescription = announcement
    }
}
