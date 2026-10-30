import re
with open("frontend/src/App.tsx", "r") as f:
    content = f.read()

dummy_state = """
  const [tokenTelemetry, setTokenTelemetry] = useState({ total_prompt: 0, total_completion: 0, total_cost: 0 });
"""

content = content.replace('const [ontologyDomain, setOntologyDomain] = useState("SYSTEMS_INTELLIGENCE");', 
'const [ontologyDomain, setOntologyDomain] = useState("SYSTEMS_INTELLIGENCE");\n' + dummy_state)

dev_lab_str = """<DevPromptLab 
            onExecutePromptInChat={(q, d) => { setOntologyDomain(d); handleSendQuery(q); setActiveTab("chat"); }}
            currentDomain={ontologyDomain}
            onSelectDomain={setOntologyDomain}
            sessionTokenTelemetry={tokenTelemetry}
            onRefreshTelemetry={async () => {}}
            onResetTelemetry={async () => setTokenTelemetry({ total_prompt: 0, total_completion: 0, total_cost: 0 })}
          />"""

content = content.replace('<DevPromptLab />', dev_lab_str)

with open("frontend/src/App.tsx", "w") as f:
    f.write(content)
