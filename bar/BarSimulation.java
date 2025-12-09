    import java.util.*;
    import java.util.concurrent.*;
    import java.util.concurrent.locks.*;

    class Order {
        int clientId;
        int roundNumber;
        
        // Latches para sincronização entre cliente e garçom
        CountDownLatch startOrderLatch = new CountDownLatch(1);      // Sinaliza quando o cliente pode fazer o pedido
        CountDownLatch orderPlacedLatch = new CountDownLatch(1);     // Sinaliza quando o cliente terminou de fazer o pedido
        CountDownLatch orderDeliveredLatch = new CountDownLatch(1);  // Sinaliza quando o pedido foi entregue

        public Order(int clientId, int roundNumber) {
            this.clientId = clientId;
            this.roundNumber = roundNumber;
        }
    }

    class BartenderRequest {
        int waiterId;                                        // ID do garçom que fez a requisição
        List<Order> orders;                                  // Lista de pedidos a serem preparados
        CountDownLatch orderReadyLatch = new CountDownLatch(1);  // Sinaliza quando os pedidos estão prontos

        public BartenderRequest(int waiterId, List<Order> orders) {
            this.waiterId = waiterId;
            this.orders = orders;
        }
    }

    class Bar {
        Lock lock = new ReentrantLock();                    // Lock para proteger acesso às variáveis compartilhadas
        List<Order> waitingClients = new ArrayList<>();     // Fila de clientes esperando atendimento
        int totalClients;                                   // Número total de clientes no bar
        int pendingClients;                                 // Número de clientes que ainda não decidiram se vão pedir
        int totalRounds;                                    // Número total de rodadas
        int currentRound = 1;                               // Rodada atual
        CyclicBarrier startBarrier;                         // Barreira para sincronizar início da rodada
        CyclicBarrier endBarrier;                           // Barreira para sincronizar fim da rodada

        public Bar(int numClients, int numWaiters, int numRounds) {
            this.totalClients = numClients;
            this.pendingClients = numClients;
            this.totalRounds = numRounds;
            
            // Cria barreiras que esperam todos os clientes e garçons
            int parties = numClients + numWaiters;
            this.startBarrier = new CyclicBarrier(parties, () -> startRound());
            this.endBarrier = new CyclicBarrier(parties, () -> endRound());
        }

        private void startRound() {
            System.out.println("\n=== Iniciando rodada " + currentRound + " ===\n");
            lock.lock();
            try {
                pendingClients = totalClients;  // Reseta o contador de clientes pendentes
                waitingClients.clear();         // Limpa a fila de espera
            } finally {
                lock.unlock();
            }
        }

        private void endRound() {
            System.out.println("\n=== Finalizando rodada " + currentRound + " ===\n");
            currentRound++;
        }
    }

    class ClientThread extends Thread {
        int clientId;
        Bar bar;
        Semaphore ordersSem;  // Semáforo para sinalizar que há pedidos disponíveis
        Random random = new Random();

        public ClientThread(int clientId, Bar bar, Semaphore ordersSem) {
            this.clientId = clientId;
            this.bar = bar;
            this.ordersSem = ordersSem;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < bar.totalRounds; i++) {
                    // Aguarda o início da rodada (sincroniza com outros clientes e garçons)
                    bar.startBarrier.await();

                    // Decide aleatoriamente se vai fazer um pedido (50% de chance)
                    boolean wantOrder = random.nextBoolean();
                    
                    if (wantOrder) {
                        Order order = new Order(clientId, bar.currentRound);
                        
                        // Adiciona o pedido na fila de espera (região crítica)
                        bar.lock.lock();
                        try {
                            bar.waitingClients.add(order);
                            bar.pendingClients--;
                        } finally {
                            bar.lock.unlock();
                        }
                        
                        // Cliente fez um pedido (incrementa no semáforo)
                        ordersSem.release();
                        System.out.println("Cliente " + clientId + " (Rodada " + bar.currentRound + "): solicitou atendimento.");
                        Thread.sleep(50);

                        order.startOrderLatch.await();

                        Thread.sleep((long)(100 + random.nextDouble() * 400));
                        System.out.println("Cliente " + clientId + " (Rodada " + bar.currentRound + "): fez o pedido.");
                        Thread.sleep(50);
                        order.orderPlacedLatch.countDown();

                        order.orderDeliveredLatch.await();
                        System.out.println("Cliente " + clientId + " (Rodada " + bar.currentRound + "): recebeu o pedido.");
                        Thread.sleep(50);

                        Thread.sleep((long)(500 + random.nextDouble() * 1000));
                    } else {
                        // Cliente não quer fazer pedido nesta rodada
                        bar.lock.lock();
                        try {
                            bar.pendingClients--;
                        } finally {
                            bar.lock.unlock();
                        }
                        System.out.println("Cliente " + clientId + " (Rodada " + bar.currentRound + "): não solicitou atendimento.");
                        Thread.sleep(50);
                        Thread.sleep((long)(100 + random.nextDouble() * 200));
                    }

                    bar.endBarrier.await();
                }
                System.out.println("Cliente " + clientId + ": saiu do bar.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class WaiterThread extends Thread {
        int waiterId;
        int capacity;
        Bar bar;
        BlockingQueue<BartenderRequest> bartenderQueue;
        Semaphore ordersSem;
        Random random = new Random();

        public WaiterThread(int waiterId, int capacity, Bar bar, BlockingQueue<BartenderRequest> bartenderQueue, Semaphore ordersSem) {
            this.waiterId = waiterId;
            this.capacity = capacity;
            this.bar = bar;
            this.bartenderQueue = bartenderQueue;
            this.ordersSem = ordersSem;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < bar.totalRounds; i++) {
                    bar.startBarrier.await();

                    while (true) {
                        List<Order> group = new ArrayList<>();

                        while (group.size() < capacity) {
                            // Garçom tenta pegar um pedido
                            boolean acquired = ordersSem.tryAcquire(100, TimeUnit.MILLISECONDS);
                            
                            if (acquired) {
                                // Remove um pedido da fila (região crítica)
                                bar.lock.lock();
                                try {
                                    if (!bar.waitingClients.isEmpty()) {
                                        Order order = bar.waitingClients.remove(0);
                                        group.add(order);
                                    }
                                } finally {
                                    bar.lock.unlock();
                                }
                            } else {
                                // Verifica se ainda há clientes que podem fazer pedidos
                                bar.lock.lock();
                                try {
                                    if (bar.pendingClients == 0) {
                                        break;
                                    }
                                } finally {
                                    bar.lock.unlock();
                                }
                            }
                        }

                        boolean noMoreClients;
                        bar.lock.lock();
                        try {
                            noMoreClients = (bar.pendingClients == 0);
                        } finally {
                            bar.lock.unlock();
                        }

                        // Se não coletou nenhum pedido e não há mais clientes, encerra a coleta
                        if (group.isEmpty() && noMoreClients) {
                            break;
                        }

                        // Chama cada cliente do grupo para fazer o pedido
                        for (Order order : group) {
                            order.startOrderLatch.countDown();
                        }

                        // Aguarda todos os clientes do grupo efetuarem seus pedidos
                        for (Order order : group) {
                            order.orderPlacedLatch.await();
                        }
                        System.out.println("Garçom " + waiterId + " (Rodada " + bar.currentRound + "): recebeu os pedidos de um grupo com " + group.size() + " pedido(s).");
                        Thread.sleep(100);  // Delay para visualização

                        // Envia os pedidos para o bartender
                        BartenderRequest request = new BartenderRequest(waiterId, group);
                        bartenderQueue.put(request);

                        request.orderReadyLatch.await();
                        System.out.println("Garçom " + waiterId + " (Rodada " + bar.currentRound + "): recebeu confirmação do bartender.");
                        Thread.sleep(100);

                        // Entrega os pedidos para cada cliente do grupo
                        for (Order order : group) {
                            order.orderDeliveredLatch.countDown();
                        }
                        System.out.println("Garçom " + waiterId + " (Rodada " + bar.currentRound + "): entregou os pedidos do grupo.");
                        Thread.sleep(100);
                    }

                    // Aguarda o fim da rodada
                    bar.endBarrier.await();
                }
                System.out.println("Garçom " + waiterId + ": encerrou o turno.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    class BartenderThread extends Thread {
        int bartenderId;
        BlockingQueue<BartenderRequest> bartenderQueue;
        Random random = new Random();

        public BartenderThread(int bartenderId, BlockingQueue<BartenderRequest> bartenderQueue) {
            this.bartenderId = bartenderId;
            this.bartenderQueue = bartenderQueue;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    // Aguarda uma requisição de um garçom (bloqueia se a fila estiver vazia)
                    BartenderRequest request = bartenderQueue.take();
                    
                    if (request.orders == null) {
                        break;
                    }
                    
                    // Obtém o número da rodada do primeiro pedido
                    int roundNum = request.orders.isEmpty() ? 0 : request.orders.get(0).roundNumber;
                    System.out.println("Bartender: processando pedido do Garçom " + request.waiterId + " (Rodada " + roundNum + ").");
                    Thread.sleep(100);

                    // Simula o tempo de preparo dos pedidos
                    Thread.sleep((long)(800 + random.nextDouble() * 600));
                    
                    System.out.println("Bartender: finalizou pedido do Garçom " + request.waiterId + ".");
                    Thread.sleep(100);
                    
                    // Sinaliza que os pedidos estão prontos
                    request.orderReadyLatch.countDown();
                }
                System.out.println("Bartender: encerrando o turno.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class BarSimulation {
        public static void main(String[] args) {
            // Valores padrão
            int numClients = 10;
            int numWaiters = 3;
            int capacity = 4;
            int numRounds = 1;

            if (args.length >= 4) {
                numClients = Integer.parseInt(args[0]);
                numWaiters = Integer.parseInt(args[1]);
                capacity = Integer.parseInt(args[2]);
                numRounds = Integer.parseInt(args[3]);
            }

            System.out.println("╔════════════════════════════════════════════════════════╗");
            System.out.println("║          SIMULAÇÃO DE ATENDIMENTO NO BAR              ║");
            System.out.println("╠════════════════════════════════════════════════════════╣");
            System.out.println("║  Clientes: " + String.format("%-42d", numClients) + "║");
            System.out.println("║  Garçons: " + String.format("%-43d", numWaiters) + "║");
            System.out.println("║  Capacidade por garçom: " + String.format("%-28d", capacity) + "║");
            System.out.println("║  Rodadas: " + String.format("%-43d", numRounds) + "║");
            System.out.println("╚════════════════════════════════════════════════════════╝\n");

            Bar bar = new Bar(numClients, numWaiters, numRounds);
            
            BlockingQueue<BartenderRequest> bartenderQueue = new LinkedBlockingQueue<>();
            
            // Cria o semáforo para controlar pedidos disponíveis
            Semaphore ordersSem = new Semaphore(0);

            BartenderThread bartender = new BartenderThread(1, bartenderQueue);
            bartender.start();

            List<WaiterThread> waiters = new ArrayList<>();
            for (int i = 1; i <= numWaiters; i++) {
                WaiterThread waiter = new WaiterThread(i, capacity, bar, bartenderQueue, ordersSem);
                waiter.start();
                waiters.add(waiter);
            }

            List<ClientThread> clients = new ArrayList<>();
            for (int i = 1; i <= numClients; i++) {
                ClientThread client = new ClientThread(i, bar, ordersSem);
                client.start();
                clients.add(client);
            }

            try {
                for (ClientThread client : clients) {
                    client.join();
                }

                for (WaiterThread waiter : waiters) {
                    waiter.join();
                }

                bartenderQueue.put(new BartenderRequest(0, null));
                bartender.join();

                System.out.println("\n╔════════════════════════════════════════════════════════╗");
                System.out.println("║            SIMULAÇÃO FINALIZADA COM SUCESSO            ║");
                System.out.println("╚════════════════════════════════════════════════════════╝");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }