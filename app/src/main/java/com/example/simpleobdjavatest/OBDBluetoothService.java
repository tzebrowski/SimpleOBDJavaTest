package com.example.simpleobdjavatest;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.obd.metrics.api.Workflow;
import org.obd.metrics.api.model.AdaptiveTimeoutPolicy;
import org.obd.metrics.api.model.Adjustments;
import org.obd.metrics.api.model.BatchPolicy;
import org.obd.metrics.api.model.CachePolicy;
import org.obd.metrics.api.model.Init;
import org.obd.metrics.api.model.Init.Header;
import org.obd.metrics.api.model.Init.Protocol;
import org.obd.metrics.api.model.Pids;
import org.obd.metrics.api.model.ProducerPolicy;
import org.obd.metrics.api.model.Query;
import org.obd.metrics.command.group.DefaultCommandGroup;
import org.obd.metrics.diagnostic.RateType;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class OBDBluetoothService extends Service {

    public static final String ACTION_OBD_STATE = "com.example.OBD.ACTION_OBD_STATE";
    public static final String EXTRA_OBD_STATE = "obd_state";
    public static final String EXTRA_OBD_SPEED = "obd_speed";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            test();
        } catch (IOException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return flags;
    }

    public void test() throws IOException, InterruptedException, ExecutionException {
//        AdapterConnection connection = BluetoothConnection.connect(getDeviceByName("OBD"));
        var collector = new DataCollector();

        final Pids pids = Pids
                .builder()
                .resource(Objects.requireNonNull(Thread.currentThread().getContextClassLoader()).getResource("giulia_2.0_gme.json"))
                .resource(Objects.requireNonNull(Thread.currentThread().getContextClassLoader()).getResource("extra.json"))
                .resource(Objects.requireNonNull(Thread.currentThread().getContextClassLoader()).getResource("mode01.json"))
                .build();

        int commandFrequency = 6;
        var workflow = Workflow.instance().pids(pids).observer(collector).initialize();

        var query = Query.builder().pid(7005l).pid(7006l).pid(7007l).pid(7008l).build();

        var optional = Adjustments
                .builder()
                .vehicleCapabilitiesReadingEnabled(Boolean.TRUE)
                .vehicleMetadataReadingEnabled(Boolean.TRUE)
                .adaptiveTimeoutPolicy(AdaptiveTimeoutPolicy.builder().enabled(Boolean.TRUE).checkInterval(5000).commandFrequency(commandFrequency).build())
                .producerPolicy(ProducerPolicy.builder().priorityQueueEnabled(Boolean.TRUE).build())
                .cachePolicy(CachePolicy.builder().resultCacheEnabled(Boolean.FALSE).build())
                .batchPolicy(
                        BatchPolicy.builder().responseLengthEnabled(Boolean.FALSE).enabled(Boolean.FALSE).build())
                .build();

        var init = Init.builder()
                .delayAfterInit(1000)
                .header(Header.builder().mode("22").header("DA10F1").build())
                .header(Header.builder().mode("01").header("DB33F1").build())
                .protocol(Protocol.CAN_29)
                .sequence(DefaultCommandGroup.INIT).build();

//        workflow.start(connection, query, init, optional);

        WorkflowFinalizer.finalizeAfter(workflow,25000);

        var registry = workflow.getPidRegistry();

        var intakePressure = registry.findBy(7005l);
        double ratePerSec = workflow.getDiagnostics().rate().findBy(RateType.MEAN, intakePressure).get().getValue();

        Assertions.assertThat(ratePerSec).isGreaterThanOrEqualTo(commandFrequency);
    }

    @SuppressLint("MissingPermission")
    private BluetoothDevice getDeviceByName(String name) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices != null && !pairedDevices.isEmpty()) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().equals(name)) {
                        return device;
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}