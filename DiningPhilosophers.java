import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Filosofo extends Thread {
    private int id;
    private Lock hashiEsquerdo;
    private Lock hashiDireito;
    private Random random;
    private volatile boolean running;
    
    // Estados possíveis do filósofo
    private static final int MEDITANDO = 0;
    private static final int COM_FOME = 1;
    private int estado;

    public Filosofo(int id, Lock hashiEsquerdo, Lock hashiDireito) {
        this.id = id;
        this.hashiEsquerdo = hashiEsquerdo;
        this.hashiDireito = hashiDireito;
        this.random = new Random();
        this.estado = MEDITANDO;
        this.running = true;
    }

    @Override
    public void run() {
        try {
            while (running) {
                meditar();

                tentarComer();
            }
        } catch (InterruptedException e) {
            System.out.println("Filósofo " + id + " foi interrompido.");
        }
        System.out.println("Filósofo " + id + " saiu da mesa.");
    }

    private void meditar() throws InterruptedException {
        estado = MEDITANDO;
        System.out.println("🧘 Filósofo " + id + " está meditando.");
        
        int tempoMeditacao = 5000 + random.nextInt(2000);
        Thread.sleep(tempoMeditacao);
        
        estado = COM_FOME;
        System.out.println("😋 Filósofo " + id + " está com fome!");
        Thread.sleep(100); // somente vizualização
    }

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

    private void pegarHashisEComer(Lock primeiroHashi, Lock segundoHashi, 
                                    String nomePrimeiro, String nomeSegundo) 
                                    throws InterruptedException {
        // Tenta pegar o primeiro hashi
        primeiroHashi.lock();
        try {
            System.out.println("🥢 Filósofo " + id + " pegou o hashi " + nomePrimeiro + ".");
            Thread.sleep(100);
            
            // Tenta pegar o segundo hashi
            segundoHashi.lock();
            try {
                System.out.println("🥢 Filósofo " + id + " pegou o hashi " + nomeSegundo + ".");
                Thread.sleep(100);
                
                comer();
                
            } finally {
                // Libera o segundo hashi
                segundoHashi.unlock();
                System.out.println("✋ Filósofo " + id + " liberou o hashi " + nomeSegundo + ".");
                Thread.sleep(100);
            }
        } finally {
            // Libera o primeiro hashi
            primeiroHashi.unlock();
            System.out.println("✋ Filósofo " + id + " liberou o hashi " + nomePrimeiro + ".");
            Thread.sleep(100);
        }
    }

    private void comer() throws InterruptedException {
        System.out.println("🍜 Filósofo " + id + " está COMENDO!");
        
        int tempoComer = 3000 + random.nextInt(12000);
        Thread.sleep(tempoComer);
        
        System.out.println("✅ Filósofo " + id + " terminou de comer.");
        Thread.sleep(100);
    }

    public void pararExecucao() {
        running = false;
    }
}

public class DiningPhilosophers {
    private static final int NUM_FILOSOFOS = 5;
    private static final int TEMPO_SIMULACAO = 10; // 60 segundos

    public static void main(String[] args) {
        int tempoSimulacao = TEMPO_SIMULACAO;
        if (args.length > 0) {
            try {
                tempoSimulacao = Integer.parseInt(args[0]) * 1000;
            } catch (NumberFormatException e) {
                System.out.println("Tempo inválido. Usando padrão: 60 segundos.");
            }
        }

        System.out.println("╔═════════════════════════════════════════════╗");
        System.out.println("║       O JANTAR DOS FILÓSOFOS CHINESES       ║");
        System.out.println("╠═════════════════════════════════════════════╣");
        System.out.println("║  Número de filósofos: 5                     ║");
        System.out.println("║  Número de hashis: 5                        ║");
        System.out.println("║  Tempo de simulação: " +  tempoSimulacao/1000 + " segundos            ║");
        System.out.println("╚═════════════════════════════════════════════╝\n");

        Lock[] hashis = new Lock[NUM_FILOSOFOS];
        for (int i = 0; i < NUM_FILOSOFOS; i++) {
            hashis[i] = new ReentrantLock();
        }

        Filosofo[] filosofos = new Filosofo[NUM_FILOSOFOS];
        for (int i = 0; i < NUM_FILOSOFOS; i++) {
            // O hashi à esquerda é o de índice i
            Lock hashiEsquerdo = hashis[i];

            Lock hashiDireito = hashis[(i + 1) % NUM_FILOSOFOS];
            
            filosofos[i] = new Filosofo(i, hashiEsquerdo, hashiDireito);
        }

        System.out.println("🚀 Iniciando a simulação...\n");
        for (Filosofo filosofo : filosofos) {
            filosofo.start();
        }

        try {
            Thread.sleep(tempoSimulacao);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\n⏰ Tempo de simulação encerrado. Finalizando...\n");
        for (Filosofo filosofo : filosofos) {
            filosofo.pararExecucao();
        }

        for (Filosofo filosofo : filosofos) {
            try {
                filosofo.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║            SIMULAÇÃO FINALIZADA COM SUCESSO            ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
    }
}