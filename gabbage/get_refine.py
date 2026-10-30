with open("frontend/src/App.tsx", "r") as f:
    lines = f.readlines()
for i, l in enumerate(lines):
    if "const handleRefineMessage =" in l:
        print("".join(lines[i:i+30]))
        break
