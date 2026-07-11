package io.github.hypercopy.clipboard.privileged;

import android.os.IBinder;

public final class MiuiXmsfFirewallBinderCommand {
    private static final int FIREWALL_CHAIN_OEM_DENY_3 = 9;
    private static final int FIREWALL_RULE_DEFAULT = 0;
    private static final int FIREWALL_RULE_DENY = 2;

    private MiuiXmsfFirewallBinderCommand() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: MiuiXmsfFirewallBinderCommand <uid> <block|restore|session>");
        }

        int uid = Integer.parseInt(args[0]);
        Object connectivity = connectivityManager();

        if ("session".equals(args[1])) {
            setUidNetworkBlocked(connectivity, uid, true);
            System.out.println("READY");
            System.out.flush();
            System.in.read();
            setUidNetworkBlocked(connectivity, uid, false);
            return;
        }

        boolean block = "block".equals(args[1]);
        setUidNetworkBlocked(connectivity, uid, block);
    }

    private static void setUidNetworkBlocked(Object connectivity, int uid, boolean block) throws Exception {
        connectivity.getClass()
                .getMethod("setFirewallChainEnabled", int.class, boolean.class)
                .invoke(connectivity, FIREWALL_CHAIN_OEM_DENY_3, true);

        connectivity.getClass()
                .getMethod("setUidFirewallRule", int.class, int.class, int.class)
                .invoke(
                        connectivity,
                        FIREWALL_CHAIN_OEM_DENY_3,
                        uid,
                        block ? FIREWALL_RULE_DENY : FIREWALL_RULE_DEFAULT
                );
    }

    private static Object connectivityManager() throws Exception {
        IBinder binder = (IBinder) Class.forName("android.os.ServiceManager")
                .getMethod("getService", String.class)
                .invoke(null, "connectivity");
        if (binder == null) throw new IllegalStateException("connectivity service not found");

        return Class.forName("android.net.IConnectivityManager$Stub")
                .getMethod("asInterface", IBinder.class)
                .invoke(null, binder);
    }
}
