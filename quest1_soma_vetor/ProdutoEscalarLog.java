import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Scanner;

/**
 * Questão 1: Produto Escalar de Vetores (Sequencial vs Paralelo)
 * Disciplina: Sistemas Operacionais - UFAM
 * * Objetivo: Calcular o produto escalar entre dois vetores grandes.
 * Estratégia: Dividir o vetor em blocos contíguos e processar cada bloco em uma thread separada.
 * Métricas: Compara o tempo de execução (Speedup) e salva em log.
 */
public class ProdutoEscalarLog {

    // Tamanho do vetor. Valores altos (ex: 50 milhões) são necessários para que o
    // custo computacional supere o overhead de criação das threads.
    private static final int TAMANHO_VETOR = 50_000_000; 

    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);

        // --- 1. Identificação do Ambiente ---
        // Permite classificar os resultados no relatório final baseados no hardware utilizado.
        System.out.println("=== Configuração do Experimento ===");
        System.out.println("Qual computador você está usando?");
        System.out.println("1 - Notebook Local");
        System.out.println("2 - PC Lab");
        System.out.print("Opção: ");
        int opcaoPc = scanner.nextInt();
        
        String nomePc = (opcaoPc == 1) ? "Notebook Local" : "PC Lab";
        if (opcaoPc != 1 && opcaoPc != 2) nomePc = "Outro/Desconhecido";

        // Geração dos dados: Preenchimento aleatório para simular carga real de processamento.
        System.out.println("\nGerando vetores de tamanho: " + TAMANHO_VETOR + "...");
        long[] vetorA = gerarVetor(TAMANHO_VETOR);
        long[] vetorB = gerarVetor(TAMANHO_VETOR);
        
        System.out.print("Digite o número de threads para o teste paralelo: ");
        int numThreads = scanner.nextInt();

        // --- 2. Execução Sequencial (Single-Thread) ---
        // Executa o cálculo na thread principal para estabelecer o tempo base (Ts).
        System.out.println("\n>>> Executando Sequencial...");
        long inicioSeq = System.nanoTime();
        long resultadoSeq = calcularSequencial(vetorA, vetorB);
        long fimSeq = System.nanoTime();
        
        // Conversão de nanosegundos para segundos
        double tempoSeq = (fimSeq - inicioSeq) / 1e9; 

        // --- 3. Execução Paralela (Multi-Thread) ---
        // Distribui a carga entre 'n' threads para obter o tempo paralelo (Tp).
        System.out.println(">>> Executando Paralelo (" + numThreads + " threads)...");
        long inicioPar = System.nanoTime();
        long resultadoPar = calcularParalelo(vetorA, vetorB, numThreads);
        long fimPar = System.nanoTime();
        double tempoPar = (fimPar - inicioPar) / 1e9; 

        // --- 4. Análise de Desempenho ---
        // Speedup (Aceleração) = Tempo Sequencial / Tempo Paralelo
        double speedup = tempoSeq / tempoPar;

        System.out.println("\n=== Resultados ===");
        System.out.printf("Tempo Sequencial: %.4f s\n", tempoSeq);
        System.out.printf("Tempo Paralelo:   %.4f s\n", tempoPar);
        System.out.printf("Speedup (Sp):     %.2f x\n", speedup);

        // Validação de Integridade: Confirma se a soma paralela é matematicamente igual à sequencial.
        if (resultadoSeq == resultadoPar) {
            System.out.println("Validação: OK (Resultados iguais)");
        } else {
            System.err.println("Validação: ERRO (Resultados diferentes - Verifique a lógica das threads)");
        }

        // --- 5. Persistência ---
        // Salva os dados para análise posterior em planilhas/gráficos.
        salvarLog(nomePc, TAMANHO_VETOR, numThreads, tempoSeq, tempoPar, speedup);
        
        scanner.close();
    }

    // --- Lógica de Persistência em Arquivo ---
    private static void salvarLog(String pc, int tamanho, int threads, double tSeq, double tPar, double sp) {
        // Usa o modo 'append' (true) para não sobrescrever testes anteriores.
        try (FileWriter fw = new FileWriter("resultados_produto_escalar.txt", true);
             PrintWriter pw = new PrintWriter(fw)) {
            
            // Formato CSV (separado por pipes) facilita a importação no Excel.
            pw.printf("PC: %s | Tamanho: %d | Threads: %d | T.Seq: %.4fs | T.Par: %.4fs | Sp: %.2f%n", 
                      pc, tamanho, threads, tSeq, tPar, sp);
            
            System.out.println("\n[!] Resultado salvo em 'resultados_produto_escalar.txt'");
            
        } catch (IOException e) {
            System.err.println("Erro ao salvar arquivo: " + e.getMessage());
        }
    }

    // --- Algoritmo Sequencial (O(N)) ---
    public static long calcularSequencial(long[] a, long[] b) {
        long soma = 0;
        for (int i = 0; i < a.length; i++) {
            soma += a[i] * b[i];
        }
        return soma;
    }

    // --- Algoritmo Paralelo ---
    public static long calcularParalelo(long[] a, long[] b, int numThreads) throws InterruptedException {
        WorkerThread[] threads = new WorkerThread[numThreads];
        
        // Define o tamanho do "chunk" (fatia) do vetor que cada thread vai processar.
        int tamanhoBloco = a.length / numThreads;

        // Criação e Disparo das Threads
        for (int i = 0; i < numThreads; i++) {
            int inicio = i * tamanhoBloco;
            
            // Tratamento de Resto da Divisão:
            // Se a divisão do vetor não for exata, a última thread assume o trabalho extra
            // indo até o final do array (a.length), garantindo que nenhum índice seja ignorado.
            int fim = (i == numThreads - 1) ? a.length : (i + 1) * tamanhoBloco;
            
            threads[i] = new WorkerThread(a, b, inicio, fim);
            threads[i].start();
        }

        // Fase de Agregação (Reduce)
        long somaTotal = 0;
        for (int i = 0; i < numThreads; i++) {
            threads[i].join(); // Main espera a thread[i] terminar
            // Soma o resultado parcial calculado por aquela thread
            somaTotal += threads[i].getSomaParcial();
        }
        return somaTotal;
    }

    /**
     * Classe WorkerThread
     * Responsável por calcular o produto escalar de uma sub-região (fatia) dos vetores.
     */
    static class WorkerThread extends Thread {
        // Referências para os vetores originais na memória (Evita cópia de dados, economizando RAM)
        private final long[] a;
        private final long[] b;
        // Limites de atuação desta thread específica
        private final int inicio;
        private final int fim;
        // Variável local para acumular o resultado parcial.
        // O uso de variável local evita a necessidade de 'synchronized' dentro do loop,
        // o que mataria a performance paralela.
        private long somaParcial = 0;

        public WorkerThread(long[] a, long[] b, int inicio, int fim) {
            this.a = a;
            this.b = b;
            this.inicio = inicio;
            this.fim = fim;
        }

        @Override
        public void run() {
            // Loop crítico: Executa o cálculo apenas na fatia designada (inicio -> fim)
            for (int i = inicio; i < fim; i++) {
                somaParcial += a[i] * b[i];
            }
        }

        // Getter para que a thread principal possa recuperar o resultado após o join()
        public long getSomaParcial() {
            return somaParcial;
        }
    }

    // Utilitário para gerar vetores com valores aleatórios (0 a 99)
    private static long[] gerarVetor(int tamanho) {
        Random rand = new Random();
        long[] v = new long[tamanho];
        for (int i = 0; i < tamanho; i++) {
            v[i] = rand.nextInt(100);
        }
        return v;
    }
}