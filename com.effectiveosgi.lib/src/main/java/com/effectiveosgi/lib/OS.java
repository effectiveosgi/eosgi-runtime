package com.effectiveosgi.lib;

import java.util.stream.Stream;

public enum OS {

	Windows(OSType.Windows),
	MacOS(OSType.UNIX, "Mac"),
	AIX(OSType.UNIX),
	HP_UX(OSType.UNIX, "HP-UX"),
	OS_400(OSType.Other, "OS/400"),
	Irix(OSType.UNIX),
	Linux(OSType.UNIX, "Linux", "LINUX"),
	FreeBSD(OSType.UNIX),
	OpenBSD(OSType.UNIX),
	NetBSD(OSType.UNIX),
	OS2(OSType.Other, "OS/2"),
	Solaris(OSType.UNIX),
	SunOS(OSType.UNIX);

	public static final OS CURRENT = get();

	private final OSType type;
	private final String[] prefixes;

	OS(OSType type, String... prefixes) {
		this.type = type;
		this.prefixes = prefixes == null || prefixes.length == 0
			? new String[] {this.name()}
			: prefixes;
	}

	public String getName() {
		return prefixes[0];
	}

	public OSType getType() {
		return type;
	}

	private boolean match(String osName) {
		return Stream.of(prefixes).anyMatch(p -> osName.startsWith(p));
	}

	public static OS get() {
		final String osName = System.getProperty("os.name");
		return Stream.of(OS.values())
			.filter(os -> os.match(osName))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unrecognized OS name: " + osName));
	}

}
