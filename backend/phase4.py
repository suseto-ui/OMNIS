from fastapi import APIRouter, HTTPException, status
from backend.schemas import ConvergenceEvaluationRequest, ConvergenceEvaluationResponse

phase4_router = APIRouter(prefix="/omnis/phase-4", tags=["OMNIS Phase IV - Convergence"])

@phase4_router.post("/evaluate-matrix", response_model=ConvergenceEvaluationResponse)
async def evaluate_impact_matrix(payload: ConvergenceEvaluationRequest):
    """
    Fáze IV (Synergická Konvergence) dle blueprintu (str. 10-11):
    Deterministický výstupní validátor (Guardrail).
    """
    if not payload.candidates:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Seznam kandidátů pro vyhodnocení je prázdný.",
        )

    rankings = []
    for cand in payload.candidates:
        s = cand.scores
        avg_score = (
            s.economic_viability * 0.3
            + s.tech_elegance * 0.3
            + s.social_ecological_impact * 0.2
            + s.psychological_acceptance * 0.2
        )
        penalty = len(cand.adversarial_vulnerabilities) * 0.5
        final_score = max(0.0, avg_score - penalty)

        rankings.append(
            {
                "candidate_id": cand.candidate_id,
                "title": cand.title,
                "final_score": round(final_score, 2),
                "avg_before_penalty": round(avg_score, 2),
                "vulnerabilities_count": len(cand.adversarial_vulnerabilities),
                "passed": final_score >= payload.minimum_threshold,
                "candidate_object": cand,
            }
        )

    rankings.sort(key=lambda x: x["final_score"], reverse=True)
    passed_candidates = [r for r in rankings if r["passed"]]
    optimal = passed_candidates[0]["candidate_object"] if passed_candidates else None

    return ConvergenceEvaluationResponse(
        selected_optimal_candidate=optimal,
        weighted_rankings=[{k: v for k, v in r.items() if k != "candidate_object"} for r in rankings],
        status=("SUCCESS" if optimal else "WARNING: Žádný kandidát nepřekročil prahovou hodnotu."),
    )