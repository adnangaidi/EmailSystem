package com.projects.adnan.EmailProject.util;


import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** XSS and header-injection guards (OWASP Java HTML Sanitizer + strict address parsing). */
public final class InputSanitizer {

    private static final PolicyFactory HTML_POLICY =
            Sanitizers.FORMATTING.and(Sanitizers.BLOCKS).and(Sanitizers.LINKS);

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "com", "scr", "msi", "js", "jar", "vbs", "ps1", "sh", "dll", "apk");

    private static final int MAX_RECIPIENTS = 50;

    private InputSanitizer() {}

    /** Removes CR/LF and other control characters (email header injection). */
    public static String cleanHeader(String value) {
        if (value == null) return "";
        return value.replaceAll("[\\p{Cntrl}\\u2028\\u2029]", " ").trim();
    }

    /** Parses "a@x.com, b@y.com" into validated plain addresses. */
    public static List<String> parseAddresses(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        List<String> out = new ArrayList<>();
        for (String part : cleanHeader(raw).split("[,;]")) {
            String candidate = part.trim();
            if (candidate.isEmpty()) continue;
            try {
                InternetAddress address = new InternetAddress(candidate, true);
                address.validate();
                out.add(address.getAddress());
            } catch (AddressException e) {
                throw new IllegalArgumentException("Invalid email address: " + candidate);
            }
        }
        if (out.size() > MAX_RECIPIENTS) {
            throw new IllegalArgumentException("Too many recipients (max " + MAX_RECIPIENTS + ").");
        }
        return out;
    }

    /** Sanitizes untrusted HTML (received emails) before it is rendered with th:utext. */
    public static String sanitizeHtml(String html) {
        return html == null ? "" : HTML_POLICY.sanitize(html);
    }

    /** Plain text -> safe HTML (escape, keep line breaks). */
    public static String plainToHtml(String text) {
        if (text == null) return "";
        return HtmlUtils.htmlEscape(text).replace("\r\n", "\n").replace("\n", "<br>");
    }

    public static String safeFilename(String name) {
        if (name == null || name.isBlank()) return "attachment";
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        base = base.replaceAll("[^\\w.\\- ]", "_").trim();
        if (base.isEmpty() || base.equals(".") || base.equals("..")) return "attachment";
        return base.length() > 150 ? base.substring(base.length() - 150) : base;
    }

    public static boolean isBlockedExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return false;
        return BLOCKED_EXTENSIONS.contains(filename.substring(dot + 1).toLowerCase(Locale.ROOT));
    }
}
