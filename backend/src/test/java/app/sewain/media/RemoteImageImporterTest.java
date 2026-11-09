package app.sewain.media;

import org.junit.jupiter.api.Test;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.*;

class RemoteImageImporterTest {
    @Test void rejectsInternalMetadataAndTransitionAddressesWithoutNetworkRequests() {
        for (var host : new String[]{"127.0.0.1","10.0.0.1","172.16.0.1","192.168.0.1","169.254.169.254","100.64.0.1","0.0.0.0","::1","fc00::1","fe80::1","2002:7f00:1::"})
            assertThrows(UnknownHostException.class,()->RemoteImageImporter.publicAddresses(host),host);
    }
}
