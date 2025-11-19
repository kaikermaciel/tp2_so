import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Scanner;

public class ProdutoEscalarLog {

    private static final int TAMANHO_VETOR = 50_000_000; 

    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);

        // --- 1. Identificação do Ambiente (Notebook ou Lab) ---
        System.out.println("=== Configuração do Experimento ===");
        System.out.println("Qual computador você está usando?");
        System.out.println("1 - Notebook Local");
        System.out.println("2 - PC Lab");
        System.out.print("Opção: ");
        int opcaoPc = scanner.nextInt();
        
        String nomePc = (opcaoPc == 1) ? "Notebook Local" : "PC Lab";
        if (opcaoPc != 1 && opcaoPc != 2) nomePc = "Outro/Desconhecido";

        System.out.println("\nGerando vetores de tamanho: " + TAMANHO_VETOR + "...");
        long[] vetorA = gerarVetor(TAMANHO_VETOR);
        long[] vetorB = gerarVetor(TAMANHO_VETOR);
        
        System.out.print("Digite o número de threads para o teste paralelo: ");
        int numThreads = scanner.nextInt();

        // --- 2. Execução Sequencial ---
        System.out.println("\n>>> Executando Sequencial...");
        long inicioSeq = System.nanoTime();
        long resultadoSeq = calcularSequencial(vetorA, vetorB);
        long fimSeq = System.nanoTime();
        double tempoSeq = (fimSeq - inicioSeq) / 1e9; 

        // --- 3. Execução Paralela ---
        System.out.println(">>> Executando Paralelo (" + numThreads + " threads)...");
        long inicioPar = System.nanoTime();
        long resultadoPar = calcularParalelo(vetorA, vetorB, numThreads);
        long fimPar = System.nanoTime();
        double tempoPar = (fimPar - inicioPar) / 1e9; 

        // --- 4. Cálculos Finais ---
        double speedup = tempoSeq / tempoPar;

        System.out.println("\n=== Resultados ===");
        System.out.printf("Tempo Sequencial: %.4f s\n", tempoSeq);
        System.out.printf("Tempo Paralelo:   %.4f s\n", tempoPar);
        System.out.printf("Speedup (Sp):     %.2f x\n", speedup);

        if (resultadoSeq == resultadoPar) {
            System.out.println("Validação: OK (Resultados iguais)");
        } else {
            System.err.println("Validação: ERRO (Resultados diferentes)");
        }

        // --- 5. Salvar em Arquivo ---
        salvarLog(nomePc, TAMANHO_VETOR, numThreads, tempoSeq, tempoPar, speedup);
        
        scanner.close();
    }

    // --- Método para salvar no TXT ---
    private static void salvarLog(String pc, int tamanho, int threads, double tSeq, double tPar, double sp) {
        // O parâmetro 'true' no FileWriter permite adicionar ao fim do arquivo sem apagar o anterior
        try (FileWriter fw = new FileWriter("resultados_produto_escalar.txt", true);
             PrintWriter pw = new PrintWriter(fw)) {
            
            // Formato: PC | Tamanho | Threads | T.Seq | T.Par | Speedup
            pw.printf("PC: %s | Tamanho: %d | Threads: %d | T.Seq: %.4fs | T.Par: %.4fs | Sp: %.2f%n", 
                      pc, tamanho, threads, tSeq, tPar, sp);
            
            System.out.println("\n[!] Resultado salvo em 'resultados_produto_escalar.txt'");
            
        } catch (IOException e) {
            System.err.println("Erro ao salvar arquivo: " + e.getMessage());
        }
    }

    // --- Métodos de Cálculo (Iguais ao anterior) ---
    public static long calcularSequencial(long[] a, long[] b) {
        long soma = 0;
        for (int i = 0; i < a.length; i++) {
            soma += a[i] * b[i];
        }
        return soma;
    }

    public static long calcularParalelo(long[] a, long[] b, int numThreads) throws InterruptedException {
        WorkerThread[] threads = new WorkerThread[numThreads];
        int tamanhoBloco = a.length / numThreads;

        for (int i = 0; i < numThreads; i++) {
            int inicio = i * tamanhoBloco;
            int fim = (i == numThreads - 1) ? a.length : (i + 1) * tamanhoBloco;
            threads[i] = new WorkerThread(a, b, inicio, fim);
            threads[i].start();
        }

        long somaTotal = 0;
        for (int i = 0; i < numThreads; i++) {
            threads[i].join();
            somaTotal += threads[i].getSomaParcial();
        }
        return somaTotal;
    }

    static class WorkerThread extends Thread {
        private final long[] a;
        private final long[] b;
        private final int inicio;
        private final int fim;
        private long somaParcial = 0;

        public WorkerThread(long[] a, long[] b, int inicio, int fim) {
            this.a = a;
            this.b = b;
            this.inicio = inicio;
            this.fim = fim;
        }

        @Override
        public void run() {
            for (int i = inicio; i < fim; i++) {
                somaParcial += a[i] * b[i];
            }
        }

        public long getSomaParcial() {
            return somaParcial;
        }
    }

    private static long[] gerarVetor(int tamanho) {
        Random rand = new Random();
        long[] v = new long[tamanho];
        for (int i = 0; i < tamanho; i++) {
            v[i] = rand.nextInt(100);
        }
        return v;
    }
}