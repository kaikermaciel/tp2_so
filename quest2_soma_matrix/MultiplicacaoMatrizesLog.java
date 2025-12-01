import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Scanner;

/**
 * Questão 2: Multiplicação de Matrizes (Sequencial vs Paralelo)
 * Disciplina: Sistemas Operacionais - UFAM
 * * Objetivo: Comparar o desempenho da multiplicação de matrizes utilizando
 * uma abordagem tradicional (Single Thread) versus uma abordagem paralela (Multi-thread).
 * O resultado é salvo em um arquivo de log para análise de Speedup.
 */
public class MultiplicacaoMatrizesLog {

    // Define a dimensão das matrizes (NxN). 
    // OBS: O algoritmo é O(N^3), então dobrar N aumenta o tempo em 8x.
    // Recomendado: 1000 a 2000 para testes rápidos.
    private static final int N = 1000; 

    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);

        // --- 1. Identificação do Ambiente ---
        // Necessário para o relatório: saber em qual hardware o teste rodou.
        System.out.println("=== Configuração: Multiplicação de Matrizes ===");
        System.out.println("Qual computador você está usando?");
        System.out.println("1 - Notebook Local");
        System.out.println("2 - PC Lab");
        System.out.print("Opção: ");
        int opcaoPc = scanner.nextInt();
        
        String nomePc = (opcaoPc == 1) ? "Notebook Local" : "PC Lab";
        if (opcaoPc != 1 && opcaoPc != 2) nomePc = "Outro/Desconhecido";

        // Geração de dados aleatórios para evitar viés de cache ou otimização do compilador
        System.out.println("\nGerando matrizes " + N + "x" + N + "... (Aguarde)");
        long[][] matA = gerarMatriz(N);
        long[][] matB = gerarMatriz(N);
        
        System.out.print("Digite o número de threads: ");
        int numThreads = scanner.nextInt();

        // --- 2. Execução Sequencial ---
        // Serve como 'Base Line' para calcular o ganho de desempenho.
        System.out.println("\n>>> Executando Sequencial (pode demorar)...");
        long inicioSeq = System.nanoTime(); // Medição precisa em nanosegundos
        long[][] resSeq = multiplicarSequencial(matA, matB);
        long fimSeq = System.nanoTime();
        
        // Conversão para segundos (1e9 = 1.000.000.000)
        double tempoSeq = (fimSeq - inicioSeq) / 1e9;
        System.out.printf("Tempo Sequencial: %.4f s\n", tempoSeq);

        // --- 3. Execução Paralela ---
        System.out.println(">>> Executando Paralelo (" + numThreads + " threads)...");
        long inicioPar = System.nanoTime();
        long[][] resPar = multiplicarParalelo(matA, matB, numThreads);
        long fimPar = System.nanoTime();
        
        double tempoPar = (fimPar - inicioPar) / 1e9;
        System.out.printf("Tempo Paralelo:   %.4f s\n", tempoPar);

        // --- 4. Análise de Resultados ---
        // Speedup = Tempo Sequencial / Tempo Paralelo.
        // Se Sp > 1, houve ganho. Se Sp < 1, o overhead das threads piorou o tempo.
        double speedup = tempoSeq / tempoPar;
        System.out.printf("Speedup (Sp):     %.2f x\n", speedup);

        // Validação de Corretude: Garante que a versão paralela não introduziu erros de cálculo.
        boolean validado = compararMatrizes(resSeq, resPar);
        if (validado) {
            System.out.println("Validação: OK (Matrizes idênticas)");
        } else {
            System.err.println("Validação: ERRO (Diferenças encontradas entre Seq e Par)");
        }

        // --- 5. Persistência de Dados ---
        // Salva em TXT (append) para facilitar a criação de tabelas e gráficos depois.
        salvarLog(nomePc, N, numThreads, tempoSeq, tempoPar, speedup);
        
        scanner.close();
    }

    /**
     * Algoritmo Clássico de Multiplicação de Matrizes.
     * Complexidade Cúbica: O(N^3).
     * Percorre Linha de A x Coluna de B.
     */
    public static long[][] multiplicarSequencial(long[][] A, long[][] B) {
        long[][] C = new long[N][N];
        for (int i = 0; i < N; i++) {           // Itera sobre linhas da Matriz A
            for (int j = 0; j < N; j++) {       // Itera sobre colunas da Matriz B
                for (int k = 0; k < N; k++) {   // Somatório do produto escalar
                    C[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        return C;
    }

    /**
     * Gerencia a execução paralela dividindo o trabalho.
     * Estratégia: Decomposição de Domínio (Divisão por linhas).
     * Cada thread fica responsável por calcular um bloco de linhas da matriz resultante.
     */
    public static long[][] multiplicarParalelo(long[][] A, long[][] B, int numThreads) throws InterruptedException {
        long[][] C = new long[N][N];
        WorkerThread[] threads = new WorkerThread[numThreads];
        
        // Define quantas linhas cada thread vai processar
        int linhasPorThread = N / numThreads;

        for (int i = 0; i < numThreads; i++) {
            int linhaInicio = i * linhasPorThread;
            
            // Tratamento de resto: A última thread pega todas as linhas restantes
            // para garantir que nenhuma linha seja esquecida se a divisão não for exata.
            int linhaFim = (i == numThreads - 1) ? N : (linhaInicio + linhasPorThread);
            
            // Cria e inicia a thread trabalhadora
            threads[i] = new WorkerThread(A, B, C, linhaInicio, linhaFim);
            threads[i].start();
        }

        // Barreira de Sincronização: O main espera todas as threads terminarem
        for (int i = 0; i < numThreads; i++) {
            threads[i].join();
        }
        
        return C;
    }

    /**
     * Classe interna que representa uma unidade de trabalho (Thread).
     * Recebe as referências das matrizes e o intervalo de linhas que deve calcular.
     */
    static class WorkerThread extends Thread {
        private final long[][] A, B, C;
        private final int linhaInicio, linhaFim;

        public WorkerThread(long[][] A, long[][] B, long[][] C, int linhaInicio, int linhaFim) {
            this.A = A;
            this.B = B;
            this.C = C; // Referência para escrever o resultado
            this.linhaInicio = linhaInicio;
            this.linhaFim = linhaFim;
        }

        @Override
        public void run() {
            // Executa o cálculo APENAS nas linhas designadas (linhaInicio até linhaFim).
            // Isso evita "Race Conditions" pois cada thread escreve em posições de memória exclusivas em C.
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

    // --- Métodos Auxiliares ---

    // Salva os resultados em formato texto formatado para leitura humana e importação (CSV-like)
    private static void salvarLog(String pc, int tamanho, int threads, double tSeq, double tPar, double sp) {
        try (FileWriter fw = new FileWriter("resultados_matriz.txt", true); // 'true' ativa modo append
             PrintWriter pw = new PrintWriter(fw)) {
            
            pw.printf("PC: %s | Matriz: %dx%d | Threads: %d | T.Seq: %.4fs | T.Par: %.4fs | Sp: %.2f%n", 
                      pc, tamanho, tamanho, threads, tSeq, tPar, sp);
            
            System.out.println("\n[!] Salvo em 'resultados_matriz.txt'");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Preenche matriz com valores aleatórios (0 a 9)
    private static long[][] gerarMatriz(int n) {
        Random rand = new Random();
        long[][] m = new long[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                m[i][j] = rand.nextInt(10); 
            }
        }
        return m;
    }

    // Verifica célula por célula se os resultados são iguais
    private static boolean compararMatrizes(long[][] m1, long[][] m2) {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (m1[i][j] != m2[i][j]) return false;
            }
        }
        return true;
    }
}