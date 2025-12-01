import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Questão 3: Simulação de Aplicação Interativa (Thread vs Sequencial)
 * Disciplina: Sistemas Operacionais
 * * Objetivo: Demonstrar visualmente o impacto do uso de Threads na responsividade (UX)
 * de uma aplicação gráfica.
 * * Cenário:
 * 1. Modo Sequencial: Executa uma tarefa pesada na thread principal de interface (EDT), causando congelamento.
 * 2. Modo Thread: Executa a tarefa em segundo plano (Background), mantendo a interface ativa.
 */
public class SimuladorAppMetricas extends JFrame {

    // Componentes da Interface Gráfica
    private JTextArea logArea;
    private JProgressBar progressBar;
    private JButton btnSequencial;
    private JButton btnThread;
    private JButton btnInteracao;
    
    // Variáveis para Mensuração (Métricas solicitadas no TP)
    private JLabel lblTempo;
    private JLabel lblCliques;
    private int contadorCliques = 0;

    public SimuladorAppMetricas() {
        // Configuração da Janela Principal
        setTitle("TP2 - Q3: Simulador com Métricas");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- PAINEL SUPERIOR (Botões de Controle) ---
        JPanel panelTopo = new JPanel(new GridLayout(4, 1, 5, 5));
        
        btnSequencial = new JButton("1. Iniciar Sequencial (Bloqueante)");
        btnSequencial.setBackground(new Color(255, 200, 200)); // Vermelho para indicar perigo/erro
        
        btnThread = new JButton("2. Iniciar com Thread (Não-Bloqueante)");
        btnThread.setBackground(new Color(200, 255, 200));     // Verde para indicar sucesso
        
        btnInteracao = new JButton("3. CLIQUE AQUI (Teste de Responsividade)");
        btnInteracao.setFont(new Font("Arial", Font.BOLD, 14));
        
        // Painel de Status (Labels de Tempo e Cliques)
        JPanel panelStatus = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        panelStatus.setBackground(Color.LIGHT_GRAY);
        lblTempo = new JLabel("Tempo: 0.000s");
        lblCliques = new JLabel("Cliques Processados: 0");
        lblTempo.setFont(new Font("Monospaced", Font.BOLD, 16));
        lblCliques.setFont(new Font("Monospaced", Font.BOLD, 16));
        panelStatus.add(lblTempo);
        panelStatus.add(lblCliques);

        panelTopo.add(btnSequencial);
        panelTopo.add(btnThread);
        panelTopo.add(btnInteracao);
        panelTopo.add(panelStatus);

        add(panelTopo, BorderLayout.NORTH);

        // --- PAINEL CENTRAL (Log de Eventos) ---
        logArea = new JTextArea();
        logArea.setEditable(false); // O usuário não deve editar o log manualmente
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // --- PAINEL INFERIOR (Barra de Progresso) ---
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(600, 30));
        add(progressBar, BorderLayout.SOUTH);

        // --- DEFINIÇÃO DAS AÇÕES DOS BOTÕES ---

        // CASO 1: EXECUÇÃO SEQUENCIAL (O "Mau Exemplo")
        // Executa a tarefa pesada DIRETAMENTE dentro do ActionListener.
        // Como o ActionListener roda na "Event Dispatch Thread" (EDT), a interface congela totalmente
        // até que o loop termine.
        btnSequencial.addActionListener(e -> {
            resetarMetricas();
            log(">>> Iniciando Modo Sequencial...");
            log("[ALERTA] A interface irá CONGELAR. Tente clicar no botão 3.");
            
            long inicio = System.currentTimeMillis();
            
            // CHAMADA BLOQUEANTE: O código não sai daqui até terminar.
            // A janela não redesenha e cliques são ignorados pelo SO.
            executarTarefaPesada(); 
            
            long fim = System.currentTimeMillis();
            double tempoTotal = (fim - inicio) / 1000.0;
            lblTempo.setText("Tempo Final: " + tempoTotal + "s");
            
            log(">>> Modo Sequencial Finalizado em " + tempoTotal + "s.");
            log(">>> Cliques processados DURANTE a execução: 0 (Visualmente travado)");
        });

        // CASO 2: EXECUÇÃO COM THREAD (O "Bom Exemplo")
        // Cria uma nova Thread (Worker) para processar a tarefa pesada.
        // A EDT fica livre para escutar cliques do mouse e redesenhar a janela.
        btnThread.addActionListener(e -> {
            resetarMetricas();
            log(">>> Iniciando Modo Thread...");
            log("[INFO] A interface deve responder. Clique no botão 3!");

            // Criação da Thread Paralela
            new Thread(() -> {
                long inicio = System.currentTimeMillis();
                
                // Executa o trabalho pesado fora da thread de interface
                executarTarefaPesada();
                
                long fim = System.currentTimeMillis();
                double tempoTotal = (fim - inicio) / 1000.0;
                
                // SwingUtilities.invokeLater:
                // Garante que a atualização final da UI (Label e Log) ocorra de volta na EDT.
                // Regra do Swing: Não toque em componentes visuais diretamente de outras threads.
                SwingUtilities.invokeLater(() -> {
                    lblTempo.setText("Tempo Final: " + tempoTotal + "s");
                    log(">>> Modo Thread Finalizado em " + tempoTotal + "s.");
                    log(">>> Total de cliques processados em tempo real: " + contadorCliques);
                });
            }).start();
        });

        // BOTÃO DE TESTE (Prova de Responsividade)
        // Se a aplicação estiver travada (Sequencial), este evento só será disparado DEPOIS que destravar.
        // Se estiver com Threads, este evento dispara imediatamente.
        btnInteracao.addActionListener(e -> {
            contadorCliques++;
            lblCliques.setText("Cliques Processados: " + contadorCliques);
            log("[INTERAÇÃO] Clique registrado: " + contadorCliques);
        });
    }

    /**
     * Simula uma tarefa computacionalmente intensiva ou de I/O (ex: Download, Processamento de Imagem).
     * Usa Thread.sleep para forçar a demora.
     */
    private void executarTarefaPesada() {
        for (int i = 0; i <= 10; i++) {
            try {
                // Simula atraso de 500ms por iteração (Total ~5 segundos)
                Thread.sleep(500); 
            } catch (InterruptedException ex) {}

            int progresso = i * 10;
            
            // Lógica Híbrida de Atualização de UI:
            // Precisamos atualizar a barra de progresso.
            
            if (SwingUtilities.isEventDispatchThread()) {
                // Se estamos no modo SEQUENCIAL, já estamos na thread certa.
                // Porém, visualmente, a barra não vai se mover até o fim, pois a thread está ocupada no sleep.
                progressBar.setValue(progresso); 
            } else {
                // Se estamos no modo THREAD, precisamos pedir para a EDT atualizar a barra
                // assim que possível. Isso cria a animação suave.
                SwingUtilities.invokeLater(() -> progressBar.setValue(progresso)); 
            }
        }
    }

    // Reseta a tela para um novo teste
    private void resetarMetricas() {
        contadorCliques = 0;
        lblCliques.setText("Cliques Processados: 0");
        lblTempo.setText("Tempo: Rodando...");
        progressBar.setValue(0);
        logArea.setText("");
    }

    // Método seguro para adicionar texto ao log vindo de qualquer thread
    private void log(String texto) {
        String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());
        
        Runnable updateLog = () -> {
            logArea.append("[" + hora + "] " + texto + "\n");
            // Rola o scroll automaticamente para baixo
            logArea.setCaretPosition(logArea.getDocument().getLength());
        };

        // Verifica se precisa agendar a atualização ou se pode fazer direto
        if (SwingUtilities.isEventDispatchThread()) {
            updateLog.run();
        } else {
            SwingUtilities.invokeLater(updateLog);
        }
    }

    public static void main(String[] args) {
        // Inicia a aplicação dentro da Thread de Eventos do Swing (Best Practice)
        SwingUtilities.invokeLater(() -> new SimuladorAppMetricas().setVisible(true));
    }
}