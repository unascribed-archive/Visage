package blue.lapis.lapitar2;

import java.util.UUID;

public class UUIDs {
	public static boolean isAlex(UUID uuid) {
		return (uuid.hashCode() & 1) == 1;
	}
}
