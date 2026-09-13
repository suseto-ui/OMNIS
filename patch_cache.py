import re

with open("backend/main.py", "r") as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    if "query_vec = await cognitive_service.generate_embedding(request.query)" in line:
        new_lines.append(line)
        new_lines.append("""
        # ==============================================================================
        # VOLBA B: PGVECTOR SEMANTIC CACHING (BYPASS LLM)
        # ==============================================================================
        cache_stmt = (
            select(VectorMemory)
            .filter(VectorMemory.memory_type == "semantic_cache")
            .filter(VectorMemory.embedding.cosine_distance(query_vec) < 0.02) # 98% shoda
            .order_by(VectorMemory.embedding.cosine_distance(query_vec))
            .limit(1)
        )
        cache_result = await db.execute(cache_stmt)
        cached_record = cache_result.scalar_one_or_none()

        if cached_record and cached_record.metadata_json:
            import logging
            logging.info(f"⚡ O.M.N.I.S. SEMANTIC CACHE HIT (Bypass Gemini LLM): {request.query}")
            
            c_meta = cached_record.metadata_json
            
            # Vytvorime kopii odpovedi, ale priradime novou message_id
            cached_msg = Message(
                conversation_id=conv_id,
                role="assistant",
                content=c_meta["answer"],
                cognitive_thoughts=c_meta.get("cognitive_process", "") + "\\n\\n[⚡ EXEKUOVÁNO Z PGVECTOR SEMANTIC CACHE: 0ms LATENCE, $0 NÁKLAD]",
                follow_up_questions=c_meta.get("follow_up_questions", []),
            )
            db.add(cached_msg)
            await db.commit()
            
            from backend.schemas import QueryResponse, ImpactMatrixScores, ConsequenceForensicsSchema, TokenUsageStats
            
            im = c_meta.get("impact_matrix", {})
            im_scores = ImpactMatrixScores(
                sys=im.get("sys", 0.5), econ=im.get("econ", 0.5), psych=im.get("psych", 0.5),
                eco=im.get("eco", 0.5), law=im.get("law", 0.5), sec=im.get("sec", 0.5),
                phys=im.get("phys", 0.5), soc=im.get("soc", 0.5), composite_score=im.get("composite_score", 0.5),
                reasoning=im.get("reasoning", "")
            )
            
            cf_dict = c_meta.get("consequence_forensics", {})
            cf_obj = None
            if cf_dict:
                cf_obj = ConsequenceForensicsSchema(
                    horizon=cf_dict.get("horizon", "T+1"),
                    risk_index=cf_dict.get("risk_index", 0.1),
                    risk_level=cf_dict.get("risk_level", "SAFE"),
                    identified_vectors=cf_dict.get("identified_vectors", []),
                    t_plus_1_systemic_drift=cf_dict.get("t_plus_1_systemic_drift", ""),
                    thermodynamic_entropy_spike=cf_dict.get("thermodynamic_entropy_spike", "")
                )
                
            return QueryResponse(
                conversation_id=conv_id,
                message_id=cached_msg.id,
                answer=c_meta["answer"],
                cognitive_process=cached_msg.cognitive_thoughts,
                follow_up_questions=cached_msg.follow_up_questions,
                impact_matrix=im_scores,
                consequence_forensics=cf_obj,
                token_usage=TokenUsageStats(prompt_tokens=0, completion_tokens=0, total_tokens=0, cost_usd=0.0),
                related_memories_count=1,
                created_at=cached_msg.created_at
            )
        # ==============================================================================
""")
    elif "background_tasks.add_task(" in line and "background_record_vector_memory" in lines[i+1]:
        new_lines.append("""
    # ==============================================================================
    # CACHE MISS -> UKLÁDÁNÍ DO SEMANTIC CACHE
    # ==============================================================================
    cache_meta = {
        "answer": answer,
        "cognitive_process": thoughts,
        "follow_up_questions": follow_ups,
        "impact_matrix": impact_matrix.model_dump() if impact_matrix else {},
        "consequence_forensics": consequence_forensics.model_dump() if consequence_forensics else {}
    }
    background_tasks.add_task(
        background_record_vector_memory,
        conversation_id=conv_id,
        content=request.query, # Klicem pro vyhledavani je samotny uzivatelsky dotaz
        memory_type="semantic_cache",
        metadata_json=cache_meta
    )
""")
        new_lines.append(line)
    else:
        new_lines.append(line)

with open("backend/main.py", "w") as f:
    f.writelines(new_lines)
