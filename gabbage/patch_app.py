import re

with open("frontend/src/App.tsx", "r") as f:
    lines = f.readlines()

new_lines = []
skip_mode = False
skip_braces = 0

i = 0
while i < len(lines):
    line = lines[i]

    if "import { omnisEngine } from" in line:
        new_lines.append(line)
        new_lines.append('import { MessageBubble } from "./components/MessageBubble";\n')
        i += 1
        continue

    # Remove states
    if "const [expandedThoughts" in line or \
       "const [feedbackRating" in line or \
       "const [feedbackSubmitted" in line or \
       "const [copiedMessageId" in line or \
       "const [refiningMessageId" in line or \
       "const [refinePrompt" in line or \
       "const [isRefining" in line:
        if "{" in line and "}" not in line:
            # Skip multiline state initialization like expandedThoughts
            i += 3
        else:
            i += 1
        continue

    # Remove toggleThoughts and handleCopyText completely
    if "const toggleThoughts =" in line:
        i += 3
        continue
    if "const handleCopyText =" in line:
        i += 5
        continue

    # Modify handleRate
    if "const handleRate =" in line:
        new_lines.append("  const handleRate = useCallback(async (messageId: string, rating: number) => {\n")
        new_lines.append('    try {\n')
        new_lines.append('      await fetch("/api/feedback", {\n')
        new_lines.append('        method: "POST",\n')
        new_lines.append('        headers: { "Content-Type": "application/json" },\n')
        new_lines.append('        body: JSON.stringify({\n')
        new_lines.append('          message_id: messageId,\n')
        new_lines.append('          conversation_id: "00000000-0000-0000-0000-000000000000",\n')
        new_lines.append('          user_rating: rating,\n')
        new_lines.append('          feedback_text: `Uživatel ohodnotil odpověď jako ${rating}/5`,\n')
        new_lines.append('        }),\n')
        new_lines.append('      });\n')
        new_lines.append('    } catch (err) {\n')
        new_lines.append('      console.error(err);\n')
        new_lines.append('    }\n')
        new_lines.append('  }, []);\n')
        i += 19
        continue

    # Modify handleRefineMessage
    if "const handleRefineMessage =" in line:
        new_lines.append("  const handleRefineMessage = useCallback(async (msgId: string, originalContent: string, instruction: string) => {\n")
        new_lines.append("    if (!instruction) return;\n")
        new_lines.append("    try {\n")
        new_lines.append("      const refinementQuery = `[Refaktoruj a vylepšete tento předchozí výstup podle pokynů]:\\nPůvodní výstup:\\n${originalContent}\\n\\nPokyny pro vylepšení: ${instruction}`;\n")
        new_lines.append("      const data = await omnisEngine.processQuery(refinementQuery, ontologyDomain, enableThinking);\n")
        new_lines.append("      const refinedMessage: MessageItem = {\n")
        new_lines.append('        id: "refine-" + Date.now(),\n')
        new_lines.append('        role: "assistant",\n')
        new_lines.append('        content: data.answer,\n')
        new_lines.append('        cognitive_process: `### Refaktoring výstupu [Pokyn: ${instruction}]\\n` + data.cognitive_process,\n')
        new_lines.append('        follow_up_questions: data.follow_up_questions,\n')
        new_lines.append('        impact_matrix: data.impact_matrix,\n')
        new_lines.append('        consequence_forensics: data.consequence_forensics,\n')
        new_lines.append('        token_usage: data.token_usage,\n')
        new_lines.append('        created_at: new Date().toISOString(),\n')
        new_lines.append('      };\n')
        new_lines.append('      setMessages((prev) => [...prev, refinedMessage]);\n')
        new_lines.append('      if (data.token_usage) {\n')
        new_lines.append('        setTokenTelemetry((prev) => ({\n')
        new_lines.append('          total_prompt: prev.total_prompt + data.token_usage!.prompt_tokens,\n')
        new_lines.append('          total_completion: prev.total_completion + data.token_usage!.completion_tokens,\n')
        new_lines.append('          total_cost: prev.total_cost + data.token_usage!.cost_usd,\n')
        new_lines.append('        }));\n')
        new_lines.append('      }\n')
        new_lines.append('    } catch (error) {\n')
        new_lines.append('      console.error(error);\n')
        new_lines.append('    }\n')
        new_lines.append('  }, [ontologyDomain, enableThinking]);\n')
        
        # skip lines until the end of the original handleRefineMessage block
        while True:
            i += 1
            if i >= len(lines): break
            if "} catch (error) {" in lines[i]:
                i += 4
                break
        continue

    # Replace the massive messages.map block
    if "                {messages.map((msg) => {" in line:
        new_lines.append("                {messages.map((msg) => (\n")
        new_lines.append("                  <MessageBubble\n")
        new_lines.append("                    key={msg.id}\n")
        new_lines.append("                    msg={msg}\n")
        new_lines.append("                    onSetActiveTab={setActiveTab}\n")
        new_lines.append("                    onSendQuery={handleSendQuery}\n")
        new_lines.append("                    onRefineMessage={handleRefineMessage}\n")
        new_lines.append("                    onRateMessage={handleRate}\n")
        new_lines.append("                  />\n")
        new_lines.append("                ))}\n")
        
        # Now we skip everything until we hit `                <div ref={messagesEndRef} />`
        while True:
            i += 1
            if i >= len(lines): break
            if "                <div ref={messagesEndRef} />" in lines[i]:
                # Don't add it here, we will add it when we hit it in the main loop, wait, no, just leave it to the next loop iteration
                break
        continue

    new_lines.append(line)
    i += 1

# Ensure useCallback is imported
import_line_idx = -1
for j, l in enumerate(new_lines):
    if "import React, {" in l:
        import_line_idx = j
        break

if import_line_idx != -1 and "useCallback" not in new_lines[import_line_idx]:
    new_lines[import_line_idx] = new_lines[import_line_idx].replace("useState,", "useState, useCallback,")


with open("frontend/src/App.tsx", "w") as f:
    f.writelines(new_lines)
