"""
Unit tests for O.M.N.I.S. 5-Phase Cognitive Pipeline.
Deterministic verification of each function's specific place in composing the final output.
"""

import unittest
from backend.omnis_pipeline import (
    phase_1_semantic_deconstruction,
    phase_2_transdisciplinary_crossing,
    phase_3_immediate_action_plan,
    phase_4_deterministic_execution,
    phase_5_impact_matrix_and_reflection,
    assemble_omnis_cognitive_cycle,
    Phase1Result,
    Phase2Result,
    Phase3Result,
    Phase4Result,
    Phase5Result,
    OmnisAssemblyOutput,
)
from backend.risk_forensics import (
    ConsequenceRiskAnalyzer,
    ConsequenceForensicsResult,
    RiskVectorItem,
)


class TestOmnisFivePhases(unittest.TestCase):
    """Test suite verifying the 5 functions composing O.M.N.I.S. output."""

    def setUp(self):
        self.sample_query = "Jak navrhnout distribuovaný caching systém s nulovou latencí a minimálními náklady?"
        self.sample_domain = "SYSTEMS_INTELLIGENCE"

    def test_phase_1_semantic_deconstruction(self):
        """Ověření Fáze 1: Očištění dotazu, rozpad na prvočinitele a typování parametrů."""
        res = phase_1_semantic_deconstruction(self.sample_query, self.sample_domain)
        self.assertIsInstance(res, Phase1Result)
        self.assertEqual(res.ontology_domain, self.sample_domain)
        self.assertIn("caching", res.cleaned_query)
        self.assertTrue(len(res.identified_assumptions) > 0)
        self.assertIn("input_length", res.hard_data_inferred)
        self.assertIn("urgency_detected", res.soft_data_inferred)

    def test_phase_2_transdisciplinary_crossing(self):
        """Ověření Fáze 2: Propojení 8 transdisciplinárních domén a izolace pákového uzlového bodu."""
        p1 = phase_1_semantic_deconstruction(self.sample_query, self.sample_domain)
        res = phase_2_transdisciplinary_crossing(p1)
        self.assertIsInstance(res, Phase2Result)
        # Ověření přítomnosti všech 8 rozšířených domén
        expected_domains = [
            "Systémové inženýrství & Kybernetika",
            "Teorie her & Asymetrická ekonomie",
            "Kognitivní vědy & Neuro-ergonomie",
            "Regenerativní dynamika & Ekologie",
            "Regulace, Právo & AI Governance",
            "Zero-Trust Bezpečnost & Kryptografie",
            "Fyzikální termodynamika & Výpočetní efektivita",
            "Socio-kulturní dynamika & Etická rezonance",
        ]
        for domain in expected_domains:
            self.assertIn(domain, res.domain_mappings, f"Chybí doména: {domain}")
        self.assertEqual(len(res.domain_mappings), 8)
        self.assertTrue("Uzlový bod" in res.leverage_point or "Leverage Point" in res.leverage_point)
        self.assertTrue(len(res.nonlinear_synergies) >= 4)

    def test_phase_3_immediate_action_plan(self):
        """Ověření Fáze 3: Win-Win-Win formulace a milníky s minimálním úsilím."""
        p1 = phase_1_semantic_deconstruction(self.sample_query, self.sample_domain)
        p2 = phase_2_transdisciplinary_crossing(p1)
        res = phase_3_immediate_action_plan(p1, p2)
        self.assertIsInstance(res, Phase3Result)
        self.assertIn("Win-Win-Win", res.win_win_win_rationale)
        self.assertTrue(len(res.strategic_milestones) >= 3)
        self.assertIn("páka", res.effort_to_leverage_ratio.lower())

    def test_phase_4_deterministic_execution(self):
        """Ověření Fáze 4: Generování konkrétního exekučního artefaktu (kód, architektura)."""
        p1 = phase_1_semantic_deconstruction(self.sample_query, self.sample_domain)
        p2 = phase_2_transdisciplinary_crossing(p1)
        p3 = phase_3_immediate_action_plan(p1, p2)
        res = phase_4_deterministic_execution(p1, p2, p3)
        self.assertIsInstance(res, Phase4Result)
        self.assertTrue(len(res.execution_steps) >= 3)
        self.assertIn("EXECUTION_INVARIANT", res.concrete_output)
        self.assertIn(self.sample_domain, res.concrete_output)

    def test_phase_5_impact_matrix_and_reflection(self):
        """Ověření Fáze 5: Výpočet 4D matice s váhami 0.3/0.3/0.2/0.2 a penalizací."""
        p1 = phase_1_semantic_deconstruction(self.sample_query, self.sample_domain)
        p2 = phase_2_transdisciplinary_crossing(p1)
        p3 = phase_3_immediate_action_plan(p1, p2)
        p4 = phase_4_deterministic_execution(p1, p2, p3)

        raw_scores = {
            "economic_viability": 0.90,
            "technological_elegance": 0.95,
            "eco_social_regeneration": 0.85,
            "psychological_acceptability": 0.90,
        }
        res = phase_5_impact_matrix_and_reflection(
            p1, p2, p3, p4, raw_scores=raw_scores, parsed_vulnerabilities=["Riziko latence"]
        )
        self.assertIsInstance(res, Phase5Result)
        # Expected: (0.90*0.3) + (0.95*0.3) + (0.85*0.2) + (0.90*0.2) - (1 * 0.02)
        # = 0.27 + 0.285 + 0.17 + 0.18 - 0.02 = 0.885
        self.assertAlmostEqual(res.composite_score, 0.885, places=2)
        self.assertTrue(0.0 <= res.composite_score <= 1.0)
        self.assertTrue(len(res.reflexive_questions) >= 3)
        self.assertIn("Riziko latence", res.adversarial_vulnerabilities)

    def test_master_orchestrator_assembly(self):
        """Ověření celého kognitivního cyklu O.M.N.I.S. – sekvenční složení všech 5 fází."""
        output = assemble_omnis_cognitive_cycle(
            raw_query=self.sample_query,
            ontology_domain=self.sample_domain,
            context_memories=["Paměť: Předchozí cache analýza s Redis"],
        )
        self.assertIsInstance(output, OmnisAssemblyOutput)
        self.assertIsNotNone(output.phase1)
        self.assertIsNotNone(output.phase2)
        self.assertIsNotNone(output.phase3)
        self.assertIsNotNone(output.phase4)
        self.assertIsNotNone(output.phase5)
        self.assertIn("1. Sémantická dekonstrukce", output.formatted_answer)
        self.assertIn("2. Transdisciplinární křížení", output.formatted_answer)
        self.assertIn("3. Okamžitý akční plán", output.formatted_answer)
        self.assertIn("4. Výstup & Exekuce", output.formatted_answer)
        self.assertIn("5. Autopoietická reflexe & 4D Matice dopadů", output.formatted_answer)
        self.assertTrue(len(output.follow_up_questions) >= 3)
        self.assertTrue(0.0 <= output.composite_score <= 1.0)
        self.assertIsNotNone(output.risk_forensics)
        self.assertIsInstance(output.risk_forensics, ConsequenceForensicsResult)
        self.assertTrue(output.risk_forensics.risk_level in ["SAFE", "ELEVATED", "CRITICAL"])
        self.assertTrue(len(output.risk_forensics.identified_vectors) >= 4)

    def test_consequence_risk_forensics_direct(self):
        """Ověření modulu ConsequenceRiskAnalyzer: evaluace T+1 až T+N, SPOF, entropie a mitigace."""
        analyzer = ConsequenceRiskAnalyzer(
            primary_payload={"query": self.sample_query, "composite_score": 0.88},
            domain_context={"ontology_domain": self.sample_domain},
            parsed_forensics={
                "risk_index": 0.042,
                "horizon": "T+30_days",
                "t_plus_1_systemic_drift": "Stabilní",
                "asymmetric_failure_modes": ["SPOF-01: Latence v cache invalidaci"],
                "regulatory_compliance_deltas": ["Plný soulad"],
                "thermodynamic_entropy_spike": "+0.015 J/op",
            },
        )
        res = analyzer.evaluate_cascade_effects()
        self.assertIsInstance(res, ConsequenceForensicsResult)
        self.assertEqual(res.risk_level, "SAFE")
        self.assertEqual(res.risk_index, 0.042)
        self.assertEqual(res.horizon, "T+30_days")
        self.assertIn("SPOF-01", res.asymmetric_failure_modes[0])
        self.assertTrue(len(res.mitigation_directives) >= 3)
        self.assertTrue(res.automatic_countermeasure_deployed)

    def test_token_telemetry_service(self):
        """Ověření výpočtu a sledování tokenů v TokenService."""
        from backend.token_service import token_telemetry_service
        token_telemetry_service.reset_telemetry()
        
        # Test odhadu textu
        tokens = token_telemetry_service.estimate_text_tokens("Testovací dotaz pro O.M.N.I.S.")
        self.assertGreater(tokens, 3)
        
        # Test odhadu celého dotazu s kontextem
        est = token_telemetry_service.estimate_query_tokens(self.sample_query)
        self.assertIn("estimated_prompt_tokens", est)
        self.assertIn("estimated_completion_tokens", est)
        self.assertIn("estimated_total_tokens", est)
        self.assertGreater(est["estimated_total_tokens"], 1200)
        self.assertGreater(est["estimated_cost_usd"], 0.0)

        # Test záznamu exekuce
        rec = token_telemetry_service.record_usage(1250, 1400, "Query test", "SYSTEMS_INTELLIGENCE")
        self.assertEqual(rec.total_tokens, 2650)
        self.assertGreater(rec.cost_usd, 0.0)

        # Test kumulativní telemetrie
        telemetry = token_telemetry_service.get_telemetry()
        self.assertEqual(telemetry.cumulative_prompt_tokens, 1250)
        self.assertEqual(telemetry.cumulative_completion_tokens, 1400)
        self.assertEqual(telemetry.cumulative_total_tokens, 2650)
        self.assertEqual(telemetry.total_queries_executed, 1)
        self.assertGreater(telemetry.estimated_total_cost_usd, 0.0)


if __name__ == "__main__":
    unittest.main()
