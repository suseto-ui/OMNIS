"""
O.M.N.I.S. Pydantic v2 Schemas
Strict ontology validation, structured outputs, and cognitive state interfaces.
"""

from __future__ import annotations
import uuid
from datetime import datetime
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, ConfigDict, Field


# ==========================================================
# Epistemic Layer Models (Blueprint Page 3-4)
# ==========================================================

class HardDataParameters(BaseModel):
    metric_name: Optional[str] = "value_and_unit"
    custom_metrics: Dict[str, Any] = Field(default_factory=dict)


class HardData(BaseModel):
    description: str = "Tvrdá numerická data - statistiky, fyzikální měření"
    strict_typing: bool = True
    parameters: Dict[str, Any] = Field(default_factory=dict)


class SoftData(BaseModel):
    description: str = "Měkká data - lidské emoce, sociokulturní kontext"
    strict_typing: bool = False
    parameters: Dict[str, Any] = Field(
        default_factory=lambda: {
            "sociological_vector": "string",
            "psychological_state": "string",
        }
    )


class SensoryHeuristic(BaseModel):
    description: str = "Senzorické vjemy i intuitivní heuristika"
    strict_typing: bool = False
    parameters: Dict[str, Any] = Field(
        default_factory=lambda: {
            "intuition_notes": "string",
            "sensory_input_raw": "string",
        }
    )


class EpistemicDataLayer(BaseModel):
    hard_data: HardData = Field(default_factory=HardData)
    soft_data: SoftData = Field(default_factory=SoftData)
    sensory_heuristic: SensoryHeuristic = Field(default_factory=SensoryHeuristic)


class OmnisEntityIngestion(BaseModel):
    omnis_entity_id: str = Field(default_factory=lambda: f"omnis-{uuid.uuid4().hex[:8]}")
    fundamental_purpose: str = "string_semantic_deconstruction"
    epistemic_data_layer: EpistemicDataLayer = Field(default_factory=EpistemicDataLayer)


# ==========================================================
# Phase IV Convergence & Guardrail Models (Blueprint Page 9-11)
# ==========================================================

class ImpactScore(BaseModel):
    """
    Skórování 0.0 až 10.0 pro každou dimenzi Matice dopadů
    dle přesné specifikace v blueprintu (str. 10).
    """
    economic_viability: float = Field(
        ..., ge=0.0, le=10.0, description="Ekonomická návratnost a udržitelnost (0-10)"
    )
    tech_elegance: float = Field(
        ..., ge=0.0, le=10.0, description="Čistota architektury a škálovatelnost (0-10)"
    )
    social_ecological_impact: float = Field(
        ..., ge=0.0, le=10.0, description="Dopad na prostředí a společnost (0-10)"
    )
    psychological_acceptance: float = Field(
        ..., ge=0.0, le=10.0, description="Míra uživatelského tření a důvěra (0-10)"
    )


class SolutionCandidate(BaseModel):
    candidate_id: str
    title: str
    description: str
    scores: ImpactScore
    adversarial_vulnerabilities: List[str] = Field(default_factory=list)


class ConvergenceEvaluationRequest(BaseModel):
    candidates: List[SolutionCandidate]
    minimum_threshold: float = Field(
        7.0, ge=0.0, le=10.0, description="Minimální průměrné skóre pro akceptaci"
    )


class ConvergenceEvaluationResponse(BaseModel):
    selected_optimal_candidate: Optional[SolutionCandidate]
    weighted_rankings: List[dict]
    status: str


# ==========================================================
# Standard 4D Impact Matrix (Normalized 0.0 - 1.0)
# ==========================================================

class ImpactMatrixScores(BaseModel):
    """Structured 4-dimensional Impact Matrix schema normalized (0.0 - 1.0)."""
    model_config = ConfigDict(from_attributes=True)

    economic_viability: float = Field(
        ...,
        ge=0.0,
        le=1.0,
        description="Ekonomická životaschopnost (0.0 - 1.0)",
    )
    eco_social_regeneration: float = Field(
        ...,
        ge=0.0,
        le=1.0,
        description="Ekologicko-sociální regenerace (0.0 - 1.0)",
    )
    technological_elegance: float = Field(
        ...,
        ge=0.0,
        le=1.0,
        description="Technologická elegance a modularita (0.0 - 1.0)",
    )
    psychological_acceptability: float = Field(
        ...,
        ge=0.0,
        le=1.0,
        description="Psychologická a etická přijatelnost (0.0 - 1.0)",
    )
    composite_score: float = Field(
        ...,
        ge=0.0,
        le=1.0,
        description="Celkový integrální index harmonie a dopadu",
    )
    reasoning: str = Field(
        ...,
        description="Stručné zdůvodnění a ontologická analýza jednotlivých dimenzí",
    )
    adversarial_vulnerabilities: List[str] = Field(
        default_factory=list,
        description="Detekovaná systémová rizika a slabiny z Red-Teamingu",
    )
    leverage_point: Optional[str] = Field(
        None,
        description="Identifikovaný uzlový bod pro maximální pákový efekt",
    )



class QueryRequest(BaseModel):
    """User prompt query with ontological parameters."""
    query: str = Field(..., min_length=2, max_length=10000, description="Uživatelský dotaz")
    conversation_id: Optional[uuid.UUID] = Field(None, description="ID existující konverzace")
    ontology_domain: str = Field(
        default="SYSTEMS_INTELLIGENCE",
        description="Ontologická doména: SYSTEMS_INTELLIGENCE, CYBERNETICS, SUSTAINABLE_TECH, COGNITIVE_SCI",
    )
    enable_thinking: bool = Field(
        default=True, description="Povolit hloubkový introspektivní kognitivní proces"
    )


class QueryResponse(BaseModel):
    """Complete O.M.N.I.S. synthesized response."""
    conversation_id: uuid.UUID
    message_id: uuid.UUID
    answer: str
    cognitive_process: Optional[str] = Field(
        None, description="Introspektivní kognitivní proud myšlenek systému"
    )
    follow_up_questions: List[str] = Field(
        default_factory=list, description="Reflexivní doplňující otázky"
    )
    impact_matrix: ImpactMatrixScores
    related_memories_count: int = 0
    created_at: datetime


class AutopoieticFeedbackRequest(BaseModel):
    """Feedback payload for self-referential autopoietic reinforcement."""
    message_id: uuid.UUID
    conversation_id: uuid.UUID
    user_rating: int = Field(..., ge=1, le=5, description="Subjektivní valence odpovědi (1-5)")
    feedback_text: Optional[str] = Field(None, description="Komentář k adaptaci sítě")
    adjusted_matrix: Optional[ImpactMatrixScores] = Field(
        None, description="Případné manuální korekce matice dopadů"
    )


class AutopoieticFeedbackResponse(BaseModel):
    status: str
    adaptation_delta: float
    message: str


class MemoryItem(BaseModel):
    id: uuid.UUID
    content: str
    memory_type: str
    importance_score: float
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class ConversationDetail(BaseModel):
    id: uuid.UUID
    title: str
    ontology_domain: str
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)
