package net.corda.samples.example.client;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import net.corda.samples.example.states.TemperatureState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class TemperatureDataChecker {
    private static final Logger logger = LoggerFactory.getLogger(TemperatureDataChecker.class);
    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: TemperatureDataChecker <host:port> <username> <password>");
            return;
        }

        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(args[0]);
        final String username = args[1];
        final String password = args[2];

        try (CordaRPCConnection connection = new CordaRPCClient(nodeAddress).start(username, password)) {
            final CordaRPCOps proxy = connection.getProxy();

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("\n==== Temperature Data Checker ====");
                System.out.println("1. 查询所有温度记录");
                System.out.println("2. 查询关键温度记录");
                System.out.println("3. 退出");
                System.out.print("请选择操作: ");

                int choice = scanner.nextInt();
                scanner.nextLine(); // 消耗换行符

                switch (choice) {
                    case 1:
                        queryTemperatures(proxy, false);
                        break;
                    case 2:
                        queryTemperatures(proxy, true);
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

    private static void queryTemperatures(CordaRPCOps proxy, boolean onlyCritical) {
        try {
            List<StateAndRef<TemperatureState>> states = proxy.startTrackedFlowDynamic(
                    net.corda.samples.example.flows.TemperatureFlows.QueryTemperaturesFlow.class,
                    onlyCritical
            ).getReturnValue().get();

            System.out.printf("\n=== %s温度记录(共%d条) ===%n",
                    onlyCritical ? "关键" : "所有", states.size());

            if (states.isEmpty()) {
                System.out.println("没有找到温度记录");
                return;
            }

            states.forEach(state -> {
                TemperatureState temp = state.getState().getData();
                System.out.printf("| 时间: %s | 温度: %.1f°C | 关键: %-5s | 所有者: %-15s | 接收方: %-15s |%n",
                        formatter.format(temp.getTimestamp()),
                        temp.getTemperature(),
                        temp.isCritical(),
                        temp.getOwner().getName().getOrganisation(),
                        temp.getReceiver().getName().getOrganisation());
            });

            System.out.println("=== 记录结束 ===");

        } catch (Exception e) {
            System.out.println("查询失败: " + e.getMessage());
        }
    }
}