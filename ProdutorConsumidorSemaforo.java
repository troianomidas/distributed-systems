import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Semaphore;

class BufferSemaforo {
    private Queue<Integer> buffer;
    private int capacidade;

    // Semáforos
    private Semaphore semVazios;   // Quantos espaços vazios
    private Semaphore semCheios;   // Quantos itens prontos
    private Semaphore mutex;       // Exclusão mútua

    public BufferSemaforo(int capacidade) {
        this.capacidade = capacidade;
        this.buffer = new LinkedList<>();

        this.semVazios = new Semaphore(capacidade);  // Inicialmente, todos vazios
        this.semCheios = new Semaphore(0);           // Inicialmente, nenhum cheio
        this.mutex = new Semaphore(1);               // Mutex binário
    }

    public void produzir(int item) throws InterruptedException {
        semVazios.acquire();   // Aguarda espaço vazio (decrementa)
        mutex.acquire();       // Entra na seção crítica

        // Adiciona item ao buffer
        buffer.add(item);
        System.out.println("✅ PRODUZIDO: " + item + " | Buffer: " + buffer.size() + "/" + capacidade);

        mutex.release();       // Sai da seção crítica
        semCheios.release();   // Incrementa contador de itens cheios
    }

    public int consumir() throws InterruptedException {
        semCheios.acquire();   // Aguarda item disponível (decrementa)
        mutex.acquire();       // Entra na seção crítica

        // Remove item do buffer
        int item = buffer.poll();
        System.out.println("🔽 CONSUMIDO: " + item + " | Buffer: " + buffer.size() + "/" + capacidade);

        mutex.release();       // Sai da seção crítica
        semVazios.release();   // Incrementa contador de espaços vazios

        return item;
    }
}

class ProdutorSemaforo extends Thread {
    private BufferSemaforo buffer;
    private int produtorId;
    private int numItens;
    private Random random;

    public ProdutorSemaforo(int id, BufferSemaforo buffer, int numItens) {
        this.produtorId = id;
        this.buffer = buffer;
        this.numItens = numItens;
        this.random = new Random();
    }

    @Override
    public void run() {
        try {
            for (int i = 1; i <= numItens; i++) {
                // Produz um item (número aleatório)
                int item = random.nextInt(100) + 1;

                System.out.println("🏭 Produtor " + produtorId + " produziu item " + item);

                // Adiciona ao buffer
                buffer.produzir(item);

                // Simula tempo de produção
                Thread.sleep(random.nextInt(1000) + 500);
            }
            System.out.println("🏁 Produtor " + produtorId + " finalizou.\n");
        } catch (InterruptedException e) {
            System.out.println("Produtor " + produtorId + " foi interrompido.");
        }
    }
}


class ConsumidorSemaforo extends Thread {
    private BufferSemaforo buffer;
    private int consumidorId;
    private int numItens;
    private Random random;

    public ConsumidorSemaforo(int id, BufferSemaforo buffer, int numItens) {
        this.consumidorId = id;
        this.buffer = buffer;
        this.numItens = numItens;
        this.random = new Random();
    }

    @Override
    public void run() {
        try {
            for (int i = 1; i <= numItens; i++) {
                // Consome item do buffer
                int item = buffer.consumir();

                System.out.println("🍽️  Consumidor " + consumidorId + " consumiu item " + item);

                // Simula tempo de consumo
                Thread.sleep(random.nextInt(1000) + 500);
            }
            System.out.println("🏁 Consumidor " + consumidorId + " finalizou.\n");
        } catch (InterruptedException e) {
            System.out.println("Consumidor " + consumidorId + " foi interrompido.");
        }
    }
}

public class ProdutorConsumidorSemaforo {
    public static void main(String[] args) {
        int capacidadeBuffer = 5;
        int numProdutores = 2;
        int numConsumidores = 2;
        int itensPorProdutor = 5;
        int itensPorConsumidor = 5;

        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║   PRODUTOR/CONSUMIDOR COM SEMÁFOROS              ║");
        System.out.println("╠═══════════════════════════════════════════════════╣");
        System.out.println("║  Capacidade do buffer: " + capacidadeBuffer + "                       ║");
        System.out.println("║  Produtores: " + numProdutores + "                                  ║");
        System.out.println("║  Consumidores: " + numConsumidores + "                                ║");
        System.out.println("║  Itens por produtor: " + itensPorProdutor + "                         ║");
        System.out.println("╚═══════════════════════════════════════════════════╝\n");

        BufferSemaforo buffer = new BufferSemaforo(capacidadeBuffer);

        // Cria produtores
        ProdutorSemaforo[] produtores = new ProdutorSemaforo[numProdutores];
        for (int i = 0; i < numProdutores; i++) {
            produtores[i] = new ProdutorSemaforo(i + 1, buffer, itensPorProdutor);
            produtores[i].start();
        }

        // Cria consumidores
        ConsumidorSemaforo[] consumidores = new ConsumidorSemaforo[numConsumidores];
        for (int i = 0; i < numConsumidores; i++) {
            consumidores[i] = new ConsumidorSemaforo(i + 1, buffer, itensPorConsumidor);
            consumidores[i].start();
        }

        try {
            // Aguarda todos terminarem
            for (ProdutorSemaforo p : produtores) {
                p.join();
            }
            for (ConsumidorSemaforo c : consumidores) {
                c.join();
            }

            System.out.println("\n╔═══════════════════════════════════════════════════╗");
            System.out.println("║         SIMULAÇÃO FINALIZADA COM SUCESSO          ║");
            System.out.println("╚═══════════════════════════════════════════════════╝");

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
