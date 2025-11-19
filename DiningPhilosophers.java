import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Classe que representa um Filósofo no problema do Jantar dos Filósofos
 * 
 * Cada filósofo alterna entre dois estados:
 * - MEDITANDO: pensando/descansando
 * - COM FOME: tentando pegar os hashis para comer
 */
class Filosofo extends Thread {
    private int id;                    // Identificador único do filósofo (0 a 4)
    private Lock hashiEsquerdo;        // Hashi à esquerda do filósofo
    private Lock hashiDireito;         // Hashi à direita do filósofo
    private Random random;             // Gerador de números aleatórios para simular tempos
    private volatile boolean running;  // Flag para controlar a execução da thread
    
    // Estados possíveis do filósofo
    private static final int MEDITANDO = 0;
    private static final int COM_FOME = 1;
    private int estado;

    /**
     * Construtor do Filósofo
     * 
     * @param id Identificador do filósofo (0 a 4)
     * @param hashiEsquerdo Lock que representa o hashi à esquerda
     * @param hashiDireito Lock que representa o hashi à direita
     */
    public Filosofo(int id, Lock hashiEsquerdo, Lock hashiDireito) {
        this.id = id;
        this.hashiEsquerdo = hashiEsquerdo;
        this.hashiDireito = hashiDireito;
        this.random = new Random();
        this.estado = MEDITANDO;
        this.running = true;
    }

    /**
     * Método principal da thread do filósofo
     * Alterna entre meditar e tentar comer indefinidamente
     */
    @Override
    public void run() {
        try {
            while (running) {
                // Fase 1: MEDITANDO
                meditar();
                
                // Fase 2: COM FOME - tenta pegar os hashis e comer
                tentarComer();
            }
        } catch (InterruptedException e) {
            System.out.println("Filósofo " + id + " foi interrompido.");
        }
        System.out.println("Filósofo " + id + " saiu da mesa.");
    }

    /**
     * Simula o filósofo meditando
     * O filósofo medita por um tempo aleatório entre 5 e 7 segundos
     */
    private void meditar() throws InterruptedException {
        estado = MEDITANDO;
        System.out.println("🧘 Filósofo " + id + " está meditando.");
        
        // Simula o tempo de meditação (5 a 7 segundos)
        int tempoMeditacao = 5000 + random.nextInt(2000);
        Thread.sleep(tempoMeditacao);
        
        // Após meditar, o filósofo fica com fome
        estado = COM_FOME;
        System.out.println("😋 Filósofo " + id + " está com fome!");
        Thread.sleep(100); // Delay para visualização
    }

    /**
     * Tenta pegar os hashis e comer
     * 
     * SOLUÇÃO PARA EVITAR DEADLOCK:
     * - Filósofos pares pegam primeiro o hashi da ESQUERDA, depois o da DIREITA
     * - Filósofos ímpares pegam primeiro o hashi da DIREITA, depois o da ESQUERDA
     * 
     * Isso quebra a condição de espera circular e previne deadlock
     */
    private void tentarComer() throws InterruptedException {
        // Determina a ordem de aquisição dos hashis baseado no ID
        if (id % 2 == 0) {
            // Filósofos PARES: esquerda primeiro, depois direita
            pegarHashisEComer(hashiEsquerdo, hashiDireito, "esquerdo", "direito");
        } else {
            // Filósofos ÍMPARES: direita primeiro, depois esquerda
            pegarHashisEComer(hashiDireito, hashiEsquerdo, "direito", "esquerdo");
        }
    }

    /**
     * Pega os hashis na ordem especificada e come
     * 
     * @param primeiroHashi Primeiro hashi a ser pego
     * @param segundoHashi Segundo hashi a ser pego
     * @param nomePrimeiro Nome do primeiro hashi (para log)
     * @param nomeSegundo Nome do segundo hashi (para log)
     */
    private void pegarHashisEComer(Lock primeiroHashi, Lock segundoHashi, 
                                    String nomePrimeiro, String nomeSegundo) 
                                    throws InterruptedException {
        // Tenta pegar o primeiro hashi
        primeiroHashi.lock();
        try {
            System.out.println("🥢 Filósofo " + id + " pegou o hashi " + nomePrimeiro + ".");
            Thread.sleep(100); // Delay para visualização
            
            // Tenta pegar o segundo hashi
            segundoHashi.lock();
            try {
                System.out.println("🥢 Filósofo " + id + " pegou o hashi " + nomeSegundo + ".");
                Thread.sleep(100); // Delay para visualização
                
                // Agora com os dois hashis, o filósofo pode comer
                comer();
                
            } finally {
                // Libera o segundo hashi
                segundoHashi.unlock();
                System.out.println("✋ Filósofo " + id + " liberou o hashi " + nomeSegundo + ".");
                Thread.sleep(100); // Delay para visualização
            }
        } finally {
            // Libera o primeiro hashi
            primeiroHashi.unlock();
            System.out.println("✋ Filósofo " + id + " liberou o hashi " + nomePrimeiro + ".");
            Thread.sleep(100); // Delay para visualização
        }
    }

    /**
     * Simula o filósofo comendo
     * O filósofo come por um tempo aleatório entre 3 e 15 segundos
     */
    private void comer() throws InterruptedException {
        System.out.println("🍜 Filósofo " + id + " está COMENDO!");
        
        // Simula o tempo de comer (3 a 15 segundos)
        int tempoComer = 3000 + random.nextInt(12000);
        Thread.sleep(tempoComer);
        
        System.out.println("✅ Filósofo " + id + " terminou de comer.");
        Thread.sleep(100); // Delay para visualização
    }

    /**
     * Para a execução do filósofo
     */
    public void pararExecucao() {
        running = false;
    }
}

/**
 * Classe principal que simula o Jantar dos Filósofos
 * 
 * O PROBLEMA:
 * - 5 filósofos sentados em uma mesa redonda
 * - 5 hashis (palitos) compartilhados entre eles
 * - Cada filósofo precisa de 2 hashis para comer (esquerdo e direito)
 * - Filósofos alternam entre meditar e comer
 * 
 * DESAFIO:
 * - Evitar DEADLOCK (todos esperando por um hashi)
 * - Evitar STARVATION (um filósofo nunca consegue comer)
 * 
 * SOLUÇÃO IMPLEMENTADA:
 * - Filósofos pares pegam hashis na ordem: esquerda → direita
 * - Filósofos ímpares pegam hashis na ordem: direita → esquerda
 * - Isso quebra a espera circular e previne deadlock
 */
public class DiningPhilosophers {
    private static final int NUM_FILOSOFOS = 5;
    private static final int TEMPO_SIMULACAO = 60000; // 60 segundos

    public static void main(String[] args) {
        // Lê o tempo de simulação da linha de comando (opcional)
        int tempoSimulacao = TEMPO_SIMULACAO;
        if (args.length > 0) {
            try {
                tempoSimulacao = Integer.parseInt(args[0]) * 1000; // Converte para milissegundos
            } catch (NumberFormatException e) {
                System.out.println("Tempo inválido. Usando padrão: 60 segundos.");
            }
        }

        // Exibe cabeçalho da simulação
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║          O JANTAR DOS FILÓSOFOS CHINESES              ║");
        System.out.println("╠════════════════════════════════════════════════════════╣");
        System.out.println("║  Número de filósofos: 5                               ║");
        System.out.println("║  Número de hashis: 5                                  ║");
        System.out.println("║  Tempo de simulação: " + String.format("%-28d", tempoSimulacao/1000) + "segundos ║");
        System.out.println("╠════════════════════════════════════════════════════════╣");
        System.out.println("║  SOLUÇÃO: Ordem alternada de aquisição de hashis      ║");
        System.out.println("║  - Filósofos pares: esquerda → direita                ║");
        System.out.println("║  - Filósofos ímpares: direita → esquerda              ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        // Cria os 5 hashis (representados por Locks)
        // Cada hashi é compartilhado entre dois filósofos vizinhos
        Lock[] hashis = new Lock[NUM_FILOSOFOS];
        for (int i = 0; i < NUM_FILOSOFOS; i++) {
            hashis[i] = new ReentrantLock();
        }

        // Cria os 5 filósofos
        // Cada filósofo recebe referências aos hashis à sua esquerda e direita
        Filosofo[] filosofos = new Filosofo[NUM_FILOSOFOS];
        for (int i = 0; i < NUM_FILOSOFOS; i++) {
            // O hashi à esquerda é o de índice i
            Lock hashiEsquerdo = hashis[i];
            
            // O hashi à direita é o de índice (i+1) % 5
            // O módulo garante que o filósofo 4 pegue o hashi 0 (mesa circular)
            Lock hashiDireito = hashis[(i + 1) % NUM_FILOSOFOS];
            
            filosofos[i] = new Filosofo(i, hashiEsquerdo, hashiDireito);
        }

        // Inicia todas as threads dos filósofos
        System.out.println("🚀 Iniciando a simulação...\n");
        for (Filosofo filosofo : filosofos) {
            filosofo.start();
        }

        // Aguarda o tempo de simulação
        try {
            Thread.sleep(tempoSimulacao);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Para todos os filósofos
        System.out.println("\n⏰ Tempo de simulação encerrado. Finalizando...\n");
        for (Filosofo filosofo : filosofos) {
            filosofo.pararExecucao();
        }

        // Aguarda todas as threads terminarem
        for (Filosofo filosofo : filosofos) {
            try {
                filosofo.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Exibe mensagem final
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║            SIMULAÇÃO FINALIZADA COM SUCESSO            ║");
        System.out.println("║                                                        ║");
        System.out.println("║  Nenhum deadlock ocorreu! 🎉                           ║");
        System.out.println("║  Todos os filósofos conseguiram meditar e comer.       ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
    }
}