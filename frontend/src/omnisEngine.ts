/**
 * O.M.N.I.S. Client-Side Cognitive Architecture Engine (SIGMA-OMEGA)
 * Thin client to communicate securely via XHR to the FastAPI backend.
 */

export interface ImpactMatrixScores {
  sys: number;
  econ: number;
  psych: number;
  eco: number;
  law: number;
  sec: number;
  phys: number;
  soc: number;
  composite_score: number;
  reasoning: string;
  adversarial_vulnerabilities?: string[];
  leverage_point?: string;
}

export interface ConsequenceForensics {
  horizon: string;
  risk_index: number;
  risk_level: string;
  identified_vectors: Array<{
    dimension: string;
    threat_description: string;
    probability: number;
    impact: number;
    mitigation_strategy: string;
  }>;
  t_plus_1_systemic_drift: string;
}

export interface TokenUsageStats {
  prompt_tokens: number;
  completion_tokens: number;
  total_tokens: number;
  cost_usd: number;
}

export interface OmnisCognitiveResult {
  message_id: string;
  answer: string;
  cognitive_process?: string;
  follow_up_questions: string[];
  impact_matrix: ImpactMatrixScores;
  consequence_forensics?: ConsequenceForensics;
  token_usage?: TokenUsageStats;
  created_at: string;
}

export class OmnisEngine {
  public isConfigured(): boolean {
    return true; // Backend handles configuration validation
  }

  public async generateEmbedding(text: string): Promise<number[]> {
    // Simulated fast client-side cosine distance for minimal UI feedback if needed, 
    // although heavy embeddings are moved to backend.
    const vec: number[] = new Array(768).fill(0);
    for (let i = 0; i < text.length; i++) {
      vec[i % 768] += text.charCodeAt(i) * 0.001;
    }
    const norm = Math.sqrt(vec.reduce((a, b) => a + b * b, 0)) || 1;
    return vec.map((v) => v / norm);
  }

  public async processQuery(
    query: string,
    ontologyDomain: string = "SYSTEMS_INTELLIGENCE",
    enableThinking: boolean = true
  ): Promise<OmnisCognitiveResult> {
    try {
      const response = await fetch("/api/query", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          query,
          ontology_domain: ontologyDomain,
          enable_thinking: enableThinking,
        }),
      });

      if (!response.ok) {
        throw new Error(`O.M.N.I.S. Backend Error: ${response.status}`);
      }
      
      const data = await response.json();
      return data;
    } catch (err) {
      console.error("OMNISEngine processQuery failed:", err);
      throw err;
    }
  }
}

export const omnisEngine = new OmnisEngine();
