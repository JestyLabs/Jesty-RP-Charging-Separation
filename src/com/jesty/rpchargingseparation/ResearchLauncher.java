package com.jesty.rpchargingseparation;

/** Staged scripts contain no privileged output redirections or mutable runtime files. */
final class ResearchLauncher {
    static String build(String apk, String entry, String arguments) {
        if (apk == null || !apk.matches("/data/local/tmp/(?:[A-Za-z0-9_-][A-Za-z0-9._-]*/)*[A-Za-z0-9_-][A-Za-z0-9._-]*\\.apk"))
            throw new IllegalArgumentException("explicit staged research APK required");
        return "#!/system/bin/sh\n"
                + "export CLASSPATH='" + apk + "'\n"
                + "case \"$1\" in\n"
                + "status|stop|clean) exec app_process / " + entry + " \"$1\" ;;\n"
                + "'') ;;\n*) exit 2 ;;\nesac\n"
                + "nohup app_process / " + entry + " run " + arguments
                + " </dev/null >/dev/null 2>&1 &\n"
                + "P=$!\nsleep 1\nkill -0 \"$P\" 2>/dev/null || exit 21\n"
                + "echo OK\n";
    }
}
