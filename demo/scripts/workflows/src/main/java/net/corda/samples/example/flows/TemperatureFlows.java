package net.corda.samples.example.flows;
import net.corda.core.node.services.vault.Builder;
import net.corda.core.node.services.vault.FieldInfo;
import net.corda.core.node.services.vault.QueryCriteria;
import co.paralleluniverse.fibers.Suspendable;
import net.corda.samples.example.contracts.TemperatureContract;
import net.corda.samples.example.states.TemperatureState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;



public class TemperatureFlows {

    @InitiatingFlow
    @StartableByRPC
    public static class CreateTemperatureFlow extends FlowLogic<SignedTransaction> {
        private final Instant timestamp;
        private final double temperature;
        private final boolean isCritical;
        private final Party receiver;

        private final ProgressTracker progressTracker = new ProgressTracker();

        public CreateTemperatureFlow(Instant timestamp, double temperature, boolean isCritical, Party receiver) {
            this.timestamp = timestamp;
            this.temperature = temperature;
            this.isCritical = isCritical;
            this.receiver = receiver;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Get notary
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Create output state
            Party owner = getOurIdentity();
            TemperatureState outputState = new TemperatureState(
                    timestamp, temperature, isCritical, owner, receiver, new UniqueIdentifier());

            // Build transaction
            Command<TemperatureContract.Commands.Create> command = new Command<>(
                    new TemperatureContract.Commands.Create(),
                    outputState.getParticipants().stream().map(it -> it.getOwningKey()).collect(Collectors.toList()));

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(outputState, TemperatureContract.ID)
                    .addCommand(command);

            // Verify and sign transaction
            txBuilder.verify(getServiceHub());
            SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Collect counterparty signatures
            FlowSession receiverSession = initiateFlow(receiver);
            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
                    partSignedTx, Collections.singletonList(receiverSession)));

            // Finalize transaction
            return subFlow(new FinalityFlow(fullySignedTx, Collections.singletonList(receiverSession)));
        }
    }

    @InitiatedBy(CreateTemperatureFlow.class)
    public static class CreateTemperatureFlowResponder extends FlowLogic<SignedTransaction> {
        private final FlowSession otherPartySession;

        public CreateTemperatureFlowResponder(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartySession, ProgressTracker progressTracker) {
                    super(otherPartySession, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    // Add any additional validation here
                }
            }

            SignedTransaction signedTx = subFlow(new SignTxFlow(otherPartySession, SignTransactionFlow.tracker()));
            return subFlow(new ReceiveFinalityFlow(otherPartySession, signedTx.getId()));
        }
    }

    @InitiatingFlow
    @StartableByRPC
    public static class TransferTemperatureFlow extends FlowLogic<SignedTransaction> {
        private final UniqueIdentifier linearId;
        private final Party newReceiver;

        private final ProgressTracker progressTracker = new ProgressTracker();

        public TransferTemperatureFlow(UniqueIdentifier linearId, Party newReceiver) {
            this.linearId = linearId;
            this.newReceiver = newReceiver;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Get notary
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Get input state
            QueryCriteria.LinearStateQueryCriteria criteria = new QueryCriteria.LinearStateQueryCriteria()
                    .withUuid(Collections.singletonList(linearId.getId()));
            StateAndRef<TemperatureState> inputStateAndRef = getServiceHub().getVaultService()
                    .queryBy(TemperatureState.class, criteria).getStates().get(0);
            TemperatureState inputState = inputStateAndRef.getState().getData();

            // Create output state
            TemperatureState outputState = new TemperatureState(
                    inputState.getTimestamp(),
                    inputState.getTemperature(),
                    inputState.isCritical(),
                    inputState.getOwner(),
                    newReceiver,
                    inputState.getLinearId());

            // Build transaction
            Command<TemperatureContract.Commands.Transfer> command = new Command<>(
                    new TemperatureContract.Commands.Transfer(),
                    outputState.getParticipants().stream().map(it -> it.getOwningKey()).collect(Collectors.toList()));

            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(inputStateAndRef)
                    .addOutputState(outputState, TemperatureContract.ID)
                    .addCommand(command);

            // Verify and sign transaction
            txBuilder.verify(getServiceHub());
            SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Collect counterparty signatures
//            List<FlowSession> sessions = outputState.getParticipants().stream()
//                    .filter(it -> !it.equals(getOurIdentity()))
//                    .map(this::initiateFlow)
//                    .collect(Collectors.toList());
            FlowSession newReceiverSession = initiateFlow(newReceiver);

            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
                    partSignedTx, Collections.singletonList(newReceiverSession)));

            return subFlow(new FinalityFlow(fullySignedTx, Collections.singletonList(newReceiverSession)));

//            SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, sessions));

            // Finalize transaction
//            return subFlow(new FinalityFlow(fullySignedTx, sessions));
        }
    }

    @InitiatedBy(TransferTemperatureFlow.class)
    public static class TransferTemperatureFlowResponder extends FlowLogic<SignedTransaction> {
        private final FlowSession otherPartySession;

        public TransferTemperatureFlowResponder(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartySession, ProgressTracker progressTracker) {
                    super(otherPartySession, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    // Add any additional validation here
                }
            }

            SignedTransaction signedTx = subFlow(new SignTxFlow(otherPartySession, SignTransactionFlow.tracker()));
            return subFlow(new ReceiveFinalityFlow(otherPartySession, signedTx.getId()));
        }
    }

    @StartableByRPC
    public static class QueryTemperaturesFlow extends FlowLogic<List<StateAndRef<TemperatureState>>> {
        private final boolean onlyCritical;

        public QueryTemperaturesFlow(boolean onlyCritical) {
            this.onlyCritical = onlyCritical;
        }

        @Suspendable
        @Override
        public List<StateAndRef<TemperatureState>> call() throws FlowException {
            if (onlyCritical) {
                // 方案1：使用内存过滤（简单但性能较差）
                return getAllStates().stream()
                        .filter(stateAndRef -> stateAndRef.getState().getData().isCritical())
                        .collect(Collectors.toList());

                // 或者方案2：使用线性ID查询（需要预先知道关键状态的ID）
                // return queryByLinearIds(getCriticalStateIds());
            } else {
                return getAllStates();
            }
        }

        // 获取所有温度状态
        private List<StateAndRef<TemperatureState>> getAllStates() {
            return getServiceHub().getVaultService()
                    .queryBy(TemperatureState.class)
                    .getStates();
        }

        // 方案2的辅助方法（需要额外实现获取关键状态ID的逻辑）
        private List<UniqueIdentifier> getCriticalStateIds() {
            return getAllStates().stream()
                    .filter(stateAndRef -> stateAndRef.getState().getData().isCritical())
                    .map(stateAndRef -> stateAndRef.getState().getData().getLinearId())
                    .collect(Collectors.toList());
        }

        // 方案2的查询方法
        private List<StateAndRef<TemperatureState>> queryByLinearIds(List<UniqueIdentifier> ids) {
            QueryCriteria.LinearStateQueryCriteria criteria = new QueryCriteria.LinearStateQueryCriteria()
                    .withUuid(ids.stream().map(UniqueIdentifier::getId).collect(Collectors.toList()));
            return getServiceHub().getVaultService()
                    .queryBy(TemperatureState.class, criteria)
                    .getStates();
        }
    }
}