import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
    //            Intent n =  context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
     //            n.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
     //    Intent.FLAG_ACTIVITY_CLEAR_TASK);
     //            context.startActivity(n);
    
            Intent myIntent = new Intent(context, MainActivity.class);
            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(myIntent);
        }
    }
}
