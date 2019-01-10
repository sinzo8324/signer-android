package com.gruutnetworks.gruutsigner.ui.dashboard;

import android.app.Application;
import android.arch.lifecycle.*;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import com.google.protobuf.ByteString;
import com.gruutnetworks.gruutsigner.*;
import com.gruutnetworks.gruutsigner.Identity;
import com.gruutnetworks.gruutsigner.R;
import com.gruutnetworks.gruutsigner.exceptions.AuthUtilException;
import com.gruutnetworks.gruutsigner.exceptions.ErrorMsgException;
import com.gruutnetworks.gruutsigner.gruut.GruutConfigs;
import com.gruutnetworks.gruutsigner.gruut.Merger;
import com.gruutnetworks.gruutsigner.gruut.MergerList;
import com.gruutnetworks.gruutsigner.model.*;
import com.gruutnetworks.gruutsigner.util.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class DashboardViewModel extends AndroidViewModel implements LifecycleObserver {

    private static final String TAG = "DashboardViewModel";

    public enum MergerNum {
        MERGER_1, MERGER_2
    }

    private MutableLiveData<String> logMerger1 = new MutableLiveData<>();
    private MutableLiveData<String> logMerger2 = new MutableLiveData<>();
    private MutableLiveData<String> ipMerger1 = new MutableLiveData<>();
    private MutableLiveData<String> ipMerger2 = new MutableLiveData<>();
    private MutableLiveData<String> portMerger1 = new MutableLiveData<>();
    private MutableLiveData<String> portMerger2 = new MutableLiveData<>();
    private MutableLiveData<Boolean> errorMerger1 = new MutableLiveData<>();
    private MutableLiveData<Boolean> errorMerger2 = new MutableLiveData<>();
    private final SingleLiveEvent refreshTriggerMerger1 = new SingleLiveEvent();
    private final SingleLiveEvent refreshTriggerMerger2 = new SingleLiveEvent();
    private final SingleLiveEvent openSetting1Dialog = new SingleLiveEvent();
    private final SingleLiveEvent openSetting2Dialog = new SingleLiveEvent();

    private static AuthCertUtil authCertUtil;
    private static AuthHmacUtil authHmacUtil;
    private static PreferenceUtil preferenceUtil;
    private static SignedBlockDao blockDao;

    private ManagedChannel channel1;
    private ManagedChannel channel2;

    private static String sId;
    private static KeyPair keyPair;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        authCertUtil = AuthCertUtil.getInstance();
        authHmacUtil = AuthHmacUtil.getInstance();
        preferenceUtil = PreferenceUtil.getInstance(application.getApplicationContext());
        sId = preferenceUtil.getString(PreferenceUtil.Key.SID_STR);

        blockDao = AppDatabase.getDatabase(application).blockDao();

        SnackbarMessage snackbarMessage = new SnackbarMessage();

        if (!NetworkUtil.isConnected(application.getApplicationContext())) {
            snackbarMessage.postValue(R.string.sign_up_error_network);
        }

        try {
            keyPair = authHmacUtil.generateEcdhKeys();
        } catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            snackbarMessage.postValue(R.string.join_error_key_gen);
            throw new AuthUtilException(AuthUtilException.AuthErr.KEY_GEN_ERROR);
        }
    }

    /**
     * 화면이 다 그려졌을 때 join 시작
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        refreshMerger1();
        refreshMerger2();
    }

    private JoiningThread thread1;
    private JoiningThread thread2;

    private Merger getRandomMerger() {
        Random rand = new Random(System.currentTimeMillis());
        int index = rand.nextInt(3);

        return MergerList.MERGER_LIST.get(index);
    }

    public void refreshMerger1() {
        if (thread1 != null) {
            thread1.interrupt();
            thread1 = null;
        }

        terminateChannel(channel1);

        refreshTriggerMerger1.call();
        errorMerger1.postValue(false);

        ipMerger1.setValue(preferenceUtil.getString(PreferenceUtil.Key.IP1_STR));
        portMerger1.setValue(preferenceUtil.getString(PreferenceUtil.Key.PORT1_STR));

        if (ipMerger1.getValue() != null && portMerger1.getValue() != null &&
                !ipMerger1.getValue().isEmpty() && !portMerger1.getValue().isEmpty()) {
            channel1 = ManagedChannelBuilder
                    .forAddress(ipMerger1.getValue(), Integer.parseInt(portMerger1.getValue()))
                    .usePlaintext()
                    .build();
            logMerger1.postValue("[Channel Setting]" + ipMerger1.getValue() + ":" + portMerger1.getValue());

            thread1 = new JoiningThread(this,
                    channel1,
                    logMerger1,
                    errorMerger1);
            thread1.start();
        } else {
            Merger merger = getRandomMerger();
            ipMerger1.setValue(merger.getUri());
            portMerger1.setValue(Integer.toString(merger.getPort()));

            preferenceUtil.put(PreferenceUtil.Key.IP1_STR, merger.getUri());
            preferenceUtil.put(PreferenceUtil.Key.PORT1_STR, Integer.toString(merger.getPort()));

            refreshMerger1();
        }
    }

    public void refreshMerger2() {
        if (thread2 != null) {
            thread2.interrupt();
            thread2 = null;
        }

        terminateChannel(channel2);

        refreshTriggerMerger2.call();
        errorMerger2.postValue(false);

        ipMerger2.setValue(preferenceUtil.getString(PreferenceUtil.Key.IP2_STR));
        portMerger2.setValue(preferenceUtil.getString(PreferenceUtil.Key.PORT2_STR));

        if (ipMerger2.getValue() != null && portMerger2.getValue() != null &&
                !ipMerger2.getValue().isEmpty() && !portMerger2.getValue().isEmpty()) {
            channel2 = ManagedChannelBuilder
                    .forAddress(ipMerger2.getValue(), Integer.parseInt(portMerger2.getValue()))
                    .usePlaintext()
                    .build();
            logMerger2.postValue("[Channel Setting]" + ipMerger2.getValue() + ":" + portMerger2.getValue());

            thread2 = new JoiningThread(this,
                    channel2,
                    logMerger2,
                    errorMerger2);
            thread2.start();
        } else {
            Merger merger = getRandomMerger();
            ipMerger2.setValue(merger.getUri());
            portMerger2.setValue(Integer.toString(merger.getPort()));

            preferenceUtil.put(PreferenceUtil.Key.IP2_STR, merger.getUri());
            preferenceUtil.put(PreferenceUtil.Key.PORT2_STR, Integer.toString(merger.getPort()));

            refreshMerger2();
        }
    }

    public void openAddressSetting(int mergerNum) {
        if (mergerNum == 1) {
            openSetting1Dialog.call();
        } else if (mergerNum == 2) {
            openSetting2Dialog.call();
        }
    }

    MutableLiveData<String> getLogMerger1() {
        return logMerger1;
    }

    MutableLiveData<String> getLogMerger2() {
        return logMerger2;
    }

    public MutableLiveData<String> getIpMerger1() {
        return ipMerger1;
    }

    public MutableLiveData<String> getIpMerger2() {
        return ipMerger2;
    }

    public MutableLiveData<String> getPortMerger1() {
        return portMerger1;
    }

    public MutableLiveData<String> getPortMerger2() {
        return portMerger2;
    }

    public MutableLiveData<Boolean> getErrorMerger1() {
        return errorMerger1;
    }

    public MutableLiveData<Boolean> getErrorMerger2() {
        return errorMerger2;
    }

    SingleLiveEvent getRefreshTriggerMerger1() {
        return refreshTriggerMerger1;
    }

    SingleLiveEvent getRefreshTriggerMerger2() {
        return refreshTriggerMerger2;
    }

    SingleLiveEvent getOpenSetting1Dialog() {
        return openSetting1Dialog;
    }

    SingleLiveEvent getOpenSetting2Dialog() {
        return openSetting2Dialog;
    }

    @Override
    protected void onCleared() {
        terminateChannel(channel1);
        terminateChannel(channel2);

        channel1 = null;
        channel2 = null;

        if (thread1 != null) {
            thread1.interrupt();
            thread1 = null;
        }

        if (thread2 != null) {
            thread2.interrupt();
            thread2 = null;
        }
    }

    private void terminateChannel(ManagedChannel channel) {
        if (channel != null && !channel.isShutdown()) {
            Log.e(TAG, channel + "::terminateChannel::ShutdownNow()");
            channel.shutdownNow();
        }
    }

    private static class JoiningThread extends Thread {

        private final WeakReference<DashboardViewModel> viewModel;
        private final ManagedChannel channel;
        private final MutableLiveData<String> log;
        private final MutableLiveData<Boolean> error;

        private String signerNonce;
        private String mergerNonce;

        JoiningThread(DashboardViewModel viewModel,
                      ManagedChannel channel,
                      MutableLiveData<String> log,
                      MutableLiveData<Boolean> error) {
            this.viewModel = new WeakReference<>(viewModel);
            this.channel = channel;
            this.log = log;
            this.error = error;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.postValue(e.getMessage());
                error.postValue(true);
            }

            if (viewModel.get() == null) {
                log.postValue("Thread is dead");
                error.postValue(true);
                return;
            }

            try {
                UnpackMsgChallenge challenge = requestJoin(channel, log);
                UnpackMsgResponse2 response2 = sendPublicKey(channel, challenge, log);
                UnpackMsgAccept accept = sendSuccess(channel, response2, log);
                if (accept.isVal()) {
                    standBy(channel, log, error);
                }
            } catch (ErrorMsgException e) {
                if (!channel.isShutdown()) {
                    log.postValue("[ERROR]" + e.getMessage());
                    Log.e(TAG, channel.toString() + "::[ERROR]" + e.getMessage());
                    error.postValue(true);
                }
            } catch (AuthUtilException e) {
                if (!channel.isShutdown()) {
                    log.postValue("[CRYPTO_ERROR]" + e.getMessage());
                    Log.e(TAG, channel.toString() + "::[CRYPTO_ERROR]" + e.getMessage());
                    error.postValue(true);
                }
            } catch (InterruptedException | ExecutionException e) {
                // AsyncTask error
                // Ignore it
            }

        }

        /**
         * Start joining request
         * SEND MSG_JOIN to merger
         *
         * @param channel target merger
         * @return received MSG_CHALLENGE
         * @throws StatusRuntimeException on GRPC error
         */
        private UnpackMsgChallenge requestJoin(ManagedChannel channel, MutableLiveData<String> log) throws StatusRuntimeException, ExecutionException, InterruptedException {
            log.postValue("START requestJoin...");

            PackMsgJoin packMsgJoin = new PackMsgJoin(
                    sId,
                    AuthGeneralUtil.getTimestamp(),
                    GruutConfigs.ver,
                    GruutConfigs.localChainId
            );

            log.postValue("[SEND]" + "MSG_JOIN");
            MsgUnpacker receivedMsg = new GrpcTask(viewModel.get(), channel).execute(packMsgJoin).get();

            // Check received message's type
            if (receivedMsg == null) {
                // This error message may be caused by a timeout.
                throw new ErrorMsgException(ErrorMsgException.MsgErr.MSG_NOT_FOUND);
            } else if (receivedMsg.getMessageType() == TypeMsg.MSG_ERROR) {
                UnpackMsgError msgError = (UnpackMsgError) receivedMsg;
                throw new ErrorMsgException(ErrorMsgException.MsgErr.MSG_ERR_RECEIVED, TypeError.convert(msgError.getErrType()).name());
            } else if (!receivedMsg.isSenderValid()) {
                throw new ErrorMsgException(ErrorMsgException.MsgErr.MSG_HEADER_NOT_MATCHED);
            }

            log.postValue("[RECEIVE]" + "MSG_CHALLENGE");
            return (UnpackMsgChallenge) receivedMsg;
        }

        /**
         * Start to exchange dh key
         * SEND MSG_RESPONSE_1 to merger
         *
         * @param channel          target merger
         * @param messageChallenge received MSG_CHALLENGE
         * @return received MSG_RESPONSE_2
         */
        private UnpackMsgResponse2 sendPublicKey(ManagedChannel channel, UnpackMsgChallenge messageChallenge, MutableLiveData<String> log) throws ExecutionException, InterruptedException {
            log.postValue("START sendPublicKey...");

            // generate signer nonce
            signerNonce = AuthGeneralUtil.getNonce();

            // get merger nonce
            mergerNonce = messageChallenge.getMergerNonce();

            if (!AuthGeneralUtil.isMsgInTime(messageChallenge.getTime())) {
                throw new ErrorMsgException(ErrorMsgException.MsgErr.MSG_EXPIRED);
            }

            if (keyPair == null) {
                throw new AuthUtilException(AuthUtilException.AuthErr.NO_KEY_ERROR);
            }

            String x = new String(authHmacUtil.pubToXpoint(keyPair.getPublic()));
            String y = new String(authHmacUtil.pubToYpoint(keyPair.getPublic()));
            String time = AuthGeneralUtil.getTimestamp();
            String signature;
            try {
                signature = authCertUtil.signMsgResponse1(messageChallenge.getMergerNonce(), signerNonce, x, y, time);
            } catch (Exception e) {
                throw new AuthUtilException(AuthUtilException.AuthErr.SIGNING_ERROR);
            }

            // Get Certificate issued by GA
            String cert;
            try {
                cert = authCertUtil.getCert(SecurityConstants.Alias.GRUUT_AUTH);
            } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
                throw new AuthUtilException(AuthUtilException.AuthErr.GET_CERT_ERROR);
            }

            if (cert.isEmpty()) {
                throw new AuthUtilException(AuthUtilException.AuthErr.NO_CERT_ERROR);
            }

            PackMsgResponse1 msgResponse1 = new PackMsgResponse1(
                    sId,
                    time,
                    cert,
                    signerNonce,
                    x,  /* HEX */
                    y,  /* HEX */
                    signature /* BASE64 */
            );

            log.postValue("[SEND]" + "MSG_RESPONSE_1");
            MsgUnpacker receivedMsg = new GrpcTask(viewModel.get(), channel).execute(msgResponse1).get();

            // Check received message's type
            if (receivedMsg == null) {
                // This error message may be caused by a timeout.
                throw new ErrorMsgException(ErrorMsgException.MsgErr.MSG_NOT_FOUND);
            } else if (receivedMsg.getMessageType() == TypeMsg.MSG_ERROR) {
                UnpackMsgError msgError = (UnpackMsgError) receivedMsg;
                throw new ErrorMsgException(ErrorMsgException.MsgErr.MSG_ERR_RECEIVED, TypeError.convert(msgError.getErrType()).name());
            } else if (!receivedMsg.isSenderValid()) {
                throw new ErrorMsgException(ErrorMsgException.MsgErr.MSG_HEADER_NOT_MATCHED);
            }

            log.postValue("[RECEIVED]" + "MSG_RESPONSE_2");
            return (UnpackMsgResponse2) receivedMsg;
        }


        /**
         * Finishing joining request
         * SEND MSG_SUCCESS to merger
         *
         * @param channel          target merger
         * @param messageResponse2 received MSG_RESPONSE_2
         * @return received MSG_ACCEPT
         */
        private UnpackMsgAccept sendSuccess(ManagedChannel channel, UnpackMsgResponse2 messageResponse2, MutableLiveData<String> log) throws ExecutionException, InterruptedException {

            try {
                // 서명 검증
                if (!authCertUtil.verifyMsgResponse2(messageResponse2.getSig(), messageResponse2.getCert(),
                        mergerNonce, signerNonce, messageResponse2.getDhPubKeyX(), messageResponse2.getDhPubKeyY(), messageResponse2.getTime())) {
                    throw new AuthUtilException(AuthUtilException.AuthErr.INVALID_SIGNATURE);
                }
            } catch (Exception e) {
                throw new AuthUtilException(AuthUtilException.AuthErr.VERIFYING_ERROR);
            }

            // X,Y 좌표로부터 Pulbic key get
            PublicKey mergerPubKey;
            try {
                mergerPubKey = authHmacUtil.pointToPub(messageResponse2.getDhPubKeyX(), messageResponse2.getDhPubKeyY());
            } catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new AuthUtilException(AuthUtilException.AuthErr.KEY_GEN_ERROR);
            }

            // HMAC KEY 계산
            byte[] hmacKey;
            try {
                hmacKey = authHmacUtil.getSharedSecreyKey(keyPair.getPrivate(), mergerPubKey);
            } catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidKeyException e) {
                throw new AuthUtilException(AuthUtilException.AuthErr.HMAC_KEY_GEN_ERROR);
            }

            // HMAC KEY 저장
            preferenceUtil.put(messageResponse2.getmID(), new String(hmacKey));

            PackMsgSuccess msgSuccess = new PackMsgSuccess(
                    sId,
                    AuthGeneralUtil.getTimestamp(),
                    true
            );
            msgSuccess.setDestinationId(messageResponse2.getmID());

            log.postValue("[SEND]" + "MSG_SUCCESS");
            MsgUnpacker receivedMsg = new GrpcTask(viewModel.get(), channel).execute(msgSuccess).get();

            // Check received message's type
            if (receivedMsg == null) {
                // This error message may be caused by a timeout.
                throw new ErrorMsgException(ErrorMsgException.MsgErr.MSG_NOT_FOUND);
            } else if (receivedMsg.getMessageType() == TypeMsg.MSG_ERROR) {
                UnpackMsgError msgError = (UnpackMsgError) receivedMsg;
                throw new ErrorMsgException(ErrorMsgException.MsgErr.MSG_ERR_RECEIVED, TypeError.convert(msgError.getErrType()).name());
            } else if (!receivedMsg.isSenderValid()) {
                throw new ErrorMsgException(ErrorMsgException.MsgErr.MSG_HEADER_NOT_MATCHED);
            } else if (!receivedMsg.isMacValid()) {
                throw new ErrorMsgException(ErrorMsgException.MsgErr.MSG_INVALID_HMAC);
            }

            log.postValue("[RECEIVED]" + "MSG_ACCEPT");
            return (UnpackMsgAccept) receivedMsg;
        }

        /**
         * Streaming Channel을 열고 MSG_REQ_SSIG를 기다린다
         *
         * @param channel target merger
         * @param log     log live data
         * @param error   error live data
         */
        private void standBy(ManagedChannel channel, MutableLiveData<String> log, MutableLiveData<Boolean> error) {
            GruutNetworkServiceGrpc.GruutNetworkServiceStub stub = GruutNetworkServiceGrpc.newStub(channel);
            StreamObserver<Identity> standBy = stub.openChannel(new StreamObserver<GrpcMsgReqSsig>() {
                @Override
                public void onNext(GrpcMsgReqSsig value) {
                    // Signature request from Merger
                    log.postValue("I've got MSG_REQ_SSIG!");
                    try {
                        sendSignature(channel, value, log);
                    } catch (ErrorMsgException e) {
                        log.postValue("[ERROR]" + e.getMessage());
                        Log.e(TAG, channel.toString() + "::[ERROR]" + e.getMessage());
                    } catch (AuthUtilException e) {
                        log.postValue(channel.toString() + "::[CRYPTO_ERROR]" + e.getMessage());
                        Log.e(TAG, "[CRYPTO_ERROR]" + e.getMessage());
                    }
                }

                @Override
                public void onError(Throwable t) {
                    if (channel.isShutdown()) {
                        Log.e(TAG, channel.toString() + "::shutDowned");
                    } else {
                        log.postValue("This Merger is DEAD... Now Dobby is free!");
                        Log.e(TAG, channel.toString() + "::ChannelClosed: " + t.getMessage());
                        error.postValue(true);
                    }
                }

                @Override
                public void onCompleted() {
                    log.postValue("GRPC stream onComplete()");
                    Log.e(TAG, channel.toString() + "::GRPC stream onComplete()");
                    error.postValue(true);
                }
            });

            standBy.onNext(Identity.newBuilder().setSender(ByteString.copyFrom(sId.getBytes())).build());
            log.postValue("Streaming channel opened...standby for signature request");
            Log.d(TAG, channel.toString() + "::Streaming channel opened...standby for signature request");
        }

        /**
         * 서명 요청이 오면 검증한 후, 서명하여 Merger에게 전송한다.
         *
         * @param channel        target merger
         * @param grpcMsgReqSsig received MSG_REQ_SSIG
         * @param log            log live data
         */
        private void sendSignature(ManagedChannel channel, GrpcMsgReqSsig grpcMsgReqSsig, MutableLiveData<String> log) {
            UnpackMsgRequestSignature msgRequestSignature
                    = new UnpackMsgRequestSignature(grpcMsgReqSsig.getMessage().toByteArray());

            if (!msgRequestSignature.isMacValid()) {
                throw new ErrorMsgException(ErrorMsgException.MsgErr.MSG_HEADER_NOT_MATCHED);
            }

            if (!msgRequestSignature.isMacValid()) {
                throw new ErrorMsgException(ErrorMsgException.MsgErr.MSG_INVALID_HMAC);
            }

            if (!AuthGeneralUtil.isBlockInTime(msgRequestSignature.getTime())) {
                throw new ErrorMsgException(ErrorMsgException.MsgErr.MSG_EXPIRED);
            }

            String time = msgRequestSignature.getTime();
            String signature;
            try {
                SignedBlock block = new SignedBlock();
                block.setChainId(msgRequestSignature.getChainId());
                block.setBlockHeight(msgRequestSignature.getBlockHeight());
                blockDao.insertAll();

                signature = authCertUtil.generateSupportSignature(sId, time, msgRequestSignature.getmID(), GruutConfigs.localChainId,
                        msgRequestSignature.getBlockHeight(), msgRequestSignature.getTransaction());
                log.postValue("Signature generated!");
            } catch (Exception e) {
                throw new AuthUtilException(AuthUtilException.AuthErr.SIGNING_ERROR);
            }

            PackMsgSignature msgSignature = new PackMsgSignature(
                    sId,
                    time,
                    signature
            );
            msgSignature.setDestinationId(msgRequestSignature.getmID());

            log.postValue("[SEND]" + "MSG_SSIG");
            new GrpcTask(viewModel.get(), channel).execute(msgSignature);
        }
    }

    private static class GrpcTask extends AsyncTask<MsgPacker, Void, MsgUnpacker> {

        private long start;

        private final WeakReference<DashboardViewModel> viewModel;
        private final ManagedChannel channel;

        private GrpcTask(DashboardViewModel viewModel, ManagedChannel channel) {
            this.viewModel = new WeakReference<>(viewModel);
            this.channel = channel;
        }

        boolean isErrorMsg(byte[] originalMsg) {
            if (originalMsg != null && originalMsg.length > 3) {
                return originalMsg[2] == TypeMsg.MSG_ERROR.getType();
            }
            return false;
        }

        @Override
        protected MsgUnpacker doInBackground(MsgPacker... msgPackers) {
            MsgPacker msg = msgPackers[0];

            GruutNetworkServiceGrpc.GruutNetworkServiceBlockingStub stub = GruutNetworkServiceGrpc.newBlockingStub(channel);
            start = System.currentTimeMillis();

            try {
                switch (msg.getMessageType()) {
                    case MSG_JOIN:
                        GrpcMsgJoin grpcMsgJoin = GrpcMsgJoin.newBuilder()
                                .setMessage(ByteString.copyFrom(msg.convertToByteArr()))
                                .build();
                        GrpcMsgChallenge grpcMsgChallenge = stub.withDeadlineAfter(GruutConfigs.GRPC_TIMEOUT, TimeUnit.SECONDS).join(grpcMsgJoin);

                        if (isErrorMsg(grpcMsgChallenge.getMessage().toByteArray())) {
                            return new UnpackMsgError(grpcMsgChallenge.getMessage().toByteArray());
                        }
                        return new UnpackMsgChallenge(grpcMsgChallenge.getMessage().toByteArray());
                    case MSG_RESPONSE_1:
                        GrpcMsgResponse1 grpcMsgResponse1 = GrpcMsgResponse1.newBuilder()
                                .setMessage(ByteString.copyFrom(msg.convertToByteArr()))
                                .build();
                        GrpcMsgResponse2 grpcMsgResponse2 = stub.withDeadlineAfter(GruutConfigs.GRPC_TIMEOUT, TimeUnit.SECONDS).dhKeyEx(grpcMsgResponse1);

                        if (isErrorMsg(grpcMsgResponse2.getMessage().toByteArray())) {
                            return new UnpackMsgError(grpcMsgResponse2.getMessage().toByteArray());
                        }
                        return new UnpackMsgResponse2(grpcMsgResponse2.getMessage().toByteArray());
                    case MSG_SUCCESS:
                        GrpcMsgSuccess grpcMsgSuccess = GrpcMsgSuccess.newBuilder()
                                .setMessage(ByteString.copyFrom(msg.convertToByteArr()))
                                .build();
                        GrpcMsgAccept grpcMsgAccept = stub.withDeadlineAfter(GruutConfigs.GRPC_TIMEOUT, TimeUnit.SECONDS).keyExFinished(grpcMsgSuccess);
                        if (isErrorMsg(grpcMsgAccept.getMessage().toByteArray())) {
                            return new UnpackMsgError(grpcMsgAccept.getMessage().toByteArray());
                        }
                        return new UnpackMsgAccept(grpcMsgAccept.getMessage().toByteArray());
                    case MSG_SSIG:
                        GrpcMsgSsig grpcMsgSsig = GrpcMsgSsig.newBuilder()
                                .setMessage(ByteString.copyFrom(msg.convertToByteArr()))
                                .build();

                        //noinspection ResultOfMethodCallIgnored
                        stub.withDeadlineAfter(GruutConfigs.GRPC_TIMEOUT, TimeUnit.SECONDS).sigSend(grpcMsgSsig);
                        return null;
                    default:
                        return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(MsgUnpacker result) {
            if (viewModel.get() == null) {
                return;
            }

            Log.d(TAG, channel.toString() + "::Result: " + result);
            Log.d(TAG, channel.toString() + "::Response Time: " + (System.currentTimeMillis() - start));
        }
    }
}
