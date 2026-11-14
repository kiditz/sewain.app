package app.sewain.media;

import org.junit.jupiter.api.Test;
import java.net.UnknownHostException;
import static org.junit.jupiter.api.Assertions.*;

class RemoteImageImporterTest {
    @Test void blocksSpecialUseAndTransitionRangesAtEveryBoundary() {
        for(var host:new String[]{"0.1.2.3","224.0.0.1","255.255.255.255","100.64.0.1","100.127.255.254",
                "169.254.1.1","172.16.0.1","172.31.255.254","192.168.1.1","192.0.2.1",
                "198.18.0.1","198.19.255.254","198.51.100.1","203.0.113.1","::","fd00::1","ff02::1",
                "2001:db8::1","2001:19ff::1","2002:0808:0808::1"})
            assertThrows(UnknownHostException.class,()->RemoteImageImporter.publicAddresses(host),host);
    }
    @Test void publicLiteralAddressesOutsideBlockedRangesRemainAllowed() throws Exception {
        for(var host:new String[]{"8.8.8.8","1.1.1.1","100.63.255.254","100.128.0.1","169.253.1.1",
                "172.15.0.1","172.32.0.1","192.1.1.1","198.17.0.1","198.20.0.1","198.52.0.1","203.1.1.1",
                "2606:4700:4700::1111","2001:4860:4860::8888","2001:2000::1"})
            assertTrue(RemoteImageImporter.publicAddresses(host).length>0,host);
    }
    @Test void rejectsInternalMetadataAndTransitionAddressesWithoutNetworkRequests() {
        for (var host : new String[]{"127.0.0.1","10.0.0.1","172.16.0.1","192.168.0.1","169.254.169.254","100.64.0.1","0.0.0.0","::1","fc00::1","fe80::1","2002:7f00:1::"})
            assertThrows(UnknownHostException.class,()->RemoteImageImporter.publicAddresses(host),host);
    }
}
