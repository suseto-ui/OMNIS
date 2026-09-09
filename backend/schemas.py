"""
O.M.N.I.S. Pydantic v2 Schemas
Strict ontology validation, structured outputs, and cognitive state interfaces.
"""

from __future__ import annotations
import uuid
from datetime import datetime
from typing import Any, List, Optional
from pydantic import BaseModel, ConfigDict, Field


class ImpactMatrixScores(BaseModel):
    """Structured 4-dimensional Impact Matrix schema."""
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
        description="Technologická elegance a robustnost (0.0 - 1.0)",
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
