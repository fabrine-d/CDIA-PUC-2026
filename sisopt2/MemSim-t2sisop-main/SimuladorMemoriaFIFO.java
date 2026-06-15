import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimuladorMemoriaFIFO {
    static class Frame {
        private final int idFrame;
        private Integer paginaAlocada;
        // private int frequency;

        Frame(int idFrame) {
            this.idFrame = idFrame;
            this.paginaAlocada = null;
            // this.frequency = 0;
            // Dica para os alunos: vocês podem adicionar atributos aqui para ajudar no algoritmo (ex: timestamp, contador)
        }

        int getIdFrame() {
            return idFrame;
        }

        Integer getPaginaAlocada() {
            return paginaAlocada;
        }

        void setPaginaAlocada(Integer paginaAlocada) {
            this.paginaAlocada = paginaAlocada;
        }
    }

    static class TabelaPaginas {
        private final List<Frame> frames;
        private int totalPageFaults;
        private int totalAcessos;

        TabelaPaginas(int numFrames) {
            // Inicializa a memória física com a quantidade de frames especificada
            this.frames = new ArrayList<>();
            for (int i = 0; i < numFrames; i++) {
                this.frames.add(new Frame(i));
            }
            this.totalPageFaults = 0;
            this.totalAcessos = 0;
        }

        ResultadoAcesso acessarPagina(int numeroPagina) {
            totalAcessos += 1;

            // 1. Verificar se a página já está em algum frame (Hit)
            for (Frame frame : frames) {
                if (frame.getPaginaAlocada() != null && frame.getPaginaAlocada() == numeroPagina) {
                    // TODO: Se necessário para o algoritmo (ex: LRU), atualize metadados aqui.
                    return new ResultadoAcesso(true, frame.getIdFrame());
                }
            }

            // 2. Se não encontrou, ocorreu um Page Fault!
            totalPageFaults += 1;

            // 3. Verificar se existe algum frame vazio disponível
            for (Frame frame : frames) {
                if (frame.getPaginaAlocada() == null) {
                    frame.setPaginaAlocada(numeroPagina);
                    // TODO: Se necessário para o algoritmo, inicialize metadados do frame aqui.
                    return new ResultadoAcesso(false, frame.getIdFrame());
                }
            }

            // 4. Memória cheia: Aplicar algoritmo de substituição de página
            int frameVitimaId = substituirPagina(numeroPagina);
            return new ResultadoAcesso(false, frameVitimaId);
        }

        private int substituirPagina(int novaPagina) {
            Frame frameRemovido = frames.remove(0);
            Frame novoFrame = new Frame(frameRemovido.getIdFrame());
            novoFrame.setPaginaAlocada(novaPagina);
            frames.add(novoFrame);
            return novoFrame.getIdFrame();
        }

        void imprimirMapaMemoria(int passo, int paginaAcessada, boolean foiHit, Integer frameAlterado) {
            /*
             * TODO: IMPLEMENTAR PELO GRUPO
             * Esta função deve imprimir o estado atual da memória física (frames) no terminal,
             * conforme o padrão visual exigido no enunciado do trabalho.
             */
            String status = foiHit ? "Hit" : "Page Fault";
            System.out.printf("%n--- Passo %d: Acesso à Página %d (%s) ---%n", passo, paginaAcessada, status);

            // Exemplo de iteração sobre os frames para os alunos completarem o print:
            for (Frame frame : frames) {
                String conteudo = frame.getPaginaAlocada() != null
                        ? "Página " + frame.getPaginaAlocada()
                        : "[Vazio]";
                String marcador = (!foiHit && frameAlterado != null && frame.getIdFrame() == frameAlterado)
                        ? " <-- Alterado"
                        : "";
                System.out.printf("[Frame %d]: %s%s%n", frame.getIdFrame(), conteudo, marcador);
            }

            System.out.println("-".repeat(40));
        }

        int getTotalPageFaults() {
            return totalPageFaults;
        }

        int getTotalAcessos() {
            return totalAcessos;
        }
    }

    static class ResultadoAcesso {
        private final boolean hit;
        private final int frameId;

        ResultadoAcesso(boolean hit, int frameId) {
            this.hit = hit;
            this.frameId = frameId;
        }

        boolean isHit() {
            return hit;
        }

        int getFrameId() {
            return frameId;
        }
    }

    static class Simulador {
        private final String caminhoArquivo;

        Simulador(String caminhoArquivo) {
            this.caminhoArquivo = caminhoArquivo;
        }

        void executar() {
            List<String> linhas = new ArrayList<>();

            try (BufferedReader arquivo = new BufferedReader(new FileReader(caminhoArquivo))) {
                String linha;
                while ((linha = arquivo.readLine()) != null) {
                    linhas.add(linha);
                }
            } catch (IOException e) {
                System.out.printf("Erro: O arquivo '%s' não foi encontrado.%n", caminhoArquivo);
                return;
            }

            // Limpa linhas vazias ou comentários se houver
            List<String> linhasFiltradas = new ArrayList<>();
            for (String linha : linhas) {
                String limpa = linha.strip();
                if (!limpa.isEmpty() && !limpa.startsWith("#")) {
                    linhasFiltradas.add(limpa);
                }
            }

            if (linhasFiltradas.isEmpty()) {
                System.out.println("Erro: Arquivo de entrada vazio.");
                return;
            }

            // A primeira linha válida define o número de frames na memória RAM simulada
            int numFrames = Integer.parseInt(linhasFiltradas.get(0));
            TabelaPaginas tabelaPaginas = new TabelaPaginas(numFrames);

            System.out.printf("Iniciando simulação com %d frames disponíveis.%n", numFrames);
            System.out.println("=".repeat(40));

            // As linhas seguintes são a sequência de acessos às páginas
            int passo = 1;
            for (int i = 1; i < linhasFiltradas.size(); i++) {
                int numeroPagina = Integer.parseInt(linhasFiltradas.get(i));

                // Processa o acesso na tabela de páginas
                ResultadoAcesso resultado = tabelaPaginas.acessarPagina(numeroPagina);

                // Renderiza o mapa de memória para o aluno ver o passo a passo
                tabelaPaginas.imprimirMapaMemoria(passo, numeroPagina, resultado.isHit(), resultado.getFrameId());
                passo += 1;
            }

            // Exibição das estatísticas finais da simulação
            System.out.println("\n================ STATS FINAIS ================");
            System.out.printf("Total de Acessos: %d%n", tabelaPaginas.getTotalAcessos());
            System.out.printf("Total de Page Faults: %d%n", tabelaPaginas.getTotalPageFaults());
            if (tabelaPaginas.getTotalAcessos() > 0) {
                double taxaFaults = (tabelaPaginas.getTotalPageFaults() / (double) tabelaPaginas.getTotalAcessos()) * 100.0;
                System.out.printf("Taxa de Page Faults: %.2f%%%n", taxaFaults);
            }
            System.out.println("==============================================");
        }
    }

    public static void main(String[] args) {
        // Permite passar o arquivo de entrada por argumento de linha de comando ou usa um padrão
        String arquivoEntrada = args.length > 0 ? args[0] : "entrada.txt";
        Simulador simulador = new Simulador(arquivoEntrada);
        simulador.executar();
    }
}