import uuid

from sqlalchemy import Column, Integer, String, Float, JSON, DateTime, ForeignKey, Text, Boolean
from sqlalchemy.orm import relationship, declarative_base
from sqlalchemy.sql import func

Base = declarative_base()

class Conversation(Base):
    __tablename__ = "conversations"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    title = Column(String(255), nullable=False)
    ontology_domain = Column(String(100), index=True, default="SYSTEMS_INTELLIGENCE")
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)


class Message(Base):
    __tablename__ = "messages"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    conversation_id = Column(String, ForeignKey("conversations.id"), nullable=False)
    role = Column(String(20), nullable=False)
    content = Column(Text, nullable=False)
    cognitive_thoughts = Column(Text)
    follow_up_questions = Column(JSON)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)


class ImpactMatrixMetric(Base):
    __tablename__ = "impact_matrix_metrics"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    message_id = Column(String, ForeignKey("messages.id"), nullable=False)
    conversation_id = Column(String, ForeignKey("conversations.id"), nullable=False)
    sys = Column(Float, default=0.0)
    econ = Column(Float, default=0.0)
    psych = Column(Float, default=0.0)
    eco = Column(Float, default=0.0)
    law = Column(Float, default=0.0)
    sec = Column(Float, default=0.0)
    phys = Column(Float, default=0.0)
    soc = Column(Float, default=0.0)
    composite_score = Column(Float, default=0.0)
    reasoning = Column(Text, default="")


class VectorMemory(Base):
    __tablename__ = "vector_memories"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    conversation_id = Column(String, ForeignKey("conversations.id"), nullable=True)
    content = Column(Text, nullable=False)
    embedding = Column(JSON, default=list)
    memory_type = Column(String(50), default="semantic")
    metadata_json = Column(JSON, default=dict)
    importance_score = Column(Float, default=1.0)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)


class OmnisSynthesis(Base):
    """
    Hlavní tabulka pro ukládání kognitivních syntéz O.M.N.I.S.
    Ukládá výsledek 5-fázového cyklu.
    """
    __tablename__ = "omnis_syntheses"

    id = Column(Integer, primary_key=True, index=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    query = Column(Text, nullable=False)
    ontology_domain = Column(String(100), index=True)
    
    # Finální výstupy
    formatted_answer = Column(Text, nullable=False)
    cognitive_process = Column(Text)
    composite_score = Column(Float)
    
    # Data z fází uložená jako JSON pro flexibilitu a audit
    phase_data = Column(JSON)  # Obsahuje Phase1 až Phase4 detaily
    
    # Relace
    impact_matrix = relationship("ImpactMatrix", uselist=False, back_populates="synthesis")
    risk_forensics = relationship("RiskForensics", uselist=False, back_populates="synthesis")

class ImpactMatrix(Base):
    """8D Matice dopadů (Oktagon)"""
    __tablename__ = "impact_matrices"

    id = Column(Integer, primary_key=True)
    synthesis_id = Column(Integer, ForeignKey("omnis_syntheses.id"))
    
    sys = Column(Float)
    econ = Column(Float)
    psych = Column(Float)
    eco = Column(Float)
    law = Column(Float)
    sec = Column(Float)
    phys = Column(Float)
    soc = Column(Float)
    
    reasoning = Column(Text)
    adversarial_vulnerabilities = Column(JSON)  # List stringů
    
    synthesis = relationship("OmnisSynthesis", back_populates="impact_matrix")

class RiskForensics(Base):
    """Prospektivní analýza rizik T+N"""
    __tablename__ = "risk_forensics"

    id = Column(Integer, primary_key=True)
    synthesis_id = Column(Integer, ForeignKey("omnis_syntheses.id"))
    
    risk_index = Column(Float)
    risk_level = Column(String(20)) # SAFE, ELEVATED, CRITICAL
    horizon = Column(String(50))
    vectors = Column(JSON) # Identified risk vectors
    
    synthesis = relationship("OmnisSynthesis", back_populates="risk_forensics")