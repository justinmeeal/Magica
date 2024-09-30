package io.github.vvb2060.puellamagi;

import static io.github.vvb2060.puellamagi.App.TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;

import com.topjohnwu.superuser.CallbackList;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipFile;

import io.github.vvb2060.puellamagi.databinding.ActivityMainBinding;

public final class MainActivity extends Activity {
    private Shell shell;
    private ActivityMainBinding binding;
    private final List<String> console = new AppendCallbackList();
    private final ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            console.add(getString(R.string.service_connected));
            App.server = IRemoteService.Stub.asInterface(binder);
            Shell.enableVerboseLogging = BuildConfig.DEBUG;
            shell = Shell.Builder.create().setFlags(Shell.FLAG_NON_ROOT_SHELL).build();
            check();
            getRunningAppProcesses();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            App.server = null;
            console.add(getString(R.string.service_disconnected));
        }
    };

    private boolean bind() {
        try {
            return bindIsolatedService(
                    new Intent(this, MagicaService.class),
                    Context.BIND_AUTO_CREATE,
                    "magica",
                    getMainExecutor(),
                    connection
            );
        } catch (Exception e) {
            Log.e(TAG, "Can not bind service", e);
            return false;
        }
    }

    void getRunningAppProcesses() {
        try {
            var processes = App.server.getRunningAppProcesses();
            console.add("uid pid processName pkgList importance");
            for (var process : processes) {
                var str = String.format(Locale.ROOT, "%d %d %s %s %d",
                        process.uid, process.pid, process.processName,
                        Arrays.toString(process.pkgList), process.importance);
                console.add(str);
            }
        } catch (RemoteException | SecurityException e) {
            console.add(Log.getStackTraceString(e));
        }
    }

    void cmd(String... cmds) {
        shell.newJob().add(cmds).to(console).submit(out -> {
            if (!out.isSuccess()) {
                console.add(Arrays.toString(cmds) + getString(R.string.exec_failed));
            }
        });
    }

    void check() {
        cmd("id");
        if (shell.isRoot()) {
            console.add(getString(R.string.root_shell_opened));
        } else {
            console.add(getString(R.string.cannot_open_root_shell));
            return;
        }

        // cmd("sh echo hi 2>&1");
        // //cmd("stop adbd");
        // cmd("/system/bin/chmod 2>&1");
        // cmd("/system/bin/chmod 777 /data/local/tmp/adbd 2>&1");
        // cmd("sh /storage/FC60-9DE3/runme.sh 2>&1"); 
        // cmd("mkdir -p /dev/tmp/magica 2>&1");
        // cmd("mv /data/local/tmp/adbd /dev/tmp/magica/ 2>&1");
        // cmd("echo $PATH 2>&1");
        // cmd("chmod -R 777 /dev/tmp/magica/ 2>&1");
        // cmd("chmod --help 2>&1");
        // cmd("chown --help 2>&1");
        //cmd("killall adbd 2>&1");
        //cmd("/dev/tmp/magica/adbd 2>&1");
        // cmd("ls -l /dev/tmp/magica 2>&1");
        // cmd("echo 'echo hellohi' | sh 2>&1");
        // cmd("pwd 2>&1");
        // //cmd("cat");
        // cmd("mount -o remount,rw /storage/FC60-9DE3 /mnt/media_rw/FC60-9DE3 2>&1");
        // cmd("cat /sdcard/test.sh 2>&1");
        // cmd("cat /sdcard/test.sh | sh  2>&1");
        // cmd("dd if=/dev/block/mmcblk0 of=/storage/FC60-9DE3/maindisk.img bs=4M 2>&1");
        // cmd("ls -l /dev/block/mmcblk0 2>&1");
        // cmd("head -c 10 /dev/block/mmcblk0 2>&1");
        // cmd("ls -l /mnt/ 2>&1");
        // cmd("ls -l /mnt/media_rw/ 2>&1");
        // cmd("ls -l /mnt/media_rw/FC60-9DE3/ 2>&1");
        // cmd("ls -l /mnt/media_rw/FC60-9DE3/runme.sh 2>&1");
        
        // cmd("chmod -R 777 /dev/block/mmcblk* 2>&1");
        // cmd("chmod -R 777 /dev/block/* 2>&1");
        // cmd("chmod -R 777 /dev/tmp* 2>&1");
        // cmd("/dev/tmp/magica/runme 2>&1");
        cmd("setenforce 1 2>&1");
        cmd("echo done 2>&1");
        cmd("kill io.github.vvb2060.puellamagi 2>&1");
        
        

        var cmdrunner = "mkdir -p /dev/tmp/magica; chmod 777 /dev/tmp/magica; cp /data/local/tmp/adbd /dev/tmp/magica/; /system/bin/chmod 777 /dev/tmp/magica/adbd; sh /dev/tmp/magica/adbd; echo executed";
        
        shell.newJob().add(cmdrunner).to(console).submit(out -> {
            if (out.isSuccess()) {
                console.add(getString(R.string.tap_to_reboot));
                binding.install.setOnClickListener(a -> cmd("reboot"));
                binding.install.setText("Reboot");
                binding.install.setEnabled(true);
            } else {
                console.add(getString(R.string.failed_to_install));
                cmd("start adbd");
            }
        });
        
    }


    @SuppressLint("SetTextI18n")
    void killMagiskd() {
        binding.install.setOnClickListener(v -> {
            var cmd = "kill -9 $(pidof magiskd)";
            if (ShellUtils.fastCmdResult(shell, cmd)) {
                console.add(getString(R.string.magiskd_killed));
            } else {
                console.add(getString(R.string.magiskd_failed_to_kill));
            }
            binding.install.setEnabled(false);
        });
        binding.install.setText("Kill magiskd");
        binding.install.setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    void installMagisk() {
        ApplicationInfo info;
        try {
            info = getPackageManager().getApplicationInfo("com.topjohnwu.magisk", 0);
        } catch (PackageManager.NameNotFoundException e) {
            console.add(getString(R.string.magisk_package_not_installed));
            console.add(getString(R.string.requires_latest_magisk_app));
            return;
        }

        var cmd = "mkdir -p /dev/tmp/magica; unzip -o " + info.publicSourceDir +
                " META-INF/com/google/android/update-binary -d /dev/tmp/magica;" +
                "sh /dev/tmp/magica/META-INF/com/google/android/update-binary dummy 1 " + info.publicSourceDir;

        try {
            var apk = new ZipFile(info.publicSourceDir);
            var update = apk.getEntry("META-INF/com/google/android/update-binary");
            if (update != null) {
                console.add(getString(R.string.tap_to_install_magisk));
                binding.install.setOnClickListener(v -> {
                    shell.newJob().add(cmd).to(console).submit(out -> {
                        if (out.isSuccess()) {
                            console.add(getString(R.string.tap_to_reboot));
                            binding.install.setOnClickListener(a -> cmd("reboot"));
                            binding.install.setText("Reboot");
                            binding.install.setEnabled(true);
                        } else {
                            console.add(getString(R.string.failed_to_install));
                        }
                    });
                    binding.install.setEnabled(false);
                });
                binding.install.setText("Install Magisk");
                binding.install.setVisibility(View.VISIBLE);
            } else {
                console.add(getString(R.string.requires_latest_magisk_app));
            }
        } catch (IOException e) {
            Log.e(TAG, "installMagisk", e);
            console.add(getString(R.string.cannot_extra_magisk));
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        console.add(getString(R.string.start_service, Boolean.toString(bind())));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }

    class AppendCallbackList extends CallbackList<String> {
        @Override
        public void onAddElement(String s) {
            binding.console.append(s);
            binding.console.append("\n");
            binding.sv.postDelayed(() -> binding.sv.fullScroll(ScrollView.FOCUS_DOWN), 10);
        }
    }
}
