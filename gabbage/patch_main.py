with open("backend/main.py", "r") as f:
    content = f.read()

content = content.replace(
"""        economic_viability=impact_matrix.economic_viability,
        eco_social_regeneration=impact_matrix.eco_social_regeneration,
        technological_elegance=impact_matrix.technological_elegance,
        psychological_acceptability=impact_matrix.psychological_acceptability,""",
"""        sys=impact_matrix.sys,
        econ=impact_matrix.econ,
        psych=impact_matrix.psych,
        eco=impact_matrix.eco,
        law=impact_matrix.law,
        sec=impact_matrix.sec,
        phys=impact_matrix.phys,
        soc=impact_matrix.soc,"""
)

content = content.replace(
"""            metric.economic_viability = payload.adjusted_matrix.economic_viability
            metric.eco_social_regeneration = payload.adjusted_matrix.eco_social_regeneration
            metric.technological_elegance = payload.adjusted_matrix.technological_elegance
            metric.psychological_acceptability = payload.adjusted_matrix.psychological_acceptability""",
"""            metric.sys = payload.adjusted_matrix.sys
            metric.econ = payload.adjusted_matrix.econ
            metric.psych = payload.adjusted_matrix.psych
            metric.eco = payload.adjusted_matrix.eco
            metric.law = payload.adjusted_matrix.law
            metric.sec = payload.adjusted_matrix.sec
            metric.phys = payload.adjusted_matrix.phys
            metric.soc = payload.adjusted_matrix.soc"""
)

with open("backend/main.py", "w") as f:
    f.write(content)
