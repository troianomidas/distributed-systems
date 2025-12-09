import java.util.Random;
import java.util.ArrayList;
import java.util.List;

class ContaBancaria {
    private int numero;
    private double saldo;

    public ContaBancaria(int numero, double saldoInicial) {
        this.numero = numero;
        this.saldo = saldoInicial;
    }

    public synchronized void depositar(double valor) {
        if (valor > 0) {
            saldo += valor;
            System.out.println(String.format("💰 Conta %d - DEPÓSITO de R$ %.2f → Saldo: R$ %.2f",
                numero, valor, saldo));
        } else {
            System.out.println(String.format("❌ Conta %d - Valor inválido para depósito: R$ %.2f",
                numero, valor));
        }
        sleep();
    }

    public synchronized void sacar(double valor) {
        if (valor > 0 && saldo >= valor) {
            saldo -= valor;
            System.out.println(String.format("💸 Conta %d - SAQUE de R$ %.2f → Saldo: R$ %.2f",
                numero, valor, saldo));
        } else {
            System.out.println(String.format("❌ Conta %d - Saldo INSUFICIENTE para saque de R$ %.2f (Saldo: R$ %.2f)",
                numero, valor, saldo));
        }
        sleep();
    }

    public void transferir(ContaBancaria destino, double valor) {
        // Adquire locks sempre na mesma ordem (pelo número da conta)
        // Isso evita deadlock circular
        ContaBancaria primeira = this.numero < destino.numero ? this : destino;
        ContaBancaria segunda = this.numero < destino.numero ? destino : this;

        synchronized (primeira) {
            synchronized (segunda) {
                if (valor > 0 && this.saldo >= valor) {
                    this.saldo -= valor;
                    destino.saldo += valor;
                    System.out.println(String.format("🔄 TRANSFERÊNCIA: Conta %d → Conta %d | R$ %.2f",
                        this.numero, destino.numero, valor));
                    System.out.println(String.format("   └─ Conta %d: R$ %.2f | Conta %d: R$ %.2f",
                        this.numero, this.saldo, destino.numero, destino.saldo));
                } else {
                    System.out.println(String.format("❌ Conta %d - Saldo INSUFICIENTE para transferência de R$ %.2f",
                        this.numero, valor));
                }
            }
        }
        sleep();
    }

    public synchronized void creditarJuros(double taxa) {
        double juros = saldo * taxa;
        saldo += juros;
        System.out.println(String.format("📈 Conta %d - JUROS de R$ %.2f (%.1f%%) → Saldo: R$ %.2f",
            numero, juros, taxa * 100, saldo));
        sleep();
    }

    public synchronized double getSaldo() {
        return saldo;
    }

    public int getNumero() {
        return numero;
    }

    private void sleep() {
        try {
            Thread.sleep(100 + new Random().nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

class OperacaoThread extends Thread {
    private String tipoOperacao;
    private ContaBancaria conta;
    private ContaBancaria contaDestino;
    private double valor;
    private int operacaoId;

    // Construtor para Depósito, Saque e Juros
    public OperacaoThread(int operacaoId, String tipo, ContaBancaria conta, double valor) {
        this.operacaoId = operacaoId;
        this.tipoOperacao = tipo;
        this.conta = conta;
        this.valor = valor;
    }

    // Construtor para Transferência
    public OperacaoThread(int operacaoId, ContaBancaria origem, ContaBancaria destino, double valor) {
        this.operacaoId = operacaoId;
        this.tipoOperacao = "TRANSFERENCIA";
        this.conta = origem;
        this.contaDestino = destino;
        this.valor = valor;
    }

    @Override
    public void run() {
        try {
            switch (tipoOperacao) {
                case "DEPOSITO":
                    conta.depositar(valor);
                    break;
                case "SAQUE":
                    conta.sacar(valor);
                    break;
                case "JUROS":
                    conta.creditarJuros(valor);
                    break;
                case "TRANSFERENCIA":
                    conta.transferir(contaDestino, valor);
                    break;
            }
        } catch (Exception e) {
            System.out.println("Erro na operação " + operacaoId + ": " + e.getMessage());
        }
    }
}

public class SimulacaoContasBancarias {
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║     SIMULAÇÃO DE OPERAÇÕES BANCÁRIAS CONCORRENTES ║");
        System.out.println("╚═══════════════════════════════════════════════════╝\n");

        // Cria contas com saldos iniciais
        ContaBancaria conta1 = new ContaBancaria(1, 1000.00);
        ContaBancaria conta2 = new ContaBancaria(2, 500.00);
        ContaBancaria conta3 = new ContaBancaria(3, 750.00);

        System.out.println("💳 Saldos Iniciais:");
        System.out.println("   Conta 1: R$ " + String.format("%.2f", conta1.getSaldo()));
        System.out.println("   Conta 2: R$ " + String.format("%.2f", conta2.getSaldo()));
        System.out.println("   Conta 3: R$ " + String.format("%.2f", conta3.getSaldo()));
        System.out.println("\n🚀 Iniciando operações concorrentes...\n");

        // Lista de threads de operações
        List<Thread> operacoes = new ArrayList<>();
        int opId = 1;

        // Operações na Conta 1
        operacoes.add(new OperacaoThread(opId++, "DEPOSITO", conta1, 200.00));
        operacoes.add(new OperacaoThread(opId++, "SAQUE", conta1, 100.00));
        operacoes.add(new OperacaoThread(opId++, conta1, conta2, 300.00));  // Transferência
        operacoes.add(new OperacaoThread(opId++, "JUROS", conta1, 0.01));  // 1% juros

        // Operações na Conta 2
        operacoes.add(new OperacaoThread(opId++, "DEPOSITO", conta2, 100.00));
        operacoes.add(new OperacaoThread(opId++, "SAQUE", conta2, 50.00));
        operacoes.add(new OperacaoThread(opId++, conta2, conta3, 200.00));  // Transferência
        operacoes.add(new OperacaoThread(opId++, "JUROS", conta2, 0.01));

        // Operações na Conta 3
        operacoes.add(new OperacaoThread(opId++, "DEPOSITO", conta3, 150.00));
        operacoes.add(new OperacaoThread(opId++, "SAQUE", conta3, 100.00));
        operacoes.add(new OperacaoThread(opId++, conta3, conta1, 250.00));  // Transferência
        operacoes.add(new OperacaoThread(opId++, "JUROS", conta3, 0.01));

        // Mais operações simultâneas para testar concorrência
        operacoes.add(new OperacaoThread(opId++, "SAQUE", conta1, 50.00));
        operacoes.add(new OperacaoThread(opId++, "DEPOSITO", conta2, 75.00));
        operacoes.add(new OperacaoThread(opId++, conta1, conta3, 100.00));

        try {
            // Inicia todas as operações
            for (Thread op : operacoes) {
                op.start();
            }

            // Aguarda todas terminarem
            for (Thread op : operacoes) {
                op.join();
            }

            System.out.println("\n╔═══════════════════════════════════════════════════╗");
            System.out.println("║              OPERAÇÕES FINALIZADAS                ║");
            System.out.println("╚═══════════════════════════════════════════════════╝");
            System.out.println("\n💳 Saldos Finais:");
            System.out.println("   Conta 1: R$ " + String.format("%.2f", conta1.getSaldo()));
            System.out.println("   Conta 2: R$ " + String.format("%.2f", conta2.getSaldo()));
            System.out.println("   Conta 3: R$ " + String.format("%.2f", conta3.getSaldo()));

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
