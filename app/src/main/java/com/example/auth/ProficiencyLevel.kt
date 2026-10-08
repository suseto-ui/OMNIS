package com.example.auth

/**
 * Kognitivní úroveň pokročilosti v uživatelském rozhraní (Dimenze B).
 * Nezávislá na bezpečnostní roli (UserRole).
 */
enum class ProficiencyLevel {
    /**
     * Režim Začátečník (Lajk):
     * Rychlé nápovědní čipy, zjednodušené formuláře, čistý vizuální výstup bez technického balastu.
     */
    BEGINNER,

    /**
     * Režim Expert (Power User):
     * Pokročilý multiline prompt editor, nastavení parametrů, zobrazení JSON payloadů a 8D matic.
     */
    EXPERT
}
