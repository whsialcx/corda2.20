package net.corda.samples.example;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.NetworkHostAndPort;
import net.corda.samples.example.flows.TemperatureFlows;
import net.corda.samples.example.states.TemperatureState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class TemperatureClient {
    private static final Logger logger = LoggerFactory.getLogger(TemperatureClient.class);

    private final CordaRPCOps proxy;
    private final CordaRPCConnection connection;

    public TemperatureClient(String host, int port, String username, String password) {
        CordaRPCClient client = new CordaRPCClient(new NetworkHostAndPort(host, port));
        this.connection = client.start(username, password);
        this.proxy = connection.getProxy();
    }

    public void close() {
        connection.notifyServerAndClose();
    }

    public SignedTransaction recordTemperature(Instant timestamp, double temperature, boolean isCritical, Party receiver)
            throws InterruptedException, ExecutionException {
        return proxy.startTrackedFlowDynamic(
                        TemperatureFlows.CreateTemperatureFlow.class,
                        timestamp, temperature, isCritical, receiver)
                .getReturnValue()
                .get();
    }

    public SignedTransaction transferTemperature(String linearId, Party newReceiver)
            throws InterruptedException, ExecutionException {
        return proxy.startTrackedFlowDynamic(
                        TemperatureFlows.TransferTemperatureFlow.class,
                        UniqueIdentifier.Companion.fromString(linearId),
                        newReceiver)
                .getReturnValue()
                .get();
    }

    public List<StateAndRef<TemperatureState>> queryTemperatures(boolean onlyCritical)
            throws InterruptedException, ExecutionException {
        return proxy.startTrackedFlowDynamic(
                        TemperatureFlows.QueryTemperaturesFlow.class,
                        onlyCritical)
                .getReturnValue()
                .get();
    }

    public Party getPartyByName(String name) {
        List<NodeInfo> nodes = proxy.networkMapSnapshot();
        return nodes.stream()
                .flatMap(it -> it.getLegalIdentities().stream())
                .filter(it -> it.getName().getOrganisation().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Party not found: " + name));
    }

    public Party getMyIdentity() {
        return proxy.nodeInfo().getLegalIdentities().get(0);
    }
}