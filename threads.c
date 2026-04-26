/*
 * threads.c — Duelo de Contextos: Experimentos com Threads POSIX
 *
 * Uso: ./threads <N> <T1|T2>
 *   N   — número de threads (ex: 2, 4, 8)
 *   T1  — sem sincronização (race condition intencional)
 *   T2  — com pthread_mutex (resultado correto garantido)
 *
 * Total de incrementos: 1.000.000.000, distribuído igualmente entre N threads.
 *
 * Compilação:
 *   gcc -Wall -Wextra -O2 -std=c11 -o threads threads.c -lpthread
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <time.h>

#define TOTAL 1000000000LL   /* 1 bilhão */

typedef long long ll;

/* ------------------------------------------------------------------ */
/*  Estado global compartilhado entre as threads                        */
/* ------------------------------------------------------------------ */

/*
 * 'volatile' força o compilador a emitir load/store em cada acesso,
 * tornando a race condition visível em T1.
 * Em T2, o mutex já garante a consistência; volatile é mantido
 * apenas para uniformidade entre os experimentos.
 */
static volatile ll counter;

static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

/* Número de iterações que cada thread deve executar */
static ll iterations_per_thread;

/* ------------------------------------------------------------------ */
/*  Funções de worker                                                   */
/* ------------------------------------------------------------------ */

/* T1: sem sincronização — demonstra race condition */
static void *worker_no_sync(void *arg)
{
    (void)arg;
    ll limit = iterations_per_thread;
    for (ll i = 0; i < limit; i++)
        counter++;      /* load → add → store: NÃO atômico */
    return NULL;
}

/* T2: com mutex — garante exclusão mútua em cada incremento */
static void *worker_with_mutex(void *arg)
{
    (void)arg;
    ll limit = iterations_per_thread;
    for (ll i = 0; i < limit; i++) {
        pthread_mutex_lock(&mutex);
        counter++;
        pthread_mutex_unlock(&mutex);
    }
    return NULL;
}

/* ------------------------------------------------------------------ */
/*  Utilitário de tempo                                                 */
/* ------------------------------------------------------------------ */

static double diff_ms(struct timespec *start, struct timespec *end)
{
    return (double)(end->tv_sec  - start->tv_sec)  * 1000.0
         + (double)(end->tv_nsec - start->tv_nsec) / 1e6;
}

/* ------------------------------------------------------------------ */
/*  Experimento genérico                                                */
/* ------------------------------------------------------------------ */

static void run_experiment(int n, int with_mutex)
{
    counter = 0;
    iterations_per_thread = TOTAL / n;

    pthread_t *tids = malloc((size_t)n * sizeof(pthread_t));
    if (!tids) { perror("malloc"); exit(1); }

    void *(*worker)(void *) = with_mutex ? worker_with_mutex : worker_no_sync;

    struct timespec t0, t1;
    clock_gettime(CLOCK_MONOTONIC, &t0);

    for (int i = 0; i < n; i++) {
        if (pthread_create(&tids[i], NULL, worker, NULL) != 0) {
            perror("pthread_create");
            exit(1);
        }
    }

    for (int i = 0; i < n; i++)
        pthread_join(tids[i], NULL);

    clock_gettime(CLOCK_MONOTONIC, &t1);

    ll   val = counter;
    int  ok  = (val == TOTAL);

    printf("%-25s | N=%-2d | Tempo=%10.3f ms | Contador=%-12lld | Correto=%s\n",
           with_mutex ? "T2 (mutex)" : "T1 (sem sync)",
           n, diff_ms(&t0, &t1), val, ok ? "SIM" : "NAO");

    free(tids);
}

/* ------------------------------------------------------------------ */
/*  main                                                                */
/* ------------------------------------------------------------------ */

int main(int argc, char *argv[])
{
    if (argc < 3) {
        fprintf(stderr, "Uso: %s <N> <T1|T2>\n", argv[0]);
        return 1;
    }

    int n = atoi(argv[1]);
    if (n < 1 || n > 64) {
        fprintf(stderr, "Erro: N deve estar entre 1 e 64\n");
        return 1;
    }

    if (strcmp(argv[2], "T1") == 0)
        run_experiment(n, 0);
    else if (strcmp(argv[2], "T2") == 0)
        run_experiment(n, 1);
    else {
        fprintf(stderr, "Erro: experimento invalido — use T1 ou T2\n");
        return 1;
    }

    return 0;
}
