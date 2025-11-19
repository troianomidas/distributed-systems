import java.util.Random;

class Casino {
    private final int numRoletas;
    private boolean[] roletas;  // true = livre, false = ocupada

    public Casino(int numRoletas) {
        this.numRoletas = numRoletas;
        this.roletas = new boolean[numRoletas];
        // Inicializa todas as roletas como livres
        for (int i = 0; i < numRoletas; i++) {
            roletas[i] = true;
        }
    }

    public synchronized int adquirirRoleta(int jogadorId) throws InterruptedException {
        if (!temRoletaLivre()) {
            System.out.println("🕐 Jogador " + jogadorId + " está ESPERANDO por uma roleta livre...");
        }

        while (!temRoletaLivre()) {
            wait();
        }

        // Encontra a primeira roleta livre
        int roletaIndex = encontrarRoletaLivre();
        roletas[roletaIndex] = false;  // Marca como ocupada

        System.out.println("🎰 Jogador " + jogadorId + " OCUPOU a Roleta " + roletaIndex);
        return roletaIndex;
    }

    public synchronized void liberarRoleta(int roletaIndex, int jogadorId) {
        roletas[roletaIndex] = true;
        System.out.println("✅ Jogador " + jogadorId + " LIBEROU a Roleta e perdeu dinheiro pro tigrin " + roletaIndex);

        notifyAll();
    }


    private boolean temRoletaLivre() {
        for (boolean livre : roletas) {
            if (livre) return true;
        }
        return false;
    }

    private int encontrarRoletaLivre() {
        for (int i = 0; i < numRoletas; i++) {
            if (roletas[i]) return i;
        }
        return -1;
    }
}

class JogadorThread extends Thread {
    private int jogadorId;
    private Casino casino;
    private int numJogadas;
    private Random random;

    public JogadorThread(int jogadorId, Casino casino, int numJogadas) {
        this.jogadorId = jogadorId;
        this.casino = casino;
        this.numJogadas = numJogadas;
        this.random = new Random();
    }

    @Override
    public void run() {
        try {
            for (int tentativa = 1; tentativa <= numJogadas; tentativa++) {
                int roletaIndex = casino.adquirirRoleta(jogadorId);

                System.out.println("🎲 Jogador " + jogadorId + " está JOGANDO na Roleta " + roletaIndex + " (jogada " + tentativa + "/" + numJogadas + ")");
                int tempoJogo = 1000 + random.nextInt(2000);
                Thread.sleep(tempoJogo);

                System.out.println("🏁 Jogador " + jogadorId + " TERMINOU de jogar na Roleta " + roletaIndex);

                casino.liberarRoleta(roletaIndex, jogadorId);

                if (tentativa < numJogadas) {
                    Thread.sleep(500 + random.nextInt(1500));
                }
            }

        } catch (InterruptedException e) {
            System.out.println("Jogador " + jogadorId + " foi interrompido.");
        }
    }
}

public class ProblemaRoletas {
    public static void main(String[] args) {
        try {
            int numRoletas = 3;
            int numJogadores = 10;
            int numJogadas = 3;

            System.out.println("╔═════════════════════════╗");
            System.out.println("║  PROBLEMA DAS ROLETAS   ║");
            System.out.println("╠═════════════════════════╣");
            System.out.println("║  Roletas disponíveis: " + numRoletas+ " ║");
            System.out.println("║  Jogadores: " + numJogadores + "          ║");
            System.out.println("║  Jogadas por jogador: " + numJogadas + " ║");
            System.out.println("╚═════════════════════════╝\n");

            Casino casino = new Casino(numRoletas);

            JogadorThread[] jogadores = new JogadorThread[numJogadores];
            for (int i = 0; i < numJogadores; i++) {
                jogadores[i] = new JogadorThread(i + 1, casino, numJogadas);
                jogadores[i].start();
            }

            // Aguarda todos os jogadores terminarem
            for (JogadorThread jogador : jogadores) {
                jogador.join();
            }

            System.out.println("\n╔═══════════════════════════════════════╗");
            System.out.println("║   SIMULAÇÃO FINALIZADA COM SUCESSO    ║");
            System.out.println("╚═══════════════════════════════════════╝");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
