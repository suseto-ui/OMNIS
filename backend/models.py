from sqlalchemy import Column, Integer, String, Float, JSON, DateTime, ForeignKey, Text, Boolean
from sqlalchemy.orm import relationship, declarative_base
from sqlalchemy.sql import func

Base = declarative_base()

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