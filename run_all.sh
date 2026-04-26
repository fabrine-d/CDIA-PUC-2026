#!/usr/bin/env bash
# run_all.sh — Executa todos os experimentos do Duelo de Contextos
#
# Uso: chmod +x run_all.sh && ./run_all.sh
#
# AVISO: T2 e P2 (com sincronizacao) executam 1 bilhao de operacoes
# de lock/semaforo. Podem levar vários minutos por configuracao.

set -euo pipefail

# Garantir finais de linha Unix neste script e em plot.py (seguro rodar múltiplas vezes)
for _f in "$0" plot.py; do
    if [ -f "$_f" ] && command -v sed > /dev/null 2>&1; then
        sed -i 's/\r//' "$_f" 2>/dev/null || true
    fi
done

RESULTS_FILE="results.txt"
SEP="================================================================"

header() {
    echo ""
    echo "$SEP"
    echo "  $1"
    echo "$SEP"
}

# ------------------------------------------------------------------ #
#  Compilacao                                                          #
# ------------------------------------------------------------------ #
header "COMPILANDO"
make all
echo "OK: binarios 'threads' e 'processes' gerados."

# Limpar resultados anteriores
> "$RESULTS_FILE"

# ------------------------------------------------------------------ #
#  Threads                                                             #
# ------------------------------------------------------------------ #
header "PARTE A — THREADS"

echo ""
echo "--- T1: sem sincronizacao (race condition esperada) ---"
for N in 2 4 8; do
    { time ./threads "$N" T1 | tee -a "$RESULTS_FILE" ; } 2>&1
done

echo ""
echo "--- T2: com pthread_mutex (resultado correto garantido) ---"
echo "    [AVISO: pode ser lento — 1 bilhao de operacoes de mutex]"
for N in 2 4 8; do
    { time ./threads "$N" T2 | tee -a "$RESULTS_FILE" ; } 2>&1
done

# ------------------------------------------------------------------ #
#  Processos                                                           #
# ------------------------------------------------------------------ #
header "PARTE B — PROCESSOS"

echo ""
echo "--- P1: sem sincronizacao (race condition esperada) ---"
for N in 2 4 8; do
    { time ./processes "$N" P1 | tee -a "$RESULTS_FILE" ; } 2>&1
done

echo ""
echo "--- P2: com semaforo POSIX nomeado (resultado correto garantido) ---"
echo "    [AVISO: muito lento — 1 bilhao de operacoes de semaforo]"
for N in 2 4 8; do
    { time ./processes "$N" P2 | tee -a "$RESULTS_FILE" ; } 2>&1
done

header "TODOS OS EXPERIMENTOS CONCLUIDOS"
echo "Preencha as tabelas do README.md com os valores acima."
echo "Resultados salvos em: $RESULTS_FILE"
echo "Execute 'python3 plot.py' para gerar o grafico de escalabilidade."
echo ""
