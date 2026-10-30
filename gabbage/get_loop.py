with open("frontend/src/App.tsx", "r") as f:
    lines = f.readlines()
start_idx = -1
end_idx = -1
for i, l in enumerate(lines):
    if "messages.map((msg) => {" in l:
        start_idx = i
    if start_idx != -1 and i > start_idx + 100:
        if "    </div>" in l and end_idx == -1:
            pass # Keep looking for the end of the map block
        if "  {/* Loading State */}" in l:
            end_idx = i - 2
            break

if start_idx != -1 and end_idx != -1:
    print("".join(lines[start_idx:end_idx]))
