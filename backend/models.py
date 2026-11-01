import uuid
from sqlalchemy import Column, Integer, BigInteger, String, Float, JSON, DateTime, ForeignKey, Text, Boolean
from sqlalchemy.orm import relationship, declarative_base
from sqlalchemy.sql import func

Base = declarative_base()

class User(Base):
    __tablename__ = "users"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    username = Column(String(100), unique=True, index=True, nullable=False)
    email = Column(String(255), unique=True, index=True, nullable=False)
    hashed_password = Column(String(255), nullable=True)  # Null for Google users
    role = Column(String(50), default="STANDARD_USER")
    is_active = Column(Boolean, default=True)
    google_id = Column(String(255), unique=True, index=True, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    profile = relationship("UserProfile", back_populates="user", uselist=False, cascade="all, delete-orphan")
    conversations = relationship("Conversation", back_populates="user", cascade="all, delete-orphan")

class UserProfile(Base):
    __tablename__ = "user_profiles"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String, ForeignKey("users.id", ondelete="CASCADE"), unique=True, nullable=False)
    avatar_url = Column(String(500), nullable=True)
    birth_date = Column(DateTime, nullable=True)
    preferences_json = Column(JSON, default=dict)
    bio = Column(Text, nullable=True)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    user = relationship("User", back_populates="profile")

class Conversation(Base):
    __tablename__ = "conversations"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String, ForeignKey("users.id", ondelete="CASCADE"), nullable=True) # Transitioning to multi-user
    title = Column(String(255), nullable=False)
    ontology_domain = Column(String(100), index=True, default="SYSTEMS_INTELLIGENCE")
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    user = relationship("User", back_populates="conversations")
    messages = relationship("Message", back_populates="conversation", cascade="all, delete-orphan", order_by="Message.created_at.asc()")

class Message(Base):
    __tablename__ = "messages"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    conversation_id = Column(String, ForeignKey("conversations.id", ondelete="CASCADE"), nullable=False)
    role = Column(String(20), nullable=False)
    content = Column(Text, nullable=False)
    cognitive_thoughts = Column(Text)
    follow_up_questions = Column(JSON)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    conversation = relationship("Conversation", back_populates="messages")

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
    
    formatted_answer = Column(Text, nullable=False)
    cognitive_process = Column(Text)
    composite_score = Column(Float)
    
    phase_data = Column(JSON)
    
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
    adversarial_vulnerabilities = Column(JSON)
    
    synthesis = relationship("OmnisSynthesis", back_populates="impact_matrix")

class RiskForensics(Base):
    """Prospektivní analýza rizik T+N"""
    __tablename__ = "risk_forensics"

    id = Column(Integer, primary_key=True)
    synthesis_id = Column(Integer, ForeignKey("omnis_syntheses.id"))
    
    risk_index = Column(Float)
    risk_level = Column(String(20))
    horizon = Column(String(50))
    vectors = Column(JSON)
    
    synthesis = relationship("RiskForensics", back_populates="synthesis")

# ==========================================================
# Mobilní Synchronizační Tabulky (Mobile API Gateway Sync)
# ==========================================================

class OmnisSyncMessage(Base):
    __tablename__ = "omnis_messages"

    id = Column(BigInteger, primary_key=True, autoincrement=False)
    role = Column(String(50), nullable=False)
    content = Column(Text, nullable=False)
    cognitive_process = Column(Text, default="")
    follow_up_questions = Column(Text, default="")
    val_sys = Column(Float, default=0.5)
    val_econ = Column(Float, default=0.5)
    val_psych = Column(Float, default=0.5)
    val_eco = Column(Float, default=0.5)
    val_law = Column(Float, default=0.5)
    val_sec = Column(Float, default=0.5)
    val_phys = Column(Float, default=0.5)
    val_soc = Column(Float, default=0.5)
    composite_score = Column(Float, default=0.5)
    domain = Column(String(100), default="SYSTEMS_INTELLIGENCE")
    attached_image_path = Column(String(255), nullable=True)
    timestamp = Column(BigInteger, nullable=False)
    defense_tier = Column(String(50), default="APPROVED")
    defense_notes = Column(Text, default="")
    is_synced_to_postgres = Column(Boolean, default=True)
    thread_id = Column(String(100), index=True, default="thread_main")
    thread_title = Column(String(255), default="Hlavní vlákno")
    user_name = Column(String(100), index=True, default="operator")
    synced_at = Column(DateTime(timezone=True), server_default=func.now())

class OmnisSyncMemory(Base):
    __tablename__ = "omnis_memory_records"

    id = Column(BigInteger, primary_key=True, autoincrement=False)
    content = Column(Text, nullable=False)
    domain = Column(String(100), default="SYSTEMS_INTELLIGENCE")
    val_sys = Column(Float, default=0.5)
    val_econ = Column(Float, default=0.5)
    val_psych = Column(Float, default=0.5)
    val_eco = Column(Float, default=0.5)
    val_law = Column(Float, default=0.5)
    val_sec = Column(Float, default=0.5)
    val_phys = Column(Float, default=0.5)
    val_soc = Column(Float, default=0.5)
    importance_score = Column(Float, default=1.0)
    timestamp = Column(BigInteger, nullable=False)
    user_name = Column(String(100), index=True, default="operator")
    synced_at = Column(DateTime(timezone=True), server_default=func.now())

class OmnisSyncTelemetry(Base):
    __tablename__ = "omnis_telemetry"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    level = Column(String(20), nullable=False)
    tag = Column(String(50), nullable=False)
    message = Column(Text, nullable=False)
    metadata_json = Column(Text, default="")
    timestamp = Column(BigInteger, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

class AuditLog(Base):
    __tablename__ = "audit_logs"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    actor_email = Column(String(255), nullable=False)
    target_email = Column(String(255), nullable=False)
    action = Column(String(50), nullable=False)
    details = Column(JSON, default=dict)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
