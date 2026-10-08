with open("backend/gemini_service.py", "r") as f:
    content = f.read()

content = content.replace(
"""            '    "economic_viability": 0.88,\n'
            '    "eco_social_regeneration": 0.92,\n'
            '    "technological_elegance": 0.96,\n'
            '    "psychological_acceptability": 0.90,\n'
            '    "composite_score": 0.916,\n'""",
"""            '    "sys": 0.95,\n'
            '    "econ": 0.88,\n'
            '    "psych": 0.91,\n'
            '    "eco": 0.94,\n'
            '    "law": 0.98,\n'
            '    "sec": 0.99,\n'
            '    "phys": 0.87,\n'
            '    "soc": 0.90,\n'
            '    "composite_score": 0.927,\n'"""
)

content = content.replace(
"""                                    "economic_viability": assembled.phase5.economic_viability,
                                    "technological_elegance": assembled.phase5.technological_elegance,
                                    "eco_social_regeneration": assembled.phase5.eco_social_regeneration,
                                    "psychological_acceptability": assembled.phase5.psychological_acceptability,""",
"""                                    "sys": assembled.phase5.sys,
                                    "econ": assembled.phase5.econ,
                                    "psych": assembled.phase5.psych,
                                    "eco": assembled.phase5.eco,
                                    "law": assembled.phase5.law,
                                    "sec": assembled.phase5.sec,
                                    "phys": assembled.phase5.phys,
                                    "soc": assembled.phase5.soc,"""
)

content = content.replace(
"""        matrix = ImpactMatrixScores(
            economic_viability=0.0,
            eco_social_regeneration=0.0,
            technological_elegance=0.0,
            psychological_acceptability=0.0,""",
"""        matrix = ImpactMatrixScores(
            sys=0.0,
            econ=0.0,
            psych=0.0,
            eco=0.0,
            law=0.0,
            sec=0.0,
            phys=0.0,
            soc=0.0,"""
)

with open("backend/gemini_service.py", "w") as f:
    f.write(content)
