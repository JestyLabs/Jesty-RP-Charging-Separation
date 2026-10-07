package com.jesty.rpchargingseparation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Read-only inspection of Retroid/AYN-style vendor process-protection settings.
 *
 * This class deliberately does not mutate vendor state. The first investigation
 * stage only needs enough evidence to correlate Retroid's "Whitelist Application"
 * UI with the setting(s) it changes on real firmware.
 */
final class RetroidProcessProtection {
    static final String SYSTEM_SETTINGS_COMMAND = "settings list system";
    static final String KNOWN_WHITELIST_KEY = "app_whiteList";
    private static final int MAX_RELEVANT_SETTINGS = 40;
    private static final int MAX_VALUE_CHARS = 1200;

    private RetroidProcessProtection() {}

    interface Shell {
        String exec(String command) throws Exception;
    }

    enum KnownWhitelistMembership {
        PRESENT,
        ABSENT,
        UNKNOWN
    }

    static final class Setting {
        final String key;
        final String value;

        Setting(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    static final class Snapshot {
        final boolean readSucceeded;
        final KnownWhitelistMembership knownWhitelistMembership;
        final boolean knownWhitelistKeyPresent;
        final List<Setting> relevantSettings;
        final String error;

        Snapshot(boolean readSucceeded,
                 KnownWhitelistMembership knownWhitelistMembership,
                 boolean knownWhitelistKeyPresent,
                 List<Setting> relevantSettings,
                 String error) {
            this.readSucceeded = readSucceeded;
            this.knownWhitelistMembership = knownWhitelistMembership;
            this.knownWhitelistKeyPresent = knownWhitelistKeyPresent;
            this.relevantSettings = relevantSettings;
            this.error = error;
        }

        void appendDiagnostics(StringBuilder out) {
            out.append("Vendor process protection probe (read-only):\n");
            if (!readSucceeded) {
                out.append("PServer/settings read: failed");
                if (error != null && !error.isEmpty()) {
                    out.append(" - ").append(error);
                }
                out.append('\n');
                out.append("Retroid app_whiteList candidate: unknown\n");
                return;
            }

            out.append("PServer/settings read: available\n");
            if (!knownWhitelistKeyPresent) {
                out.append("Retroid app_whiteList candidate: not present in system settings\n");
            } else if (knownWhitelistMembership == KnownWhitelistMembership.PRESENT) {
                out.append("Retroid app_whiteList candidate: package present\n");
            } else {
                out.append("Retroid app_whiteList candidate: package absent\n");
            }

            out.append("Relevant system settings:\n");
            if (relevantSettings.isEmpty()) {
                out.append("(none)\n");
                return;
            }
            int count = Math.min(MAX_RELEVANT_SETTINGS, relevantSettings.size());
            for (int i = 0; i < count; i++) {
                Setting setting = relevantSettings.get(i);
                out.append(setting.key).append('=')
                        .append(clip(setting.value, MAX_VALUE_CHARS))
                        .append('\n');
            }
            if (relevantSettings.size() > count) {
                out.append("(...").append(relevantSettings.size() - count)
                        .append(" more relevant settings omitted)\n");
            }
        }
    }

    static Snapshot inspect(Shell shell, String packageName) {
        try {
            String raw = shell.exec(SYSTEM_SETTINGS_COMMAND);
            List<Setting> relevant = parseRelevantSettings(raw, packageName);
            Setting known = findKey(raw, KNOWN_WHITELIST_KEY);
            KnownWhitelistMembership membership = known == null
                    ? KnownWhitelistMembership.UNKNOWN
                    : containsExactCsvToken(known.value, packageName)
                    ? KnownWhitelistMembership.PRESENT
                    : KnownWhitelistMembership.ABSENT;
            return new Snapshot(true, membership, known != null,
                    Collections.unmodifiableList(relevant), null);
        } catch (Throwable error) {
            String message = error.getClass().getSimpleName();
            if (error.getMessage() != null && !error.getMessage().trim().isEmpty()) {
                message += ": " + error.getMessage().trim();
            }
            return new Snapshot(false, KnownWhitelistMembership.UNKNOWN, false,
                    Collections.emptyList(), message);
        }
    }

    static List<Setting> parseRelevantSettings(String raw, String packageName) {
        List<Setting> result = new ArrayList<>();
        for (Setting setting : parse(raw)) {
            if (isCandidateKey(setting.key) || valueContainsPackage(setting.value, packageName)) {
                result.add(setting);
            }
        }
        result.sort(Comparator.comparing(setting -> setting.key));
        return result;
    }

    static boolean containsExactCsvToken(String value, String packageName) {
        if (value == null || packageName == null || packageName.isEmpty()) return false;
        for (String token : value.split(",")) {
            if (packageName.equals(token.trim())) return true;
        }
        return false;
    }

    static boolean valueContainsPackage(String value, String packageName) {
        if (value == null || packageName == null || packageName.isEmpty()) return false;
        int from = 0;
        while (from <= value.length() - packageName.length()) {
            int at = value.indexOf(packageName, from);
            if (at < 0) return false;
            int before = at - 1;
            int after = at + packageName.length();
            boolean leftBoundary = before < 0 || !isPackageChar(value.charAt(before));
            boolean rightBoundary = after >= value.length() || !isPackageChar(value.charAt(after));
            if (leftBoundary && rightBoundary) return true;
            from = at + 1;
        }
        return false;
    }

    private static List<Setting> parse(String raw) {
        List<Setting> result = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return result;
        for (String line : raw.split("\\r?\\n")) {
            int separator = line.indexOf('=');
            if (separator <= 0) continue;
            String key = line.substring(0, separator).trim();
            if (key.isEmpty()) continue;
            result.add(new Setting(key, line.substring(separator + 1).trim()));
        }
        return result;
    }

    private static Setting findKey(String raw, String wantedKey) {
        for (Setting setting : parse(raw)) {
            if (wantedKey.equals(setting.key)) return setting;
        }
        return null;
    }

    private static boolean isCandidateKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return KNOWN_WHITELIST_KEY.toLowerCase(Locale.ROOT).equals(lower)
                || lower.contains("whitelist")
                || lower.contains("white_list")
                || lower.contains("white-list")
                || lower.contains("clean")
                || lower.contains("standby")
                || lower.contains("ignore")
                || lower.contains("process");
    }

    private static boolean isPackageChar(char value) {
        return (value >= 'a' && value <= 'z')
                || (value >= 'A' && value <= 'Z')
                || (value >= '0' && value <= '9')
                || value == '_' || value == '.';
    }

    private static String clip(String value, int maxChars) {
        if (value == null) return "";
        if (value.length() <= maxChars) return value;
        return value.substring(0, maxChars) + "...[truncated]";
    }
}
