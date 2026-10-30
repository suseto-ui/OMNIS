#!/bin/bash
# swegemma_eval.sh - Lokální validace patchů

echo "[ADMIN] Spouštím lokální evaluaci..."

# 1. Kontrola integrity (zda nebyl změněn test)
if [ -d "tests" ]; then
  echo "[ADMIN] Prověřuji izolaci testů..."
  # Jednoduchá kontrola, zda testy neobsahují úpravy v posledních 10 minutách
  if [ -n "$(find tests -mmin -10)" ]; then
    echo "[WARN] Detekována modifikace v testech! Odeslání na Kaggle může být zamítnuto."
  fi
fi

# 2. Spuštění lokálního evaluátoru
echo "[ADMIN] Spouštím swegemma eval..."
swegemma eval --adapter-path ./adapters/adapter_model.safetensors --target-dir ./

if [ $? -eq 0 ]; then
  echo "[SUCCESS] Lokální validace prošla. Patch je připraven k odeslání."
else
  echo "[FAIL] Lokální validace selhala. Revidujte logy."
  exit 1
fi
