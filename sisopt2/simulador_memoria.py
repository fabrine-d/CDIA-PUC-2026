###
###     S I M U L A D O R    D E    M E M Ó R I A
###
### Prof. Filipo - github.com/ProfessorFilipo/MemSim/
###

import sys


class Frame:
    def __init__(self, id_frame):
        self.id_frame = id_frame
        self.pagina_alocada = None  # Armazena o número da página ou None se estiver vazio
        # Dica para os alunos: vocês podem adicionar atributos aqui para ajudar no algoritmo (ex: timestamp, contador)


class TabelaPaginas:
    def __init__(self, num_frames, algoritmo="FIFO"):
        # Inicializa a memória física com a quantidade de frames especificada
        self.frames = [Frame(i) for i in range(num_frames)]
        self.total_page_faults = 0
        self.total_acessos = 0
        self.algoritmo = algoritmo.upper()
        self.fila_fifo = []
        self.frequencias = {}
        self.ordem_insercao = []

    def acessar_pagina(self, numero_pagina):
        self.total_acessos += 1
        if self.algoritmo == "LFU":
            # LFU global: incrementa em todo acesso, independentemente de hit/fault.
            self.frequencias[numero_pagina] = self.frequencias.get(numero_pagina, 0) + 1

        # 1. Verificar se a página já está em algum frame (Hit)
        for frame in self.frames:
            if frame.pagina_alocada == numero_pagina:
                return True, frame.id_frame  # Retorna (Hit=True, frame_id)

        # 2. Se não encontrou, ocorreu um Page Fault!
        self.total_page_faults += 1

        # 3. Verificar se existe algum frame vazio disponível
        for frame in self.frames:
            if frame.pagina_alocada is None:
                frame.pagina_alocada = numero_pagina
                if self.algoritmo == "FIFO":
                    self.fila_fifo.append(numero_pagina)
                elif self.algoritmo == "LFU":
                    self.ordem_insercao.append(numero_pagina)
                return False, frame.id_frame  # Retorna (Hit=False, frame_id)

        # 4. Memória cheia: Aplicar algoritmo de substituição de página
        frame_vitima_id = self.substituir_pagina(numero_pagina)
        return False, frame_vitima_id

    def substituir_pagina(self, nova_pagina):
        if self.algoritmo == "FIFO":
            # FIFO: remove a página mais antiga da fila, substitui no frame correspondente
            # e adiciona a nova página ao final da fila.
            pagina_vitima = self.fila_fifo.pop(0)

            for frame in self.frames:
                if frame.pagina_alocada == pagina_vitima:
                    frame.pagina_alocada = nova_pagina
                    self.fila_fifo.append(nova_pagina)
                    return frame.id_frame

            raise RuntimeError("Estado FIFO inconsistente: página vítima não encontrada nos frames.")

        if self.algoritmo == "LFU":
            if not self.ordem_insercao:
                raise RuntimeError("Estado LFU inconsistente: estruturas auxiliares vazias com memória cheia.")

            paginas_residentes = [
                frame.pagina_alocada for frame in self.frames if frame.pagina_alocada is not None
            ]
            menor_frequencia = min(self.frequencias[pagina] for pagina in paginas_residentes)
            paginas_candidatas = {
                pagina for pagina in paginas_residentes if self.frequencias[pagina] == menor_frequencia
            }

            pagina_vitima = None
            for pagina in self.ordem_insercao:
                if pagina in paginas_candidatas:
                    pagina_vitima = pagina
                    break

            if pagina_vitima is None:
                raise RuntimeError("Estado LFU inconsistente: vítima não encontrada no desempate FIFO.")

            for frame in self.frames:
                if frame.pagina_alocada == pagina_vitima:
                    frame.pagina_alocada = nova_pagina
                    self.ordem_insercao.remove(pagina_vitima)
                    self.ordem_insercao.append(nova_pagina)
                    return frame.id_frame

            raise RuntimeError("Estado LFU inconsistente: página vítima não encontrada nos frames.")

        raise NotImplementedError(
            f"Algoritmo '{self.algoritmo}' não suportado. Use FIFO ou LFU."
        )

    def imprimir_mapa_memoria(self, passo, pagina_acessada, foi_hit, frame_alterado=None):
        status = "Hit" if foi_hit else "Page Fault"
        print(f"\n--- Passo {passo}: Acesso à Página {pagina_acessada} ({status}) ---")

        for frame in self.frames:
            conteudo = f"Página {frame.pagina_alocada}" if frame.pagina_alocada is not None else "[Vazio]"
            marcador = " <-- Alterado" if frame.id_frame == frame_alterado and not foi_hit else ""
            print(f"[Frame {frame.id_frame}]: {conteudo}{marcador}")

        print("-" * 40)


class Simulador:
    def __init__(self, caminho_arquivo, algoritmo="FIFO"):
        self.caminho_arquivo = caminho_arquivo
        self.algoritmo = algoritmo.upper()

    def executar(self):
        if self.algoritmo not in {"FIFO", "LFU"}:
            print(f"Erro: Algoritmo '{self.algoritmo}' inválido. Use FIFO ou LFU.")
            print("Uso: python simulador_memoria.py entrada.txt FIFO")
            print("Uso: python simulador_memoria.py entrada.txt LFU")
            return

        try:
            with open(self.caminho_arquivo, 'r') as arquivo:
                linhas = arquivo.readlines()
        except FileNotFoundError:
            print(f"Erro: O arquivo '{self.caminho_arquivo}' não foi encontrado.")
            return

        # Limpa linhas vazias ou comentários se houver
        linhas = [l.strip() for l in linhas if l.strip() and not l.strip().startswith('#')]

        if not linhas:
            print("Erro: Arquivo de entrada vazio.")
            return

        # A primeira linha válida define o número de frames na memória RAM simulada
        try:
            num_frames = int(linhas[0])
            if num_frames <= 0:
                raise ValueError
        except ValueError:
            print("Erro: A primeira linha do arquivo deve conter um número inteiro positivo de frames.")
            return

        tabela_paginas = TabelaPaginas(num_frames, self.algoritmo)

        print(f"Iniciando simulação com {num_frames} frames disponíveis.")
        print("=" * 40)

        # As linhas seguintes são a sequência de acessos às páginas
        passo = 1
        for indice_linha, linha in enumerate(linhas[1:], start=2):
            try:
                numero_pagina = int(linha)
            except ValueError:
                print(
                    f"Erro: Valor inválido na linha {indice_linha}. "
                    "Cada acesso de página deve ser um número inteiro."
                )
                return

            # Processa o acesso na tabela de páginas
            foi_hit, frame_id = tabela_paginas.acessar_pagina(numero_pagina)

            # Renderiza o mapa de memória para o aluno ver o passo a passo
            tabela_paginas.imprimir_mapa_memoria(passo, numero_pagina, foi_hit, frame_id)
            passo += 1

        # Exibição das estatísticas finais da simulação
        print("\n================ STATS FINAIS ================")
        print(f"Total de Acessos: {tabela_paginas.total_acessos}")
        print(f"Total de Page Faults: {tabela_paginas.total_page_faults}")
        if tabela_paginas.total_acessos > 0:
            taxa_faults = (tabela_paginas.total_page_faults / tabela_paginas.total_acessos) * 100
            print(f"Taxa de Page Faults: {taxa_faults:.2f}%")
        print("==============================================")


if __name__ == "__main__":
    # Permite passar o arquivo de entrada e algoritmo por argumento, ou usa padrões
    arquivo_entrada = sys.argv[1] if len(sys.argv) > 1 else "entrada.txt"
    algoritmo = sys.argv[2] if len(sys.argv) > 2 else "FIFO"
    simulador = Simulador(arquivo_entrada, algoritmo)
    simulador.executar()