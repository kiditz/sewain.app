package app.sewain.media;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

interface MediaStorage {

	StoredMedia store(
			MultipartFile file,
			String objectKey
	);

	String getUrl(String objectKey);

	void delete(String objectKey);

	Resource read(String objectKey);

	MediaReplacement replace(MultipartFile file, String objectKey);
}
