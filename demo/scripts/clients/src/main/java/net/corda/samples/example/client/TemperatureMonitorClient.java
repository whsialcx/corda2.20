package net.corda.samples.example.client;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.NetworkHostAndPort;
import net.corda.samples.example.states.TemperatureState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

public class TemperatureMonitorClient {
    private static final Logger logger = LoggerFactory.getLogger(TemperatureMonitorClient.class);

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: TemperatureMonitorClient <host:port> <username> <password>");
            return;
        }

        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(args[0]);
        final String username = args[1];
        final String password = args[2];

        try {
            // 1. 连接到节点
            logger.info("Connecting to node at {}...", nodeAddress);
            final CordaRPCClient client = new CordaRPCClient(nodeAddress);
            final CordaRPCConnection connection = client.start(username, password);
            final CordaRPCOps proxy = connection.getProxy();

            // 2. 获取网络参与方
            Party notary = proxy.notaryIdentities().get(0);
            Party self = proxy.nodeInfo().getLegalIdentities().get(0);
            Party otherParty = proxy.partiesFromName("O=PartyB,L=New York,C=US", false).iterator().next();

            logger.info("Notary: {}", notary);
            logger.info("Self: {}", self);
            logger.info("Other Party: {}", otherParty);

            // 3. 测试记录温度
            testRecordTemperature(proxy, self, otherParty);

            // 4. 测试查询温度
            testQueryTemperatures(proxy);

            // 5. 测试转移温度记录
            testTransferTemperature(proxy, self, otherParty);

        } catch (Exception e) {
            logger.error("Error during client operation", e);
        } finally {
            // connection.notifyServerAndClose(); // 实际使用时取消注释
        }
    }

    private static void testRecordTemperature(CordaRPCOps proxy, Party owner, Party receiver) {
        logger.info("\n=== Testing Temperature Recording ===");

        try {
            // 创建温度记录
            Instant timestamp = Instant.now();
            double temperature = 28.5;
            boolean isCritical = true;

            logger.info("Recording temperature: {}°C (Critical: {}) from {} to {}",
                    temperature, isCritical, owner, receiver);

            SignedTransaction tx = proxy.startTrackedFlowDynamic(
                    net.corda.samples.example.flows.TemperatureFlows.CreateTemperatureFlow.class,
                    timestamp, temperature, isCritical, receiver
            ).getReturnValue().get();

            logger.info("Transaction recorded: {}", tx.getId());
            logger.info("Output states:");
            tx.getTx().getOutputs().forEach(out ->
                    logger.info("- {}", out.getData()));

        } catch (Exception e) {
            logger.error("Failed to record temperature", e);
        }
    }

    private static void testQueryTemperatures(CordaRPCOps proxy) {
        logger.info("\n=== Testing Temperature Query ===");

        try {
            // 查询所有温度记录
            List<StateAndRef<TemperatureState>> allTemps = proxy.startTrackedFlowDynamic(
                    net.corda.samples.example.flows.TemperatureFlows.QueryTemperaturesFlow.class,
                    false
            ).getReturnValue().get();

            logger.info("All temperature records ({}):", allTemps.size());
            allTemps.forEach(state -> {
                TemperatureState temp = state.getState().getData();
                logger.info("- {}°C at {} (Critical: {}) from {} to {}",
                        temp.getTemperature(), temp.getTimestamp(),
                        temp.isCritical(), temp.getOwner(), temp.getReceiver());
            });

            // 查询关键温度记录
            List<StateAndRef<TemperatureState>> criticalTemps = proxy.startTrackedFlowDynamic(
                    net.corda.samples.example.flows.TemperatureFlows.QueryTemperaturesFlow.class,
                    true
            ).getReturnValue().get();

            logger.info("Critical temperature records ({}):", criticalTemps.size());
            criticalTemps.forEach(state -> {
                TemperatureState temp = state.getState().getData();
                logger.info("- {}°C at {} from {} to {}",
                        temp.getTemperature(), temp.getTimestamp(),
                        temp.getOwner(), temp.getReceiver());
            });

        } catch (Exception e) {
            logger.error("Failed to query temperatures", e);
        }
    }

    private static void testTransferTemperature(CordaRPCOps proxy, Party owner, Party originalReceiver) {
        logger.info("\n=== Testing Temperature Transfer ===");

        try {
            // 1. 首先查询现有记录
            List<StateAndRef<TemperatureState>> temps = proxy.startTrackedFlowDynamic(
                    net.corda.samples.example.flows.TemperatureFlows.QueryTemperaturesFlow.class,
                    false
            ).getReturnValue().get();

            if (temps.isEmpty()) {
                logger.info("No temperature records found to transfer");
                return;
            }

            // 2. 获取第一条记录的ID
            TemperatureState temp = temps.get(0).getState().getData();
            String linearId = temp.getLinearId().toString();
            Party newReceiver = proxy.partiesFromName("O=PartyC,L=Paris,C=FR", false).iterator().next();

            logger.info("Transferring temperature record {} from {} to new receiver {}",
                    linearId, temp.getReceiver(), newReceiver);

            // 3. 执行转移
            SignedTransaction tx = proxy.startTrackedFlowDynamic(
                    net.corda.samples.example.flows.TemperatureFlows.TransferTemperatureFlow.class,
                    temp.getLinearId(), newReceiver
            ).getReturnValue().get();

            logger.info("Transfer transaction recorded: {}", tx.getId());
            logger.info("Updated state:");
            tx.getTx().getOutputs().forEach(out ->
                    logger.info("- {}", out.getData()));

        } catch (Exception e) {
            logger.error("Failed to transfer temperature record", e);
        }
    }
}