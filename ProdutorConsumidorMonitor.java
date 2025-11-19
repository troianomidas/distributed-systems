import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

class BufferMonitor {
    private Queue<Integer> buffer;
    private int capacidade;

    public BufferMonitor(int capacidade) {
        this.capacidade = capacidade;
        this.buffer = new LinkedList<>();
    }

    public synchronized void produzir(int item) throws InterruptedException {
        // Aguarda enquanto buffer está cheio
        while (buffer.size() >= capacidade) {
            System.out.println("⚠️  Buffer CHEIO! Produtor aguardando...");
            wait();
        }

        // Adiciona item ao buffer
        buffer.add(item);
        System.out.println("✅ PRODUZIDO: " + item + " | Buffer: " + buffer.size() + "/" + capacidade);

        // Notifica consumidores que há item disponível
        notifyAll();
    }

    public synchronized int consumir() throws InterruptedException {
        // Aguarda enquanto buffer está vazio
        while (buffer.isEmpty()) {
            System.out.println("⚠️  Buffer VAZIO! Consumidor aguardando...");
            wait();
        }

        // Remove item do buffer
        int item = buffer.poll();
        System.out.println("🔽 CONSUMIDO: " + item + " | Buffer: " + buffer.size() + "/" + capacidade);

        // Notifica produtores que há espaço disponível
        notifyAll();

        return item;
    }
}

class ProdutorMonitor extends Thread {
    private BufferMonitor buffer;
    private int produtorId;
    private int numItens;
    private Random random;

    public ProdutorMonitor(int id, BufferMonitor buffer, int numItens) {
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

class ConsumidorMonitor extends Thread {
    private BufferMonitor buffer;
    private int consumidorId;
    private int numItens;
    private Random random;

    public ConsumidorMonitor(int id, BufferMonitor buffer, int numItens) {
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

public class ProdutorConsumidorMonitor {
    public static void main(String[] args) {
        int capacidadeBuffer = 5;
        int numProdutores = 2;
        int numConsumidores = 2;
        int itensPorProdutor = 5;
        int itensPorConsumidor = 5;

        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║   PRODUTOR/CONSUMIDOR COM MONITORES              ║");
        System.out.println("╠═══════════════════════════════════════════════════╣");
        System.out.println("║  Capacidade do buffer: " + capacidadeBuffer + "                       ║");
        System.out.println("║  Produtores: " + numProdutores + "                                  ║");
        System.out.println("║  Consumidores: " + numConsumidores + "                                ║");
        System.out.println("║  Itens por produtor: " + itensPorProdutor + "                         ║");
        System.out.println("╚═══════════════════════════════════════════════════╝\n");

        BufferMonitor buffer = new BufferMonitor(capacidadeBuffer);

        // Cria produtores
        ProdutorMonitor[] produtores = new ProdutorMonitor[numProdutores];
        for (int i = 0; i < numProdutores; i++) {
            produtores[i] = new ProdutorMonitor(i + 1, buffer, itensPorProdutor);
            produtores[i].start();
        }

        // Cria consumidores
        ConsumidorMonitor[] consumidores = new ConsumidorMonitor[numConsumidores];
        for (int i = 0; i < numConsumidores; i++) {
            consumidores[i] = new ConsumidorMonitor(i + 1, buffer, itensPorConsumidor);
            consumidores[i].start();
        }

        try {
            // Aguarda todos terminarem
            for (ProdutorMonitor p : produtores) {
                p.join();
            }
            for (ConsumidorMonitor c : consumidores) {
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
