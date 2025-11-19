import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

class Barbearia {
    private final int numCadeiras;
    private Queue<Integer> salaEspera;
    private boolean barbeiroOcupado;
    private boolean barbeiroDormindo;
    private int clienteAtual;
    private boolean corteFinalizado;

    public Barbearia(int numCadeiras) {
        this.numCadeiras = numCadeiras;
        this.salaEspera = new LinkedList<>();
        this.barbeiroOcupado = false;
        this.barbeiroDormindo = true;
        this.clienteAtual = -1;
        this.corteFinalizado = false;
    }

    public synchronized boolean clienteChega(int clienteId) {
        if (salaEspera.size() >= numCadeiras) {
            System.out.println("👋 Cliente " + clienteId + " chegou, mas a barbearia está LOTADA. Saiu imediatamente.");
            return false;
        }

        salaEspera.add(clienteId);
        System.out.println("🚶 Cliente " + clienteId + " entrou na barbearia. Cadeiras ocupadas: " + salaEspera.size() + "/" + numCadeiras);

        if (barbeiroDormindo) {
            System.out.println("⏰ Cliente " + clienteId + " ACORDOU o barbeiro!\n");
            barbeiroDormindo = false;
            notifyAll();
        }

        return true;
    }

    public synchronized void aguardarChamada(int clienteId) throws InterruptedException {
        while (clienteAtual != clienteId) {
            wait();
        }
        System.out.println("📢 Cliente " + clienteId + " foi CHAMADO e sentou na cadeira do barbeiro.");
    }

    public synchronized void aguardarCorte(int clienteId) throws InterruptedException {
        while (!corteFinalizado) {
            wait();
        }
        System.out.println("✂️ Cliente " + clienteId + " teve o corte FINALIZADO. Saindo da barbearia.\n");
    }

    public synchronized void barbeiroAguardaCliente() throws InterruptedException {
        while (salaEspera.isEmpty()) {
            System.out.println("😴 Barbeiro está DORMINDO (sem clientes)...");
            barbeiroDormindo = true;
            wait();
        }

        barbeiroDormindo = false;
    }

    public synchronized int chamarProximoCliente() {
        if (salaEspera.isEmpty()) {
            return -1;
        }

        clienteAtual = salaEspera.poll();
        barbeiroOcupado = true;
        corteFinalizado = false;

        System.out.println("💺 Barbeiro CHAMOU o Cliente " + clienteAtual + ". Clientes esperando: " + salaEspera.size());

        notifyAll();

        return clienteAtual;
    }

    public synchronized void finalizarCorte() {
        System.out.println("✅ Barbeiro FINALIZOU o corte do Cliente " + clienteAtual + ".");
        corteFinalizado = true;

        notifyAll();
    }
}

class BarbeiroThread extends Thread {
    private Barbearia barbearia;
    private Random random;
    private volatile boolean running;

    public BarbeiroThread(Barbearia barbearia) {
        this.barbearia = barbearia;
        this.random = new Random();
        this.running = true;
    }

    @Override
    public void run() {
        try {
            while (running) {
                barbearia.barbeiroAguardaCliente();

                int clienteId = barbearia.chamarProximoCliente();

                if (clienteId != -1) {
                    Thread.sleep(100);
                    System.out.println("✂️ Barbeiro está CORTANDO o cabelo do Cliente " + clienteId + "...");
                    int tempoCorte = 2000 + random.nextInt(3000);
                    Thread.sleep(tempoCorte);

                    barbearia.finalizarCorte();

                    Thread.sleep(100);
                }
            }
        } catch (InterruptedException e) {
        }
        System.out.println("🔚 Barbeiro encerrou o expediente.");
    }

    public void encerrar() {
        running = false;
        interrupt();
    }
}

class ClienteThread extends Thread {
    private int clienteId;
    private Barbearia barbearia;
    private Random random;

    public ClienteThread(int clienteId, Barbearia barbearia) {
        this.clienteId = clienteId;
        this.barbearia = barbearia;
        this.random = new Random();
    }

    @Override
    public void run() {
        try {
            Thread.sleep(3000 + random.nextInt(10000));

            boolean entrou = barbearia.clienteChega(clienteId);

            if (!entrou) {
                return;
            }

            barbearia.aguardarChamada(clienteId);

            barbearia.aguardarCorte(clienteId);

        } catch (InterruptedException e) {
            System.out.println("Cliente " + clienteId + " foi interrompido.");
        }
    }
}

public class BarbeiroDorminhoco {
    public static void main(String[] args) {
        try {
            int numCadeiras = 5;
            int numClientes = 15;

            if (args.length >= 2) {
                numCadeiras = Integer.parseInt(args[0]);
                numClientes = Integer.parseInt(args[1]);
            }

            System.out.println("╔═══════════════════════════════════╗");
            System.out.println("║        BARBEIRO DORMINHOCO        ║");
            System.out.println("╠═══════════════════════════════════╣");
            System.out.println("║  Cadeiras na sala de espera: " + numCadeiras + "    ║");
            System.out.println("║  Número de clientes: " + numClientes + "           ║");
            System.out.println("╚═══════════════════════════════════╝");

            Barbearia barbearia = new Barbearia(numCadeiras);

            BarbeiroThread barbeiro = new BarbeiroThread(barbearia);
            barbeiro.start();

            System.out.println("🚀 Iniciando a simulação...\n");
            Thread.sleep(500);

            // Cria e inicia as threads dos clientes
            ClienteThread[] clientes = new ClienteThread[numClientes];
            for (int i = 0; i < numClientes; i++) {
                clientes[i] = new ClienteThread(i + 1, barbearia);
                clientes[i].start();
            }

            // Aguarda todos os clientes terminarem
            for (ClienteThread cliente : clientes) {
                cliente.join();
            }

            Thread.sleep(2000);

            System.out.println("\n⏰ Todos os clientes foram atendidos. Encerrando...\n");
            barbeiro.encerrar();
            barbeiro.join();

            System.out.println("\n╔════════════════════════════════════════════════════════╗");
            System.out.println("║            SIMULAÇÃO FINALIZADA COM SUCESSO            ║");
            System.out.println("╚════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
