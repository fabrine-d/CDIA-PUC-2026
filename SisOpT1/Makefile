# Makefile — Duelo de Contextos: Processos vs. Threads

CC     = gcc
CFLAGS = -Wall -Wextra -O2 -std=c11

# Detectar sistema operacional
UNAME := $(shell uname -s)

ifeq ($(UNAME),Linux)
    # Linux: -lpthread para threads; -lrt para sem_open e clock_gettime
    # (em sistemas com glibc >= 2.17 -lrt é opcional, mas incluímos para compatibilidade)
    LIBS_T = -lpthread -lrt
    LIBS_P = -lrt
else
    # macOS: pthread, sem_open e clock_gettime já estão na libc do sistema
    LIBS_T = -lpthread
    LIBS_P =
endif

.PHONY: all clean

all: threads processes

threads: threads.c
	$(CC) $(CFLAGS) -o $@ $< $(LIBS_T)

processes: processes.c
	$(CC) $(CFLAGS) -o $@ $< $(LIBS_P)

clean:
	rm -f threads processes
