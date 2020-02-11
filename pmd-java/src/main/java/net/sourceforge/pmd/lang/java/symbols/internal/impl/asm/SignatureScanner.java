/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.symbols.internal.impl.asm;

import org.apache.commons.lang3.StringUtils;

/**
 * Base class to scan a type signature.
 */
class SignatureScanner {

    protected final char[] chars;
    protected final int start;
    protected final int end;


    SignatureScanner(String descriptor) {
        if (descriptor == null || descriptor.isEmpty()) {
            throw new IllegalArgumentException("Type descriptor \"" + descriptor + "\" is empty or null");
        }

        this.chars = new char[descriptor.length() + 1]; // Add one last entry (\0) to not overflow on EOI
        this.start = 0;
        this.end = descriptor.length();
        descriptor.getChars(start, end, chars, 0);
    }

    SignatureScanner(char[] chars, int start, int end) {
        assert chars != null;
        assert start <= end && start >= 0 && start < chars.length
            : "Invalid range " + start + ".." + end + " in 0.." + (chars.length + 1);
        this.chars = chars;
        this.start = start;
        this.end = end;
    }


    public char charAt(int off) {
        return chars[off];
    }

    public void dumpChars(int start, int end, StringBuilder builder) {
        builder.append(chars, start, end - start);
    }

    public int consumeChar(int start, char l, String s) {
        if (chars[start] != l) {
            throw expected(s, start);
        }
        return start + 1;
    }

    public int consumeChar(int start, char l) {
        return consumeChar(start, l, "" + l);
    }

    public int nextIndexOf(final int start, char stop) {
        int cur = start;
        while (cur < end && charAt(cur) != stop) {
            cur++;
        }
        return cur;
    }

    public int nextIndexOfAny(final int start, char stop, char stop2) {
        int cur = start;
        while (cur < end) {
            char c = charAt(cur);
            if (c == stop || c == stop2) {
                break;
            }
            cur++;
        }
        return cur;
    }


    public RuntimeException expected(String expectedWhat, int pos) {
        final String indent = "    ";
        String sb = "Expected " + expectedWhat + ":\n"
            + indent + bufferToString() + "\n"
            + indent + StringUtils.repeat(' ', pos - start) + '^' + "\n";
        return new IllegalArgumentException(sb);
    }

    public void expectEoI(int e) {
        consumeChar(e, (char) 0, "end of input");
    }

    public String bufferToString() {
        return bufferToString(start, end);
    }

    public String bufferToString(int start, int end) {
        if (start == end) {
            return "";
        }
        return new String(chars, start, end - start);
    }

    @Override
    public String toString() {
        return "TypeBuilder{sig=" + bufferToString() + '}';
    }
}
