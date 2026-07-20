package yaroslavalexashkin.cheburping;

public class Pinger {
    boolean successful = false;
    boolean unreachable = false;
    boolean timeout = false;
    boolean otherErr = false;
    String ping = "";

    Pinger(String ip, int timeoutSeconds) {
        try {
            long t1 = System.currentTimeMillis();
            Process p = Runtime.getRuntime().exec("ping -c 1 -W " + timeoutSeconds + " " + ip);
            p.waitFor();
            long t2 = System.currentTimeMillis();
            byte[] pingOutput = new byte[p.getInputStream().available()];
            if (p.getInputStream().read(pingOutput) == pingOutput.length) {
                String str = new String(pingOutput);
                String[] strs = str.split("\n");

                boolean found = false;
                for (String s : strs) {
                    if (s.contains("rom " + ip + ": icmp_seq=1 ")) {
                        found = true;
                        if (s.contains("Unreachable"))
                            unreachable = true;
                        else if (s.contains(" time="))
                            ping = s.split(" time=")[1];
                    }
                }
                if (!found) {
                    if (t2 - t1 >= timeoutSeconds * 1000L) {
                        timeout = true;
                    } else
                        otherErr = true;
                }
            } else
                otherErr = true;
            p.destroy();
            successful = true;
        } catch (Exception ignored) {}
    }
}
