package net.corda.samples.example.client;

import java.util.concurrent.atomic.AtomicInteger;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.NetworkHostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TemperatureDataWriter {
    private static final Logger logger = LoggerFactory.getLogger(TemperatureDataWriter.class);
    private static final Random random = new Random();

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: TemperatureDataWriter <host:port> <username> <password>");
            return;
        }

        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(args[0]);
        final String username = args[1];
        final String password = args[2];

        try (CordaRPCConnection connection = new CordaRPCClient(nodeAddress).start(username, password)) {
            final CordaRPCOps proxy = connection.getProxy();

            // 获取参与方信息
            Party self = proxy.nodeInfo().getLegalIdentities().get(0);
            Party otherParty = proxy.partiesFromName("O=PartyB,L=New York,C=US", false).iterator().next();

            logger.info("Connected to node: {}", self);
            logger.info("Default receiver: {}", otherParty);

            // 创建交互式菜单
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("\n==== Temperature Data Writer ====");
                System.out.println("1. 手动输入温度数据");
                System.out.println("2. 启动自动模拟数据生成");
                System.out.println("3. 退出");
                System.out.print("请选择操作: ");

                int choice = scanner.nextInt();
                scanner.nextLine(); // 消耗换行符

                switch (choice) {
                    case 1:
                        manualInput(proxy, self, otherParty, scanner);
                        break;
                    case 2:
                        autoGenerate(proxy, self, otherParty, scanner);
                        break;
                    case 3:
                        System.out.println("退出程序");
                        return;
                    default:
                        System.out.println("无效选择");
                }
            }
        } catch (Exception e) {
            logger.error("发生错误: ", e);
        }
    }

    private static void manualInput(CordaRPCOps proxy, Party owner, Party receiver, Scanner scanner) {
        System.out.println("\n--- 手动输入温度数据 ---");

        System.out.print("输入温度值(°C): ");
        double temperature = scanner.nextDouble();

        System.out.print("是否为关键数据(true/false): ");
        boolean isCritical = scanner.nextBoolean();

        System.out.print("使用当前时间戳?(y/n): ");
        boolean useCurrentTime = scanner.next().equalsIgnoreCase("y");

        Instant timestamp = useCurrentTime ? Instant.now() : Instant.now().minusSeconds(3600); // 或自定义时间

        try {
            SignedTransaction tx = proxy.startTrackedFlowDynamic(
                    net.corda.samples.example.flows.TemperatureFlows.CreateTemperatureFlow.class,
                    timestamp, temperature, isCritical, receiver
            ).getReturnValue().get();

            System.out.println("\n成功记录温度数据!");
            System.out.println("交易ID: " + tx.getId());
            System.out.println("温度: " + temperature + "°C");
            System.out.println("时间: " + timestamp);
            System.out.println("关键: " + isCritical);
            System.out.println("接收方: " + receiver.getName());

        } catch (Exception e) {
            System.out.println("记录失败: " + e.getMessage());
        }
    }

    private static void autoGenerate(CordaRPCOps proxy, Party owner, Party receiver, Scanner scanner) {
        System.out.println("\n--- 自动生成模拟数据 ---");
        System.out.print("输入生成间隔(秒): ");
        long interval = scanner.nextLong();

        System.out.print("输入生成次数: ");
        AtomicInteger count = new AtomicInteger(scanner.nextInt());

        System.out.println("开始自动生成温度数据...");

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            try {
                double temperature = 20 + random.nextDouble() * 15; // 20-35°C
                boolean isCritical = temperature > 30; // 超过30°C为关键数据
                Instant timestamp = Instant.now();

                SignedTransaction tx = proxy.startTrackedFlowDynamic(
                        net.corda.samples.example.flows.TemperatureFlows.CreateTemperatureFlow.class,
                        timestamp, temperature, isCritical, receiver
                ).getReturnValue().get();

                System.out.printf("\n自动生成数据: %.1f°C, 关键: %s, 时间: %s, 交易ID: %s%n",
                        temperature, isCritical, timestamp, tx.getId().toString().substring(0, 8));

                if (count.decrementAndGet() <= 0) {
                    executor.shutdown();
                    System.out.println("自动生成完成");
                }
            } catch (Exception e) {
                System.out.println("自动生成失败: " + e.getMessage());
                executor.shutdown();
            }
        }, 0, interval, TimeUnit.SECONDS);

        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}