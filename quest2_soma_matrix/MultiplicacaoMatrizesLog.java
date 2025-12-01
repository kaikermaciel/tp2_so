import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Scanner;

public class MultiplicacaoMatrizesLog {

    // IMPORTANTE: Matriz O(N^3) é muito pesado.
    // Comece com 500 ou 1000. Se seu PC for forte, tente 2000.
    // Não use valores como 50 milhões aqui, ou travará a máquina.
    private static final int N = 1000; // Matrizes quadradas N x N

    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);

        // --- 1. Identificação ---
        System.out.println("=== Configuração: Multiplicação de Matrizes ===");
        System.out.println("Qual computador você está usando?");
        System.out.println("1 - Notebook Local");
        System.out.println("2 - PC Lab");
        System.out.print("Opção: ");
        int opcaoPc = scanner.nextInt();
        
        String nomePc = (opcaoPc == 1) ? "Notebook Local" : "PC Lab";
        if (opcaoPc != 1 && opcaoPc != 2) nomePc = "Outro/Desconhecido";

        System.out.println("\nGerando matrizes " + N + "x" + N + "... (Aguarde)");
        long[][] matA = gerarMatriz(N);
        long[][] matB = gerarMatriz(N);
        
        System.out.print("Digite o número de threads: ");
        int numThreads = scanner.nextInt();

        // --- 2. Execução Sequencial ---
        System.out.println("\n>>> Executando Sequencial (pode demorar)...");
        long inicioSeq = System.nanoTime();
        long[][] resSeq = multiplicarSequencial(matA, matB);
        long fimSeq = System.nanoTime();
        double tempoSeq = (fimSeq - inicioSeq) / 1e9;
        System.out.printf("Tempo Sequencial: %.4f s\n", tempoSeq);

        // --- 3. Execução Paralela ---
        System.out.println(">>> Executando Paralelo (" + numThreads + " threads)...");
        long inicioPar = System.nanoTime();
        long[][] resPar = multiplicarParalelo(matA, matB, numThreads);
        long fimPar = System.nanoTime();
        double tempoPar = (fimPar - inicioPar) / 1e9;
        System.out.printf("Tempo Paralelo:   %.4f s\n", tempoPar);

        // --- 4. Validação e Speedup ---
        double speedup = tempoSeq / tempoPar;
        System.out.printf("Speedup (Sp):     %.2f x\n", speedup);

        boolean validado = compararMatrizes(resSeq, resPar);
        if (validado) {
            System.out.println("Validação: OK (Matrizes idênticas)");
        } else {
            System.err.println("Validação: ERRO (Diferenças encontradas)");
        }

        // --- 5. Salvar Log ---
        salvarLog(nomePc, N, numThreads, tempoSeq, tempoPar, speedup);
        
        scanner.close();
    }

    // --- Lógica Sequencial (O(N^3)) ---
    public static long[][] multiplicarSequencial(long[][] A, long[][] B) {
        long[][] C = new long[N][N];
        for (int i = 0; i < N; i++) {       // Linha de A
            for (int j = 0; j < N; j++) {   // Coluna de B
                for (int k = 0; k < N; k++) { // Somatório
                    C[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        return C;
    }

    // --- Lógica Paralela ---
    public static long[][] multiplicarParalelo(long[][] A, long[][] B, int numThreads) throws InterruptedException {
        long[][] C = new long[N][N];
        WorkerThread[] threads = new WorkerThread[numThreads];
        int linhasPorThread = N / numThreads;

        for (int i = 0; i < numThreads; i++) {
            int linhaInicio = i * linhasPorThread;
            int linhaFim = (i == numThreads - 1) ? N : (linhaInicio + linhasPorThread);
            
            threads[i] = new WorkerThread(A, B, C, linhaInicio, linhaFim);
            threads[i].start();
        }

        for (int i = 0; i < numThreads; i++) {
            threads[i].join();
        }
        return C;
    }

    // Thread que calcula um bloco de LINHAS da matriz resultante
    static class WorkerThread extends Thread {
        private final long[][] A, B, C;
        private final int linhaInicio, linhaFim;

        public WorkerThread(long[][] A, long[][] B, long[][] C, int linhaInicio, int linhaFim) {
            this.A = A;
            this.B = B;
            this.C = C;
            this.linhaInicio = linhaInicio;
            this.linhaFim = linhaFim;
        }

        @Override
        public void run() {
            // Itera apenas sobre as linhas designadas para esta thread
            for (int i = linhaInicio; i < linhaFim; i++) {
                for (int j = 0; j < N; j++) {
                    long soma = 0;
                    for (int k = 0; k < N; k++) {
                        soma += A[i][k] * B[k][j];
                    }
                    C[i][j] = soma;
                }
            }
        }
    }

    // --- Utilitários ---
    private static void salvarLog(String pc, int tamanho, int threads, double tSeq, double tPar, double sp) {
        try (FileWriter fw = new FileWriter("resultados_matriz.txt", true);
             PrintWriter pw = new PrintWriter(fw)) {
            
            pw.printf("PC: %s | Matriz: %dx%d | Threads: %d | T.Seq: %.4fs | T.Par: %.4fs | Sp: %.2f%n", 
                      pc, tamanho, tamanho, threads, tSeq, tPar, sp);
            
            System.out.println("\n[!] Salvo em 'resultados_matriz.txt'");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static long[][] gerarMatriz(int n) {
        Random rand = new Random();
        long[][] m = new long[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                m[i][j] = rand.nextInt(10); // Valores pequenos para evitar overflow
            }
        }
        return m;
    }

    private static boolean compararMatrizes(long[][] m1, long[][] m2) {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (m1[i][j] != m2[i][j]) return false;
            }
        }
        return true;
    }
}