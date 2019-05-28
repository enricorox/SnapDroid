package snaphttpd.server;

import android.support.annotation.Nullable;

// Maps string to another string
public interface Resource {
	// Given the path, return a string
	@Nullable
    String send(String path);
}
