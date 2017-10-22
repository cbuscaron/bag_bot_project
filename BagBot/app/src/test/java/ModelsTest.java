import com.flomio.smartcartlib.api.model.EPC;
import com.flomio.smartcartlib.api.model.ProductsResponse;
import com.flomio.smartcartlib.json.GSON;
import com.flomio.smartcartlib.ws.model.FoundTagsEvent;
import com.flomio.smartcartlib.ws.model.Tag;
import com.google.gson.*;
import junit.framework.TestCase;
import org.junit.Test;


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ModelsTest extends TestCase {
    private Gson gson = GSON.Configured;

    @Test
    public void testEpcSerialization() throws Exception {
        String json = "{\"epc\":\"FFFF\"}";
        EPC epc = gson.fromJson(json, EPC.class);
        assertEquals("FFFF", epc.epc);
        assertEquals(json, gson.toJson(epc));
    }

    @Test
    public void testProductsResponseDeserialization() throws Exception {

        ProductsResponse resp = gson.fromJson("{\n" +
                "  \"items\": [{\n" +
                "    \"sku\" : 5,\n" +
                "    \"price\": 2.5,\n" +
                "    \"image\" : \"aXQgcmVhbGx5IHdvcmtz\"\n" +
                "  }]\n" +
                "}", ProductsResponse.class);
        String actual = new String(resp.products.get(0).image);
        assertEquals("it really works", actual);
    }

    @Test
    public void testFromJson() throws Exception {
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse("{\"eventName\" : \"foundTags\", " +
                "\"tags\" : [\n" +
                "  {\"epc\": \"E28011606000020528F95005\"},\n" +
                "  {\"epc\": \"E28011606000020528F95006\"},\n" +
                "  {\"epc\": \"E28011606000020528F95007\"}\n" +
                "]}");
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String expectedConcat =
                "E28011606000020528F95005" +
                "E28011606000020528F95006" +
                "E28011606000020528F95007";
        StringBuilder accum = new StringBuilder();
        if (jsonObject.get("eventName").getAsString().equals("foundTags")) {
            FoundTagsEvent event = gson.fromJson(jsonObject, FoundTagsEvent.class);
            for (Tag tag : event.tags) {
                accum.append(tag.epc);
            }
        }
        assertEquals(expectedConcat, accum.toString());
    }


}