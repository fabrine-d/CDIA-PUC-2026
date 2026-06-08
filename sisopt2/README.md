# Simulador de Memória — T2 SisOp

Simulador de substituição de páginas em memória virtual com suporte aos algoritmos **FIFO** e **LFU**.

## Requisitos

- Python 3.8+

## Formato do Arquivo de Entrada

A primeira linha define o número de frames físicos disponíveis. As linhas seguintes são a sequência de acessos a páginas.

```
4
7
0
1
2
0
3
```

Linhas em branco e linhas começando com `#` são ignoradas.

## Como Executar

```bash
python simulador_memoria.py <arquivo_entrada> <algoritmo>
```

### FIFO

```bash
python simulador_memoria.py entrada.txt FIFO
```

### LFU

```bash
python simulador_memoria.py entrada.txt LFU
```

## Como Testar

Use o arquivo de validação incluído:

```bash
python simulador_memoria.py entrada_validacao.txt FIFO
python simulador_memoria.py entrada_validacao.txt LFU
```

### Resultados Esperados (4 frames, sequência padrão)

| Algoritmo | Acessos | Page Faults | Taxa |
|-----------|---------|-------------|------|
| FIFO      | 12      | 7           | 58.33% |
| LFU       | 12      | 6           | 50.00% |

## Exemplo de Saída

```
Iniciando simulação com 4 frames disponíveis.
========================================
--- Passo 1: Acesso à Página 7 (Page Fault) ---
[Frame 0]: Página 7 <-- Alterado
[Frame 1]: [Vazio]
[Frame 2]: [Vazio]
[Frame 3]: [Vazio]
----------------------------------------
...
================ STATS FINAIS ================
Total de Acessos: 12
Total de Page Faults: 7
Taxa de Page Faults: 58.33%
==============================================
```

## Algoritmos

**FIFO** — substitui a página que está há mais tempo na memória (a mais antiga na ordem de inserção).

**LFU** — substitui a página com menor frequência de acessos global. Em caso de empate, usa FIFO como critério de desempate (a que foi inserida primeiro é removida).
