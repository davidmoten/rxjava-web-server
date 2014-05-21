package com.github.davidmoten.rx.web;

/**
 * <p>
 * Knuth-Morris-Pratt Algorithm for Pattern Matching.
 * </p>
 * 
 * <p>
 * From <a href=
 * "http://stackoverflow.com/questions/1507780/searching-for-a-sequence-of-bytes-in-a-binary-file-with-java"
 * >stackoverflow</a>.
 * </p>
 */
class ByteMatcher {

	private final byte[] pattern;
	private final int[] failure;

	ByteMatcher(byte[] pattern) {
		this.pattern = pattern;
		failure = computeFailure(pattern);
	}

	public int patternLength() {
		return pattern.length;
	}

	/**
	 * Returns -1 if the pattern does not exist in the byte array otherwise
	 * returns the index of the start of the pattern.
	 */
	public int search(byte[] data) {

		int j = 0;
		if (data.length == 0)
			return -1;

		for (int i = 0; i < data.length; i++) {
			while (j > 0 && pattern[j] != data[i]) {
				j = failure[j - 1];
			}
			if (pattern[j] == data[i]) {
				j++;
			}
			if (j == pattern.length) {
				return i - pattern.length + 1;
			}
		}
		return -1;
	}

	/**
	 * Computes the failure function using a boot-strapping process, where the
	 * pattern is matched against itself.
	 */
	private int[] computeFailure(byte[] pattern) {
		int[] failure = new int[pattern.length];

		int j = 0;
		for (int i = 1; i < pattern.length; i++) {
			while (j > 0 && pattern[j] != pattern[i]) {
				j = failure[j - 1];
			}
			if (pattern[j] == pattern[i]) {
				j++;
			}
			failure[i] = j;
		}

		return failure;
	}
}
