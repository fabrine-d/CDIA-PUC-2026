# Duelo de Contextos: Processos vs. Threads

Nomes: Fabrine Drescher Machado e Vitor Rafael Goncalves Bastos Borges

Este projeto apresenta um experimento exploratório comparando **threads POSIX (`pthreads`)** e **processos POSIX (`fork`)** em um ambiente Unix-like, considerando três aspectos principais:

- **Overhead de criação**
- **Custo de comunicação e sincronização**
- **Consistência de dados em memória compartilhada**

O problema utilizado é o clássico **contador compartilhado**. Um contador deve ser incrementado até o valor de:

```text
1.000.000.000
```

O trabalho é dividido entre diferentes quantidades de unidades de execução:

```text
N = 2, 4 e 8
```

Foram implementados quatro cenários:

| Parte | Cenário | Descrição |
|------|---------|-----------|
| A | T1 | Threads sem sincronização |
| A | T2 | Threads com `pthread_mutex` |
| B | P1 | Processos sem sincronização |
| B | P2 | Processos com semáforo POSIX nomeado |

---

## Estrutura do Projeto

```text
.
├── threads.c      # Parte A: T1 sem sync e T2 com pthread_mutex
├── processes.c    # Parte B: P1 sem sync e P2 com semáforo POSIX
├── Makefile       # Compilação dos programas
├── run_all.sh     # Executa todos os experimentos automaticamente
├── plot.py        # Gera gráfico de escalabilidade a partir de results.txt
├── results.txt    # Resultados obtidos nos testes
└── README.md      # Documentação do experimento
```

---

## Compilação e Execução

### Compilar os programas

```bash
make all
```

Isso gera os binários:

```text
threads
processes
```

---

### Executar experimentos individuais

#### Parte A — Threads

```bash
./threads <N> T1    # Threads sem sincronização
./threads <N> T2    # Threads com pthread_mutex
```

Exemplos:

```bash
./threads 2 T1
./threads 4 T2
./threads 8 T2
```

#### Parte B — Processos

```bash
./processes <N> P1  # Processos sem sincronização
./processes <N> P2  # Processos com semáforo POSIX nomeado
```

Exemplos:

```bash
./processes 2 P1
./processes 4 P2
./processes 8 P2
```

---

### Executar todos os testes automaticamente

Em alguns ambientes, especialmente quando os arquivos foram editados no Windows, pode ser necessário corrigir os line endings dos scripts.

#### macOS

```bash
sed -i '' 's/\r//' run_all.sh plot.py
```

#### Linux

```bash
sed -i 's/\r//' run_all.sh plot.py
```

Depois, execute:

```bash
chmod +x run_all.sh
./run_all.sh
```

O script executa todos os experimentos para:

```text
N = 2, 4 e 8
```

e gera o arquivo:

```text
results.txt
```

---

### Gerar gráfico de escalabilidade

Instalar o `matplotlib`, se necessário:

```bash
pip install matplotlib
```

Gerar o gráfico:

```bash
python3 plot.py
```

O gráfico gerado compara o tempo de execução em função do número de workers.

---

### Limpar arquivos compilados

```bash
make clean
```

---

## Ambiente de Execução

Os experimentos foram executados no seguinte ambiente:

| Item | Valor |
|------|-------|
| Sistema Operacional | macOS 15.6.1 24G90 |
| Kernel | Darwin 24.6.0 |
| CPU | Apple M1 |
| Núcleos físicos / lógicos | 8 / 8 |
| RAM | 8 GB |
| Compilador | Apple clang 17.0.0 |

---

## Assinatura do Hardware

Para registrar a quantidade de núcleos disponíveis no ambiente de teste, foi utilizado o comando:

```bash
sysctl -a | grep hw.ncpu
```

Saída obtida:

```text
hw.ncpu: 8
```

---

## Tabela Geral de Tempos

Os tempos abaixo foram registrados no arquivo `results.txt`.

O campo **Tempo (ms)** representa o tempo total de execução medido pelo próprio programa para cada cenário.

| Cenário | N | Tempo (ms) | Contador Final | Correto? |
|:-------:|:-:|-----------:|---------------:|:--------:|
| T1 — Threads sem sync | 2 | 768.458 | 500.781.324 | Não |
| T1 — Threads sem sync | 4 | 407.375 | 251.557.444 | Não |
| T1 — Threads sem sync | 8 | 425.325 | 208.786.584 | Não |
| T2 — Threads com mutex | 2 | 14.983,797 | 1.000.000.000 | Sim |
| T2 — Threads com mutex | 4 | 28.011,320 | 1.000.000.000 | Sim |
| T2 — Threads com mutex | 8 | 25.543,228 | 1.000.000.000 | Sim |
| P1 — Processos sem sync | 2 | 685.323 | 500.691.727 | Não |
| P1 — Processos sem sync | 4 | 375.707 | 259.977.193 | Não |
| P1 — Processos sem sync | 8 | 379.196 | 197.772.466 | Não |
| P2 — Processos com semáforo | 2 | 1.573.728,967 | 1.000.000.000 | Sim |
| P2 — Processos com semáforo | 4 | 1.683.384,402 | 1.000.000.000 | Sim |
| P2 — Processos com semáforo | 8 | 2.596.231,994 | 1.000.000.000 | Sim |

> Observação: os tempos de P2 são muito maiores porque o semáforo é utilizado a cada incremento do contador. Como o experimento realiza 1 bilhão de incrementos, o custo acumulado de sincronização se torna extremamente alto.

---

# Resultados por Experimento

## Parte A — Threads com `pthreads`

### T1 — Threads sem sincronização

| N threads | Tempo (ms) | Valor Final | Valor Esperado | Correto? |
|:---------:|-----------:|------------:|---------------:|:--------:|
| 2 | 768.458 | 500.781.324 | 1.000.000.000 | Não |
| 4 | 407.375 | 251.557.444 | 1.000.000.000 | Não |
| 8 | 425.325 | 208.786.584 | 1.000.000.000 | Não |

Neste cenário, várias threads acessam e modificam o mesmo contador global ao mesmo tempo, sem nenhum mecanismo de sincronização.

Como resultado, o valor final do contador fica incorreto.

---

### T2 — Threads com `pthread_mutex`

| N threads | Tempo (ms) | Valor Final | Valor Esperado | Correto? |
|:---------:|-----------:|------------:|---------------:|:--------:|
| 2 | 14.983,797 | 1.000.000.000 | 1.000.000.000 | Sim |
| 4 | 28.011,320 | 1.000.000.000 | 1.000.000.000 | Sim |
| 8 | 25.543,228 | 1.000.000.000 | 1.000.000.000 | Sim |

Neste cenário, o incremento do contador é protegido por um `pthread_mutex`.

Isso garante que apenas uma thread por vez execute a seção crítica, tornando o resultado correto. Porém, essa proteção adiciona custo de sincronização e reduz o paralelismo efetivo.

---

## Parte B — Processos com `fork` e memória compartilhada

### P1 — Processos sem sincronização

| N processos | Tempo (ms) | Valor Final | Valor Esperado | Correto? |
|:-----------:|-----------:|------------:|---------------:|:--------:|
| 2 | 685.323 | 500.691.727 | 1.000.000.000 | Não |
| 4 | 375.707 | 259.977.193 | 1.000.000.000 | Não |
| 8 | 379.196 | 197.772.466 | 1.000.000.000 | Não |

Neste cenário, os processos acessam o mesmo contador por meio de memória compartilhada criada com `shmget` e associada com `shmat`.

Como não há sincronização, múltiplos processos podem ler e escrever no contador ao mesmo tempo, causando perda de incrementos.

---

### P2 — Processos com semáforo POSIX nomeado

| N processos | Tempo (ms) | Valor Final | Valor Esperado | Correto? |
|:-----------:|-----------:|------------:|---------------:|:--------:|
| 2 | 1.573.728,967 | 1.000.000.000 | 1.000.000.000 | Sim |
| 4 | 1.683.384,402 | 1.000.000.000 | 1.000.000.000 | Sim |
| 8 | 2.596.231,994 | 1.000.000.000 | 1.000.000.000 | Sim |

Neste cenário, o contador compartilhado é protegido por um semáforo POSIX nomeado criado com `sem_open`.

O resultado final fica correto, mas o custo de execução aumenta muito. Isso ocorre porque cada incremento exige operações de sincronização entre processos, usando `sem_wait` e `sem_post`.

---

# Análise dos Resultados

## 1. Consistência de Dados

Os experimentos T1 e P1 mostram que acessar uma variável compartilhada sem sincronização causa inconsistência.

A operação:

```c
counter++;
```

parece simples, mas não é atômica. Em termos simplificados, ela pode ser dividida em três etapas:

```text
1. Ler o valor atual da memória
2. Somar 1
3. Escrever o novo valor na memória
```

Quando dois ou mais workers executam essas etapas ao mesmo tempo, pode ocorrer uma race condition.

Exemplo:

```text
Worker A lê counter = 100
Worker B lê counter = 100

Worker A calcula 101
Worker B calcula 101

Worker A grava 101
Worker B grava 101
```

Nesse caso, dois incrementos foram executados, mas o contador aumentou apenas uma unidade. Um incremento foi perdido.

Esse comportamento explica por que os valores finais de T1 e P1 ficaram muito abaixo de 1 bilhão.

---

## 2. Observação sobre `volatile`

Em alguns trechos do código, o uso de `volatile` pode ajudar a evitar certas otimizações do compilador durante o acesso ao contador.

No entanto, é importante destacar que:

```text
volatile não garante atomicidade.
volatile não resolve race condition.
volatile não substitui mutex, semáforo ou operações atômicas.
```

Portanto, mesmo usando `volatile`, o contador continua incorreto nos cenários sem sincronização.

A sincronização correta é obtida apenas em T2, com `pthread_mutex`, e em P2, com semáforo POSIX.

---

## 3. Overhead de Criação

Em geral, threads tendem a ter menor custo de criação do que processos, pois compartilham o mesmo espaço de endereçamento do processo principal.

Processos criados com `fork`, por outro lado, possuem espaços de endereçamento separados. Sistemas modernos utilizam copy-on-write, evitando cópias completas de memória logo no momento do `fork`, mas ainda há maior envolvimento do sistema operacional.

| Aspecto | Threads (`pthread_create`) | Processos (`fork`) |
|--------|:--------------------------:|:------------------:|
| Espaço de endereçamento | Compartilhado | Separado |
| Criação | Geralmente mais leve | Geralmente mais custosa |
| Comunicação | Direta, via memória compartilhada do processo | Requer IPC |
| Isolamento | Menor | Maior |

Nos resultados obtidos, T1 e P1 tiveram tempos próximos. Em algumas execuções, P1 foi ligeiramente mais rápido que T1. Isso não significa necessariamente que processos sejam mais leves que threads.

Neste experimento, o tempo total é dominado principalmente por:

- 1 bilhão de incrementos;
- contenção na memória compartilhada;
- comportamento do escalonador;
- arquitetura da CPU;
- custo de sincronização ou ausência dela.

Portanto, o experimento mostra o comportamento geral de execução, mas não isola apenas o custo puro de criação.

---

## 4. Custo de Comunicação e Sincronização

### Threads: T1 vs. T2

Comparando T1 com T2, observa-se que o uso de `pthread_mutex` corrige o valor final do contador, mas aumenta bastante o tempo de execução.

Isso acontece porque todas as threads disputam a mesma seção crítica:

```c
pthread_mutex_lock(&mutex);
counter++;
pthread_mutex_unlock(&mutex);
```

Como apenas uma thread pode incrementar o contador por vez, o paralelismo é reduzido.

---

### Processos: P1 vs. P2

Comparando P1 com P2, o efeito é ainda mais forte.

No cenário P2, cada incremento é protegido por:

```c
sem_wait(sem);
(*counter)++;
sem_post(sem);
```

Como o contador é incrementado 1 bilhão de vezes, o programa também executa um volume muito grande de operações de semáforo.

O semáforo POSIX nomeado envolve maior participação do sistema operacional do que um mutex de threads comum, por isso o overhead acumulado é muito maior.

| Mecanismo | Uso no experimento | Custo relativo |
|----------|--------------------|----------------|
| Sem sincronização | T1 / P1 | Baixo, mas incorreto |
| `pthread_mutex` | T2 | Médio/alto, mas correto |
| Semáforo POSIX nomeado | P2 | Muito alto, mas correto |

---

## 5. Escalabilidade com N

### Cenários sem sincronização

Em T1 e P1, aumentar N reduziu o tempo de execução de N=2 para N=4. Porém, os valores finais ficaram incorretos.

Isso mostra que houve ganho de paralelismo, mas sem garantia de consistência.

Além disso, quanto maior o número de workers, maior é a chance de conflitos no acesso ao contador compartilhado. Por isso, o valor final tende a se afastar ainda mais do valor esperado.

---

### Cenários com sincronização

Em T2 e P2, o valor final foi sempre correto. Porém, o aumento de N não gerou melhora proporcional de desempenho.

Isso ocorre porque todos os workers disputam a mesma seção crítica. Como o contador é único, apenas um worker por vez consegue incrementá-lo com segurança.

Assim, o gargalo passa a ser o mecanismo de sincronização.

No caso de P2, esse efeito é ainda mais visível devido ao alto custo do semáforo entre processos.

---

# Conclusão

O experimento mostra que desempenho e corretude precisam ser avaliados em conjunto.

Os cenários sem sincronização, T1 e P1, foram mais rápidos, mas produziram resultados incorretos. Isso demonstra a ocorrência de race conditions no acesso ao contador compartilhado.

Os cenários com sincronização, T2 e P2, produziram o valor correto de 1 bilhão, mas apresentaram tempos de execução muito maiores.

A principal conclusão é que:

```text
Sem sincronização, o programa pode ser rápido, mas incorreto.
Com sincronização, o programa fica correto, mas pode perder desempenho.
```

Além disso, o experimento mostra que a comunicação entre threads tende a ser mais simples e barata, pois as threads compartilham o mesmo espaço de endereçamento. Já processos oferecem maior isolamento, mas exigem mecanismos explícitos de IPC, como memória compartilhada e semáforos.

No caso específico deste trabalho, o uso de semáforo em processos apresentou o maior custo, principalmente porque a sincronização foi feita a cada incremento do contador.

---

# Conceitos Fundamentais

## Memória Compartilhada com `shmget` e `shmat`

Na parte de processos, o contador precisa ser compartilhado explicitamente entre os processos filhos. Para isso, foi utilizada memória compartilhada System V.

Exemplo simplificado:

```c
int shmid = shmget(IPC_PRIVATE, sizeof(long long), IPC_CREAT | 0600);

void *ptr = shmat(shmid, NULL, 0);

/* uso da memória compartilhada */

shmdt(ptr);

shmctl(shmid, IPC_RMID, NULL);
```

A função `shmget` cria o segmento de memória compartilhada, enquanto `shmat` associa esse segmento ao espaço de endereçamento do processo.

Após o `fork`, os processos filhos conseguem acessar o mesmo segmento de memória.

---

## Semáforo POSIX Nomeado com `sem_open`

Para sincronizar os processos em P2, foi utilizado um semáforo POSIX nomeado.

Exemplo simplificado:

```c
sem_t *sem = sem_open("/nome_semaforo", O_CREAT | O_EXCL, 0644, 1);

sem_wait(sem);

/* seção crítica */

sem_post(sem);

sem_close(sem);
sem_unlink("/nome_semaforo");
```

O semáforo garante exclusão mútua entre processos. Assim, apenas um processo por vez consegue acessar a seção crítica responsável por incrementar o contador.

---

## Processos vs. Threads — Resumo Comparativo

| Aspecto | Threads | Processos |
|--------|:-------:|:---------:|
| Criação | Mais leve | Mais custosa |
| Espaço de endereçamento | Compartilhado | Separado |
| Comunicação | Direta pela memória do processo | Requer IPC |
| Isolamento | Menor | Maior |
| Compartilhamento de dados | Natural | Explícito |
| Sincronização usada neste projeto | `pthread_mutex` | `sem_open` |
| Overhead observado | Menor que processos com semáforo | Maior em P2 |
| Resultado sem sync | Incorreto | Incorreto |
| Resultado com sync | Correto | Correto |

---

## Observações Finais

Este experimento não tem como objetivo produzir a implementação mais otimizada possível de um contador paralelo. O objetivo principal é demonstrar, de forma prática, os efeitos de:

- race conditions;
- sincronização;
- contenção;
- comunicação entre workers;
- diferenças entre threads e processos.

Uma implementação otimizada poderia reduzir drasticamente o overhead, por exemplo fazendo cada worker acumular um contador local e sincronizar apenas uma vez ao final. Porém, isso mudaria o foco do experimento, que é justamente observar o custo de compartilhar e sincronizar um contador global.

