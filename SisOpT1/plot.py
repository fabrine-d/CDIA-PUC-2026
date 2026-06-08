#!/usr/bin/env python3
"""
plot.py — Gráfico de Escalabilidade: Duelo de Contextos
Lê results.txt gerado por run_all.sh e plota Tempo (ms) vs. N workers.

Uso:
    python3 plot.py               # lê results.txt (padrão)
    python3 plot.py outro.txt     # lê arquivo alternativo

Saída:
    escalabilidade.png
"""

import re
import sys
from pathlib import Path

# ------------------------------------------------------------------ #
#  Parsear results.txt                                                 #
# ------------------------------------------------------------------ #

# Formato de cada linha:
# T1 (sem sync)             | N=2  | Tempo=   1234.567 ms | Contador=... | Correto=...
LINE_RE = re.compile(
    r"(T1|T2|P1|P2)\s*\([^)]+\)\s*\|\s*N=\s*(\d+)\s*\|\s*Tempo=\s*([\d.]+)\s*ms"
)

def parse_results(filepath: str) -> dict:
    """Retorna {cenario: {N: tempo_ms}}"""
    data: dict = {}
    try:
        with open(filepath, encoding="utf-8") as f:
            for line in f:
                m = LINE_RE.search(line)
                if m:
                    scenario = m.group(1)   # T1 / T2 / P1 / P2
                    n        = int(m.group(2))
                    tempo    = float(m.group(3))
                    data.setdefault(scenario, {})[n] = tempo
    except FileNotFoundError:
        print(f"Erro: arquivo '{filepath}' não encontrado.")
        print("Execute './run_all.sh' primeiro para gerar os resultados.")
        sys.exit(1)
    return data

# ------------------------------------------------------------------ #
#  Plotar                                                              #
# ------------------------------------------------------------------ #

def plot(data: dict, output: str = "escalabilidade.png"):
    try:
        import matplotlib.pyplot as plt
        import matplotlib.ticker as ticker
    except ImportError:
        print("matplotlib não encontrado. Instale com: pip install matplotlib")
        sys.exit(1)

    N_VALUES = [2, 4, 8]

    STYLE = {
        "T1": dict(color="#e74c3c", marker="o", linestyle="--", label="T1 — Threads sem sync"),
        "T2": dict(color="#c0392b", marker="s", linestyle="-",  label="T2 — Threads com mutex"),
        "P1": dict(color="#2980b9", marker="o", linestyle="--", label="P1 — Processos sem sync"),
        "P2": dict(color="#1a5276", marker="s", linestyle="-",  label="P2 — Processos com semáforo"),
    }

    fig, axes = plt.subplots(1, 2, figsize=(13, 5), sharey=False)
    fig.suptitle("Duelo de Contextos — Escalabilidade: Tempo vs. N workers",
                 fontsize=14, fontweight="bold", y=1.02)

    # Subplot A — Threads
    ax_t = axes[0]
    ax_t.set_title("Parte A — Threads (pthreads)", fontsize=12)
    for scenario in ("T1", "T2"):
        if scenario not in data:
            continue
        times = [data[scenario].get(n, None) for n in N_VALUES]
        ax_t.plot(N_VALUES, times, **STYLE[scenario])
    ax_t.set_xlabel("N (número de threads)")
    ax_t.set_ylabel("Tempo (ms)")
    ax_t.set_xticks(N_VALUES)
    ax_t.yaxis.set_major_formatter(ticker.FuncFormatter(lambda x, _: f"{x:,.0f}"))
    ax_t.legend()
    ax_t.grid(True, linestyle=":", alpha=0.6)

    # Subplot B — Processos
    ax_p = axes[1]
    ax_p.set_title("Parte B — Processos (fork + shmget)", fontsize=12)
    for scenario in ("P1", "P2"):
        if scenario not in data:
            continue
        times = [data[scenario].get(n, None) for n in N_VALUES]
        ax_p.plot(N_VALUES, times, **STYLE[scenario])
    ax_p.set_xlabel("N (número de processos)")
    ax_p.set_ylabel("Tempo (ms)")
    ax_p.set_xticks(N_VALUES)
    ax_p.yaxis.set_major_formatter(ticker.FuncFormatter(lambda x, _: f"{x:,.0f}"))
    ax_p.legend()
    ax_p.grid(True, linestyle=":", alpha=0.6)

    plt.tight_layout()
    plt.savefig(output, dpi=150, bbox_inches="tight")
    print(f"Gráfico salvo em: {output}")

    # Imprimir tabela resumo no terminal
    print("\n--- Tabela de Tempos (ms) ---")
    print(f"{'Cenário':<8} | {'N=2':>12} | {'N=4':>12} | {'N=8':>12}")
    print("-" * 52)
    for scenario in ("T1", "T2", "P1", "P2"):
        if scenario in data:
            row_vals = [
                f"{data[scenario].get(n, float('nan')):>12.3f}" for n in N_VALUES
            ]
            cells = " | ".join(row_vals)
        else:
            cells = " | ".join(["           —"] * 3)
        print(f"{scenario:<8} | {cells}")

# ------------------------------------------------------------------ #
#  Main                                                                #
# ------------------------------------------------------------------ #

if __name__ == "__main__":
    results_file = sys.argv[1] if len(sys.argv) > 1 else "results.txt"
    data = parse_results(results_file)

    if not data:
        print("Nenhum resultado encontrado no arquivo. Verifique o formato.")
        sys.exit(1)

    plot(data)
