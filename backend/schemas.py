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
    """Structured 8-dimensional Impact Matrix schema normalized (0.0 - 1.0)."""
    model_config = ConfigDict(from_attributes=True)

    sys: float = Field(..., ge=0.0, le=1.0, description="Systems Engineering (0.0 - 1.0)")
    econ: float = Field(..., ge=0.0, le=1.0, description="Economics (0.0 - 1.0)")
    psych: float = Field(..., ge=0.0, le=1.0, description="Psychology (0.0 - 1.0)")
    eco: float = Field(..., ge=0.0, le=1.0, description="Ecology (0.0 - 1.0)")
    law: float = Field(..., ge=0.0, le=1.0, description="Law & Regulation (0.0 - 1.0)")
    sec: float = Field(..., ge=0.0, le=1.0, description="Security (0.0 - 1.0)")
    phys: float = Field(..., ge=0.0, le=1.0, description="Physics (0.0 - 1.0)")
    soc: float = Field(..., ge=0.0, le=1.0, description="Sociology (0.0 - 1.0)")
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



class RiskVectorSchema(BaseModel):
    domain: str
    vector: str
    severity: str = "LOW"
    probability: str = "LOW"
    mitigation: str
    cascade_timeline: str = "T+30_days"
    entropy_impact: float = 0.05


class ConsequenceForensicsSchema(BaseModel):
    risk_index: float = Field(0.042, description="Kompozitní index rizika (0.0 až 1.0)")
    risk_level: str = Field("SAFE", description="SAFE, ELEVATED, CRITICAL")
    horizon: str = Field("T+30_days", description="Horizont kaskádových dopadů")
    t_plus_1_systemic_drift: str = Field(
        ..., description="Měření odchylky od First Principles v T+1"
    )
    asymmetric_failure_modes: List[str] = Field(
        default_factory=list, description="Asymetrické módy selhání a SPOF"
    )
    regulatory_compliance_deltas: List[str] = Field(
        default_factory=list, description="Regulační odchylky a compliance delty"
    )
    thermodynamic_entropy_spike: str = Field(
        ..., description="Nárůst entropie a termodynamických nákladů"
    )
    identified_vectors: List[RiskVectorSchema] = Field(
        default_factory=list, description="Jednotlivé identifikované rizikové vektory"
    )
    mitigation_directives: List[str] = Field(
        default_factory=list, description="Okamžité zmírňující direktivy"
    )
    automatic_countermeasure_deployed: bool = Field(
        True, description="Indikátor automatického nasazení protiopatření"
    )


class ChatMessage(BaseModel):
    """Representuje jednu zprávu v historii chatu."""
    role: str  # 'user' nebo 'assistant'
    content: str

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
    history: List[ChatMessage] = Field(default_factory=list, description="Historie konverzace pro udržení kontextu")
    image_data: Optional[str] = Field(None, description="Base64 kódovaná obrazová data pro multimodální analýzu v Gemini 3.1")
    image_mime: Optional[str] = Field("image/jpeg", description="MIME typ obrázku (např. image/jpeg, image/png)")


class TokenUsageStats(BaseModel):
    prompt_tokens: int = Field(0, description="Vstupní tokeny dotazu a systémového promptu")
    completion_tokens: int = Field(0, description="Výstupní tokeny generované odpovědi")
    total_tokens: int = Field(0, description="Celkový součet tokenů pro tento dotaz")
    cost_usd: float = Field(0.0, description="Odhadované finanční náklady v USD")


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
    consequence_forensics: Optional[ConsequenceForensicsSchema] = Field(
        None, description="Prospektivní forenzní analýza rizik T+1 až T+N"
    )
    token_usage: Optional[TokenUsageStats] = Field(
        None, description="Metrika spotřeby tokenů pro tento dotaz"
    )
    adversarial_score: Optional[float] = Field(0.15, description="Skóre oponenta (0.0 až 1.0)")
    flagged_issues: List[str] = Field(default_factory=list, description="Seznam odhalených problémů")
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


class AdversarialReviewRequest(BaseModel):
    query: str
    answer: str
    ontology_domain: str = "SYSTEMS_INTELLIGENCE"


class AdversarialReviewResponse(BaseModel):
    vulnerabilities: List[str]
    critique_summary: str
    adversarial_score: float = Field(0.0, description="Skóre oponenta (0.0 až 1.0)")
    flagged_issues: List[str] = Field(default_factory=list, description="Seznam odhalených problémů")


