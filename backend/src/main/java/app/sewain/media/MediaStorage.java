package app.sewain.media;

import org.springframework.web.multipart.MultipartFile;

interface MediaStorage {

	StoredMedia store(
			MultipartFile file,
			String objectKey
	);

	String getUrl(String objectKey);

	void delete(String objectKey);
}