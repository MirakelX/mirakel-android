package de.azapps.mirakel;

import java.io.IOException;
import java.net.URISyntaxException;

public interface DownloadCommand {
	public String downloadUrl(String myurl) throws IOException,URISyntaxException;

}
