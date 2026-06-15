import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimuladorMemoriaLFU {

    static class Frame {
        private final int idFrame;
        private Integer paginaAlocada;
        private int frequency;
        private long ordemChegada;

        Frame(int idFrame) {
            this.idFrame = idFrame;
            this.paginaAlocada = null;
            this.frequency = 0;
            this.ordemChegada = 0;
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

        int getFrequency() {
            return frequency;
        }

        void setFrequency(int frequency) {
            this.frequency = frequency;
        }

        void incrementarFrequency() {
            this.frequency++;
        }

        long getOrdemChegada() {
            return ordemChegada;
        }

        void setOrdemChegada(long ordemChegada) {
            this.ordemChegada = ordemChegada;
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

    static class TabelaPaginas {
        private final List<Frame> frames;
        private int totalPageFaults;
        private int totalAcessos;
        private long contadorFIFO;

        TabelaPaginas(int numFrames) {
            this.frames = new ArrayList<>();

            for (int i = 0; i < numFrames; i++) {
                this.frames.add(new Frame(i));
            }

            this.totalPageFaults = 0;
            this.totalAcessos = 0;
            this.contadorFIFO = 0;
        }

        ResultadoAcesso acessarPagina(int numeroPagina) {
            totalAcessos++;

            // HIT
            for (Frame frame : frames) {
                if (frame.getPaginaAlocada() != null
                        && frame.getPaginaAlocada() == numeroPagina) {

                    frame.incrementarFrequency();

                    return new ResultadoAcesso(true, frame.getIdFrame());
                }
            }

            // PAGE FAULT
            totalPageFaults++;

            // Procura frame vazio
            for (Frame frame : frames) {
                if (frame.getPaginaAlocada() == null) {

                    frame.setPaginaAlocada(numeroPagina);
                    frame.setFrequency(1);
                    frame.setOrdemChegada(contadorFIFO++);

                    return new ResultadoAcesso(false, frame.getIdFrame());
                }
            }

            // Memória cheia → LFU com fallback FIFO
            int frameVitimaId = substituirPagina(numeroPagina);

            return new ResultadoAcesso(false, frameVitimaId);
        }

        private int substituirPagina(int novaPagina) {

            Frame vitima = frames.get(0);

            for (Frame frame : frames) {

                // LFU: menor frequência
                if (frame.getFrequency() < vitima.getFrequency()) {
                    vitima = frame;
                }

                // Empate → FIFO
                else if (frame.getFrequency() == vitima.getFrequency()) {

                    if (frame.getOrdemChegada() < vitima.getOrdemChegada()) {
                        vitima = frame;
                    }
                }
            }

            vitima.setPaginaAlocada(novaPagina);
            vitima.setFrequency(1);
            vitima.setOrdemChegada(contadorFIFO++);

            return vitima.getIdFrame();
        }

        void imprimirMapaMemoria(int passo,
                                 int paginaAcessada,
                                 boolean foiHit,
                                 Integer frameAlterado) {

            String status = foiHit ? "Hit" : "Page Fault";

            System.out.printf(
                    "%n--- Passo %d: Acesso à Página %d (%s) ---%n",
                    passo,
                    paginaAcessada,
                    status);

            for (Frame frame : frames) {

                String conteudo;

                if (frame.getPaginaAlocada() != null) {
                    conteudo = String.format(
                            "Página %d | Freq=%d",
                            frame.getPaginaAlocada(),
                            frame.getFrequency());
                } else {
                    conteudo = "[Vazio]";
                }

                String marcador =
                        (!foiHit
                                && frameAlterado != null
                                && frame.getIdFrame() == frameAlterado)
                                ? " <-- Alterado"
                                : "";

                System.out.printf(
                        "[Frame %d]: %s%s%n",
                        frame.getIdFrame(),
                        conteudo,
                        marcador);
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

    static class Simulador {
        private final String caminhoArquivo;

        Simulador(String caminhoArquivo) {
            this.caminhoArquivo = caminhoArquivo;
        }

        void executar() {

            List<String> linhas = new ArrayList<>();

            try (BufferedReader arquivo =
                         new BufferedReader(
                                 new FileReader(caminhoArquivo))) {

                String linha;

                while ((linha = arquivo.readLine()) != null) {
                    linhas.add(linha);
                }

            } catch (IOException e) {
                System.out.printf(
                        "Erro: O arquivo '%s' não foi encontrado.%n",
                        caminhoArquivo);
                return;
            }

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

            int numFrames = Integer.parseInt(linhasFiltradas.get(0));

            TabelaPaginas tabelaPaginas =
                    new TabelaPaginas(numFrames);

            System.out.printf(
                    "Iniciando simulação LFU (fallback FIFO) com %d frames disponíveis.%n",
                    numFrames);

            System.out.println("=".repeat(40));

            int passo = 1;

            for (int i = 1; i < linhasFiltradas.size(); i++) {

                int numeroPagina =
                        Integer.parseInt(linhasFiltradas.get(i));

                ResultadoAcesso resultado =
                        tabelaPaginas.acessarPagina(numeroPagina);

                tabelaPaginas.imprimirMapaMemoria(
                        passo,
                        numeroPagina,
                        resultado.isHit(),
                        resultado.getFrameId());

                passo++;
            }

            System.out.println("\n================ STATS FINAIS ================");
            System.out.printf(
                    "Total de Acessos: %d%n",
                    tabelaPaginas.getTotalAcessos());

            System.out.printf(
                    "Total de Page Faults: %d%n",
                    tabelaPaginas.getTotalPageFaults());

            if (tabelaPaginas.getTotalAcessos() > 0) {

                double taxaFaults =
                        (tabelaPaginas.getTotalPageFaults()
                                / (double) tabelaPaginas.getTotalAcessos())
                                * 100.0;

                System.out.printf(
                        "Taxa de Page Faults: %.2f%%%n",
                        taxaFaults);
            }

            System.out.println("==============================================");
        }
    }

    public static void main(String[] args) {

        String arquivoEntrada =
                args.length > 0
                        ? args[0]
                        : "entrada.txt";

        Simulador simulador =
                new Simulador(arquivoEntrada);

        simulador.executar();
    }
}