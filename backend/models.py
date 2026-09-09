"""
O.M.N.I.S. SQLAlchemy 2.0 Domain Models
Includes full pgvector support for semantic memory indexing and Impact Matrix metrics.
"""

from __future__ import annotations
import uuid
from datetime import datetime
from typing import Any, List, Optional
from pgvector.sqlalchemy import Vector
from sqlalchemy import (
    Boolean,
    DateTime,
    Float,
    ForeignKey,
    JSON,
    String,
    Text,
    func,
)
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from .database import Base


class Conversation(Base):
    __tablename__ = "conversations"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
        index=True,
    )
    title: Mapped[str] = mapped_column(String(255), default="Nová relace O.M.N.I.S.")
    ontology_domain: Mapped[str] = mapped_column(
        String(100), default="SYSTEMS_INTELLIGENCE", index=True
    )
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
    )

    # Relationships
    messages: Mapped[List[Message]] = relationship(
        "Message", back_populates="conversation", cascade="all, delete-orphan"
    )
    memories: Mapped[List[VectorMemory]] = relationship(
        "VectorMemory", back_populates="conversation", cascade="all, delete-orphan"
    )
    metrics: Mapped[List[ImpactMatrixMetric]] = relationship(
        "ImpactMatrixMetric", back_populates="conversation", cascade="all, delete-orphan"
    )


class Message(Base):
    __tablename__ = "messages"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
        index=True,
    )
    conversation_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("conversations.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    role: Mapped[str] = mapped_column(String(32), nullable=False)  # "user" | "assistant" | "system"
    content: Mapped[str] = mapped_column(Text, nullable=False)
    cognitive_thoughts: Mapped[Optional[str]] = mapped_column(
        Text, nullable=True, comment="Introspection & high thinking reasoning trace"
    )
    follow_up_questions: Mapped[Optional[List[str]]] = mapped_column(
        JSON, nullable=True, comment="System-generated reflective follow-up questions"
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    # Relationships
    conversation: Mapped[Conversation] = relationship("Conversation", back_populates="messages")
    impact_metric: Mapped[Optional[ImpactMatrixMetric]] = relationship(
        "ImpactMatrixMetric", back_populates="message", uselist=False, cascade="all, delete-orphan"
    )


class VectorMemory(Base):
    """
    Semantic Vector Imprint table storing embeddings for autopoietic memory retrieval.
    Default dimension 768 matches Google GenAI text-embedding-004.
    """
    __tablename__ = "vector_memories"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
        index=True,
    )
    conversation_id: Mapped[Optional[uuid.UUID]] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("conversations.id", ondelete="SET NULL"),
        nullable=True,
        index=True,
    )
    content: Mapped[str] = mapped_column(Text, nullable=False)
    embedding: Mapped[List[float]] = mapped_column(Vector(768), nullable=False)
    memory_type: Mapped[str] = mapped_column(
        String(50), default="semantic", index=True
    )  # "episodic", "semantic", "autopoietic_feedback"
    metadata_json: Mapped[dict[str, Any]] = mapped_column(JSON, default=dict)
    importance_score: Mapped[float] = mapped_column(Float, default=1.0)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    # Relationships
    conversation: Mapped[Optional[Conversation]] = relationship(
        "Conversation", back_populates="memories"
    )


class ImpactMatrixMetric(Base):
    """
    Stores the 4 core dimensions of the O.M.N.I.S. Impact Matrix for every evaluated solution/query:
    1. Ekonomická životaschopnost (Economic Viability)
    2. Ekologicko-sociální regenerace (Eco-Social Regeneration)
    3. Technologická elegance (Technological Elegance)
    4. Psychologická přijatelnost (Psychological Acceptability)
    """
    __tablename__ = "impact_matrix_metrics"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
        index=True,
    )
    message_id: Mapped[Optional[uuid.UUID]] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("messages.id", ondelete="CASCADE"),
        nullable=True,
        index=True,
    )
    conversation_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("conversations.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    economic_viability: Mapped[float] = mapped_column(Float, nullable=False, default=0.5)
    eco_social_regeneration: Mapped[float] = mapped_column(Float, nullable=False, default=0.5)
    technological_elegance: Mapped[float] = mapped_column(Float, nullable=False, default=0.5)
    psychological_acceptability: Mapped[float] = mapped_column(Float, nullable=False, default=0.5)
    composite_score: Mapped[float] = mapped_column(Float, nullable=False, default=0.5)
    reasoning: Mapped[str] = mapped_column(Text, nullable=False, default="")
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now()
    )

    # Relationships
    conversation: Mapped[Conversation] = relationship("Conversation", back_populates="metrics")
    message: Mapped[Optional[Message]] = relationship("Message", back_populates="impact_metric")
