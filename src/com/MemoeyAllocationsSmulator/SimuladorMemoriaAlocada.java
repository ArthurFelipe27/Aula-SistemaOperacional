package com.MemoeyAllocationsSmulator;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class SimuladorMemoriaAlocada extends JFrame {

    //Atributos
    private final DefaultTableModel tabelaDeProcessosModel;
    private final JTable tabelaDeProcessos;

    private final JComboBox<String> estrategiaComCaixa;
    private final DefaultListModel<String> listadeModelodeProcessos;
    private final List<BlocosDeMemoria> memoriaBloqueada;
    private final List<Process> processos;

    private final JPanel memoriadePainel;
    private final JButton botaodeAlocar, botaodeResetar, simuladordeES;
    private final JTextField campoNome, campoTamanho;
    private JLabel statusDaMemoria;
    private int proximoNextFit = 0;

    public SimuladorMemoriaAlocada() {
        super("Simulador de alocação de memória");
        setLayout(new BorderLayout());

        estrategiaComCaixa = new JComboBox<>(new String[]{
                "First Fit", "Best Fit", "Worst Fit", "Next Fit"
        });

        listadeModelodeProcessos = new DefaultListModel<>();
        processos = new ArrayList<>();
        memoriaBloqueada = new ArrayList<>(Arrays.asList(
                new BlocosDeMemoria(0, 100),
                new BlocosDeMemoria(1, 150),
                new BlocosDeMemoria(2, 200),
                new BlocosDeMemoria(3, 250),
                new BlocosDeMemoria(4, 300),
                new BlocosDeMemoria(5, 350)
        ));

        // Painel de entrada
        JPanel painelDeEntrada = new JPanel(new GridLayout(2, 1));
        JPanel painelDeProcesso = new JPanel();

        painelDeProcesso.add(new JLabel("Nome:"));
        campoNome = new JTextField(5);
        painelDeProcesso.add(campoNome);

        painelDeProcesso.add(new JLabel("Tamanho:"));
        campoTamanho = new JTextField(5);
        painelDeProcesso.add(campoTamanho);

        painelDeProcesso.add(new JLabel("Prioridade"));
        String[] opcoesPrioridade = {"1 (Alta)", "2 (Média)", "3 (Baixa)"};
        JComboBox<String> caixaDePrioridade = new JComboBox<>(opcoesPrioridade);
        painelDeProcesso.add(caixaDePrioridade);

        botaodeAlocar = new JButton("Alocar");
        painelDeProcesso.add(botaodeAlocar);
        botaodeResetar = new JButton("Reiniciar");
        painelDeProcesso.add(botaodeResetar);
        simuladordeES = new JButton("Simular E/S bloqueante");
        painelDeProcesso.add(simuladordeES);
        painelDeEntrada.add(painelDeProcesso);

        JPanel painelDeEstrategia = new JPanel();
        painelDeEstrategia.add(new JLabel("Estratégia:"));
        painelDeEstrategia.add(estrategiaComCaixa);
        painelDeEntrada.add(painelDeEstrategia);
        add(painelDeEntrada, BorderLayout.NORTH);

        String[] colunas = {"Nome", "Prioridade", "Estado"};
        tabelaDeProcessosModel = new DefaultTableModel(colunas, 0);
        tabelaDeProcessos = new JTable(tabelaDeProcessosModel);
        add(new JScrollPane(tabelaDeProcessos), BorderLayout.WEST);

        // Painel de memória
        memoriadePainel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                desenharBlocosDeMemoria(g);
            }
        };

        memoriadePainel.setPreferredSize(new Dimension(600, 400));
        add(memoriadePainel, BorderLayout.CENTER);

        // Lista de processos
        JList<String> listaDeProcessos = new JList<>(listadeModelodeProcessos);
        add(new JScrollPane(listaDeProcessos), BorderLayout.EAST);

        // Status da memória
        statusDaMemoria = new JLabel("Memória: Total: 0KB | Ocupado: 0KB | Livre: 0KB");
        add(statusDaMemoria, BorderLayout.SOUTH);

        // Ações
        botaodeAlocar.addActionListener(e -> {
    String nome = campoNome.getText().trim();
    int size;
    try {
        size = Integer.parseInt(campoTamanho.getText().trim());
    } catch (NumberFormatException ex) {
        JOptionPane.showMessageDialog(this, "Tamanho inválido");
        return;
    }

    String strategy = (String) estrategiaComCaixa.getSelectedItem();
    int prioridade = caixaDePrioridade.getSelectedIndex() + 1;
    Process p = new Process(nome, size, prioridade);
    p.estado = Process.Estado.PRONTO;  // Defina o estado como PRONTO
    processos.add(p);

    // Tenta alocar as páginas antes de alocar o processo
    boolean alocouComSucesso = switch (strategy) {
        case "First Fit" -> allocateFirstFit(p);
        case "Best Fit" -> allocateBestFit(p);
        case "Worst Fit" -> allocateWorstFit(p);
        case "Next Fit" -> allocateNextFit(p);
        default -> false;
    };

    if (alocouComSucesso) {
        listadeModelodeProcessos.addElement(p.toString());
        repaint();
        atualizarTabelaDeProcessos();  // Chamando o método para atualizar a tabela
    } else {
        JOptionPane.showMessageDialog(this, "Não foi possível alocar o processo.");
    }

    atualizaStatusDaMemoria();
});

        

        botaodeResetar.addActionListener(e -> {
            processos.clear();
            listadeModelodeProcessos.clear();
            memoriaBloqueada.forEach(BlocosDeMemoria::clear);
            proximoNextFit = 0;
            atualizaStatusDaMemoria();
            repaint();
        });
        
        //Simulador de ES
        simuladordeES.addActionListener(e -> {
            if (processos.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nenhum processo em execução!");
                return;
            }
            new Thread(() -> {
                Process p = processos.get(new Random().nextInt(processos.size()));
                p.bloqueado = true;
                p.estado = Process.Estado.BLOQUEADO;  // Mudar para BLOQUEADO
                atualizarTabelaDeProcessos();
                repaint();
                try {
                    Thread.sleep(3000); // Simulando o tempo de bloqueio
                } catch (InterruptedException ignored) {}
                p.bloqueado = false;
                p.estado = Process.Estado.PRONTO;  // Mudar para PRONTO
                atualizarTabelaDeProcessos();
                repaint();
            }).start();
        });

        pack();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);

        inicializarMemoria();
        iniciarEscalonador();
    }
    
    private boolean allocateFirstFit(Process p) {
        for (BlocosDeMemoria bloco : memoriaBloqueada) {
            if (bloco.estar() && bloco.size >= p.size) {
                bloco.alocar(p);
                return true;
            }
        }
        return false;
    }

    private boolean allocateBestFit(Process p) {
        BlocosDeMemoria best = null;
        for (BlocosDeMemoria bloco : memoriaBloqueada) {
            if (bloco.estar() && bloco.size >= p.size) {
                if (best == null || bloco.size < best.size) {
                    best = bloco;
                }
            }
        }
        if (best != null) {
            best.alocar(p);
            return true;
        }
        return false;
    }
    
    private boolean allocateWorstFit(Process p) {
        BlocosDeMemoria worst = null;
        for (BlocosDeMemoria bloco : memoriaBloqueada) {
            if (bloco.estar() && bloco.size >= p.size) {
                if (worst == null || bloco.size > worst.size) {
                    worst = bloco;
                }
            }
        }
        if (worst != null) {
            worst.alocar(p);
            return true;
        }
        return false;
    }

    private boolean allocateNextFit(Process p) {
    int n = memoriaBloqueada.size();
    for (int i = 0; i < n; i++) {
        // Calculando o índice do próximo bloco a ser alocado
        int index = (proximoNextFit + i) % n;
        BlocosDeMemoria bloco = memoriaBloqueada.get(index);

        // Verifica se o bloco está livre e se o tamanho é suficiente
        if (bloco.estar() && bloco.size >= p.size) {
            bloco.alocar(p);  // Aloca o processo
            proximoNextFit = (index + 1) % n;  // Atualiza o próximo índice de alocação
            return true;
        }
    }
    return false;  // Se não conseguir alocar em nenhum bloco
    }

    
    private void desenharBlocosDeMemoria(Graphics g) {
        int y = 20;
        for (BlocosDeMemoria bloco : memoriaBloqueada) {
            g.setColor(bloco.processo == null ? Color.LIGHT_GRAY :
            (bloco.processo.bloqueado ? Color.ORANGE : Color.GREEN));
            g.fillRect(50, y, 200, 40);
            g.setColor(Color.BLACK);
            g.drawRect(50, y, 200, 40);
            g.drawString("Bloco " + bloco.id + ": " + bloco.size + "KB", 60, y + 15);
            
            if (bloco.processo != null) {
                g.drawString(bloco.processo.nome + " (" + bloco.processo.size + "KB)", 60, y + 35);
            }
            y += 60;
        }
    }
    
    private void atualizaStatusDaMemoria() {
        int totalDeMemoria = 0;
        int usoDeMemoria = 0;
        
        for (BlocosDeMemoria bloco : memoriaBloqueada) {
            totalDeMemoria += bloco.size;
            if (bloco.processo != null) {
                usoDeMemoria += bloco.size;
            }
        }
        int memoriaLivre = totalDeMemoria - usoDeMemoria;
        statusDaMemoria.setText("Memória: Total: " + totalDeMemoria + "KB | Ocupado: " + usoDeMemoria +
        "KB | Livre: " + memoriaLivre + "KB");
    }

    
    private void atualizarTabelaDeProcessos(){
        tabelaDeProcessosModel.setRowCount(0);
        for (Process p : processos) {
            tabelaDeProcessosModel.addRow(new Object[]{
                p.nome, p.prioridade, p.estado
            });
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(SimuladorMemoriaAlocada::new);
    }
    
    static class BlocosDeMemoria {
        int id, size;
        Process processo;

        BlocosDeMemoria(int id, int size) {
            this.id = id;
            this.size = size;
        }

        boolean estar() {
            return processo == null;
        }

        void alocar(Process p) {
            this.processo = p;
        }

        void clear() {
            this.processo = null;
        }
    }

    //Pagina
    static class Pagina {
        int id;
        boolean carregado;

        Pagina(int id) {
            this.id = id;
            this.carregado = false;
        }
    }

    //Moldura
    static class Moldura {
        int id;
        Pagina pagina;

        Moldura(int id){
            this.id = id;
            this.pagina = null;
        }

        boolean estaVazia() {
            return pagina == null;
        }

        void CarregarPagina(Pagina pagina) {
            this.pagina = pagina;
        }

        void liberar() {
            this.pagina = null;
        }
    }

    static class Process {
        String nome;
        int size;
        boolean bloqueado = false;
        int prioridade;
        Estado estado = Estado.NOVO;
        //Acrescentei a lista
        List<Pagina> paginas;

        enum Estado {
            NOVO, PRONTO, EXECUTANDO, BLOQUEADO, FINALIZADO
        }

        Process(String nome, int size, int prioridade) {
            this.nome = nome;
            this.size = size;
            this.prioridade = prioridade;
            this.estado = Estado.NOVO;
            this.paginas = new ArrayList<>();

            int numPaginas = (int) Math.ceil((double) size / 50);
            for (int i = 0; i < numPaginas; i++){
                paginas.add(new Pagina(i));
            }
        }


        public String toString() {
            return nome + " (" + size + "KB, P" + prioridade + ") - " +
            estado + (bloqueado ? " [BLOQUEADO]" : "");
        }
    }

    private void iniciarEscalonador() {
        new Thread(() ->{
            while (true){
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                }

                Process proximo = processos.stream()
                .filter(p -> p.estado == Process.Estado.PRONTO && !p.bloqueado)
                .min((p1, p2) -> Integer.compare(p1.prioridade, p2.prioridade))
                .orElse(null);

                if (proximo != null) {
                    for (Process p : processos) {
                        if (p.estado == Process.Estado.EXECUTANDO) {
                            p.estado = Process.Estado.FINALIZADO;
                        }
                    }

                    proximo.estado = Process.Estado.EXECUTANDO;
                    atualizarTabelaDeProcessos();
                    repaint();
                }
            }
        }).start();
    }

    //Lista e inicializador de memória
        private List<Moldura> moldurasDeMemoria;

        private void inicializarMemoria() {
            moldurasDeMemoria = new ArrayList<>();
            int numMolduras = 10;
            for (int i = 0; i < numMolduras; i++) {
                moldurasDeMemoria.add(new Moldura(i));
            }
        }

        //Alocar falha de página
        private int contadorDePageFaults = 0;

        private boolean alocarPaginaNaMemoria(Process p){
    for (Pagina pagina : p.paginas) {
        Moldura molduraLivre = moldurasDeMemoria.stream()
            .filter(Moldura::estaVazia)
            .findFirst()
            .orElse(null);

        if (molduraLivre != null) {
            molduraLivre.CarregarPagina(pagina);
            pagina.carregado = true;
        } else {
            contadorDePageFaults++;
            System.out.println("Falha de página! Contador de Page faults: " + contadorDePageFaults);
            p.estado = Process.Estado.BLOQUEADO; 
            atualizarTabelaDeProcessos();
            return false;  
        }
    }
    return true; 
    }

}
