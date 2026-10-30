import re
with open("frontend/src/App.tsx", "r") as f:
    content = f.read()

content = content.replace("total_prompt: 0, total_completion: 0, total_cost: 0", 
"cumulative_prompt_tokens: 0, cumulative_completion_tokens: 0, cumulative_total_tokens: 0, total_queries_executed: 0, estimated_total_cost_usd: 0, estimated_total_cost_czk: 0, recent_records: []")

with open("frontend/src/App.tsx", "w") as f:
    f.write(content)
