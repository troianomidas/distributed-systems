import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 * Classe Monitor que representa a Barbearia
 *
 * Controla o acesso sincronizado entre o barbeiro e os clientes
 * usando monitores (synchronized) em Java
 */
class Barbearia {
    private final int numCadeiras;
    private Queue<Integer> salaEspera;
    private boolean barbeiroOcupado;
    private boolean barbeiroDormindo;
    private int clienteAtual;
    private boolean corteFinalizado;

    /**
     * Construtor da Barbearia
     *
     * @param numCadeiras Número de cadeiras na sala de espera
     */
    public Barbearia(int numCadeiras) {
        this.numCadeiras = numCadeiras;
        this.salaEspera = new LinkedList<>();
        this.barbeiroOcupado = false;
        this.barbeiroDormindo = true;
        this.clienteAtual = -1;
        this.corteFinalizado = false;
    }

    /**
     * Método chamado pelo cliente ao chegar na barbearia
     *
     * @param clienteId ID do cliente
     * @return true se conseguiu entrar, false se saiu imediatamente
     */
    public synchronized boolean clienteChega(int clienteId) {
        // Verifica se há cadeiras disponíveis (sala de espera + cadeira do barbeiro)
        if (salaEspera.size() >= numCadeiras) {
            // Barbearia lotada, cliente vai embora
            System.out.println("👋 Cliente " + clienteId + " chegou, mas a barbearia está LOTADA. Saiu imediatamente.");
            return false;
        }

        // Adiciona o cliente na fila de espera
        salaEspera.add(clienteId);
        System.out.println("🚶 Cliente " + clienteId + " entrou na barbearia. Cadeiras ocupadas: " + salaEspera.size() + "/" + numCadeiras);

        // Se o barbeiro está dormindo, acorda-o
        if (barbeiroDormindo) {
            System.out.println("⏰ Cliente " + clienteId + " ACORDOU o barbeiro!\n");
            barbeiroDormindo = false;
            notifyAll();  // Acorda o barbeiro
        }

        return true;
    }

    /**
     * Método chamado pelo cliente para aguardar ser chamado
     *
     * @param clienteId ID do cliente
     */
    public synchronized void aguardarChamada(int clienteId) throws InterruptedException {
        // Aguarda até ser o cliente atual
        while (clienteAtual != clienteId) {
            wait();
        }
        System.out.println("📢 Cliente " + clienteId + " foi CHAMADO e sentou na cadeira do barbeiro.");
    }

    /**
     * Método chamado pelo cliente para aguardar o corte finalizar
     *
     * @param clienteId ID do cliente
     */
    public synchronized void aguardarCorte(int clienteId) throws InterruptedException {
        // Aguarda o corte ser finalizado
        // (já sabemos que somos o cliente atual, pois passamos por aguardarChamada)
        while (!corteFinalizado) {
            wait();
        }
        System.out.println("✂️ Cliente " + clienteId + " teve o corte FINALIZADO. Saindo da barbearia.\n");
    }

    /**
     * Método chamado pelo barbeiro para aguardar clientes
     * Dorme se não há clientes
     */
    public synchronized void barbeiroAguardaCliente() throws InterruptedException {
        // Se não há clientes na sala de espera, barbeiro dorme
        while (salaEspera.isEmpty()) {
            System.out.println("😴 Barbeiro está DORMINDO (sem clientes)...");
            barbeiroDormindo = true;
            wait();
        }

        barbeiroDormindo = false;
    }

    /**
     * Método chamado pelo barbeiro para chamar o próximo cliente
     *
     * @return ID do próximo cliente, ou -1 se não há clientes
     */
    public synchronized int chamarProximoCliente() {
        if (salaEspera.isEmpty()) {
            return -1;
        }

        // Remove o primeiro cliente da fila
        clienteAtual = salaEspera.poll();
        barbeiroOcupado = true;
        corteFinalizado = false;  // Reseta para o novo cliente

        System.out.println("💺 Barbeiro CHAMOU o Cliente " + clienteAtual + ". Clientes esperando: " + salaEspera.size());

        // Notifica o cliente que foi chamado
        notifyAll();

        return clienteAtual;
    }

    /**
     * Método chamado pelo barbeiro após finalizar o corte
     */
    public synchronized void finalizarCorte() {
        System.out.println("✅ Barbeiro FINALIZOU o corte do Cliente " + clienteAtual + ".");
        corteFinalizado = true;

        // Notifica o cliente que o corte foi finalizado
        notifyAll();
    }
}

/**
 * Thread que representa o Barbeiro
 */
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
                // Aguarda clientes (dorme se não há ninguém)
                barbearia.barbeiroAguardaCliente();

                // Chama o próximo cliente
                int clienteId = barbearia.chamarProximoCliente();

                if (clienteId != -1) {
                    // Simula o tempo de corte de cabelo (2 a 5 segundos)
                    Thread.sleep(100);  // Pequeno delay para visualização
                    System.out.println("✂️ Barbeiro está CORTANDO o cabelo do Cliente " + clienteId + "...");
                    int tempoCorte = 2000 + random.nextInt(3000);
                    Thread.sleep(tempoCorte);

                    // Finaliza o corte
                    barbearia.finalizarCorte();

                    // Pequeno delay para o cliente processar e sair
                    Thread.sleep(100);
                }
            }
        } catch (InterruptedException e) {
            // Thread foi interrompida (encerramento normal)
        }
        System.out.println("🔚 Barbeiro encerrou o expediente.");
    }

    public void encerrar() {
        running = false;
        interrupt();
    }
}

/**
 * Thread que representa um Cliente
 */
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
            // Simula o tempo até o cliente chegar na barbearia
            Thread.sleep(3000 + random.nextInt(10000));

            // Cliente tenta entrar na barbearia
            boolean entrou = barbearia.clienteChega(clienteId);

            if (!entrou) {
                // Barbearia lotada, cliente vai embora
                return;
            }

            // Cliente aguarda ser chamado
            barbearia.aguardarChamada(clienteId);

            // Cliente aguarda o corte ser finalizado
            barbearia.aguardarCorte(clienteId);

        } catch (InterruptedException e) {
            System.out.println("Cliente " + clienteId + " foi interrompido.");
        }
    }
}

/**
 * Classe principal que simula o Barbeiro Dorminhoco
 */

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
