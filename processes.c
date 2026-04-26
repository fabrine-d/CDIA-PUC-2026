/*
 * processes.c — Duelo de Contextos: Experimentos com Processos POSIX
 *
 * Uso: ./processes <N> <P1|P2>
 *   N   — número de processos filhos (ex: 2, 4, 8)
 *   P1  — sem sincronização (race condition intencional)
 *   P2  — com semáforo POSIX nomeado sem_open (resultado correto garantido)
 *
 * IPC: memória compartilhada via shmget / shmat (System V IPC).
 * Sincronização: semáforo nomeado POSIX (sem_open / sem_wait / sem_post).
 *
 * Total de incrementos: 1.000.000.000, distribuído igualmente entre N processos.
 *
 * Compilação — Linux:
 *   gcc -Wall -Wextra -O2 -std=c11 -o processes processes.c -lrt
 * Compilação — macOS:
 *   gcc -Wall -Wextra -O2 -std=c11 -o processes processes.c
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/ipc.h>
#include <sys/shm.h>
#include <semaphore.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <time.h>

#define TOTAL    1000000000LL   /* 1 bilhão */
#define SEM_NAME "/duelo_counter_sem"

typedef long long ll;

/* ------------------------------------------------------------------ */
/*  Utilitário de tempo                                                 */
/* ------------------------------------------------------------------ */

static double diff_ms(struct timespec *start, struct timespec *end)
{
    return (double)(end->tv_sec  - start->tv_sec)  * 1000.0
         + (double)(end->tv_nsec - start->tv_nsec) / 1e6;
}

/* ------------------------------------------------------------------ */
/*  Experimento P1 — sem sincronização                                  */
/* ------------------------------------------------------------------ */

static void run_p1(int n)
{
    /* Criar segmento de memória compartilhada para o contador */
    int shmid = shmget(IPC_PRIVATE, sizeof(ll), IPC_CREAT | 0600);
    if (shmid == -1) { perror("shmget"); exit(1); }

    void *shm_ptr = shmat(shmid, NULL, 0);
    if (shm_ptr == (void *)-1) { perror("shmat"); exit(1); }

    volatile ll *counter = (volatile ll *)shm_ptr;
    *counter = 0;

    ll ipt = TOTAL / n;   /* iterações por processo */

    struct timespec t0, t1;
    clock_gettime(CLOCK_MONOTONIC, &t0);

    for (int i = 0; i < n; i++) {
        pid_t pid = fork();
        if (pid < 0) { perror("fork"); exit(1); }

        if (pid == 0) {                         /* — filho — */
            /*
             * O filho herda o mapeamento de memória compartilhada do pai
             * (fork copia o espaço de endereçamento). O ponteiro 'counter'
             * aponta para o mesmo segmento físico em todos os processos.
             *
             * Sem sincronização: load → add → store NÃO é atômico,
             * portanto race conditions são esperadas.
             */
            for (ll j = 0; j < ipt; j++)
                (*counter)++;

            shmdt(shm_ptr);
            exit(0);
        }
    }

    /* Pai aguarda todos os filhos terminarem */
    for (int i = 0; i < n; i++)
        waitpid(-1, NULL, 0);

    clock_gettime(CLOCK_MONOTONIC, &t1);

    ll  val = *counter;
    int ok  = (val == TOTAL);

    printf("%-25s | N=%-2d | Tempo=%10.3f ms | Contador=%-12lld | Correto=%s\n",
           "P1 (sem sync)",
           n, diff_ms(&t0, &t1), val, ok ? "SIM" : "NAO");

    /* Limpeza */
    shmdt(shm_ptr);
    shmctl(shmid, IPC_RMID, NULL);
}

/* ------------------------------------------------------------------ */
/*  Experimento P2 — com semáforo POSIX nomeado                        */
/* ------------------------------------------------------------------ */

static void run_p2(int n)
{
    /* Remover eventual semáforo de execução anterior interrompida */
    (void)sem_unlink(SEM_NAME);

    /* Criar segmento de memória compartilhada */
    int shmid = shmget(IPC_PRIVATE, sizeof(ll), IPC_CREAT | 0600);
    if (shmid == -1) { perror("shmget"); exit(1); }

    void *shm_ptr = shmat(shmid, NULL, 0);
    if (shm_ptr == (void *)-1) { perror("shmat"); exit(1); }

    volatile ll *counter = (volatile ll *)shm_ptr;
    *counter = 0;

    /*
     * Criar semáforo nomeado inicializado com valor 1 (mutex binário).
     * É criado antes do fork para que os filhos possam abri-lo pelo nome.
     */
    sem_t *sem = sem_open(SEM_NAME, O_CREAT | O_EXCL, 0644, 1);
    if (sem == SEM_FAILED) { perror("sem_open (parent)"); exit(1); }

    ll ipt = TOTAL / n;

    struct timespec t0, t1;
    clock_gettime(CLOCK_MONOTONIC, &t0);

    for (int i = 0; i < n; i++) {
        pid_t pid = fork();
        if (pid < 0) { perror("fork"); exit(1); }

        if (pid == 0) {                         /* — filho — */
            /*
             * Cada filho abre o semáforo de forma independente para
             * ter seu próprio descritor (sem_close no filho não afeta pai).
             */
            sem_t *csem = sem_open(SEM_NAME, 0);
            if (csem == SEM_FAILED) { perror("sem_open (child)"); exit(1); }

            for (ll j = 0; j < ipt; j++) {
                sem_wait(csem);     /* P — entra na seção crítica */
                (*counter)++;
                sem_post(csem);     /* V — libera a seção crítica */
            }

            sem_close(csem);
            shmdt(shm_ptr);
            exit(0);
        }
    }

    /* Pai aguarda todos os filhos */
    for (int i = 0; i < n; i++)
        waitpid(-1, NULL, 0);

    clock_gettime(CLOCK_MONOTONIC, &t1);

    ll  val = *counter;
    int ok  = (val == TOTAL);

    printf("%-25s | N=%-2d | Tempo=%10.3f ms | Contador=%-12lld | Correto=%s\n",
           "P2 (semaforo)",
           n, diff_ms(&t0, &t1), val, ok ? "SIM" : "NAO");

    /* Limpeza */
    sem_close(sem);
    sem_unlink(SEM_NAME);
    shmdt(shm_ptr);
    shmctl(shmid, IPC_RMID, NULL);
}

/* ------------------------------------------------------------------ */
/*  main                                                                */
/* ------------------------------------------------------------------ */

int main(int argc, char *argv[])
{
    if (argc < 3) {
        fprintf(stderr, "Uso: %s <N> <P1|P2>\n", argv[0]);
        return 1;
    }

    int n = atoi(argv[1]);
    if (n < 1 || n > 64) {
        fprintf(stderr, "Erro: N deve estar entre 1 e 64\n");
        return 1;
    }

    if (strcmp(argv[2], "P1") == 0)
        run_p1(n);
    else if (strcmp(argv[2], "P2") == 0)
        run_p2(n);
    else {
        fprintf(stderr, "Erro: experimento invalido — use P1 ou P2\n");
        return 1;
    }

    return 0;
}
