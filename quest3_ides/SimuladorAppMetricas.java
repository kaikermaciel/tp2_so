import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SimuladorAppMetricas extends JFrame {

    private JTextArea logArea;
    private JProgressBar progressBar;
    private JButton btnSequencial;
    private JButton btnThread;
    private JButton btnInteracao;
    
    // Componentes de Mensuração
    private JLabel lblTempo;
    private JLabel lblCliques;
    private int contadorCliques = 0;
    private long inicioExecucao = 0;

    public SimuladorAppMetricas() {
        setTitle("TP2 - Q3: Simulador com Métricas");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- PAINEL SUPERIOR (Controles) ---
        JPanel panelTopo = new JPanel(new GridLayout(4, 1, 5, 5));
        
        btnSequencial = new JButton("1. Iniciar Sequencial (Bloqueante)");
        btnSequencial.setBackground(new Color(255, 200, 200)); // Vermelho
        
        btnThread = new JButton("2. Iniciar com Thread (Não-Bloqueante)");
        btnThread.setBackground(new Color(200, 255, 200));     // Verde
        
        btnInteracao = new JButton("3. CLIQUE AQUI (Teste de Responsividade)");
        btnInteracao.setFont(new Font("Arial", Font.BOLD, 14));
        
        // Painel de Status
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

        // --- PAINEL CENTRAL (Log) ---
        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // --- PAINEL INFERIOR (Progresso) ---
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(600, 30));
        add(progressBar, BorderLayout.SOUTH);

        // --- AÇÕES ---

        // 1. MODO SEQUENCIAL
        btnSequencial.addActionListener(e -> {
            resetarMetricas();
            log(">>> Iniciando Modo Sequencial...");
            log("[ALERTA] A interface irá CONGELAR. Tente clicar no botão 3.");
            
            long inicio = System.currentTimeMillis();
            
            // Executa na mesma thread da interface (Bloqueia tudo)
            executarTarefaPesada(); 
            
            long fim = System.currentTimeMillis();
            double tempoTotal = (fim - inicio) / 1000.0;
            lblTempo.setText("Tempo Final: " + tempoTotal + "s");
            
            log(">>> Modo Sequencial Finalizado em " + tempoTotal + "s.");
            log(">>> Cliques processados DURANTE a execução: 0 (Visualmente travado)");
        });

        // 2. MODO THREAD
        btnThread.addActionListener(e -> {
            resetarMetricas();
            log(">>> Iniciando Modo Thread...");
            log("[INFO] A interface deve responder. Clique no botão 3!");

            // Nova Thread para processamento
            new Thread(() -> {
                long inicio = System.currentTimeMillis();
                
                executarTarefaPesada();
                
                long fim = System.currentTimeMillis();
                double tempoTotal = (fim - inicio) / 1000.0;
                
                // Atualiza UI ao final
                SwingUtilities.invokeLater(() -> {
                    lblTempo.setText("Tempo Final: " + tempoTotal + "s");
                    log(">>> Modo Thread Finalizado em " + tempoTotal + "s.");
                    log(">>> Total de cliques processados em tempo real: " + contadorCliques);
                });
            }).start();
        });

        // 3. BOTÃO DE INTERAÇÃO (O TESTE)
        btnInteracao.addActionListener(e -> {
            contadorCliques++;
            lblCliques.setText("Cliques Processados: " + contadorCliques);
            log("[INTERAÇÃO] Clique registrado: " + contadorCliques);
        });
    }

    private void executarTarefaPesada() {
        for (int i = 0; i <= 10; i++) {
            try {
                Thread.sleep(500); // Simula trabalho pesado (500ms)
            } catch (InterruptedException ex) {}

            int progresso = i * 10;
            
            // Atualização segura da interface
            // No modo sequencial, isso só desenha quando o loop acabar (efeito colateral do travamento)
            // No modo thread, isso desenha em tempo real
            if (SwingUtilities.isEventDispatchThread()) {
                progressBar.setValue(progresso); // Modo Sequencial
            } else {
                SwingUtilities.invokeLater(() -> progressBar.setValue(progresso)); // Modo Thread
            }
        }
    }

    private void resetarMetricas() {
        contadorCliques = 0;
        lblCliques.setText("Cliques Processados: 0");
        lblTempo.setText("Tempo: Rodando...");
        progressBar.setValue(0);
        logArea.setText("");
    }

    private void log(String texto) {
        String hora = new SimpleDateFormat("HH:mm:ss").format(new Date());
        // Se estiver fora da thread principal (Modo Thread), usa invokeLater para evitar erro
        if (SwingUtilities.isEventDispatchThread()) {
            logArea.append("[" + hora + "] " + texto + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        } else {
            SwingUtilities.invokeLater(() -> {
                logArea.append("[" + hora + "] " + texto + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SimuladorAppMetricas().setVisible(true));
    }
}