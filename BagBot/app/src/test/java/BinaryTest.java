import com.flomio.smartcartlib.binary.CheckSum;
import com.flomio.smartcartlib.binary.MessageDecoder;
import com.flomio.smartcartlib.binary.MessageEncoder;
import com.flomio.smartcartlib.binary.Message;
import com.flomio.smartcartlib.util.Hex;
import junit.framework.TestCase;

import java.util.ArrayList;

public class BinaryTest extends TestCase {
    public void testXOR() {
        byte [] data = Hex.decode("0344616E08313230393130303404C0A80106");
        int xor = CheckSum.xor(data, 0, data.length);
        assertEquals(36, xor);
    }
    public void testXOR2() {
        byte [] data = Hex.decode("0000DEADBEEF");
        assertEquals(6, data.length);
        int xor = CheckSum.xor(data, 0, data.length);
        assertEquals(34, xor);
    }
    public void testXOR3() {
        byte [] data = Hex.decode("0000DEADBEAF");
        assertEquals(6, data.length);
        int xor = CheckSum.xor(data, 0, data.length);
        assertEquals(98, xor);
    }

    public void testChunkEncoding() {
        byte[] ip = new byte[]{127, 0, 0, 1};
        ArrayList<byte[]> bytes = MessageEncoder.encodeMessage(0, 0, ip);
        MessageDecoder encoder = new MessageDecoder();
        Message push = encoder.push(bytes.get(0));
        assertEquals("7F000001", Hex.encode(push.data));
    }
    public void testChunkEncoding2() {
        MessageDecoder encoder = new MessageDecoder();
        Message push = encoder.push(Hex.decode("0001000000210943617361426168696109436173"));

    }
}
