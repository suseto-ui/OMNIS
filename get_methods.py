with open("frontend/src/App.tsx", "r") as f:
    lines = f.readlines()
for i, l in enumerate(lines):
    if "const toggleThoughts =" in l: print(f"toggleThoughts: {i+1}")
    if "const handleCopyText =" in l: print(f"handleCopyText: {i+1}")
    if "const handleRate =" in l: print(f"handleRate: {i+1}")
    if "const handleRefineMessage =" in l: print(f"handleRefine: {i+1}")
