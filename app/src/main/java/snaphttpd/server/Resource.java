package snaphttpd.server;

import android.support.annotation.Nullable;

public interface Resource {
	@Nullable
    String send(String path);
}
